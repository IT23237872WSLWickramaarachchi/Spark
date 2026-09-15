package com.example.spark.ui.mood

import com.example.spark.data.local.dao.MoodDao
import com.example.spark.data.local.entity.MoodEntryEntity
import com.example.spark.data.repository.MoodRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MoodCheckInViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedToday = LocalDate.of(2026, 9, 12)
    private lateinit var fakeMoodDao: FakeMoodDao
    private lateinit var moodRepository: MoodRepository
    private lateinit var viewModel: MoodCheckInViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeMoodDao = FakeMoodDao()
        moodRepository = MoodRepository(fakeMoodDao, testDispatcher)
        viewModel = MoodCheckInViewModel(
            moodRepository = moodRepository,
            todayProvider = { fixedToday }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultState_hasNullSelection_and_isSaveDisabled() {
        val state = viewModel.formState.value
        assertNull(state.selectedMoodIndex)
        assertEquals("", state.note)
        assertFalse(state.isSaveEnabled)
    }

    @Test
    fun onMoodSelect_updatesSelection_and_enablesSave() {
        viewModel.onMoodSelect(2) // 0-based index 2 -> Okay

        val state = viewModel.formState.value
        assertEquals(2, state.selectedMoodIndex)
        assertTrue(state.isSaveEnabled)
    }

    @Test
    fun onNoteChange_updatesNote() {
        viewModel.onNoteChange("Sunny morning meditation")

        val state = viewModel.formState.value
        assertEquals("Sunny morning meditation", state.note)
    }

    @Test
    fun saveMood_insertsMoodEntryWithCalculatedScore_and_emitsDismiss() = runTest {
        var dismissed = false
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.dismissEvent.collect {
                dismissed = true
            }
        }

        viewModel.onMoodSelect(4) // index 4 -> score 5 (Radiant)
        viewModel.onNoteChange("Feeling energized and productive")
        viewModel.saveMood(userId = 1L)

        advanceUntilIdle()

        assertTrue(dismissed)
        val todayEntry = moodRepository.getTodayMood(fixedToday).first()
        assertNotNull(todayEntry)
        assertEquals(5, todayEntry?.moodScore)
        assertEquals("Feeling energized and productive", todayEntry?.note)
        assertEquals(fixedToday, todayEntry?.date)
        assertEquals(1L, todayEntry?.userId)
    }

    @Test
    fun saveMood_whenNoMoodSelected_doesNotInsertOrDismiss() = runTest {
        var dismissed = false
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.dismissEvent.collect {
                dismissed = true
            }
        }

        viewModel.saveMood(userId = 1L)
        advanceUntilIdle()

        assertFalse(dismissed)
        val todayEntry = moodRepository.getTodayMood(fixedToday).first()
        assertNull(todayEntry)
    }

    @Test
    fun skip_emitsDismiss_withoutDatabaseWrite_upholdingShameFreePrinciple() = runTest {
        var dismissed = false
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.dismissEvent.collect {
                dismissed = true
            }
        }

        viewModel.skip()
        advanceUntilIdle()

        // 1. Dialog is dismissed smoothly
        assertTrue(dismissed)

        // 2. Confirmed: No mood entry created in database
        val todayEntry = moodRepository.getTodayMood(fixedToday).first()
        assertNull("Shame-free check: No mood record should be written when skipped", todayEntry)

        // 3. No habit or streak penalty is applied
        assertEquals(0, fakeMoodDao.getEntryCount())
    }

    private class FakeMoodDao : MoodDao {
        private var counter = 1L
        private val entries = MutableStateFlow<List<MoodEntryEntity>>(emptyList())

        fun getEntryCount(): Int = entries.value.size

        override suspend fun insertMoodEntry(entry: MoodEntryEntity): Long {
            val id = if (entry.id == 0L) counter++ else entry.id
            val newEntry = entry.copy(id = id)
            entries.value = entries.value.filter { !(it.userId == entry.userId && it.date == entry.date) } + newEntry
            return id
        }

        override fun getMoodEntriesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<MoodEntryEntity>> {
            return entries.map { list ->
                list.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }
            }
        }

        override fun getTodayMood(date: LocalDate): Flow<MoodEntryEntity?> {
            return entries.map { list ->
                list.firstOrNull { it.date == date }
            }
        }

        override fun getMoodForDateFlow(userId: Long, date: LocalDate): Flow<MoodEntryEntity?> {
            return entries.map { list ->
                list.firstOrNull { it.userId == userId && it.date == date }
            }
        }

        override suspend fun getMoodForDate(userId: Long, date: LocalDate): MoodEntryEntity? {
            return entries.value.firstOrNull { it.userId == userId && it.date == date }
        }

        override fun getAllMoodEntries(userId: Long): Flow<List<MoodEntryEntity>> {
            return entries.map { list ->
                list.filter { it.userId == userId }
            }
        }

        override fun getRecentMoodEntries(userId: Long, limit: Int): Flow<List<MoodEntryEntity>> {
            return entries.map { list ->
                list.filter { it.userId == userId }.sortedByDescending { it.date }.take(limit)
            }
        }

        override suspend fun deleteMoodEntry(moodId: Long) {
            entries.value = entries.value.filter { it.id != moodId }
        }

        override fun getTotalMoodCount(userId: Long): Flow<Int> {
            return entries.map { list ->
                list.count { it.userId == userId }
            }
        }
    }
}
