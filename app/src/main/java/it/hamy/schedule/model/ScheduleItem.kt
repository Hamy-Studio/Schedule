// ScheduleItem.kt
package it.hamy.schedule.model

data class ScheduleItem(
    val day: String,
    val time: String,
    val subject: String,
    val teacher: String,
    val room: String,
    val actualDayOfWeek: String? = null  // ← ДОБАВЛЕНО: "понедельник", "вторник" и т.д.
)