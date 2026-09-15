package com.example.spark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing user settings and preferences.
 */
@Entity(
    tableName = "user_prefs",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"], unique = true)]
)
data class UserPrefsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "userId")
    val userId: Long = 1L,
    val notificationsEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val reminderTime: String = "08:00 AM",
    val language: String = "English"
)
