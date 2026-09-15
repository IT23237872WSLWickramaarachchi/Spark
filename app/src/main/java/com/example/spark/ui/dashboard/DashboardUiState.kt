package com.example.spark.ui.dashboard

/**
 * UI State for the Dashboard screen.
 * Contains purely calculated data derived reactively from Room Flows.
 */
data class DashboardUiState(
    val greeting: String,
    val userName: String,
    val habits: List<HabitUiModel>,
    val completedToday: Int,
    val totalToday: Int,
    val progressPercent: Int,
    val moodLabel: String?,
    val streakDays: Int,
    val isEmpty: Boolean,
    val pendingTasksCount: Int = 0,
    val topTasks: List<com.example.spark.data.local.entity.TodoEntity> = emptyList()
) {
    companion object {
        fun initial(): DashboardUiState = DashboardUiState(
            greeting = "Good morning",
            userName = "Alex",
            habits = emptyList(),
            completedToday = 0,
            totalToday = 0,
            progressPercent = 0,
            moodLabel = null,
            streakDays = 0,
            isEmpty = true,
            pendingTasksCount = 0,
            topTasks = emptyList()
        )
    }
}
