package com.example.spark.util

import android.content.Context
import android.content.SharedPreferences
import com.example.spark.data.entity.UserEntity

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "spark_session_prefs"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_PROFILE_IMAGE = "key_user_profile_image"
    }

    fun saveSession(user: UserEntity) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putLong(KEY_USER_ID, user.id)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_NAME, user.fullName)
            putString(KEY_USER_PROFILE_IMAGE, user.profileImageUri)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getCurrentUserId(): Long = prefs.getLong(KEY_USER_ID, -1L)

    fun getCurrentUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)

    fun getCurrentUserName(): String? = prefs.getString(KEY_USER_NAME, null)

    fun getCurrentUserProfileImageUri(): String? = prefs.getString(KEY_USER_PROFILE_IMAGE, null)

    fun saveUserProfileImageUri(uri: String?) {
        prefs.edit().putString(KEY_USER_PROFILE_IMAGE, uri).apply()
    }

    fun updateUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
