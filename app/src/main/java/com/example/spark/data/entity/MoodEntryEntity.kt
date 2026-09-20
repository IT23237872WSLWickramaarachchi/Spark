package com.example.spark.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "mood_entries",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["user_id"])
    ]
)
data class MoodEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "date")
    val date: String, // Format: YYYY-MM-DD

    @ColumnInfo(name = "mood_level")
    val moodLevel: Int, // 1 (Difficult) .. 5 (Radiant)

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "tags")
    val tags: String = "",

    @ColumnInfo(name = "is_demo")
    val isDemo: Boolean = false,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "time")
    val time: String = "",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getTagsList(): List<String> {
        return if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    companion object {
        fun joinTags(list: List<String>): String = list.filter { it.isNotBlank() }.joinToString(",")
    }
}

