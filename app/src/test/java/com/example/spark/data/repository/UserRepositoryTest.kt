package com.example.spark.data.repository

import com.example.spark.data.local.dao.UserDao
import com.example.spark.data.local.entity.UserEntity
import com.example.spark.data.local.entity.UserPrefsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class UserRepositoryTest {

    private lateinit var fakeUserDao: FakeUserDao
    private lateinit var repository: UserRepository

    @Before
    fun setUp() {
        fakeUserDao = FakeUserDao()
        repository = UserRepository(fakeUserDao)
    }

    @Test
    fun register_and_login_success() = runBlocking {
        val user = UserEntity(
            name = "Alex",
            email = "alex@example.com",
            passwordHash = "hash123"
        )
        val id = repository.register(user)
        assertEquals(1L, id)

        val loggedIn = repository.login("alex@example.com", "hash123")
        assertNotNull(loggedIn)
        assertEquals("Alex", loggedIn?.name)

        val failedLogin = repository.login("alex@example.com", "wronghash")
        assertNull(failedLogin)
    }

    @Test
    fun getCurrentUser_returnsUserFlow() = runBlocking {
        val user = UserEntity(name = "Jordan", email = "jordan@example.com", passwordHash = "pass")
        val id = repository.register(user)

        val userFlow = repository.getCurrentUser(id.toString()).first()
        assertNotNull(userFlow)
        assertEquals("Jordan", userFlow?.name)
    }

    @Test
    fun updatePrefs_persistsPreferences() = runBlocking {
        val prefs = UserPrefsEntity(
            userId = 1L,
            notificationsEnabled = false,
            darkModeEnabled = true,
            reminderTime = "09:00 AM"
        )
        repository.updatePrefs(prefs)

        val stored = fakeUserDao.prefsState.value
        assertEquals(false, stored?.notificationsEnabled)
        assertEquals(true, stored?.darkModeEnabled)
        assertEquals("09:00 AM", stored?.reminderTime)

        val retrieved = repository.getUserPrefs(1L)
        assertEquals(false, retrieved?.notificationsEnabled)
        assertEquals(true, retrieved?.darkModeEnabled)

        val retrievedFlow = repository.getUserPrefsFlow(1L).first()
        assertEquals(false, retrievedFlow?.notificationsEnabled)
        assertEquals(true, retrievedFlow?.darkModeEnabled)
    }

    private class FakeUserDao : UserDao {
        private var counter = 1L
        private val users = MutableStateFlow<List<UserEntity>>(emptyList())
        val prefsState = MutableStateFlow<UserPrefsEntity?>(null)

        override suspend fun register(user: UserEntity): Long {
            val id = if (user.id == 0L) counter++ else user.id
            val newUser = user.copy(id = id)
            users.value = users.value + newUser
            return id
        }

        override suspend fun getUserByEmail(email: String): UserEntity? {
            return users.value.find { it.email == email }
        }

        override suspend fun login(email: String, passwordHash: String): UserEntity? {
            return users.value.find { it.email == email && it.passwordHash == passwordHash }
        }

        override fun getCurrentUser(userId: Long): Flow<UserEntity?> {
            return users.map { list -> list.find { it.id == userId } }
        }

        override suspend fun updatePrefs(prefs: UserPrefsEntity) {
            prefsState.value = prefs
        }

        override suspend fun getUserPrefs(userId: Long): UserPrefsEntity? {
            return prefsState.value?.takeIf { it.userId == userId }
        }

        override fun getUserPrefsFlow(userId: Long): Flow<UserPrefsEntity?> {
            return prefsState.map { if (it?.userId == userId) it else null }
        }

        override suspend fun deleteUser(userId: Long) {
            users.value = users.value.filter { it.id != userId }
        }
    }
}
