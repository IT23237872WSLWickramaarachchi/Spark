package com.example.spark.ui.habit

import com.example.spark.data.local.dao.HabitDao
import com.example.spark.data.local.dao.MoodDao
import com.example.spark.data.local.entity.HabitEntity
import com.example.spark.data.local.entity.HabitLogEntity
import com.example.spark.data.local.entity.HabitWithLogs
import com.example.spark.data.local.entity.MoodEntryEntity
import com.example.spark.data.repository.HabitRepository
import com.example.spark.data.repository.MoodRepository
import com.example.spark.ui.dashboard.DashboardViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class AddHabitViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeHabitDao: FakeHabitDao
    private lateinit var habitRepository: HabitRepository
    private lateinit var viewModel: AddHabitViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeHabitDao = FakeHabitDao()
        habitRepository = HabitRepository(fakeHabitDao, testDispatcher)
        viewModel = AddHabitViewModel(habitRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultFormState_isSaveDisabled() {
        val state = viewModel.formState.value
        assertEquals("", state.name)
        assertEquals("Health", state.category)
        assertEquals("Daily", state.frequency)
        assertEquals("08:00", state.reminderTime)
        assertEquals("#52559C", state.colorTag)
        assertFalse(state.isSaveEnabled)
    }

    @Test
    fun onNameChange_togglesSaveEnabled() {
        viewModel.onNameChange("Morning Jog")
        assertTrue(viewModel.formState.value.isSaveEnabled)
        assertEquals("Morning Jog", viewModel.formState.value.name)

        viewModel.onNameChange("   ")
        assertFalse(viewModel.formState.value.isSaveEnabled)

        viewModel.onNameChange("Yoga")
        assertTrue(viewModel.formState.value.isSaveEnabled)
    }

    @Test
    fun formUpdates_persistInFormState() {
        viewModel.onNameChange("Read Book")
        viewModel.onCategorySelect("Productivity")
        viewModel.onFrequencySelect("Weekly")
        viewModel.onReminderTimeSelect("21:30")
        viewModel.onColorSelect("#006A3F")

        val state = viewModel.formState.value
        assertEquals("Read Book", state.name)
        assertEquals("Productivity", state.category)
        assertEquals("Weekly", state.frequency)
        assertEquals("21:30", state.reminderTime)
        assertEquals("#006A3F", state.colorTag)
        assertTrue(state.isSaveEnabled)
    }

    @Test
    fun saveHabit_whenValid_insertsToRepositoryAndEmitsHabitSaved() = runTest {
        viewModel.onNameChange("Meditation")
        viewModel.onCategorySelect("Mindfulness")
        viewModel.onFrequencySelect("Daily")
        viewModel.onReminderTimeSelect("07:00")
        viewModel.onColorSelect("#874D5E")

        var savedEmitted = false
        val collectJob = launch {
            viewModel.habitSaved.collect {
                savedEmitted = true
            }
        }

        viewModel.saveHabit()
        testScheduler.advanceUntilIdle()

        assertTrue(savedEmitted)

        val allHabits = habitRepository.getAllHabits().first()
        assertEquals(1, allHabits.size)
        val saved = allHabits.first()
        assertEquals("Meditation", saved.title)
        assertEquals("Mindfulness", saved.category)
        assertEquals("Daily", saved.frequency)
        assertEquals(7, saved.targetDaysPerWeek)
        assertEquals(LocalTime.of(7, 0), saved.reminderTime)
        assertEquals("#874D5E", saved.colorHex)

        collectJob.cancel()
    }

    @Test
    fun saveHabit_whenSaveDisabled_doesNothing() = runTest {
        var savedEmitted = false
        val collectJob = launch {
            viewModel.habitSaved.collect {
                savedEmitted = true
            }
        }

        viewModel.saveHabit()
        testScheduler.advanceUntilIdle()

        assertFalse(savedEmitted)
        val allHabits = habitRepository.getAllHabits().first()
        assertTrue(allHabits.isEmpty())

        collectJob.cancel()
    }

    @Test
    fun savingHabit_automaticallyUpdatesDashboardUiStateWithoutCallbacks() = runTest {
        val fakeMoodDao = FakeMoodDao()
        val moodRepository = MoodRepository(fakeMoodDao)
        val dashboardViewModel = DashboardViewModel(
            habitRepository = habitRepository,
            moodRepository = moodRepository,
            sharingStarted = SharingStarted.Eagerly
        )
        testScheduler.advanceUntilIdle()

        // Initially dashboard is empty
        val initialDashboard = dashboardViewModel.uiState.value
        assertTrue(initialDashboard.isEmpty)
        assertEquals(0, initialDashboard.totalToday)

        // Add a habit via AddHabitViewModel
        viewModel.onNameChange("Hydrate")
        viewModel.saveHabit()
        testScheduler.advanceUntilIdle()

        // DashboardViewModel automatically received the new habit via Room Flow combine pipeline!
        val updatedDashboard = dashboardViewModel.uiState.value
        assertFalse(updatedDashboard.isEmpty)
        assertEquals(1, updatedDashboard.totalToday)
        assertEquals("Hydrate", updatedDashboard.habits.first().title)
    }

    @Test
    fun editMode_prepopulatesFormAndCallsUpdateHabit() = runTest {
        val existingHabit = HabitEntity(
            id = 5L,
            userId = 1L,
            title = "Sleep Early",
            category = "Health",
            frequency = "Daily",
            reminderTime = LocalTime.of(22, 0),
            colorHex = "#1C192A"
        )
        habitRepository.insertHabit(existingHabit)
        testScheduler.advanceUntilIdle()

        val editViewModel = AddHabitViewModel(
            habitRepository = habitRepository,
            savedStateHandle = androidx.lifecycle.SavedStateHandle(mapOf("habitId" to "5"))
        )
        testScheduler.advanceUntilIdle()

        // Verify pre-populated form state
        val formState = editViewModel.formState.value
        assertEquals("Sleep Early", formState.name)
        assertEquals("Health", formState.category)
        assertEquals("Daily", formState.frequency)
        assertEquals("22:00", formState.reminderTime)
        assertEquals("#1C192A", formState.colorTag)
        assertTrue(formState.isSaveEnabled)

        // Modify title and category
        editViewModel.onNameChange("Sleep 8 Hours")
        editViewModel.onCategorySelect("Mindfulness")

        var savedEmitted = false
        val job = launch {
            editViewModel.habitSaved.collect {
                savedEmitted = true
            }
        }

        editViewModel.saveHabit()
        testScheduler.advanceUntilIdle()

        assertTrue(savedEmitted)

        val updated = habitRepository.getHabitById("5").first()
        assertEquals("Sleep 8 Hours", updated?.title)
        assertEquals("Mindfulness", updated?.category)
        assertEquals(5L, updated?.id)

        job.cancel()
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
