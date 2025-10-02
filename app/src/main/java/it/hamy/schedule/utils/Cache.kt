package it.hamy.schedule.utils

import android.content.Context
import com.google.gson.Gson
import it.hamy.schedule.model.BellSchedule

object Cache {
    private const val PREFS_NAME = "schedule_cache"
    private const val KEY_BELL_SCHEDULE = "bell_schedule"
    private const val KEY_TODAY_SCHEDULE = "cached_today_schedule"
    private const val KEY_FULL_SCHEDULE = "cached_schedule"

    private val gson = Gson()

    // --- Кеширование расписания звонков ---
    fun saveBellSchedule(context: Context, bellSchedule: BellSchedule) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(bellSchedule)
        prefs.edit().putString(KEY_BELL_SCHEDULE, json).apply()
    }

    fun loadBellSchedule(context: Context): BellSchedule? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_BELL_SCHEDULE, null) ?: return null
        return gson.fromJson(json, BellSchedule::class.java)
    }

    // --- Кеширование расписания на сегодня ---
    fun saveTodaySchedule(context: Context, schedule: List<it.hamy.schedule.model.ScheduleItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(schedule)
        prefs.edit().putString(KEY_TODAY_SCHEDULE, json).apply()
    }

    fun loadTodaySchedule(context: Context): List<it.hamy.schedule.model.ScheduleItem>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_TODAY_SCHEDULE, null) ?: return null
        val type = object : com.google.gson.reflect.TypeToken<List<it.hamy.schedule.model.ScheduleItem>>() {}.type
        return gson.fromJson(json, type)
    }

    // --- Кеширование полного расписания (на несколько дней) ---
    fun saveFullSchedule(context: Context, schedule: Map<String, MutableList<it.hamy.schedule.model.ScheduleItem>>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(schedule)
        prefs.edit().putString(KEY_FULL_SCHEDULE, json).apply()
    }

    fun loadFullSchedule(context: Context): Map<String, MutableList<it.hamy.schedule.model.ScheduleItem>>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_FULL_SCHEDULE, null) ?: return null
        val type = object : com.google.gson.reflect.TypeToken<Map<String, MutableList<it.hamy.schedule.model.ScheduleItem>>>() {}.type
        return gson.fromJson(json, type)
    }
}