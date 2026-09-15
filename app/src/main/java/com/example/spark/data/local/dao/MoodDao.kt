package com.example.spark.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.spark.data.local.entity.MoodEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Data Access Object for [MoodEntryEntity].
 *
 * Provides mood check-in insert, date-based lookup, and trend queries
 * for the Stats/Insights screen.
 */
@Dao
interface MoodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodEntry(entry: MoodEntryEntity): Long

    @Query("SELECT * FROM mood_entries WHERE date = :date LIMIT 1")
    fun getTodayMood(date: LocalDate): Flow<MoodEntryEntity?>

    @Query("SELECT * FROM mood_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getMoodEntriesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<MoodEntryEntity>>

    @Query("SELECT * FROM mood_entries WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getMoodForDate(userId: Long, date: LocalDate): MoodEntryEntity?

    @Query("SELECT * FROM mood_entries WHERE userId = :userId AND date = :date LIMIT 1")
    fun getMoodForDateFlow(userId: Long, date: LocalDate): Flow<MoodEntryEntity?>

    /**
     * Reactive stream of all mood entries for a user, ordered by date descending.
     * Powers the mood trend chart on StatsFragment.
     */
    @Query("SELECT * FROM mood_entries WHERE userId = :userId ORDER BY date DESC")
    fun getAllMoodEntries(userId: Long): Flow<List<MoodEntryEntity>>

    /**
     * Returns the last N mood entries for charting trends.
     */
    @Query("SELECT * FROM mood_entries WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun getRecentMoodEntries(userId: Long, limit: Int): Flow<List<MoodEntryEntity>>

    @Query("DELETE FROM mood_entries WHERE id = :moodId")
    suspend fun deleteMoodEntry(moodId: Long)

    @Query("SELECT COUNT(*) FROM mood_entries WHERE userId = :userId")
    fun getTotalMoodCount(userId: Long): Flow<Int>
}
