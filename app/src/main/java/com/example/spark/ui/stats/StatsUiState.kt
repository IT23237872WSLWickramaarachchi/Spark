package com.example.spark.ui.stats

import com.example.spark.ui.custom.DayStatus
import java.time.LocalDate

/**
 * Period toggle for statistics calculations.
 */
enum class Period {
    WEEK,
    MONTH
}

typealias StatsPeriod = Period

/**
 * Daily habit completion status and percentage for the 7-day strip.
 */
data class DayStat(
    val date: LocalDate,
    val dayLabel: String,
    val status: DayStatus,
    val completionPercent: Int,
    val isToday: Boolean
)

/**
 * Individual data point for the MPAndroidChart mood trend curve.
 */
data class MoodChartPoint(
    val x: Float,
    val y: Float,
    val date: LocalDate
)

/**
 * Immutable UI state for the Stats screen.
 * Contains [completionPercent], [dayLogStatus], [moodTrendPoints], [currentStreak], and [todaysRate].
 */
data class StatsUiState(
    val period: Period = Period.WEEK,
    val completionPercent: Int = 0,
    val dayLogStatus: List<Boolean> = emptyList(),
    val moodTrendPoints: List<Float> = emptyList(),
    val currentStreak: Int = 0,
    val todaysRate: Int = 0,
    // Auxiliary and backward-compatible fields
    val completionRatePercent: Int = completionPercent,
    val completionSubtitle: String = if (period == Period.WEEK) "This Week" else "Past 30 Days",
    val dayStats: List<DayStat> = emptyList(),
    val moodPoints: List<MoodChartPoint> = emptyList(),
    val moodTrendStatus: String = "Stable",
    val isTrendPositive: Boolean = true,
    val streakDays: Int = currentStreak,
    val todayCompletedCount: Int = 0,
    val todayTotalCount: Int = 0
) {
    companion object {
        fun initial(): StatsUiState = StatsUiState()
    }
}
