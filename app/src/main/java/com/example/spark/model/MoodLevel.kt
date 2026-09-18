package com.example.spark.model

import com.example.spark.R

enum class MoodLevel(
    val level: Int,
    val title: String,
    val dashboardText: String,
    val iconResId: Int
) {
    VERY_BAD(1, "Very bad", "Feeling down today", R.drawable.ic_mood_1),
    BAD(2, "Bad", "Feeling bad today", R.drawable.ic_mood_2),
    NEUTRAL(3, "Neutral", "Feeling okay today", R.drawable.ic_mood_3),
    GOOD(4, "Good", "Feeling good today", R.drawable.ic_mood_4),
    GREAT(5, "Great", "Feeling great today", R.drawable.ic_mood_5);

    companion object {
        fun fromLevel(level: Int): MoodLevel {
            return entries.find { it.level == level } ?: NEUTRAL
        }
    }
}
