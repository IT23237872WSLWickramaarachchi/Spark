package com.example.spark

import com.example.spark.data.entity.HabitCompletionEntity
import com.example.spark.data.entity.HabitEntity
import com.example.spark.data.entity.MoodEntryEntity
import com.example.spark.util.StatisticsCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatisticsCalculatorTest {

    @Test
    fun calculateDailySuccessRate_normalAndEdgeCases() {
        assertEquals(0.0, StatisticsCalculator.calculateDailySuccessRate(0, 0), 0.001)
        assertEquals(0.0, StatisticsCalculator.calculateDailySuccessRate(0, 5), 0.001)
        assertEquals(50.0, StatisticsCalculator.calculateDailySuccessRate(2, 4), 0.001)
        assertEquals(100.0, StatisticsCalculator.calculateDailySuccessRate(5, 5), 0.001)
        assertEquals(80.0, StatisticsCalculator.calculateDailySuccessRate(4, 5), 0.001)
    }

    @Test
    fun calculatePeriodSuccessRate_calculatesAccurately() {
        val habit1 = HabitEntity(id = 1L, userId = 1L, name = "Habit 1", category = "Health")
        val habit2 = HabitEntity(id = 2L, userId = 1L, name = "Habit 2", category = "Fitness")
        val activeHabits = listOf(habit1, habit2)

        val dates = listOf("2026-09-14", "2026-09-15", "2026-09-16", "2026-09-17", "2026-09-18", "2026-09-19", "2026-09-20")

        // 7 days * 2 habits = 14 total applicable occurrences
        // Suppose 7 completions exist within this range
        val completions = listOf(
            HabitCompletionEntity(habitId = 1L, completionDate = "2026-09-14", completed = true),
            HabitCompletionEntity(habitId = 2L, completionDate = "2026-09-14", completed = true),
            HabitCompletionEntity(habitId = 1L, completionDate = "2026-09-15", completed = true),
            HabitCompletionEntity(habitId = 1L, completionDate = "2026-09-16", completed = true),
            HabitCompletionEntity(habitId = 2L, completionDate = "2026-09-16", completed = true),
            HabitCompletionEntity(habitId = 1L, completionDate = "2026-09-17", completed = true),
            HabitCompletionEntity(habitId = 2L, completionDate = "2026-09-17", completed = true),
            // Completion outside date range (should be ignored)
            HabitCompletionEntity(habitId = 1L, completionDate = "2026-09-01", completed = true),
            // Completion for unassociated habit (should be ignored)
            HabitCompletionEntity(habitId = 99L, completionDate = "2026-09-15", completed = true)
        )

        val rate = StatisticsCalculator.calculatePeriodSuccessRate(dates, activeHabits, completions)
        // 7 out of 14 = 50%
        assertEquals(50, rate)
    }

    @Test
    fun calculatePeriodSuccessRate_emptyHabitsOrDates_returnsZero() {
        val rate1 = StatisticsCalculator.calculatePeriodSuccessRate(emptyList(), emptyList(), emptyList())
        assertEquals(0, rate1)

        val dates = listOf("2026-09-14")
        val rate2 = StatisticsCalculator.calculatePeriodSuccessRate(dates, emptyList(), emptyList())
        assertEquals(0, rate2)
    }

    @Test
    fun calculateTodayRate_returnsCompletedAndTotal() {
        val today = "2026-09-16"
        val habit1 = HabitEntity(id = 1L, userId = 1L, name = "Habit 1", category = "Health")
        val habit2 = HabitEntity(id = 2L, userId = 1L, name = "Habit 2", category = "Health")
        val habit3 = HabitEntity(id = 3L, userId = 1L, name = "Habit 3", category = "Health")
        val habits = listOf(habit1, habit2, habit3)

        val completions = listOf(
            HabitCompletionEntity(habitId = 1L, completionDate = today, completed = true),
            HabitCompletionEntity(habitId = 2L, completionDate = today, completed = true),
            HabitCompletionEntity(habitId = 3L, completionDate = "2026-09-15", completed = true) // yesterday
        )

        val (completed, total) = StatisticsCalculator.calculateTodayRate(today, habits, completions)
        assertEquals(2, completed)
        assertEquals(3, total)
    }

    @Test
    fun calculateMoodAverage_andTrendLabel() {
        assertNull(StatisticsCalculator.calculateMoodAverage(emptyList()))
        assertEquals("No Data", StatisticsCalculator.getMoodTrendLabel(null))

        val moods = listOf(
            MoodEntryEntity(id = 1, userId = 1, date = "2026-09-14", moodLevel = 4),
            MoodEntryEntity(id = 2, userId = 1, date = "2026-09-15", moodLevel = 5),
            MoodEntryEntity(id = 3, userId = 1, date = "2026-09-16", moodLevel = 4)
        )
        val avg = StatisticsCalculator.calculateMoodAverage(moods)
        assertEquals(4.333, avg!!, 0.01)
        assertEquals("Positive", StatisticsCalculator.getMoodTrendLabel(avg))

        val neutralMoods = listOf(
            MoodEntryEntity(id = 4, userId = 1, date = "2026-09-14", moodLevel = 3),
            MoodEntryEntity(id = 5, userId = 1, date = "2026-09-15", moodLevel = 3)
        )
        val neutralAvg = StatisticsCalculator.calculateMoodAverage(neutralMoods)
        assertEquals("Neutral", StatisticsCalculator.getMoodTrendLabel(neutralAvg))

        val lowMoods = listOf(
            MoodEntryEntity(id = 6, userId = 1, date = "2026-09-14", moodLevel = 1),
            MoodEntryEntity(id = 7, userId = 1, date = "2026-09-15", moodLevel = 2)
        )
        val lowAvg = StatisticsCalculator.calculateMoodAverage(lowMoods)
        assertEquals("Low", StatisticsCalculator.getMoodTrendLabel(lowAvg))
    }
}
