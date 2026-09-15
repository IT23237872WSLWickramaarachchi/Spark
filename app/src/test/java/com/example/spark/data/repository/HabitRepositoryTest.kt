package com.example.spark.data.repository

import com.example.spark.data.local.dao.HabitDao
import com.example.spark.data.local.entity.HabitEntity
import com.example.spark.data.local.entity.HabitLogEntity
import com.example.spark.data.local.entity.HabitWithLogs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class HabitRepositoryTest {

    private lateinit var fakeDao: FakeHabitDao
    private lateinit var repository: HabitRepository

    @Before
    fun setUp() {
        fakeDao = FakeHabitDao()
        repository = HabitRepository(fakeDao)
    }

    @Test
    fun insertHabit_and_getAllHabits_emitsHabitEntities() = runBlocking {
        val habit = HabitEntity(
            userId = 1L,
            title = "Morning Meditation",
            category = "Mindfulness"
        )
        val id = repository.insertHabit(habit)

        val habits = repository.getAllHabits().first()
        assertEquals(1, habits.size)
        assertEquals("Morning Meditation", habits[0].title)
        assertEquals("Mindfulness", habits[0].category)
    }

    @Test
    fun logCompletion_upsertsWithoutDuplicates() = runBlocking {
        val habit = HabitEntity(userId = 1L, title = "Drink Water", category = "Health")
        val habitId = repository.insertHabit(habit)

        val today = LocalDate.now()

        // 1. Initial completion log
        repository.logCompletion(habitId.toString(), today, true)
        var logs = repository.getLogsForHabit(habitId.toString()).first()
        assertEquals(1, logs.size)
        assertTrue(logs[0].isCompleted)

        // 2. Toggle to false on the same date - should update, not duplicate
        repository.logCompletion(habitId.toString(), today, false)
        logs = repository.getLogsForHabit(habitId.toString()).first()
        assertEquals(1, logs.size)
        assertFalse(logs[0].isCompleted)

        // 3. Toggle back to true on the same date
        repository.logCompletion(habitId.toString(), today, true)
        logs = repository.getLogsForHabit(habitId.toString()).first()
        assertEquals(1, logs.size)
        assertTrue(logs[0].isCompleted)
    }

    @Test
    fun getTodayLogs_returnsOnlyLogsForSpecifiedDate() = runBlocking {
        val habit = HabitEntity(userId = 1L, title = "Exercise", category = "Health")
        val habitId = repository.insertHabit(habit)

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        repository.logCompletion(habitId.toString(), today, true)
        repository.logCompletion(habitId.toString(), yesterday, true)

        val todayLogs = repository.getTodayLogs(today).first()
        assertEquals(1, todayLogs.size)
        assertEquals(today, todayLogs[0].completedDate)
    }

    @Test
    fun deleteHabit_removesHabit() = runBlocking {
        val habit = HabitEntity(userId = 1L, title = "Read", category = "Productivity")
        val id = repository.insertHabit(habit)

        var list = repository.getAllHabits().first()
        assertEquals(1, list.size)

        repository.deleteHabit(id.toString())
        list = repository.getAllHabits().first()
        assertTrue(list.isEmpty())
    }

    private class FakeHabitDao : HabitDao {
        private var idCounter = 1L
        private var logCounter = 1L
        private val habitsState = MutableStateFlow<List<HabitEntity>>(emptyList())
        private val logsState = MutableStateFlow<List<HabitLogEntity>>(emptyList())

        override suspend fun insertHabit(habit: HabitEntity): Long {
            val assignedId = if (habit.id == 0L) idCounter++ else habit.id
            val newHabit = habit.copy(id = assignedId)
            val current = habitsState.value.filter { it.id != assignedId } + newHabit
            habitsState.value = current
            return assignedId
        }

        override suspend fun updateHabit(habit: HabitEntity) {
            habitsState.value = habitsState.value.map { if (it.id == habit.id) habit else it }
        }

        override suspend fun deleteHabit(habit: HabitEntity) {
            deleteHabitById(habit.id)
        }

        override suspend fun deleteHabitById(habitId: Long) {
            habitsState.value = habitsState.value.filter { it.id != habitId }
            logsState.value = logsState.value.filter { it.habitId != habitId }
        }

        override fun getAllHabits(): Flow<List<HabitEntity>> = habitsState

        override fun getActiveHabitsForUser(userId: Long): Flow<List<HabitEntity>> {
            return habitsState.map { list -> list.filter { it.userId == userId && !it.isArchived } }
        }

        override suspend fun getHabitById(habitId: Long): HabitEntity? {
            return habitsState.value.find { it.id == habitId }
        }

        override fun getHabitByIdFlow(habitId: Long): Flow<HabitEntity?> {
            return habitsState.map { list -> list.find { it.id == habitId } }
        }

        override fun getHabitWithLogs(habitId: Long): Flow<HabitWithLogs?> {
            return habitsState.map { list ->
                val habit = list.find { it.id == habitId } ?: return@map null
                val logs = logsState.value.filter { it.habitId == habitId }
                HabitWithLogs(habit, logs)
            }
        }

        override fun getAllHabitsWithLogs(userId: Long): Flow<List<HabitWithLogs>> {
            return habitsState.map { list ->
                list.filter { it.userId == userId && !it.isArchived }.map { habit ->
                    val logs = logsState.value.filter { it.habitId == habit.id }
                    HabitWithLogs(habit, logs)
                }
            }
        }

        override suspend fun insertLog(log: HabitLogEntity): Long {
            val existing = logsState.value.find { it.habitId == log.habitId && it.completedDate == log.completedDate }
            val id = existing?.id ?: (if (log.id == 0L) logCounter++ else log.id)
            val newLog = log.copy(id = id)
            logsState.value = logsState.value.filter { !(it.habitId == log.habitId && it.completedDate == log.completedDate) } + newLog
            return id
        }

        override suspend fun deleteLog(habitId: Long, date: LocalDate) {
            logsState.value = logsState.value.filter { !(it.habitId == habitId && it.completedDate == date) }
        }

        override fun getLogsForHabit(habitId: Long): Flow<List<HabitLogEntity>> {
            return logsState.map { list -> list.filter { it.habitId == habitId } }
        }

        override fun getLogsForDate(date: LocalDate): Flow<List<HabitLogEntity>> {
            return logsState.map { list -> list.filter { it.completedDate == date } }
        }

        override fun getTodayLogs(date: LocalDate): Flow<List<HabitLogEntity>> {
            return logsState.map { list -> list.filter { it.completedDate == date } }
        }

        override fun getLogsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<HabitLogEntity>> {
            return logsState.map { list ->
                list.filter { !it.completedDate.isBefore(startDate) && !it.completedDate.isAfter(endDate) }
            }
        }

        override fun getAllLogs(): Flow<List<HabitLogEntity>> = logsState

        override suspend fun getLogForDate(habitId: Long, date: LocalDate): HabitLogEntity? {
            return logsState.value.find { it.habitId == habitId && it.completedDate == date }
        }

        override suspend fun getCompletedCountForDate(userId: Long, date: LocalDate): Int {
            val userHabitIds = habitsState.value.filter { it.userId == userId }.map { it.id }.toSet()
            return logsState.value.count { it.habitId in userHabitIds && it.completedDate == date && it.isCompleted }
        }
    }
}
