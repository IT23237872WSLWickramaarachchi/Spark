package com.example.spark.ui.dashboard

import com.example.spark.data.local.dao.HabitDao
import com.example.spark.data.local.dao.MoodDao
import com.example.spark.data.local.dao.UserDao
import com.example.spark.data.local.entity.HabitEntity
import com.example.spark.data.local.entity.HabitLogEntity
import com.example.spark.data.local.entity.HabitWithLogs
import com.example.spark.data.local.entity.MoodEntryEntity
import com.example.spark.data.local.entity.UserEntity
import com.example.spark.data.local.entity.UserPrefsEntity
import com.example.spark.data.repository.HabitRepository
import com.example.spark.data.repository.MoodRepository
import com.example.spark.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeHabitDao: FakeHabitDao
    private lateinit var fakeMoodDao: FakeMoodDao
    private lateinit var habitRepository: HabitRepository
    private lateinit var moodRepository: MoodRepository
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeHabitDao = FakeHabitDao()
        fakeMoodDao = FakeMoodDao()
        habitRepository = HabitRepository(fakeHabitDao, testDispatcher)
        moodRepository = MoodRepository(fakeMoodDao)
        viewModel = DashboardViewModel(
            habitRepository = habitRepository,
            moodRepository = moodRepository,
            sharingStarted = kotlinx.coroutines.flow.SharingStarted.Eagerly
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_computesCompletedAndProgressReactively() = runTest {
        testScheduler.advanceUntilIdle()
        val initialState = viewModel.uiState.value
        assertTrue(initialState.isEmpty)
        assertEquals(0, initialState.completedToday)
        assertEquals(0, initialState.totalToday)
        assertEquals(0, initialState.progressPercent)

        // 2. Add two habits
        val habit1 = HabitEntity(id = 1L, userId = 1L, title = "Drink Water", category = "Health")
        val habit2 = HabitEntity(id = 2L, userId = 1L, title = "Meditation", category = "Mindfulness")
        habitRepository.insertHabit(habit1)
        habitRepository.insertHabit(habit2)
        testScheduler.advanceUntilIdle()

        val stateAfterInsert = viewModel.uiState.value
        assertFalse(stateAfterInsert.isEmpty)
        assertEquals(2, stateAfterInsert.totalToday)
        assertEquals(0, stateAfterInsert.completedToday)
        assertEquals(0, stateAfterInsert.progressPercent)

        // 3. Toggle completion on habit 1
        viewModel.onHabitToggle("1", true)
        testScheduler.advanceUntilIdle()

        val stateAfterToggle = viewModel.uiState.value
        assertEquals(1, stateAfterToggle.completedToday)
        assertEquals(2, stateAfterToggle.totalToday)
        assertEquals(50, stateAfterToggle.progressPercent)
        assertTrue(stateAfterToggle.habits.first { it.id == 1L }.isCompleted)
        assertFalse(stateAfterToggle.habits.first { it.id == 2L }.isCompleted)
    }

    @Test
    fun onHabitToggle_toFalse_updatesProgressPercent() = runTest {
        val habit = HabitEntity(id = 10L, userId = 1L, title = "Read", category = "Productivity")
        habitRepository.insertHabit(habit)
        viewModel.onHabitToggle("10", true)
        testScheduler.advanceUntilIdle()

        val completedState = viewModel.uiState.value
        assertEquals(1, completedState.completedToday)
        assertEquals(100, completedState.progressPercent)

        // Un-check
        viewModel.onHabitToggle("10", false)
        testScheduler.advanceUntilIdle()

        val uncompletedState = viewModel.uiState.value
        assertEquals(0, uncompletedState.completedToday)
        assertEquals(0, uncompletedState.progressPercent)
        assertFalse(uncompletedState.habits.first().isCompleted)
    }

    @Test
    fun onSearchQueryChange_filtersHabitsByTitle() = runTest {
        val habit1 = HabitEntity(id = 1L, userId = 1L, title = "Drink Water", category = "Health")
        val habit2 = HabitEntity(id = 2L, userId = 1L, title = "Meditation", category = "Mindfulness")
        habitRepository.insertHabit(habit1)
        habitRepository.insertHabit(habit2)
        testScheduler.advanceUntilIdle()

        viewModel.onSearchQueryChange("water")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.habits.size)
        assertEquals("Drink Water", state.habits.first().title)
        assertEquals(2, state.totalToday)
        assertFalse(state.isEmpty)
    }

    @Test
    fun onSearchQueryChange_filtersHabitsByCategory() = runTest {
        val habit1 = HabitEntity(id = 1L, userId = 1L, title = "Drink Water", category = "Health")
        val habit2 = HabitEntity(id = 2L, userId = 1L, title = "Meditation", category = "Mindfulness")
        habitRepository.insertHabit(habit1)
        habitRepository.insertHabit(habit2)
        testScheduler.advanceUntilIdle()

        viewModel.onSearchQueryChange("mindfulness")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.habits.size)
        assertEquals("Meditation", state.habits.first().title)
        assertEquals("Mindfulness", state.habits.first().category)
    }

    @Test
    fun onSearchQueryChange_caseInsensitiveAndTrimmed() = runTest {
        val habit1 = HabitEntity(id = 1L, userId = 1L, title = "Drink Water", category = "Health")
        val habit2 = HabitEntity(id = 2L, userId = 1L, title = "Meditation", category = "Mindfulness")
        habitRepository.insertHabit(habit1)
        habitRepository.insertHabit(habit2)
        testScheduler.advanceUntilIdle()

        viewModel.onSearchQueryChange("   DRINK   ")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.habits.size)
        assertEquals("Drink Water", state.habits.first().title)
    }

    @Test
    fun onSearchQueryChange_noMatches_returnsEmptyListAndSetsIsEmptyTrue() = runTest {
        val habit1 = HabitEntity(id = 1L, userId = 1L, title = "Drink Water", category = "Health")
        habitRepository.insertHabit(habit1)
        testScheduler.advanceUntilIdle()

        viewModel.onSearchQueryChange("nonexistent habit")
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.habits.isEmpty())
        assertTrue(state.isEmpty)
        assertEquals(1, state.totalToday)
    }

    @Test
    fun onSearchQueryChange_blankQuery_resetsToAllHabits() = runTest {
        val habit1 = HabitEntity(id = 1L, userId = 1L, title = "Drink Water", category = "Health")
        val habit2 = HabitEntity(id = 2L, userId = 1L, title = "Meditation", category = "Mindfulness")
        habitRepository.insertHabit(habit1)
        habitRepository.insertHabit(habit2)
        testScheduler.advanceUntilIdle()

        viewModel.onSearchQueryChange("water")
        testScheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.habits.size)

        viewModel.onSearchQueryChange("")
        testScheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(2, state.habits.size)
        assertFalse(state.isEmpty)
    }

    @Test
    fun uiState_userName_reflectsCurrentUserFromUserRepository() = runTest {
        val fakeUserDao = FakeUserDao()
        val userRepo = UserRepository(fakeUserDao)
        fakeUserDao.register(UserEntity(id = 5L, name = "Jordan", email = "jordan@example.com", passwordHash = "pass"))

        val customViewModel = DashboardViewModel(
            habitRepository = habitRepository,
            moodRepository = moodRepository,
            userRepository = userRepo,
            currentUserIdProvider = { 5L },
            sharingStarted = kotlinx.coroutines.flow.SharingStarted.Eagerly
        )
        testScheduler.advanceUntilIdle()

        assertEquals("Jordan", customViewModel.uiState.value.userName)
    }

    private class FakeUserDao : UserDao {
        val users = MutableStateFlow<List<UserEntity>>(emptyList())
        override suspend fun register(user: UserEntity): Long {
            users.value = users.value.filter { it.id != user.id } + user
            return user.id
        }
        override suspend fun getUserByEmail(email: String): UserEntity? = users.value.find { it.email == email }
        override suspend fun login(email: String, passwordHash: String): UserEntity? =
            users.value.find { it.email == email && it.passwordHash == passwordHash }
        override fun getCurrentUser(userId: Long): Flow<UserEntity?> = users.map { list -> list.find { it.id == userId } }
        override suspend fun updatePrefs(prefs: UserPrefsEntity) {}
        override suspend fun getUserPrefs(userId: Long): UserPrefsEntity? = null
        override fun getUserPrefsFlow(userId: Long): Flow<UserPrefsEntity?> = MutableStateFlow(null)
        override suspend fun deleteUser(userId: Long) {
            users.value = users.value.filter { it.id != userId }
        }
    }

    private class FakeHabitDao : HabitDao {
        private val habits = MutableStateFlow<List<HabitEntity>>(emptyList())
        private val logs = MutableStateFlow<List<HabitLogEntity>>(emptyList())

        override suspend fun insertHabit(habit: HabitEntity): Long {
            habits.value = habits.value.filter { it.id != habit.id } + habit
            return habit.id
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
            entries.value = entries.value.filter { !(it.userId == entry.userId && it.date == entry.date) } + entry
            return entry.id
        }

        override fun getTodayMood(date: LocalDate): Flow<MoodEntryEntity?> = entries.map { list -> list.find { it.date == date } }

        override fun getMoodEntriesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<MoodEntryEntity>> = entries

        override suspend fun getMoodForDate(userId: Long, date: LocalDate): MoodEntryEntity? = entries.value.find { it.userId == userId && it.date == date }

        override fun getMoodForDateFlow(userId: Long, date: LocalDate): Flow<MoodEntryEntity?> = entries.map { list -> list.find { it.userId == userId && it.date == date } }

        override fun getAllMoodEntries(userId: Long): Flow<List<MoodEntryEntity>> = entries

        override fun getRecentMoodEntries(userId: Long, limit: Int): Flow<List<MoodEntryEntity>> = entries

        override suspend fun deleteMoodEntry(moodId: Long) {
            entries.value = entries.value.filter { it.id != moodId }
        }

        override fun getTotalMoodCount(userId: Long): Flow<Int> = entries.map { it.size }
    }
}
