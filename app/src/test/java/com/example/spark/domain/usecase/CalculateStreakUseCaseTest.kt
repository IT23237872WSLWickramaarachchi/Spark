package com.example.spark.domain.usecase

import com.example.spark.data.local.entity.HabitLogEntity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Pure JUnit unit tests for [CalculateStreakUseCase].
 * Zero Android or Robolectric dependencies.
 */
class CalculateStreakUseCaseTest {

    private lateinit var useCase: CalculateStreakUseCase
    private val today = LocalDate.of(2026, 9, 12)

    @Before
    fun setUp() {
        useCase = CalculateStreakUseCase()
    }

    @Test
    fun emptyLogList_returnsStreakZero() {
        val result = useCase.execute(emptyList(), today)
        assertEquals(0, result.currentStreak)
        assertEquals(0, result.bestStreak)
    }

    @Test
    fun logsWithNoCompletions_returnsStreakZero() {
        val logs = listOf(
            HabitLogEntity(id = 1L, habitId = 1L, completedDate = today, isCompleted = false),
            HabitLogEntity(id = 2L, habitId = 1L, completedDate = today.minusDays(1), isCompleted = false)
        )
        val result = useCase.execute(logs, today)
        assertEquals(0, result.currentStreak)
        assertEquals(0, result.bestStreak)
    }

    @Test
    fun singleCompletedDay_completedToday_returnsStreakOne() {
        val logs = listOf(
            HabitLogEntity(id = 1L, habitId = 1L, completedDate = today, isCompleted = true)
        )
        val result = useCase.execute(logs, today)
        assertEquals(1, result.currentStreak)
        assertEquals(1, result.bestStreak)
    }

    @Test
    fun singleCompletedDay_completedYesterday_returnsStreakOne() {
        // User hasn't logged today yet; streak starting yesterday is preserved
        val logs = listOf(
            HabitLogEntity(id = 1L, habitId = 1L, completedDate = today.minusDays(1), isCompleted = true)
        )
        val result = useCase.execute(logs, today)
        assertEquals(1, result.currentStreak)
        assertEquals(1, result.bestStreak)
    }

    @Test
    fun singleCompletedDay_twoDaysAgo_returnsCurrentStreakZeroAndBestStreakOne() {
        // Completed 2 days ago, but yesterday and today were missed
        val logs = listOf(
            HabitLogEntity(id = 1L, habitId = 1L, completedDate = today.minusDays(2), isCompleted = true)
        )
        val result = useCase.execute(logs, today)
        assertEquals(0, result.currentStreak)
        assertEquals(1, result.bestStreak)
    }

    @Test
    fun fiveConsecutiveCompletedDays_returnsStreakFive() {
        val logs = (0L..4L).map { offset ->
            HabitLogEntity(
                id = offset + 1,
                habitId = 1L,
                completedDate = today.minusDays(offset),
                isCompleted = true
            )
        }
        val result = useCase.execute(logs, today)
        assertEquals(5, result.currentStreak)
        assertEquals(5, result.bestStreak)
    }

    @Test
    fun gapInTheMiddle_currentResetsToUnbrokenRun_bestReflectsLongestHistoricalRun() {
        // Historical unbroken run: 7 consecutive days (from 15 days ago to 9 days ago)
        val historicalRun = (9L..15L).map { offset ->
            HabitLogEntity(
                id = offset,
                habitId = 1L,
                completedDate = today.minusDays(offset),
                isCompleted = true
            )
        }

        // Gap: 8, 7, 6, 5, 4, 3 days ago are missed

        // Recent unbroken run: 3 consecutive days (today, yesterday, 2 days ago)
        val recentRun = (0L..2L).map { offset ->
            HabitLogEntity(
                id = offset + 100,
                habitId = 1L,
                completedDate = today.minusDays(offset),
                isCompleted = true
            )
        }

        // Shuffle logs to ensure order-independence
        val logs = (historicalRun + recentRun).shuffled()

        val result = useCase.execute(logs, today)
        assertEquals("Current streak must count only the unbroken run", 3, result.currentStreak)
        assertEquals("Best streak must retain the longest historical run", 7, result.bestStreak)
    }

    @Test
    fun multipleHistoricalRuns_bestStreakIsMaximumOfAllRuns() {
        // Run 1: 4 consecutive days (30..27 days ago)
        val run1 = (27L..30L).map { offset ->
            HabitLogEntity(id = offset, habitId = 1L, completedDate = today.minusDays(offset), isCompleted = true)
        }
        // Run 2: 6 consecutive days (20..15 days ago) -> Best historical
        val run2 = (15L..20L).map { offset ->
            HabitLogEntity(id = offset, habitId = 1L, completedDate = today.minusDays(offset), isCompleted = true)
        }
        // Run 3: 2 consecutive days (yesterday and today) -> Current
        val run3 = (0L..1L).map { offset ->
            HabitLogEntity(id = offset, habitId = 1L, completedDate = today.minusDays(offset), isCompleted = true)
        }

        val logs = (run1 + run2 + run3).shuffled()
        val result = useCase.execute(logs, today)

        assertEquals(2, result.currentStreak)
        assertEquals(6, result.bestStreak)
    }

    @Test
    fun duplicateLogsOnSameDay_handledDeduplicated() {
        val logs = listOf(
            HabitLogEntity(id = 1L, habitId = 1L, completedDate = today, isCompleted = true),
            HabitLogEntity(id = 2L, habitId = 1L, completedDate = today, isCompleted = true),
            HabitLogEntity(id = 3L, habitId = 1L, completedDate = today.minusDays(1), isCompleted = true),
            HabitLogEntity(id = 4L, habitId = 1L, completedDate = today.minusDays(1), isCompleted = true)
        )
        val result = useCase.execute(logs, today)
        assertEquals(2, result.currentStreak)
        assertEquals(2, result.bestStreak)
    }
}
