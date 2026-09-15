package com.example.spark.data.repository

import com.example.spark.data.local.dao.MoodDao
import com.example.spark.data.local.entity.MoodEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class MoodRepositoryTest {

    private lateinit var fakeMoodDao: FakeMoodDao
    private lateinit var repository: MoodRepository

    @Before
    fun setUp() {
        fakeMoodDao = FakeMoodDao()
        repository = MoodRepository(fakeMoodDao)
    }

    @Test
    fun insertMoodEntry_and_getTodayMood_returnsEntry() = runBlocking {
        val today = LocalDate.now()
        val entry = MoodEntryEntity(
            userId = 1L,
            date = today,
            moodScore = 4,
            note = "Feeling energized"
        )
        repository.insertMoodEntry(entry)

        val retrieved = repository.getTodayMood(today).first()
        assertNotNull(retrieved)
        assertEquals(4, retrieved?.moodScore)
        assertEquals("Feeling energized", retrieved?.note)
    }

    @Test
    fun getMoodEntriesInRange_filtersCorrectDates() = runBlocking {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val threeDaysAgo = today.minusDays(3)

        repository.insertMoodEntry(MoodEntryEntity(userId = 1L, date = today, moodScore = 5))
        repository.insertMoodEntry(MoodEntryEntity(userId = 1L, date = yesterday, moodScore = 3))
        repository.insertMoodEntry(MoodEntryEntity(userId = 1L, date = threeDaysAgo, moodScore = 2))

        val inRange = repository.getMoodEntriesInRange(yesterday, today).first()
        assertEquals(2, inRange.size)
    }

    private class FakeMoodDao : MoodDao {
        private var counter = 1L
        private val entries = MutableStateFlow<List<MoodEntryEntity>>(emptyList())

        override suspend fun insertMoodEntry(entry: MoodEntryEntity): Long {
            val id = if (entry.id == 0L) counter++ else entry.id
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
                    .sortedByDescending { it.date }
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
            return entries.map { list -> list.filter { it.userId == userId }.take(limit) }
        }

        override suspend fun deleteMoodEntry(moodId: Long) {
            entries.value = entries.value.filter { it.id != moodId }
        }

        override fun getTotalMoodCount(userId: Long): Flow<Int> {
            return entries.map { list -> list.count { it.userId == userId } }
        }
    }
}
