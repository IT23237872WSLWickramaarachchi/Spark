package com.example.spark.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_completions",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habit_id", "completion_date"], unique = true),
        Index(value = ["habit_id"])
    ]
)
data class HabitCompletionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "habit_id")
    val habitId: Long,

    @ColumnInfo(name = "completion_date")
    val completionDate: String, // Format: YYYY-MM-DD

    @ColumnInfo(name = "completed")
    val completed: Boolean = true,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "is_demo")
    val isDemo: Boolean = false
)
