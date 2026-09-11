package com.example.spark.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Data class representing a Habit along with all of its associated log entries.
 *
 * Used in @Transaction queries to fetch a habit and its completion history atomically.
 */
data class HabitWithLogs(
    @Embedded
    val habit: HabitEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "habitId"
    )
    val logs: List<HabitLogEntity>
)
