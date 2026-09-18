package com.example.spark.data.repository

import com.example.spark.data.dao.UserDao
import com.example.spark.data.entity.UserEntity
import com.example.spark.util.PasswordHasher

class UserRepository(private val userDao: UserDao) {

    suspend fun registerUser(fullName: String, email: String, password: String): Result<UserEntity> {
        val trimmedEmail = email.trim().lowercase()
        val existing = userDao.getUserByEmail(trimmedEmail)
        if (existing != null) {
            return Result.failure(Exception("An account with this email already exists"))
        }

        val hashedPassword = PasswordHasher.hashPassword(password)
        val user = UserEntity(
            fullName = fullName.trim(),
            email = trimmedEmail,
            passwordHash = hashedPassword
        )
        val generatedId = userDao.insertUser(user)
        return Result.success(user.copy(id = generatedId))
    }

    suspend fun loginUser(email: String, password: String): Result<UserEntity> {
        val trimmedEmail = email.trim().lowercase()
        val user = userDao.getUserByEmail(trimmedEmail)
            ?: return Result.failure(Exception("No account found with this email"))

        if (!PasswordHasher.verifyPassword(password, user.passwordHash)) {
            return Result.failure(Exception("Incorrect password"))
        }

        return Result.success(user)
    }

    suspend fun getUserById(userId: Long): UserEntity? {
        return userDao.getUserById(userId)
    }

    fun getUserByIdLiveData(userId: Long): androidx.lifecycle.LiveData<UserEntity?> {
        return userDao.getUserByIdLiveData(userId)
    }

    suspend fun updateProfileImage(userId: Long, imageUri: String?) {
        userDao.updateProfileImage(userId, imageUri)
    }

    suspend fun updateFullName(userId: Long, fullName: String) {
        userDao.updateFullName(userId, fullName)
    }
}
