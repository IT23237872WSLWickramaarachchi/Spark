package com.example.spark

import androidx.lifecycle.LiveData
import com.example.spark.data.dao.UserDao
import com.example.spark.data.entity.UserEntity
import com.example.spark.data.repository.UserRepository
import com.example.spark.util.PasswordHasher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserRepositoryTest {

    private lateinit var fakeUserDao: FakeUserDao
    private lateinit var userRepository: UserRepository

    class FakeUserDao : UserDao {
        private val users = mutableMapOf<Long, UserEntity>()
        private var currentId = 1L

        override suspend fun insertUser(user: UserEntity): Long {
            val id = currentId++
            users[id] = user.copy(id = id)
            return id
        }

        override suspend fun getUserByEmail(email: String): UserEntity? {
            return users.values.find { it.email.equals(email, ignoreCase = true) }
        }

        override suspend fun getUserById(id: Long): UserEntity? {
            return users[id]
        }

        override fun getUserByIdLiveData(id: Long): LiveData<UserEntity?> {
            return androidx.lifecycle.MutableLiveData(users[id])
        }

        override suspend fun updateProfileImage(userId: Long, imageUri: String?) {
            val u = users[userId]
            if (u != null) {
                users[userId] = u.copy(profileImageUri = imageUri)
            }
        }

        override suspend fun updateFullName(userId: Long, fullName: String) {
            val u = users[userId]
            if (u != null) {
                users[userId] = u.copy(fullName = fullName)
            }
        }

        override suspend fun updateUser(user: UserEntity) {
            users[user.id] = user
        }
    }

    @Before
    fun setUp() {
        fakeUserDao = FakeUserDao()
        userRepository = UserRepository(fakeUserDao)
    }

    @Test
    fun passwordHasher_hashesAndVerifiesCorrectly() {
        val pass = "mySecretPassword123"
        val hash = PasswordHasher.hashPassword(pass)

        assertNotEquals(pass, hash)
        assertTrue(PasswordHasher.verifyPassword(pass, hash))
        assertFalse(PasswordHasher.verifyPassword("wrongPassword", hash))
    }

    @Test
    fun registerUser_createsUserWithHashedPassword() = runBlocking {
        val result = userRepository.registerUser("Jane Doe", "jane@example.com", "securePassword123")

        assertTrue(result.isSuccess)
        val user = result.getOrThrow()
        assertEquals("Jane Doe", user.fullName)
        assertEquals("jane@example.com", user.email)
        assertNotEquals("securePassword123", user.passwordHash)
        assertTrue(PasswordHasher.verifyPassword("securePassword123", user.passwordHash))
    }

    @Test
    fun registerUser_duplicateEmail_fails() = runBlocking {
        val result1 = userRepository.registerUser("Jane Doe", "jane@example.com", "pass12345")
        assertTrue(result1.isSuccess)

        val result2 = userRepository.registerUser("Jane Duplicate", "jane@example.com", "anotherPass123")
        assertTrue(result2.isFailure)
        assertEquals("An account with this email already exists", result2.exceptionOrNull()?.message)
    }

    @Test
    fun loginUser_validCredentials_succeeds() = runBlocking {
        userRepository.registerUser("Alex Johnson", "alex@spark.com", "password88")

        val loginResult = userRepository.loginUser("alex@spark.com", "password88")
        assertTrue(loginResult.isSuccess)
        val loggedUser = loginResult.getOrThrow()
        assertEquals("Alex Johnson", loggedUser.fullName)
        assertEquals("alex@spark.com", loggedUser.email)
    }

    @Test
    fun loginUser_wrongPassword_fails() = runBlocking {
        userRepository.registerUser("Alex Johnson", "alex@spark.com", "correctPassword")

        val loginResult = userRepository.loginUser("alex@spark.com", "wrongPassword")
        assertTrue(loginResult.isFailure)
        assertEquals("Incorrect password", loginResult.exceptionOrNull()?.message)
    }

    @Test
    fun loginUser_nonExistentEmail_fails() = runBlocking {
        val loginResult = userRepository.loginUser("notfound@spark.com", "anyPassword")
        assertTrue(loginResult.isFailure)
        assertEquals("No account found with this email", loginResult.exceptionOrNull()?.message)
    }

    @Test
    fun updateProfileImage_updatesPersistedUri() = runBlocking {
        val reg = userRepository.registerUser("Image User", "img@spark.com", "pass123")
        val user = reg.getOrThrow()
        val dummyUri = "file:///data/user/0/com.example.spark/files/profiles/avatar.jpg"

        userRepository.updateProfileImage(user.id, dummyUri)

        val updated = userRepository.getUserById(user.id)
        assertNotNull(updated)
        assertEquals(dummyUri, updated?.profileImageUri)
    }
}
