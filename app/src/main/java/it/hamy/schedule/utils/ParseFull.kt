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

object ParseFull {

    suspend fun fetchFullSchedule(group: String): Map<String, MutableList<ScheduleItem>> = withContext(Dispatchers.IO) {
        try {
            val url = "http://schedule.ckstr.ru/$group.htm"
            val response = Jsoup.connect(url).execute()
            val html = String(response.bodyAsBytes(), Charset.forName("windows-1251"))
            val doc = Jsoup.parse(html)

            val scheduleList = parseSchedule(doc)
            return@withContext groupScheduleByDay(scheduleList)
        } catch (e: Exception) {
            Log.e("ParseFull", "Ошибка загрузки полного расписания", e)
            throw e
        }
    }

    private fun parseSchedule(doc: Document): List<ScheduleItem> {
        val scheduleList = mutableListOf<ScheduleItem>()
        var currentDay = ""

        val rows: Elements = doc.select("table.inf tr")

        for (row in rows) {
            val dayCell = row.select("td.hd[rowspan]")
            if (dayCell.isNotEmpty()) {
                currentDay = formatDate(dayCell.text())
            }

            val timeCell = row.select("td.hd").firstOrNull { it.attr("rowspan").isEmpty() }?.text()
            val subjectRaw = row.select("a.z1").map { it.text() }
            val teacherRaw = row.select("a.z3").map { it.text() }
            val roomRaw = row.select("a.z2").map { it.text() }

            val subject = subjectRaw.distinct().joinToString(" / ")
            val teacher = teacherRaw.distinct().joinToString(" / ")
            val room = roomRaw.distinct().joinToString(" / ")

            if (!timeCell.isNullOrEmpty() && subject.isNotEmpty()) {
                scheduleList.add(ScheduleItem(currentDay, timeCell, subject, teacher, room))
            }
        }

        return scheduleList
    }

    private fun groupScheduleByDay(scheduleList: List<ScheduleItem>): Map<String, MutableList<ScheduleItem>> {
        val grouped = mutableMapOf<String, MutableList<ScheduleItem>>()
        for (item in scheduleList) {
            grouped.getOrPut(item.day) { mutableListOf() }.add(item)
        }
        return grouped
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
            val date = inputFormat.parse(dateStr) ?: return dateStr

            val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale("ru"))
            val dayMonthFormat = SimpleDateFormat("d MMMM", Locale("ru"))

            val dayOfWeek = dayOfWeekFormat.format(date)
            val dayMonth = dayMonthFormat.format(date)

            "$dayMonth, $dayOfWeek"
        } catch (e: Exception) {
            Log.e("ParseFull", "Ошибка форматирования даты: $dateStr", e)
            dateStr
        }
    }
}