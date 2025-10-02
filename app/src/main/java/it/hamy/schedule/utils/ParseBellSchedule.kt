package it.hamy.schedule.utils

import android.util.Log
import it.hamy.schedule.model.BellSchedule
import it.hamy.schedule.model.LessonTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object ParseBellSchedule {

    private const val TAG = "BELL_SCHEDULE"
    private const val API_URL = "https://schedule-admin-ten.vercel.app/api/schedule"
    private val client = OkHttpClient()

    suspend fun fetchBellSchedule(): BellSchedule? = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "🌐 [1/5] Отправляем запрос к: $API_URL")

            val request = Request.Builder().url(API_URL).build()
            val response = client.newCall(request).execute()

            Log.d(TAG, "📡 [2/5] Код ответа: ${response.code}")
            Log.d(TAG, "🧾 [3/5] Content-Type: ${response.header("Content-Type")}")

            val body = response.body?.string()
            if (body == null) {
                Log.e(TAG, "❌ [4/5] Тело ответа null")
                return@withContext null
            }

            Log.d(TAG, "📄 [4/5] Сырое тело ответа (первые 200 символов): ${body.take(200)}")

            // Проверяем, не HTML ли это
            if (body.contains("<html") || body.contains("<!DOCTYPE")) {
                Log.e(TAG, "🔥 [АЛАРМ] Это HTML, а не JSON! Тело ответа: $body")
                return@withContext null
            }

            val jsonObject = JSONObject(body)

            Log.d(TAG, "✅ [5/5] JSON успешно распарсен. Ключи: ${jsonObject.keys().asSequence().toList()}")

            val monday = parseDay(jsonObject.optJSONArray("понедельник"), "понедельник")
            val tuesday = parseDay(jsonObject.optJSONArray("вторник"), "вторник")
            val wednesday = parseDay(jsonObject.optJSONArray("среда"), "среда")
            val thursday = parseDay(jsonObject.optJSONArray("четверг"), "четверг")
            val friday = parseDay(jsonObject.optJSONArray("пятница"), "пятница")
            val saturday = parseDay(jsonObject.optJSONArray("суббота"), "суббота")

            val result = BellSchedule(
                monday = monday,
                tuesday = tuesday,
                wednesday = wednesday,
                thursday = thursday,
                friday = friday,
                saturday = saturday
            )

            Log.d(TAG, "🎉 [ГОТОВО] Расписание загружено: $result")
            result

        } catch (e: Exception) {
            Log.e(TAG, "💥 [ОШИБКА] ${e.message}", e)
            null
        }
    }

    private fun parseDay(array: org.json.JSONArray?, dayName: String): List<LessonTime> {
        if (array == null) {
            Log.w(TAG, "⚠️ $dayName: массив null или не найден")
            return emptyList()
        }

        val list = mutableListOf<LessonTime>()
        for (i in 0 until array.length()) {
            try {
                val obj = array.getJSONObject(i)
                val lesson = LessonTime(obj.getString("number"), obj.getString("time"))
                list.add(lesson)
                Log.d(TAG, "➕ $dayName: добавлена пара ${lesson.number} = ${lesson.time}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Ошибка парсинга элемента $i в $dayName", e)
            }
        }
        return list
    }
}