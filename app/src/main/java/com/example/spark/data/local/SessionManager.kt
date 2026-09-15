package com.example.spark.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages the current active user session using SharedPreferences.
 * Provides thread-safe access to logged-in status and the current active user ID.
 */
class SessionManager(
    private val prefs: SharedPreferences
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    )

    var currentUserId: Long
        get() = prefs.getLong(KEY_CURRENT_USER_ID, DEFAULT_USER_ID)
        set(value) = prefs.edit().putLong(KEY_CURRENT_USER_ID, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    fun saveSession(userId: Long) {
        prefs.edit()
            .putLong(KEY_CURRENT_USER_ID, userId)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_CURRENT_USER_ID)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }

    companion object {
        private const val PREF_NAME = "spark_session_prefs"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val DEFAULT_USER_ID = 1L
    }
}
