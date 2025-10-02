package it.hamy.schedule.model

data class BellSchedule(
    val monday: List<LessonTime> = emptyList(),
    val tuesday: List<LessonTime> = emptyList(),
    val wednesday: List<LessonTime> = emptyList(),
    val thursday: List<LessonTime> = emptyList(),
    val friday: List<LessonTime> = emptyList(),
    val saturday: List<LessonTime> = emptyList()
)

data class LessonTime(
    val number: String,
    val time: String // например, "08:30 – 10:00"
)