package com.example.spark.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * Represents a to-do item / task belonging to a user.
 *
 * Relationship: User 1—* Todo.
 * Foreign key ensures referential integrity with cascade delete on user deletion.
 */
@Entity(
    tableName = "todos",
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
data class TodoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "userId")
    val userId: Long,
    val title: String,
    val description: String? = null,
    val isCompleted: Boolean = false,
    val dueDate: LocalDate? = null,
    val priority: String = "Medium", // "Low", "Medium", "High"
    val createdAt: Instant = Instant.now(),
    val completedAt: Instant? = null
)
