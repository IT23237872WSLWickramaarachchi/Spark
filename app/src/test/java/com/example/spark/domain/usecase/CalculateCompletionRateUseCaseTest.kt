package com.example.spark.domain.usecase

import com.example.spark.data.local.entity.HabitEntity
import com.example.spark.data.local.entity.HabitLogEntity
import com.example.spark.data.local.entity.HabitWithLogs
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Pure JUnit unit tests for [CalculateCompletionRateUseCase].
 * Zero Android or Robolectric dependencies.
 */
class CalculateCompletionRateUseCaseTest {

    private lateinit var useCase: CalculateCompletionRateUseCase
    private val today = LocalDate.of(2026, 9, 12)

    @Before
    fun setUp() {
        useCase = CalculateCompletionRateUseCase()
    }

    @Test
    fun forDate_emptyHabits_returnsZero() {
        val rate = useCase.forDate(emptyList(), today)
        assertEquals(0, rate)
    }

    @Test
    fun forDate_allHabitsCompleted_returns100() {
        val habit1 = HabitEntity(id = 1L, userId = 1L, title = "Exercise", category = "Health")
        val habit2 = HabitEntity(id = 2L, userId = 1L, title = "Read", category = "Mindfulness")

        val logs1 = listOf(HabitLogEntity(id = 1L, habitId = 1L, completedDate = today, isCompleted = true))
        val logs2 = listOf(HabitLogEntity(id = 2L, habitId = 2L, completedDate = today, isCompleted = true))

        val habitsWithLogs = listOf(
            HabitWithLogs(habit1, logs1),
            HabitWithLogs(habit2, logs2)
        )

        val rate = useCase.forDate(habitsWithLogs, today)
        assertEquals(100, rate)
    }

    @Test
    fun forDate_partialCompletion_returnsAccuratePercent() {
        val habit1 = HabitEntity(id = 1L, userId = 1L, title = "Habit 1", category = "Health")
        val habit2 = HabitEntity(id = 2L, userId = 1L, title = "Habit 2", category = "Health")
        val habit3 = HabitEntity(id = 3L, userId = 1L, title = "Habit 3", category = "Health")

        // Only habit1 completed today
        val habitsWithLogs = listOf(
            HabitWithLogs(habit1, listOf(HabitLogEntity(id = 1L, habitId = 1L, completedDate = today, isCompleted = true))),
            HabitWithLogs(habit2, listOf(HabitLogEntity(id = 2L, habitId = 2L, completedDate = today, isCompleted = false))),
            HabitWithLogs(habit3, emptyList())
        )

        val rate = useCase.forDate(habitsWithLogs, today)
        // 1 out of 3 = 33%
        assertEquals(33, rate)
    }

    @Test
    fun forDate_zeroCompletions_returnsZero() {
        val habit = HabitEntity(id = 1L, userId = 1L, title = "Habit 1", category = "Health")
        val habitsWithLogs = listOf(
            HabitWithLogs(habit, listOf(HabitLogEntity(id = 1L, habitId = 1L, completedDate = today, isCompleted = false)))
        )

        val rate = useCase.forDate(habitsWithLogs, today)
        assertEquals(0, rate)
    }

    @Test
    fun forTrailingDays_calculatesAverageAccurately() {
        val habit1 = HabitEntity(id = 1L, userId = 1L, title = "Habit 1", category = "Health")
        val habit2 = HabitEntity(id = 2L, userId = 1L, title = "Habit 2", category = "Health")

        // Day 0 (today): 2/2 completed = 100%
        // Day 1 (yesterday): 1/2 completed = 50%
        // Day 2 (2 days ago): 0/2 completed = 0%
        val logs1 = listOf(
            HabitLogEntity(id = 1L, habitId = 1L, completedDate = today, isCompleted = true),
            HabitLogEntity(id = 2L, habitId = 1L, completedDate = today.minusDays(1), isCompleted = true)
        )
        val logs2 = listOf(
            HabitLogEntity(id = 3L, habitId = 2L, completedDate = today, isCompleted = true),
            HabitLogEntity(id = 4L, habitId = 2L, completedDate = today.minusDays(1), isCompleted = false)
        )

        val habitsWithLogs = listOf(
            HabitWithLogs(habit1, logs1),
            HabitWithLogs(habit2, logs2)
        )

        val avgRate = useCase.forTrailingDays(habitsWithLogs, days = 3, endDate = today)
        // (100 + 50 + 0) / 3 = 50%
        assertEquals(50, avgRate)
    }

    @Test
    fun forTrailingDays_invalidDaysOrEmpty_returnsZero() {
        assertEquals(0, useCase.forTrailingDays(emptyList(), days = 7))
        assertEquals(0, useCase.forTrailingDays(emptyList(), days = 0))
        assertEquals(0, useCase.forTrailingDays(emptyList(), days = -1))
    }

    @Test
    fun forPeriod_withLogsList_calculatesAccurateRate() {
        // 2 active habits across a 5-day window = 10 scheduled opportunities
        val logs = listOf(
            HabitLogEntity(id = 1L, habitId = 1L, completedDate = today, isCompleted = true),
            HabitLogEntity(id = 2L, habitId = 1L, completedDate = today.minusDays(1), isCompleted = true),
            HabitLogEntity(id = 3L, habitId = 1L, completedDate = today.minusDays(2), isCompleted = true),
            HabitLogEntity(id = 4L, habitId = 1L, completedDate = today.minusDays(3), isCompleted = true),
            HabitLogEntity(id = 5L, habitId = 2L, completedDate = today, isCompleted = true),
            HabitLogEntity(id = 6L, habitId = 2L, completedDate = today.minusDays(1), isCompleted = true),
            HabitLogEntity(id = 7L, habitId = 2L, completedDate = today.minusDays(2), isCompleted = true)
        )
        // 7 completions out of 10 = 70%
        val rate = useCase.forPeriod(totalActiveHabits = 2, logsInPeriod = logs, totalDays = 5)
        assertEquals(70, rate)
    }

    @Test
    fun forDay_withDirectCounts_calculatesAccurateRate() {
        // 4 total habits, 3 completed -> 75%
        val rate = useCase.forDay(totalActiveHabits = 4, completedLogsForDay = 3)
        assertEquals(75, rate)
    }

    @Test
    fun calculate_divisionByZeroGuards_returnsZero() {
        assertEquals(0, useCase.calculate(totalHabits = 0, completedLogsCount = 5, totalDays = 1))
        assertEquals(0, useCase.calculate(totalHabits = 5, completedLogsCount = 0, totalDays = 1))
        assertEquals(0, useCase.calculate(totalHabits = 5, completedLogsCount = 5, totalDays = 0))
    }
}
