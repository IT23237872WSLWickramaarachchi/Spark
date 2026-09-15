package com.example.spark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Represents a daily completion log entry for a habit.
 *
 * Foreign key cascade: deleting a Habit automatically removes all its log entries.
 * Indexed on habitId for efficient streak and completion queries.
 */
@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId"]),
        Index(value = ["habitId", "completedDate"], unique = true)
    ]
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "habitId")
    val habitId: Long,
    val completedDate: LocalDate,
    val isCompleted: Boolean = true
)
