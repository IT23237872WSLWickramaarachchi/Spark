package com.example.spark.data.repository

import com.example.spark.data.local.dao.UserDao
import com.example.spark.data.local.entity.UserEntity
import com.example.spark.data.local.entity.UserPrefsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

/**
 * Repository wrapping [UserDao] exposing suspend functions and Flows only.
 * No business logic, no UI concerns. Every suspend function executes on [Dispatchers.IO].
 */
class UserRepository(
    private val userDao: UserDao
) {

    /**
     * Registers a new user account.
     */
    suspend fun register(user: UserEntity): Long = withContext(Dispatchers.IO) {
        userDao.register(user)
    }

    /**
     * Authenticates a user by email and password hash.
     */
    suspend fun login(email: String, passwordHash: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.login(email, passwordHash)
    }

    /**
     * Reactive stream of current user account details by ID.
     */
    fun getCurrentUser(userId: String): Flow<UserEntity?> {
        val id = userId.toLongOrNull() ?: return flowOf(null)
        return userDao.getCurrentUser(id)
    }

    /**
     * Updates or inserts user preferences.
     */
    suspend fun updatePrefs(prefs: UserPrefsEntity): Unit = withContext(Dispatchers.IO) {
        userDao.updatePrefs(prefs)
    }

    /**
     * Retrieves user preferences for the given user ID.
     */
    suspend fun getUserPrefs(userId: Long = 1L): UserPrefsEntity? = withContext(Dispatchers.IO) {
        userDao.getUserPrefs(userId)
    }

    /**
     * Reactive stream of user preferences.
     */
    fun getUserPrefsFlow(userId: Long = 1L): Flow<UserPrefsEntity?> {
        return userDao.getUserPrefsFlow(userId)
    }

    // ── Additional Helpers ──────────────────────────────────────────────

    suspend fun deleteUser(userId: String): Unit = withContext(Dispatchers.IO) {
        val id = userId.toLongOrNull() ?: return@withContext
        userDao.deleteUser(id)
    }

    suspend fun getUserByEmail(email: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserByEmail(email)
    }
}
