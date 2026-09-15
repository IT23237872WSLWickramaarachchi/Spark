package com.example.spark.domain.model

import com.example.spark.data.local.entity.HabitLogEntity
import java.time.LocalDate

/**
 * Domain model representing a habit completion log entry.
 */
data class HabitLog(
    val id: String = "",
    val habitId: String,
    val completedDate: LocalDate,
    val isCompleted: Boolean = true
)

fun HabitLogEntity.toDomain(): HabitLog = HabitLog(
    id = id.toString(),
    habitId = habitId.toString(),
    completedDate = completedDate,
    isCompleted = isCompleted
)

fun HabitLog.toEntity(): HabitLogEntity = HabitLogEntity(
    id = id.toLongOrNull() ?: 0L,
    habitId = habitId.toLongOrNull() ?: 0L,
    completedDate = completedDate,
    isCompleted = isCompleted
)
