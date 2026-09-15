package com.example.spark.ui.stats

import com.example.spark.data.local.dao.HabitDao
import com.example.spark.data.local.dao.MoodDao
import com.example.spark.data.local.entity.HabitEntity
import com.example.spark.data.local.entity.HabitLogEntity
import com.example.spark.data.local.entity.HabitWithLogs
import com.example.spark.data.local.entity.MoodEntryEntity
import com.example.spark.data.repository.HabitRepository
import com.example.spark.data.repository.MoodRepository
import com.example.spark.ui.custom.DayStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedToday = LocalDate.of(2026, 9, 11) // Friday

    private lateinit var fakeHabitDao: FakeHabitDao
    private lateinit var fakeMoodDao: FakeMoodDao
    private lateinit var habitRepository: HabitRepository
    private lateinit var moodRepository: MoodRepository
    private lateinit var viewModel: StatsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeHabitDao = FakeHabitDao()
        fakeMoodDao = FakeMoodDao()
        habitRepository = HabitRepository(fakeHabitDao, testDispatcher)
        moodRepository = MoodRepository(fakeMoodDao, testDispatcher)
        viewModel = StatsViewModel(
            habitRepository = habitRepository,
            moodRepository = moodRepository,
            sharingStarted = SharingStarted.Eagerly,
            todayProvider = { fixedToday }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_defaultsToWeekPeriod() = runTest {
        testScheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(Period.WEEK, state.period)
        assertEquals("This Week", state.completionSubtitle)
        assertEquals(0, state.completionPercent)
        assertEquals(0, state.currentStreak)
        assertEquals(0, state.todaysRate)
        assertEquals(7, state.dayLogStatus.size)
        assertEquals(7, state.dayStats.size)
        assertEquals(7, state.moodTrendPoints.size)
    }

    @Test
    fun setPeriod_month_updatesPeriodAndSubtitle() = runTest {
        testScheduler.advanceUntilIdle()
        viewModel.onPeriodChange(Period.MONTH)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(Period.MONTH, state.period)
        assertEquals("Past 30 Days", state.completionSubtitle)
        assertEquals(30, state.moodTrendPoints.size)
    }

    @Test
    fun dayStats_stripCalculatesMondayToSundayCorrectly() = runTest {
        // Monday is 2026-09-07, Friday is 2026-09-11 (today), Sunday is 2026-09-13
        val habit = HabitEntity(id = 1L, userId = 1L, title = "Exercise", category = "Health")
        habitRepository.insertHabit(habit)

        // Complete habit on Monday, Wednesday, Friday
        habitRepository.logCompletion("1", LocalDate.of(2026, 9, 7), true)
        habitRepository.logCompletion("1", LocalDate.of(2026, 9, 9), true)
        habitRepository.logCompletion("1", LocalDate.of(2026, 9, 11), true)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(7, state.dayStats.size)
        assertEquals(7, state.dayLogStatus.size)

        // dayLogStatus: Monday=T, Tuesday=F, Wednesday=T, Thursday=F, Friday=T, Saturday=F, Sunday=F
        assertEquals(listOf(true, false, true, false, true, false, false), state.dayLogStatus)

        // Day 0: Monday (Completed)
        assertEquals(DayStatus.COMPLETED, state.dayStats[0].status)
        assertEquals(100, state.dayStats[0].completionPercent)

        // Day 1: Tuesday (Missed)
        assertEquals(DayStatus.MISSED, state.dayStats[1].status)
        assertEquals(0, state.dayStats[1].completionPercent)

        // Day 2: Wednesday (Completed)
        assertEquals(DayStatus.COMPLETED, state.dayStats[2].status)

        // Day 4: Friday (Today)
        assertTrue(state.dayStats[4].isToday)
        assertEquals(DayStatus.TODAY, state.dayStats[4].status)

        // Day 5: Saturday (Future)
        assertEquals(DayStatus.FUTURE, state.dayStats[5].status)

        // Day 6: Sunday (Future)
        assertEquals(DayStatus.FUTURE, state.dayStats[6].status)
    }

    @Test
    fun completionRate_calculatesAccuratePercent() = runTest {
        val habit1 = HabitEntity(id = 1L, userId = 1L, title = "Hydrate", category = "Health")
        val habit2 = HabitEntity(id = 2L, userId = 1L, title = "Read", category = "Mindfulness")
        habitRepository.insertHabit(habit1)
        habitRepository.insertHabit(habit2)

        // 2 habits * 5 elapsed days (Monday to Friday) = 10 scheduled instances
        // Complete 5 instances -> 50%
        val monday = LocalDate.of(2026, 9, 7)
        for (i in 0 until 5) {
            habitRepository.logCompletion("1", monday.plusDays(i.toLong()), true)
        }
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(50, state.completionPercent)
        assertEquals(50, state.completionRatePercent)
    }

    @Test
    fun moodTrend_computesSmoothedPointsAndDetectsTrend() = runTest {
        // Insert mood entries with an upward trend over past few days
        moodRepository.insertMoodEntry(MoodEntryEntity(userId = 1L, date = fixedToday.minusDays(4), moodScore = 2))
        moodRepository.insertMoodEntry(MoodEntryEntity(userId = 1L, date = fixedToday.minusDays(3), moodScore = 3))
        moodRepository.insertMoodEntry(MoodEntryEntity(userId = 1L, date = fixedToday.minusDays(2), moodScore = 4))
        moodRepository.insertMoodEntry(MoodEntryEntity(userId = 1L, date = fixedToday.minusDays(1), moodScore = 5))
        moodRepository.insertMoodEntry(MoodEntryEntity(userId = 1L, date = fixedToday, moodScore = 5))
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.moodPoints.isEmpty())
        assertFalse(state.moodTrendPoints.isEmpty())
        assertEquals("Positive", state.moodTrendStatus)
        assertTrue(state.isTrendPositive)
    }

    @Test
    fun streakAndTodayRate_reflectsCurrentDay() = runTest {
        val habit = HabitEntity(id = 1L, userId = 1L, title = "Jogging", category = "Health")
        habitRepository.insertHabit(habit)

        // Complete Thursday and Friday -> 2-day streak
        habitRepository.logCompletion("1", fixedToday.minusDays(1), true)
        habitRepository.logCompletion("1", fixedToday, true)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.currentStreak)
        assertEquals(2, state.streakDays)
        assertEquals(1, state.todayCompletedCount)
        assertEquals(1, state.todayTotalCount)
        assertEquals(100, state.todaysRate)
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

    private class FakeMoodDao : MoodDao {
        private val entries = MutableStateFlow<List<MoodEntryEntity>>(emptyList())

        override suspend fun insertMoodEntry(entry: MoodEntryEntity): Long {
            val id = if (entry.id == 0L) (entries.value.size + 1).toLong() else entry.id
            val newEntry = entry.copy(id = id)
            entries.value = entries.value.filter { !(it.userId == entry.userId && it.date == entry.date) } + newEntry
            return id
        }

        override fun getTodayMood(date: LocalDate): Flow<MoodEntryEntity?> {
            return entries.map { list -> list.find { it.date == date } }
        }

        override fun getMoodEntriesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<MoodEntryEntity>> {
            return entries.map { list ->
                list.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }
            }
        }

        override suspend fun getMoodForDate(userId: Long, date: LocalDate): MoodEntryEntity? {
            return entries.value.find { it.userId == userId && it.date == date }
        }

        override fun getMoodForDateFlow(userId: Long, date: LocalDate): Flow<MoodEntryEntity?> {
            return entries.map { list -> list.find { it.userId == userId && it.date == date } }
        }

        override fun getAllMoodEntries(userId: Long): Flow<List<MoodEntryEntity>> {
            return entries.map { list -> list.filter { it.userId == userId } }
        }

        override fun getRecentMoodEntries(userId: Long, limit: Int): Flow<List<MoodEntryEntity>> {
            return entries.map { list -> list.filter { it.userId == userId }.sortedByDescending { it.date }.take(limit) }
        }

        override suspend fun deleteMoodEntry(moodId: Long) {
            entries.value = entries.value.filter { it.id != moodId }
        }

        override fun getTotalMoodCount(userId: Long): Flow<Int> {
            return entries.map { list -> list.count { it.userId == userId } }
        }
    }
}
