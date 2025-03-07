package it.hamy.shedule

import android.content.Context
import android.content.SharedPreferences

object PreferencesManager {
    private const val PREFS_NAME = "schedule_prefs"
    private const val KEY_GROUP = "selected_group"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveGroup(context: Context, group: String) {
        getPreferences(context).edit().putString(KEY_GROUP, group).apply()
    }

    fun getGroup(context: Context): String? {
        return getPreferences(context).getString(KEY_GROUP, null)
    }

    fun isGroupSelected(context: Context): Boolean {
        return getGroup(context) != null
    }
}