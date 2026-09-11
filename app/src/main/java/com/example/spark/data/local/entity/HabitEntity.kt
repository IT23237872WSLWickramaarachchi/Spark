package com.example.spark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalTime

/**
 * Represents a habit belonging to a user.
 *
 * Relationship: User 1—* Habit 1—* HabitLog.
 * Foreign key ensures referential integrity with cascade delete.
 */
@Entity(
    tableName = "habits",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "userId")
    val userId: Long,
    val title: String,
    val category: String,
    val frequency: String = "Daily",
    val targetDaysPerWeek: Int = 7,
    val reminderTime: LocalTime? = null,
    val colorHex: String = "#52559c",
    val createdAt: Instant = Instant.now(),
    val isArchived: Boolean = false
)
