package com.example.spark.work

import com.example.spark.data.local.entity.HabitEntity
import com.example.spark.data.local.entity.HabitLogEntity
import com.example.spark.data.local.entity.UserPrefsEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class HabitReminderWorkerTest {

    // ── Scheduling Window Unit Tests ────────────────────────────────────

    @Test
    fun isReminderMatching_dueRecently_returnsTrue() {
        val reminder = LocalTime.of(8, 0)
        val now = LocalTime.of(8, 7) // 7 min after reminder
        assertTrue(HabitReminderWorker.isReminderMatching(reminder, now, 15))
    }

    @Test
    fun isReminderMatching_exactMatch_returnsTrue() {
        val reminder = LocalTime.of(8, 0)
        val now = LocalTime.of(8, 0)
        assertTrue(HabitReminderWorker.isReminderMatching(reminder, now, 15))
    }

    @Test
    fun isReminderMatching_dueInNext3Minutes_returnsTrue() {
        val reminder = LocalTime.of(8, 5)
        val now = LocalTime.of(8, 2) // 3 min before reminder (within drift window)
        assertTrue(HabitReminderWorker.isReminderMatching(reminder, now, 15))
    }

    @Test
    fun isReminderMatching_dueHalfHourAgo_returnsFalse() {
        val reminder = LocalTime.of(8, 0)
        val now = LocalTime.of(8, 30) // 30 min after reminder
        assertFalse(HabitReminderWorker.isReminderMatching(reminder, now, 15))
    }

    @Test
    fun isReminderMatching_dueHoursAhead_returnsFalse() {
        val reminder = LocalTime.of(18, 0)
        val now = LocalTime.of(8, 0)
        assertFalse(HabitReminderWorker.isReminderMatching(reminder, now, 15))
    }

    @Test
    fun isReminderMatching_midnightWrap_returnsTrue() {
        val reminder = LocalTime.of(23, 58)
        val now = LocalTime.of(0, 3) // 5 minutes across midnight
        assertTrue(HabitReminderWorker.isReminderMatching(reminder, now, 15))
    }

    // ── User Preference Check Unit Tests ────────────────────────────────

    @Test
    fun shouldRunReminders_whenNull_defaultsToTrue() {
        assertTrue(HabitReminderWorker.shouldRunReminders(null))
    }

    @Test
    fun shouldRunReminders_whenDisabled_returnsFalse() {
        val prefs = UserPrefsEntity(userId = 1L, notificationsEnabled = false)
        assertFalse(HabitReminderWorker.shouldRunReminders(prefs))
    }

    @Test
    fun shouldRunReminders_whenEnabled_returnsTrue() {
        val prefs = UserPrefsEntity(userId = 1L, notificationsEnabled = true)
        assertTrue(HabitReminderWorker.shouldRunReminders(prefs))
    }

    // ── Incomplete Habit Filtering Unit Tests ───────────────────────────

    @Test
    fun filterDueIncompleteHabits_dueAndIncomplete_included() {
        val today = LocalDate.of(2026, 9, 12)
        val now = LocalTime.of(9, 0)

        val habit = HabitEntity(
            id = 1L,
            userId = 1L,
            title = "Morning Meditation",
            category = "Mindfulness",
            reminderTime = LocalTime.of(8, 55)
        )

        val result = HabitReminderWorker.filterDueIncompleteHabits(
            allHabits = listOf(habit),
            todayLogs = emptyList(),
            today = today,
            currentTime = now
        )

        assertEquals(1, result.size)
        assertEquals("Morning Meditation", result.first().title)
    }

    @Test
    fun filterDueIncompleteHabits_dueAndCompletedToday_excluded() {
        val today = LocalDate.of(2026, 9, 12)
        val now = LocalTime.of(9, 0)

        val habit = HabitEntity(
            id = 1L,
            userId = 1L,
            title = "Morning Meditation",
            category = "Mindfulness",
            reminderTime = LocalTime.of(8, 55)
        )

        val log = HabitLogEntity(
            id = 10L,
            habitId = 1L,
            completedDate = today,
            isCompleted = true
        )

        val result = HabitReminderWorker.filterDueIncompleteHabits(
            allHabits = listOf(habit),
            todayLogs = listOf(log),
            today = today,
            currentTime = now
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun filterDueIncompleteHabits_dueAndCompletedYesterday_included() {
        val today = LocalDate.of(2026, 9, 12)
        val now = LocalTime.of(9, 0)

        val habit = HabitEntity(
            id = 1L,
            userId = 1L,
            title = "Drink Water",
            category = "Health",
            reminderTime = LocalTime.of(9, 0)
        )

        val yesterdayLog = HabitLogEntity(
            id = 10L,
            habitId = 1L,
            completedDate = today.minusDays(1),
            isCompleted = true
        )

        val result = HabitReminderWorker.filterDueIncompleteHabits(
            allHabits = listOf(habit),
            todayLogs = listOf(yesterdayLog),
            today = today,
            currentTime = now
        )

        assertEquals(1, result.size)
        assertEquals(1L, result.first().id)
    }

    @Test
    fun filterDueIncompleteHabits_uncompletedLogToday_included() {
        val today = LocalDate.of(2026, 9, 12)
        val now = LocalTime.of(9, 0)

        val habit = HabitEntity(
            id = 1L,
            userId = 1L,
            title = "Read",
            category = "Productivity",
            reminderTime = LocalTime.of(9, 0)
        )

        val uncompletedLog = HabitLogEntity(
            id = 10L,
            habitId = 1L,
            completedDate = today,
            isCompleted = false
        )

        val result = HabitReminderWorker.filterDueIncompleteHabits(
            allHabits = listOf(habit),
            todayLogs = listOf(uncompletedLog),
            today = today,
            currentTime = now
        )

        assertEquals(1, result.size)
        assertEquals("Read", result.first().title)
    }

    @Test
    fun filterDueIncompleteHabits_archived_excluded() {
        val today = LocalDate.of(2026, 9, 12)
        val now = LocalTime.of(9, 0)

        val habit = HabitEntity(
            id = 1L,
            userId = 1L,
            title = "Archived Habit",
            category = "Health",
            reminderTime = LocalTime.of(9, 0),
            isArchived = true
        )

        val result = HabitReminderWorker.filterDueIncompleteHabits(
            allHabits = listOf(habit),
            todayLogs = emptyList(),
            today = today,
            currentTime = now
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun filterDueIncompleteHabits_noReminderTime_excluded() {
        val today = LocalDate.of(2026, 9, 12)
        val now = LocalTime.of(9, 0)

        val habit = HabitEntity(
            id = 1L,
            userId = 1L,
            title = "Untimed Habit",
            category = "Health",
            reminderTime = null
        )

        val result = HabitReminderWorker.filterDueIncompleteHabits(
            allHabits = listOf(habit),
            todayLogs = emptyList(),
            today = today,
            currentTime = now
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun filterDueIncompleteHabits_differentTime_excluded() {
        val today = LocalDate.of(2026, 9, 12)
        val now = LocalTime.of(9, 0)

        val habit = HabitEntity(
            id = 1L,
            userId = 1L,
            title = "Evening Habit",
            category = "Health",
            reminderTime = LocalTime.of(20, 0)
        )

        val result = HabitReminderWorker.filterDueIncompleteHabits(
            allHabits = listOf(habit),
            todayLogs = emptyList(),
            today = today,
            currentTime = now
        )

        assertTrue(result.isEmpty())
    }
}
