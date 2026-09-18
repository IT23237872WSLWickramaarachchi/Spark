package com.example.spark.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.spark.data.entity.UserEntity

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserByIdLiveData(id: Long): LiveData<UserEntity?>

    @Query("UPDATE users SET profile_image_uri = :imageUri WHERE id = :userId")
    suspend fun updateProfileImage(userId: Long, imageUri: String?)

    @Query("UPDATE users SET full_name = :fullName WHERE id = :userId")
    suspend fun updateFullName(userId: Long, fullName: String)

    @Update
    suspend fun updateUser(user: UserEntity)
}
