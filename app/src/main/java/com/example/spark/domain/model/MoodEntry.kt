package com.example.spark.domain.model

import com.example.spark.data.local.entity.MoodEntryEntity
import java.time.Instant
import java.time.LocalDate

/**
 * Domain model representing a daily mood check-in entry.
 * [moodScore] ranges from 1 (Difficult) to 5 (Radiant).
 */
data class MoodEntry(
    val id: String = "",
    val userId: String = "1",
    val date: LocalDate = LocalDate.now(),
    val moodScore: Int,
    val note: String? = null,
    val loggedAt: Instant = Instant.now()
)

fun MoodEntryEntity.toDomain(): MoodEntry = MoodEntry(
    id = id.toString(),
    userId = userId.toString(),
    date = date,
    moodScore = moodScore,
    note = note,
    loggedAt = loggedAt
)

fun MoodEntry.toEntity(): MoodEntryEntity = MoodEntryEntity(
    id = id.toLongOrNull() ?: 0L,
    userId = userId.toLongOrNull() ?: 1L,
    date = date,
    moodScore = moodScore,
    note = note,
    loggedAt = loggedAt
)
