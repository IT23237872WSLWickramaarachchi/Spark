package com.example.spark.data.local

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionManagerTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        sessionManager = SessionManager(fakePrefs)
    }

    @Test
    fun defaultValues_areDefaultUserIdAndNotLoggedIn() {
        assertEquals(SessionManager.DEFAULT_USER_ID, sessionManager.currentUserId)
        assertFalse(sessionManager.isLoggedIn)
    }

    @Test
    fun saveSession_persistsUserIdAndLoggedInTrue() {
        sessionManager.saveSession(42L)

        assertEquals(42L, sessionManager.currentUserId)
        assertTrue(sessionManager.isLoggedIn)
        assertEquals(42L, fakePrefs.getLong("current_user_id", -1L))
        assertTrue(fakePrefs.getBoolean("is_logged_in", false))
    }

    @Test
    fun clearSession_removesUserIdAndSetsLoggedInFalse() {
        sessionManager.saveSession(42L)
        assertTrue(sessionManager.isLoggedIn)

        sessionManager.clearSession()

        assertEquals(SessionManager.DEFAULT_USER_ID, sessionManager.currentUserId)
        assertFalse(sessionManager.isLoggedIn)
        assertFalse(fakePrefs.getBoolean("is_logged_in", true))
    }

    @Test
    fun currentUserId_setterUpdatesPreferences() {
        sessionManager.currentUserId = 100L
        assertEquals(100L, sessionManager.currentUserId)
    }

    private class FakeSharedPreferences : SharedPreferences {
        val map = mutableMapOf<String, Any>()

        override fun getAll(): MutableMap<String, *> = map
        override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            @Suppress("UNCHECKED_CAST") (map[key] as? MutableSet<String> ?: defValues)
        override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(this)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        class FakeEditor(private val prefs: FakeSharedPreferences) : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()
            private val removed = mutableSetOf<String>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                if (key != null) pending[key] = values
                return this
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) pending[key] = value
                return this
            }

            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) removed.add(key)
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                clearAll = true
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearAll) prefs.map.clear()
                removed.forEach { prefs.map.remove(it) }
                pending.forEach { (k, v) -> if (v != null) prefs.map[k] = v else prefs.map.remove(k) }
            }
        }
    }
}
