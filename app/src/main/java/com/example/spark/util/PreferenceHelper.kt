package com.example.spark.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

class PreferenceHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "spark_app_preferences"
        private const val KEY_DARK_MODE = "key_dark_mode"
        private const val KEY_NOTIFICATIONS_MASTER = "key_notifications_master"
        private const val KEY_MOOD_REMINDER_TIME = "key_mood_reminder_time"
        private const val KEY_LANGUAGE = "key_language"

        const val DEFAULT_MOOD_REMINDER_TIME = "20:00" // 8:00 PM
    }

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()
            applyNightMode(value)
        }

    var isNotificationsMasterEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_MASTER, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_MASTER, value).apply()

    var moodReminderTime: String
        get() = prefs.getString(KEY_MOOD_REMINDER_TIME, DEFAULT_MOOD_REMINDER_TIME) ?: DEFAULT_MOOD_REMINDER_TIME
        set(value) = prefs.edit().putString(KEY_MOOD_REMINDER_TIME, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "English") ?: "English"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    fun applyTheme() {
        applyNightMode(isDarkMode)
    }

    private fun applyNightMode(darkMode: Boolean) {
        val mode = if (darkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
