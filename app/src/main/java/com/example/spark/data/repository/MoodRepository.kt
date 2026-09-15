package com.example.spark.data.repository

import com.example.spark.data.local.dao.MoodDao
import com.example.spark.data.local.entity.MoodEntryEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Repository wrapping [MoodDao] exposing suspend functions and Flows only.
 * No business logic, no UI concerns. Every suspend function executes on [ioDispatcher].
 */
class MoodRepository(
    private val moodDao: MoodDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Inserts or replaces a mood entry in the database.
     */
    suspend fun insertMoodEntry(entry: MoodEntryEntity): Unit = withContext(ioDispatcher) {
        moodDao.insertMoodEntry(entry)
    }

    /**
     * Reactive stream of mood entries within a date range [startDate, endDate].
     */
    fun getMoodEntriesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<MoodEntryEntity>> {
        return moodDao.getMoodEntriesInRange(startDate, endDate)
    }

    /**
     * Reactive stream of today's mood entry.
     */
    fun getTodayMood(date: LocalDate): Flow<MoodEntryEntity?> {
        return moodDao.getTodayMood(date)
    }

    // ── Additional Helpers ──────────────────────────────────────────────

    fun getMoodForDateFlow(userId: Long, date: LocalDate): Flow<MoodEntryEntity?> {
        return moodDao.getMoodForDateFlow(userId, date)
    }

    suspend fun getMoodForDate(userId: Long, date: LocalDate): MoodEntryEntity? = withContext(ioDispatcher) {
        moodDao.getMoodForDate(userId, date)
    }

    fun getAllMoodEntries(userId: Long = 1L): Flow<List<MoodEntryEntity>> {
        return moodDao.getAllMoodEntries(userId)
    }

    fun getRecentMoodEntries(userId: Long = 1L, limit: Int = 7): Flow<List<MoodEntryEntity>> {
        return moodDao.getRecentMoodEntries(userId, limit)
    }

    suspend fun deleteMoodEntry(moodId: String): Unit = withContext(ioDispatcher) {
        val id = moodId.toLongOrNull() ?: return@withContext
        moodDao.deleteMoodEntry(id)
    }
}
