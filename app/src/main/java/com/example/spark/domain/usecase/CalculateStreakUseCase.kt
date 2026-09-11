package com.example.spark.domain.usecase

import com.example.spark.data.local.entity.HabitLogEntity
import java.time.LocalDate

/**
 * Calculates the current and best streaks for a given habit
 * based on its completion log entries.
 *
 * A "streak" is a consecutive run of days where the habit was completed.
 * Streak resets when a day is missed (no log entry or isCompleted == false).
 *
 * Pure Kotlin logic — no Android dependencies, easily unit-testable.
 */
class CalculateStreakUseCase {

    /**
     * Data class holding both the current active streak and the
     * all-time best streak for a habit.
     */
    data class StreakResult(
        val currentStreak: Int,
        val bestStreak: Int
    )

    /**
     * Computes streak information from a list of habit log entries.
     *
     * @param logs All completion logs for a single habit, in any order.
     * @param today The reference date for calculating "current" streak
     *              (defaults to today).
     * @return [StreakResult] containing current and best streak counts.
     */
    fun execute(
        logs: List<HabitLogEntity>,
        today: LocalDate = LocalDate.now()
    ): StreakResult {
        if (logs.isEmpty()) return StreakResult(currentStreak = 0, bestStreak = 0)

        // Extract unique completed dates, sorted descending (most recent first)
        val completedDates = logs
            .filter { it.isCompleted }
            .map { it.completedDate }
            .distinct()
            .sortedDescending()

        if (completedDates.isEmpty()) return StreakResult(currentStreak = 0, bestStreak = 0)

        // --- Current Streak ---
        // Count consecutive days backwards from today
        var currentStreak = 0
        var checkDate = today

        // Allow the current streak to start from today or yesterday
        // (user may not have logged today yet)
        if (completedDates.first() != today && completedDates.first() != today.minusDays(1)) {
            currentStreak = 0
        } else {
            if (completedDates.first() == today.minusDays(1)) {
                checkDate = today.minusDays(1)
            }
            for (date in completedDates) {
                if (date == checkDate) {
                    currentStreak++
                    checkDate = checkDate.minusDays(1)
                } else if (date.isBefore(checkDate)) {
                    break
                }
            }
        }

        // --- Best Streak ---
        // Walk through all completed dates chronologically
        val sortedAsc = completedDates.sortedDescending().reversed()
        var bestStreak = 0
        var tempStreak = 1

        for (i in 1 until sortedAsc.size) {
            if (sortedAsc[i] == sortedAsc[i - 1].plusDays(1)) {
                tempStreak++
            } else {
                bestStreak = maxOf(bestStreak, tempStreak)
                tempStreak = 1
            }
        }
        bestStreak = maxOf(bestStreak, tempStreak)

        // Ensure best streak is at least as large as current streak
        bestStreak = maxOf(bestStreak, currentStreak)

        return StreakResult(currentStreak = currentStreak, bestStreak = bestStreak)
    }
}
