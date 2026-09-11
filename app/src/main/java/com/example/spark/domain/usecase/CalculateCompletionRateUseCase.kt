package com.example.spark.domain.usecase

import com.example.spark.data.local.entity.HabitLogEntity
import com.example.spark.data.local.entity.HabitWithLogs
import java.time.LocalDate

/**
 * Calculates the completion rate (percentage) for habits
 * over a given time window.
 *
 * Pure Kotlin logic — no Android dependencies, easily unit-testable.
 */
class CalculateCompletionRateUseCase {

    /**
     * Calculates the completion percentage for a single day.
     *
     * @param habitsWithLogs All active habits with their log entries.
     * @param date The target date to calculate completion for.
     * @return Completion percentage as an integer (0–100).
     */
    fun forDate(
        habitsWithLogs: List<HabitWithLogs>,
        date: LocalDate = LocalDate.now()
    ): Int {
        if (habitsWithLogs.isEmpty()) return 0

        val totalHabits = habitsWithLogs.size
        val completedHabits = habitsWithLogs.count { habitWithLogs ->
            habitWithLogs.logs.any { log ->
                log.completedDate == date && log.isCompleted
            }
        }

        return ((completedHabits.toFloat() / totalHabits) * 100).toInt()
    }

    /**
     * Calculates the average completion percentage over a trailing window.
     *
     * @param habitsWithLogs All active habits with their log entries.
     * @param days Number of trailing days to compute (default 7).
     * @param endDate The most recent date in the window.
     * @return Average completion percentage as an integer (0–100).
     */
    fun forTrailingDays(
        habitsWithLogs: List<HabitWithLogs>,
        days: Int = 7,
        endDate: LocalDate = LocalDate.now()
    ): Int {
        if (habitsWithLogs.isEmpty() || days <= 0) return 0

        var totalPercentage = 0
        for (i in 0 until days) {
            val date = endDate.minusDays(i.toLong())
            totalPercentage += forDate(habitsWithLogs, date)
        }

        return totalPercentage / days
    }
}
