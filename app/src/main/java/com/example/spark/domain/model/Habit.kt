package com.example.spark.domain.model

import com.example.spark.data.local.entity.HabitEntity
import java.time.Instant
import java.time.LocalTime

/**
 * Domain model representing a habit.
 * Decouples the UI and business logic layers from Room entity details.
 */
data class Habit(
    val id: String = "",
    val userId: String = "1",
    val title: String,
    val category: String,
    val frequency: String = "Daily",
    val targetDaysPerWeek: Int = 7,
    val reminderTime: LocalTime? = null,
    val colorHex: String = "#52559C",
    val createdAt: Instant = Instant.now(),
    val isArchived: Boolean = false
)

fun HabitEntity.toDomain(): Habit = Habit(
    id = id.toString(),
    userId = userId.toString(),
    title = title,
    category = category,
    frequency = frequency,
    targetDaysPerWeek = targetDaysPerWeek,
    reminderTime = reminderTime,
    colorHex = colorHex,
    createdAt = createdAt,
    isArchived = isArchived
)

fun Habit.toEntity(): HabitEntity = HabitEntity(
    id = id.toLongOrNull() ?: 0L,
    userId = userId.toLongOrNull() ?: 1L,
    title = title,
    category = category,
    frequency = frequency,
    targetDaysPerWeek = targetDaysPerWeek,
    reminderTime = reminderTime,
    colorHex = colorHex,
    createdAt = createdAt,
    isArchived = isArchived
)
