package com.example.spark.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.spark.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [UserEntity].
 *
 * Provides authentication queries and user management operations.
 */
@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun register(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email AND passwordHash = :passwordHash LIMIT 1")
    suspend fun login(email: String, passwordHash: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getCurrentUser(userId: Long): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePrefs(prefs: com.example.spark.data.local.entity.UserPrefsEntity)

    @Query("SELECT * FROM user_prefs WHERE userId = :userId LIMIT 1")
    suspend fun getUserPrefs(userId: Long): com.example.spark.data.local.entity.UserPrefsEntity?

    @Query("SELECT * FROM user_prefs WHERE userId = :userId LIMIT 1")
    fun getUserPrefsFlow(userId: Long): Flow<com.example.spark.data.local.entity.UserPrefsEntity?>

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: Long)
}
