package com.example.spark.model

import com.example.spark.R

enum class HabitCategory(
    val title: String,
    val iconResId: Int
) {
    HEALTH("Health", R.drawable.ic_water_drop),
    FITNESS("Fitness", R.drawable.ic_walk),
    MINDFULNESS("Mindfulness", R.drawable.ic_meditation),
    PRODUCTIVITY("Productivity", R.drawable.ic_check_circle),
    STUDY("Study", R.drawable.ic_book),
    WORK("Work", R.drawable.ic_clock),
    SLEEP("Sleep", R.drawable.ic_moon),
    NUTRITION("Nutrition", R.drawable.ic_streak_flame),
    HYDRATION("Hydration", R.drawable.ic_water_drop),
    READING("Reading", R.drawable.ic_book),
    SELF_CARE("Self-care", R.drawable.ic_meditation),
    SOCIAL("Social", R.drawable.ic_person),
    FINANCE("Finance", R.drawable.ic_chart),
    CREATIVITY("Creativity", R.drawable.ic_flare),
    HOME("Home", R.drawable.ic_home),
    PERSONAL("Personal", R.drawable.ic_person),
    ROUTINE("Routine", R.drawable.ic_moon);

    companion object {
        fun fromTitle(title: String): HabitCategory {
            return entries.find { it.title.equals(title, ignoreCase = true) } ?: HEALTH
        }
    }
}
