package com.example.spark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * Represents a daily mood check-in entry.
 *
 * Foreign key links to the owning user with cascade delete.
 * [moodScore] ranges from 1 (Difficult) to 5 (Radiant).
 */
@Entity(
    tableName = "mood_entries",
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
data class MoodEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "userId")
    val userId: Long,
    val date: LocalDate,
    val moodScore: Int,
    val note: String? = null,
    val loggedAt: Instant = Instant.now()
)
