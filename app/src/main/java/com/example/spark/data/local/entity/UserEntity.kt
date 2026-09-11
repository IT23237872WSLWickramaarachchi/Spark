package com.example.spark.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Represents a user account in the local Room database.
 *
 * Relationship: User 1—* Habit, User 1—* MoodEntry.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val email: String,
    val passwordHash: String,
    val createdAt: Instant = Instant.now()
)
