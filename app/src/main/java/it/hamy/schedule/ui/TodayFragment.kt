package it.hamy.schedule.ui

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.cardview.widget.CardView
import it.hamy.schedule.R
import it.hamy.schedule.databinding.FragmentTodayBinding
import it.hamy.schedule.model.ScheduleItem
import it.hamy.schedule.utils.Cache
import it.hamy.schedule.utils.ParseBellSchedule
import it.hamy.schedule.utils.ParseToday
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!
    private val scheduleItems = mutableListOf<ScheduleItem>()
    private var bellSchedule: it.hamy.schedule.model.BellSchedule? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("settings", 0)
        val group = prefs.getString("group", null)

        if (group == null) {
            Toast.makeText(requireContext(), "Группа не выбрана", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            Log.d("BELL_SCHEDULE", "🚀 [ФРАГМЕНТ] Начинаем загрузку расписания для группы: $group")

            bellSchedule = loadBellSchedule()
            Log.d("BELL_SCHEDULE", "🔔 [ФРАГМЕНТ] Расписание звонков загружено: $bellSchedule")

            if (isNetworkAvailable()) {
                loadTodaySchedule(group)
            } else {
                loadScheduleFromCache()
            }
        }
    }

    private suspend fun loadTodaySchedule(group: String) {
        withContext(Dispatchers.IO) {
            try {
                val fetchedSchedule = ParseToday.fetchTodaySchedule(group)
                withContext(Dispatchers.Main) {
                    scheduleItems.clear()
                    scheduleItems.addAll(fetchedSchedule)
                    saveScheduleToCache()
                    showSchedule()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка загрузки расписания", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private suspend fun loadBellSchedule(): it.hamy.schedule.model.BellSchedule? {
//        val cached = Cache.loadBellSchedule(requireContext())
//        if (cached != null) {
//            Log.d("BELL_SCHEDULE", "💾 [ЗВОНКИ] Используем кешированное расписание: $cached")
//            return cached
//        }

        if (!isNetworkAvailable()) {
            Log.w("BELL_SCHEDULE", "🌐 [ЗВОНКИ] Нет сети — не можем загрузить")
            return null
        }

        val fetched = ParseBellSchedule.fetchBellSchedule()
        if (fetched != null) {
            Cache.saveBellSchedule(requireContext(), fetched)
            Log.d("BELL_SCHEDULE", "✅ [ЗВОНКИ] Сохранили в кеш: $fetched")
        } else {
            Log.e("BELL_SCHEDULE", "❌ [ЗВОНКИ] Не удалось загрузить расписание звонков")
        }
        return fetched
    }

    private fun loadScheduleFromCache() {
        val cachedList = Cache.loadTodaySchedule(requireContext())
        if (cachedList != null) {
            scheduleItems.clear()
            scheduleItems.addAll(cachedList)
            showSchedule()
        } else {
            Toast.makeText(requireContext(), "Нет сохранённого расписания", Toast.LENGTH_SHORT).show()
        }
        binding.progressBar.visibility = View.GONE
    }

    private fun saveScheduleToCache() {
        Cache.saveTodaySchedule(requireContext(), scheduleItems)
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = activity?.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        return cm.activeNetworkInfo?.isConnected == true
    }

    private fun showSchedule() {
        binding.progressBar.visibility = View.GONE

        if (scheduleItems.isEmpty()) {
            binding.textView.text = "Расписание отсутствует"
            binding.textView.visibility = View.VISIBLE
            return
        } else {
            binding.textView.visibility = View.GONE
        }

        val container = binding.scheduleContainer
        container.removeAllViews()

        container.addView(TextView(requireContext()).apply {
            text = getGreeting()
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setPadding(26, 16, 26, 8)
        })

        val dateTitle = scheduleItems.firstOrNull()?.day ?: "Сегодня"
        container.addView(TextView(requireContext()).apply {
            text = dateTitle
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setPadding(26, 16, 26, 8)
        })

        var isShowingTimeToLesson = false
        val timeView = TextView(requireContext()).apply {
            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            textSize = 36f
            setTypeface(null, Typeface.BOLD)
            setPadding(26, 16, 26, 12)
        }

        timeView.setOnClickListener {
            isShowingTimeToLesson = !isShowingTimeToLesson
            timeView.text = if (isShowingTimeToLesson) getTimeToNextLesson() else getCurrentTime()
            timeView.textSize = if (isShowingTimeToLesson) 22f else 36f
        }

        container.addView(timeView)

        for (lesson in scheduleItems) {
            Log.d("BELL_SCHEDULE", "📄 [РАСПИСАНИЕ] Обрабатываем урок: '${lesson.subject}', номер: '${lesson.time}', день: '${lesson.actualDayOfWeek}'")

            val card = LayoutInflater.from(requireContext()).inflate(R.layout.lesson_item, null) as CardView

            card.findViewById<TextView>(R.id.lessonNumber).text = lesson.time
            card.findViewById<TextView>(R.id.lessonSubject).text = lesson.subject
            card.findViewById<TextView>(R.id.lessonTeacher).text = lesson.teacher
            card.findViewById<TextView>(R.id.lessonRoom).text = if (lesson.room.isNotBlank()) "Кабинет: ${lesson.room}" else "Дистант"

            val lessonTime = getLessonTime(lesson.time, lesson.actualDayOfWeek)
            card.findViewById<TextView>(R.id.lessonTime).text = lessonTime

            Log.d("BELL_SCHEDULE", "⏱️ [РАСПИСАНИЕ] Для урока '${lesson.subject}' получено время: '$lessonTime'")

            val progressBar = card.findViewById<ProgressBar>(R.id.lessonProgress)
            setupProgressBar(progressBar, lessonTime)

            card.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(16, 16, 16, 16) }

            container.addView(card)
        }
    }

    private fun getLessonTime(lessonNumber: String, dayOfWeek: String?): String {
        Log.d("BELL_SCHEDULE", "🔍 [ПОИСК ВРЕМЕНИ] Ищем пару номер '$lessonNumber' для дня '$dayOfWeek'")

        if (dayOfWeek == null) {
            Log.w("BELL_SCHEDULE", "⚠️ [ПОИСК ВРЕМЕНИ] dayOfWeek = null")
            return "Время не указано"
        }

        if (bellSchedule == null) {
            Log.w("BELL_SCHEDULE", "⚠️ [ПОИСК ВРЕМЕНИ] bellSchedule = null")
            return "Время не указано"
        }

        val foundTime = when (dayOfWeek) {
            "понедельник" -> {
                Log.d("BELL_SCHEDULE", "📅 [ПОИСК ВРЕМЕНИ] Ищем в bellSchedule.monday: ${bellSchedule!!.monday}")
                bellSchedule!!.monday.find { it.number == lessonNumber }?.time
            }
            "вторник" -> {
                Log.d("BELL_SCHEDULE", "📅 [ПОИСК ВРЕМЕНИ] Ищем в bellSchedule.tuesday: ${bellSchedule!!.tuesday}")
                bellSchedule!!.tuesday.find { it.number == lessonNumber }?.time
            }
            "среда" -> {
                Log.d("BELL_SCHEDULE", "📅 [ПОИСК ВРЕМЕНИ] Ищем в bellSchedule.wednesday: ${bellSchedule!!.wednesday}")
                bellSchedule!!.wednesday.find { it.number == lessonNumber }?.time
            }
            "четверг" -> {
                Log.d("BELL_SCHEDULE", "📅 [ПОИСК ВРЕМЕНИ] Ищем в bellSchedule.thursday: ${bellSchedule!!.thursday}")
                bellSchedule!!.thursday.find { it.number == lessonNumber }?.time
            }
            "пятница" -> {
                Log.d("BELL_SCHEDULE", "📅 [ПОИСК ВРЕМЕНИ] Ищем в bellSchedule.friday: ${bellSchedule!!.friday}")
                bellSchedule!!.friday.find { it.number == lessonNumber }?.time
            }
            "суббота" -> ""
            else -> {
                Log.w("BELL_SCHEDULE", "❓ [ПОИСК ВРЕМЕНИ] Неизвестный день: $dayOfWeek")
                null
            }
        }

        if (foundTime != null) {
            Log.d("BELL_SCHEDULE", "✅ [ПОИСК ВРЕМЕНИ] Найдено время: '$foundTime'")
            return foundTime
        } else {
            Log.w("BELL_SCHEDULE", "❌ [ПОИСК ВРЕМЕНИ] Время НЕ найдено для пары '$lessonNumber' в день '$dayOfWeek'")
            return "Время не указано"
        }
    }

    private fun setupProgressBar(progressBar: ProgressBar, lessonTime: String) {
        if (lessonTime == "Время не указано" || !lessonTime.contains("–")) {
            progressBar.visibility = View.GONE
            return
        }

        val parts = lessonTime.split(" – ")
        if (parts.size != 2) {
            progressBar.visibility = View.GONE
            return
        }

        val now = Calendar.getInstance()
        val start = parseTime(parts[0]).apply {
            set(Calendar.YEAR, now.get(Calendar.YEAR))
            set(Calendar.MONTH, now.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
        }
        val end = parseTime(parts[1]).apply {
            set(Calendar.YEAR, now.get(Calendar.YEAR))
            set(Calendar.MONTH, now.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
        }

        if (now.after(start) && now.before(end)) {
            val total = end.timeInMillis - start.timeInMillis
            val elapsed = now.timeInMillis - start.timeInMillis
            val progress = (elapsed * 100 / total).toInt()

            progressBar.apply {
                visibility = View.VISIBLE
                max = 100
                this.progress = progress
            }
        } else {
            progressBar.visibility = View.GONE
        }
    }

    private fun parseTime(timeStr: String): Calendar {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        cal.time = sdf.parse(timeStr) ?: Date()
        return cal
    }

    private fun getGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Доброе утро ☺️"
            in 12..17 -> "Добрый день 🤪"
            in 18..21 -> "Добрый вечер 🙂"
            else -> "Доброй ночи 😴"
        }
    }

    private fun getCurrentTime(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    fun getTimeToNextLesson(): String {
        val now = Calendar.getInstance()
        val targetDay = scheduleItems.firstOrNull()?.actualDayOfWeek ?: return "День не определен"
        val schedule = when (targetDay) {
            "понедельник" -> bellSchedule?.monday
            "вторник" -> bellSchedule?.tuesday
            "среда" -> bellSchedule?.wednesday
            "четверг" -> bellSchedule?.thursday
            "пятница" -> bellSchedule?.friday
            "суббота" -> return "Выходной"
            else -> null
        } ?: return "Расписание не загружено"

        for (lesson in schedule) {
            val parts = lesson.time.split(" – ")
            if (parts.size != 2) continue

            val start = parseTime(parts[0]).apply {
                set(Calendar.YEAR, now.get(Calendar.YEAR))
                set(Calendar.MONTH, now.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
            }
            val end = parseTime(parts[1]).apply {
                set(Calendar.YEAR, now.get(Calendar.YEAR))
                set(Calendar.MONTH, now.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
            }

            if (now.before(start)) {
                val mins = (start.timeInMillis - now.timeInMillis) / 60000
                return "До пары: $mins мин"
            }
            if (now.after(start) && now.before(end)) {
                val mins = (end.timeInMillis - now.timeInMillis) / 60000
                return "До конца: $mins мин"
            }
        }
        return "Пар больше нет"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}