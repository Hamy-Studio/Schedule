package it.hamy.schedule.utils

import android.util.Log
import it.hamy.schedule.model.ScheduleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

object ParseToday {

    private const val SCHEDULE_URL = "http://schedule.ckstr.ru/hg.htm"

    suspend fun fetchTodaySchedule(group: String): List<ScheduleItem> = withContext(Dispatchers.IO) {
        try {
            val response = Jsoup.connect(SCHEDULE_URL).execute()
            val html = String(response.bodyAsBytes(), Charset.forName("windows-1251"))
            val doc = Jsoup.parse(html)

            val date = parseDate(doc)
            return@withContext parseSchedule(doc, group, date)
        } catch (e: Exception) {
            Log.e("ParseToday", "Ошибка загрузки расписания", e)
            throw e
        }
    }

    private fun parseDate(doc: Document): String {
        val dateElement = doc.select("ul.zg li.zgr").first()
        return dateElement?.text() ?: SimpleDateFormat("dd.MM.yyyy", Locale("ru")).format(Date())
    }

    private fun parseSchedule(doc: Document, group: String, date: String): List<ScheduleItem> {
        val scheduleItems = mutableListOf<ScheduleItem>()
        var currentGroupLink = ""

        val rows: Elements = doc.select("table.inf tr")

        for (row in rows) {
            val groupCell = row.select("td.hd[rowspan] a[href]")
            if (groupCell.isNotEmpty()) {
                val groupLink = groupCell.attr("href").substringBefore(".htm")
                currentGroupLink = groupLink
                Log.d("GroupComparison", "Group: $group, Link: $currentGroupLink")
            }

            if (currentGroupLink != group) continue

            val timeCell = row.select("td.hd").firstOrNull { it.attr("rowspan").isEmpty() }?.text()
            val subjectRaw = row.select("a.z1").map { it.text() }
            val teacherRaw = row.select("a.z3").map { it.text() }
            val roomRaw = row.select("a.z2").map { it.text() }

            val subject = subjectRaw.distinct().joinToString(" / ")
            val teacher = teacherRaw.distinct().joinToString(" / ")
            val room = roomRaw.distinct().joinToString(" / ")

            if (!timeCell.isNullOrEmpty() && subject.isNotEmpty()) {
                val formattedDate = formatDate(date)
                val dayOfWeek = extractDayOfWeekFromFormattedDate(formattedDate)
                scheduleItems.add(ScheduleItem(formattedDate, timeCell, subject, teacher, room, dayOfWeek))
            }
        }

        return scheduleItems
    }

    private fun extractDayOfWeekFromFormattedDate(formattedDate: String): String? {
        return try {
            val parts = formattedDate.split(", ")
            if (parts.size == 2) {
                val shortDay = parts[1].lowercase().trim()
                normalizeDayOfWeek(shortDay)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ParseToday", "Не удалось извлечь день недели из: $formattedDate", e)
            null
        }
    }

    private fun normalizeDayOfWeek(shortDay: String): String? {
        return when (shortDay.lowercase().trim()) {
            "пн", "понедельник" -> "понедельник"
            "вт", "вторник" -> "вторник"
            "ср", "среда" -> "среда"
            "чт", "четверг" -> "четверг"
            "пт", "пятница" -> "пятница"
            "сб", "суббота" -> "суббота"
            "вс", "воскресенье" -> "воскресенье"
            else -> null
        }
    }

    private fun formatDate(dateStr: String): String {
        return try {
            // Сначала пробуем распарсить с коротким днём недели
            val inputFormatShort = SimpleDateFormat("dd.MM.yyyy E", Locale("ru"))
            var date = inputFormatShort.parse(dateStr)

            // Если не получилось — пробуем без дня
            if (date == null) {
                val inputFormatNoDay = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
                date = inputFormatNoDay.parse(dateStr)
            }

            if (date == null) return dateStr

            val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale("ru"))
            val dayMonthFormat = SimpleDateFormat("d MMMM", Locale("ru"))

            val dayOfWeek = dayOfWeekFormat.format(date)
            val dayMonth = dayMonthFormat.format(date)

            "$dayMonth, $dayOfWeek"
        } catch (e: Exception) {
            Log.e("ParseToday", "Ошибка форматирования даты: $dateStr", e)
            dateStr
        }
    }
}