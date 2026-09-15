package com.example.spark.ui.habitdetail

import com.example.spark.data.local.entity.HabitEntity
import com.example.spark.ui.custom.DayStatus

/**
 * UI State for [HabitDetailFragment].
 */
data class HabitDetailUiState(
    val habitId: String = "",
    val title: String = "",
    val category: String = "",
    val frequency: String = "",
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val weeklyStatuses: List<DayStatus> = emptyList(),
    val notes: String = "",
    val habit: HabitEntity? = null,
    val isLoading: Boolean = false
) {
    companion object {
        fun initial(): HabitDetailUiState = HabitDetailUiState(isLoading = true)
    }
}
