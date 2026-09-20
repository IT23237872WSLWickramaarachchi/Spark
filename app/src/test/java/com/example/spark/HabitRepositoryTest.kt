package com.example.spark

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.spark.data.dao.HabitCompletionDao
import com.example.spark.data.dao.HabitDao
import com.example.spark.data.entity.HabitCompletionEntity
import com.example.spark.data.entity.HabitEntity
import com.example.spark.data.repository.HabitRepository
import com.example.spark.model.HabitItemUiModel
import com.example.spark.util.DateUtils
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HabitRepositoryTest {

    private lateinit var fakeHabitDao: FakeHabitDao
    private lateinit var fakeCompletionDao: FakeCompletionDao
    private lateinit var habitRepository: HabitRepository

    class FakeHabitDao : HabitDao {
        private val habits = mutableMapOf<Long, HabitEntity>()
        private var currentId = 1L

        override suspend fun insertHabit(habit: HabitEntity): Long {
            val id = currentId++
            habits[id] = habit.copy(id = id)
            return id
        }

        override suspend fun updateHabit(habit: HabitEntity) {
            habits[habit.id] = habit
        }

        override suspend fun deleteHabit(habit: HabitEntity) {
            habits.remove(habit.id)
        }

        override suspend fun deleteHabitById(habitId: Long) {
            habits.remove(habitId)
        }

        override suspend fun getHabitById(habitId: Long): HabitEntity? {
            return habits[habitId]
        }

        override fun getActiveHabitsLiveData(userId: Long): LiveData<List<HabitEntity>> {
            return MutableLiveData(habits.values.filter { it.userId == userId && !it.isArchived })
        }

        override suspend fun getActiveHabits(userId: Long): List<HabitEntity> {
            return habits.values.filter { it.userId == userId && !it.isArchived }
        }

        override fun getAllHabitsLiveData(userId: Long): LiveData<List<HabitEntity>> {
            return MutableLiveData(habits.values.filter { it.userId == userId })
        }

        override suspend fun setArchived(habitId: Long, archived: Boolean) {
            val h = habits[habitId]
            if (h != null) {
                habits[habitId] = h.copy(isArchived = archived)
            }
        }

        override suspend fun updateHabitNotes(habitId: Long, notes: String?) {
            val h = habits[habitId]
            if (h != null) {
                habits[habitId] = h.copy(notes = notes)
            }
        }

        override suspend fun getAllActiveReminders(): List<HabitEntity> {
            return habits.values.filter { !it.isArchived && it.reminderEnabled }
        }
    }

    class FakeCompletionDao : HabitCompletionDao {
        private val completions = mutableListOf<HabitCompletionEntity>()
        private var currentId = 1L

        override suspend fun insertOrUpdate(completion: HabitCompletionEntity): Long {
            val existing = completions.indexOfFirst {
                it.habitId == completion.habitId && it.completionDate == completion.completionDate
            }
            return if (existing != -1) {
                completions[existing] = completion.copy(id = completions[existing].id)
                completions[existing].id
            } else {
                val id = currentId++
                completions.add(completion.copy(id = id))
                id
            }
        }

        override suspend fun deleteCompletion(habitId: Long, date: String) {
            completions.removeAll { it.habitId == habitId && it.completionDate == date }
        }

        override suspend fun getCompletion(habitId: Long, date: String): HabitCompletionEntity? {
            return completions.find { it.habitId == habitId && it.completionDate == date }
        }

        override fun getCompletionLiveData(habitId: Long, date: String): LiveData<HabitCompletionEntity?> {
            return MutableLiveData(completions.find { it.habitId == habitId && it.completionDate == date })
        }

        override suspend fun getCompletionsForHabit(habitId: Long): List<HabitCompletionEntity> {
            return completions.filter { it.habitId == habitId }
        }

        override fun getCompletionsForHabitLiveData(habitId: Long): LiveData<List<HabitCompletionEntity>> {
            return MutableLiveData(completions.filter { it.habitId == habitId })
        }

        override fun getCompletionsForUserAndDateLiveData(userId: Long, date: String): LiveData<List<HabitCompletionEntity>> {
            return MutableLiveData(completions.filter { it.completionDate == date })
        }

        override suspend fun getCompletionsForUserAndDate(userId: Long, date: String): List<HabitCompletionEntity> {
            return completions.filter { it.completionDate == date }
        }

        override suspend fun getCompletionsForUserBetween(userId: Long, startDate: String, endDate: String): List<HabitCompletionEntity> {
            return completions.filter { it.completionDate in startDate..endDate }
        }

        override fun getCompletionsForUserBetweenLiveData(userId: Long, startDate: String, endDate: String): LiveData<List<HabitCompletionEntity>> {
            return MutableLiveData(completions.filter { it.completionDate in startDate..endDate })
        }

        override suspend fun getTotalCompletedCountForHabit(habitId: Long): Int {
            return completions.count { it.habitId == habitId && it.completed }
        }

        override suspend fun deleteDemoCompletions(userId: Long): Int {
            val count = completions.count { it.isDemo }
            completions.removeAll { it.isDemo }
            return count
        }

        override suspend fun getDemoCompletionCount(userId: Long): Int {
            return completions.count { it.isDemo }
        }
    }

    @Before
    fun setUp() {
        fakeHabitDao = FakeHabitDao()
        fakeCompletionDao = FakeCompletionDao()
        habitRepository = HabitRepository(fakeHabitDao, fakeCompletionDao)
    }

    @Test
    fun toggleHabitCompletion_togglesOnAndOff() = runBlocking {
        val today = DateUtils.getTodayDateString()

        // Toggle on
        val result1 = habitRepository.toggleHabitCompletion(1L, today)
        assertTrue(result1)
        assertTrue(habitRepository.isCompletedOn(1L, today))

        // Toggle off
        val result2 = habitRepository.toggleHabitCompletion(1L, today)
        assertFalse(result2)
        assertFalse(habitRepository.isCompletedOn(1L, today))
    }

    @Test
    fun calculateCurrentStreak_consecutiveDays_returnsCorrectStreak() = runBlocking {
        val habitId = 1L
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        // Completed today
        val todayStr = sdf.format(cal.time)
        fakeCompletionDao.insertOrUpdate(HabitCompletionEntity(habitId = habitId, completionDate = todayStr, completed = true))

        // Completed yesterday
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)
        fakeCompletionDao.insertOrUpdate(HabitCompletionEntity(habitId = habitId, completionDate = yesterdayStr, completed = true))

        // Completed 2 days ago
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val twoDaysAgoStr = sdf.format(cal.time)
        fakeCompletionDao.insertOrUpdate(HabitCompletionEntity(habitId = habitId, completionDate = twoDaysAgoStr, completed = true))

        val streak = habitRepository.calculateCurrentStreak(habitId)
        assertEquals(3, streak)
    }

    @Test
    fun calculateBestStreak_computesLongestConsecutiveRecord() = runBlocking {
        val habitId = 10L
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        // Streak 1: 5 consecutive days 20 days ago
        cal.add(Calendar.DAY_OF_YEAR, -20)
        for (i in 0 until 5) {
            fakeCompletionDao.insertOrUpdate(
                HabitCompletionEntity(habitId = habitId, completionDate = sdf.format(cal.time), completed = true)
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Streak 2: 2 consecutive days (yesterday and today)
        val calNow = Calendar.getInstance()
        fakeCompletionDao.insertOrUpdate(
            HabitCompletionEntity(habitId = habitId, completionDate = sdf.format(calNow.time), completed = true)
        )
        calNow.add(Calendar.DAY_OF_YEAR, -1)
        fakeCompletionDao.insertOrUpdate(
            HabitCompletionEntity(habitId = habitId, completionDate = sdf.format(calNow.time), completed = true)
        )

        val currentStreak = habitRepository.calculateCurrentStreak(habitId)
        val bestStreak = habitRepository.calculateBestStreak(habitId)

        assertEquals(2, currentStreak)
        assertEquals(5, bestStreak)
    }

    @Test
    fun updateHabitNotes_persistsNoteText() = runBlocking {
        val habit = HabitEntity(
            userId = 1L,
            name = "Morning Walk",
            category = "Fitness",
            notes = "Initial note"
        )
        val id = fakeHabitDao.insertHabit(habit)

        habitRepository.updateHabitNotes(id, "Updated progress notes for week 1")
        val updated = habitRepository.getHabitById(id)

        assertEquals("Updated progress notes for week 1", updated?.notes)
    }

    @Test
    fun calculateCurrentStreak_notCompletedToday_butCompletedYesterday_keepsStreakAlive() = runBlocking {
        val habitId = 2L
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        // Not completed today
        // Completed yesterday
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)
        fakeCompletionDao.insertOrUpdate(HabitCompletionEntity(habitId = habitId, completionDate = yesterdayStr, completed = true))

        val streak = habitRepository.calculateCurrentStreak(habitId)
        assertEquals(1, streak)
    }

    @Test
    fun calculateCurrentStreak_brokenStreak_returnsZero() = runBlocking {
        val habitId = 3L
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()

        // Completed 3 days ago, but missed yesterday and today
        cal.add(Calendar.DAY_OF_YEAR, -3)
        val threeDaysAgoStr = sdf.format(cal.time)
        fakeCompletionDao.insertOrUpdate(HabitCompletionEntity(habitId = habitId, completionDate = threeDaysAgoStr, completed = true))

        val streak = habitRepository.calculateCurrentStreak(habitId)
        assertEquals(0, streak)
    }

    @Test
    fun habitItemUiModel_subtitleFormatting_morningAndEvening() {
        val morningHabit = HabitEntity(
            userId = 1L,
            name = "Drink Water",
            category = "Health",
            reminderEnabled = true,
            reminderHour = 8,
            reminderMinute = 0
        )
        val model1 = HabitItemUiModel.from(morningHabit, isCompletedToday = false)
        assertTrue(model1.subtitle.contains("Morning"))
        assertTrue(model1.subtitle.contains("08:00 AM") || model1.subtitle.contains("8:00 AM"))

        val eveningHabit = HabitEntity(
            userId = 1L,
            name = "Read Book",
            category = "Productivity",
            reminderEnabled = true,
            reminderHour = 20,
            reminderMinute = 30
        )
        val model2 = HabitItemUiModel.from(eveningHabit, isCompletedToday = true)
        assertTrue(model2.subtitle.contains("Evening") || model2.subtitle.contains("Night"))
        assertTrue(model2.subtitle.contains("08:30 PM") || model2.subtitle.contains("8:30 PM"))
    }
}
