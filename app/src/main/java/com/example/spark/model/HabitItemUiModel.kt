package com.example.spark.model

import com.example.spark.data.entity.HabitEntity
import com.example.spark.util.DateUtils

/**
 * UI representation of a habit row displayed on the Dashboard and lists.
 */
data class HabitItemUiModel(
    val habit: HabitEntity,
    val isCompletedToday: Boolean,
    val categoryIconRes: Int,
    val subtitle: String
) {
    companion object {
        fun from(habit: HabitEntity, isCompletedToday: Boolean): HabitItemUiModel {
            val category = HabitCategory.fromTitle(habit.category)
            val timePeriod = when (habit.reminderHour) {
                in 5..11 -> "Morning"
                in 12..16 -> "Afternoon"
                in 17..20 -> "Evening"
                else -> "Night"
            }
            val subtitle = if (habit.reminderEnabled) {
                val timeStr = DateUtils.formatTime12Hour(habit.reminderHour, habit.reminderMinute)
                "$timePeriod • $timeStr"
            } else {
                "$timePeriod • ${category.title}"
            }

            return HabitItemUiModel(
                habit = habit,
                isCompletedToday = isCompletedToday,
                categoryIconRes = category.iconResId,
                subtitle = subtitle
            )
        }
    }
}
