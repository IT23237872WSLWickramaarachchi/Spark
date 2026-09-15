package com.example.spark.ui.habitdetail

import androidx.lifecycle.SavedStateHandle
import com.example.spark.data.local.dao.HabitDao
import com.example.spark.data.local.entity.HabitEntity
import com.example.spark.data.local.entity.HabitLogEntity
import com.example.spark.data.local.entity.HabitWithLogs
import com.example.spark.data.repository.HabitRepository
import com.example.spark.domain.usecase.CalculateStreakUseCase
import com.example.spark.ui.custom.DayStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HabitDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedToday = LocalDate.of(2026, 9, 11) // Friday

    private lateinit var fakeHabitDao: FakeHabitDao
    private lateinit var habitRepository: HabitRepository
    private lateinit var viewModel: HabitDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeHabitDao = FakeHabitDao()
        habitRepository = HabitRepository(fakeHabitDao, testDispatcher)

        val handle = SavedStateHandle(mapOf("habitId" to "1"))
        viewModel = HabitDetailViewModel(
            habitRepository = habitRepository,
            savedStateHandle = handle,
            streakUseCase = CalculateStreakUseCase(),
            sharingStarted = SharingStarted.Eagerly,
            todayProvider = { fixedToday }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_loadsHabitAndComputesStreaksAndWeeklyStrip() = runTest {
        val habit = HabitEntity(
            id = 1L,
            userId = 1L,
            title = "Morning Meditation",
            category = "Mindfulness",
            frequency = "Daily"
        )
        habitRepository.insertHabit(habit)

        // Monday 2026-09-07, Thursday 2026-09-10, Friday 2026-09-11 (today)
        habitRepository.logCompletion("1", LocalDate.of(2026, 9, 7), true)
        habitRepository.logCompletion("1", LocalDate.of(2026, 9, 10), true)
        habitRepository.logCompletion("1", LocalDate.of(2026, 9, 11), true)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Morning Meditation", state.title)
        assertEquals("Mindfulness", state.category)
        assertEquals("Daily", state.frequency)
        assertEquals(2, state.currentStreak) // Thursday + Friday
        assertEquals(2, state.bestStreak)
        assertEquals(7, state.weeklyStatuses.size)

        // Monday (offset 0) was completed
        assertEquals(DayStatus.COMPLETED, state.weeklyStatuses[0])
        // Tuesday (offset 1) was missed
        assertEquals(DayStatus.MISSED, state.weeklyStatuses[1])
        // Friday (offset 4, today) is completed
        assertEquals(DayStatus.COMPLETED, state.weeklyStatuses[4])
        // Saturday (offset 5) is future
        assertEquals(DayStatus.FUTURE, state.weeklyStatuses[5])
    }

    @Test
    fun deleteHabit_removesFromRepositoryAndEmitsHabitDeleted() = runTest {
        val habit = HabitEntity(id = 1L, userId = 1L, title = "Yoga", category = "Health")
        habitRepository.insertHabit(habit)
        testScheduler.advanceUntilIdle()

        var deletedEmitted = false
        val collectJob = launch {
            viewModel.habitDeleted.collect {
                deletedEmitted = true
            }
        }

        viewModel.deleteHabit()
        testScheduler.advanceUntilIdle()

        assertTrue(deletedEmitted)
        val deletedHabit = habitRepository.getHabitById("1").first()
        assertNull(deletedHabit)

        collectJob.cancel()
    }

    @Test
    fun updateNotes_updatesUiState() = runTest {
        val habit = HabitEntity(id = 1L, userId = 1L, title = "Running", category = "Health")
        habitRepository.insertHabit(habit)
        testScheduler.advanceUntilIdle()

        viewModel.updateNotes("5km personal best!")
        testScheduler.advanceUntilIdle()

        assertEquals("5km personal best!", viewModel.uiState.value.notes)
    }

    private class FakeHabitDao : HabitDao {
        private val habits = MutableStateFlow<List<HabitEntity>>(emptyList())
        private val logs = MutableStateFlow<List<HabitLogEntity>>(emptyList())

        override suspend fun insertHabit(habit: HabitEntity): Long {
            val id = if (habit.id == 0L) (habits.value.size + 1).toLong() else habit.id
            val stored = habit.copy(id = id)
            habits.value = habits.value.filter { it.id != id } + stored
            return id
        }

        override suspend fun updateHabit(habit: HabitEntity) {
            habits.value = habits.value.map { if (it.id == habit.id) habit else it }
        }

        override suspend fun deleteHabit(habit: HabitEntity) {
            deleteHabitById(habit.id)
        }

        override suspend fun deleteHabitById(habitId: Long) {
            habits.value = habits.value.filter { it.id != habitId }
        }

        override fun getAllHabits(): Flow<List<HabitEntity>> = habits

        override fun getActiveHabitsForUser(userId: Long): Flow<List<HabitEntity>> {
            return habits.map { list -> list.filter { it.userId == userId && !it.isArchived } }
        }

        override suspend fun getHabitById(habitId: Long): HabitEntity? = habits.value.find { it.id == habitId }

        override fun getHabitByIdFlow(habitId: Long): Flow<HabitEntity?> = habits.map { list -> list.find { it.id == habitId } }

        override fun getHabitWithLogs(habitId: Long): Flow<HabitWithLogs?> = habits.map { null }

        override fun getAllHabitsWithLogs(userId: Long): Flow<List<HabitWithLogs>> = habits.map { emptyList() }

        override suspend fun insertLog(log: HabitLogEntity): Long {
            logs.value = logs.value.filter { !(it.habitId == log.habitId && it.completedDate == log.completedDate) } + log
            return log.id
        }

        override suspend fun deleteLog(habitId: Long, date: LocalDate) {
            logs.value = logs.value.filter { !(it.habitId == habitId && it.completedDate == date) }
        }

        override fun getLogsForHabit(habitId: Long): Flow<List<HabitLogEntity>> = logs.map { list -> list.filter { it.habitId == habitId } }

        override fun getLogsForDate(date: LocalDate): Flow<List<HabitLogEntity>> = logs.map { list -> list.filter { it.completedDate == date } }

        override fun getTodayLogs(date: LocalDate): Flow<List<HabitLogEntity>> = logs.map { list -> list.filter { it.completedDate == date } }

        override fun getLogsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<HabitLogEntity>> =
            logs.map { list -> list.filter { !it.completedDate.isBefore(startDate) && !it.completedDate.isAfter(endDate) } }

        override fun getAllLogs(): Flow<List<HabitLogEntity>> = logs

        override suspend fun getLogForDate(habitId: Long, date: LocalDate): HabitLogEntity? = logs.value.find { it.habitId == habitId && it.completedDate == date }

        override suspend fun getCompletedCountForDate(userId: Long, date: LocalDate): Int = logs.value.count { it.completedDate == date && it.isCompleted }
    }
}
