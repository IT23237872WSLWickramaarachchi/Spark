package com.example.spark.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.spark.data.entity.MoodEntryEntity

@Dao
interface MoodEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(mood: MoodEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertNew(mood: MoodEntryEntity): Long

    @Update
    suspend fun update(mood: MoodEntryEntity)

    @Delete
    suspend fun delete(mood: MoodEntryEntity)

    // Legacy: returns the first mood for a date (used during migration transition)
    @Query("SELECT * FROM mood_entries WHERE user_id = :userId AND date = :date ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMoodForDate(userId: Long, date: String): MoodEntryEntity?

    // Returns all mood entries for a given date, newest first
    @Query("SELECT * FROM mood_entries WHERE user_id = :userId AND date = :date ORDER BY timestamp DESC")
    suspend fun getMoodsForDate(userId: Long, date: String): List<MoodEntryEntity>

    // LiveData: all mood entries for a given date
    @Query("SELECT * FROM mood_entries WHERE user_id = :userId AND date = :date ORDER BY timestamp DESC")
    fun getMoodsForDateLiveData(userId: Long, date: String): LiveData<List<MoodEntryEntity>>

    // LiveData: the latest (most recent) mood entry for a given date
    @Query("SELECT * FROM mood_entries WHERE user_id = :userId AND date = :date ORDER BY timestamp DESC LIMIT 1")
    fun getLatestMoodForDateLiveData(userId: Long, date: String): LiveData<MoodEntryEntity?>

    // Legacy single-entry LiveData (kept for backwards compat)
    @Query("SELECT * FROM mood_entries WHERE user_id = :userId AND date = :date ORDER BY timestamp DESC LIMIT 1")
    fun getMoodForDateLiveData(userId: Long, date: String): LiveData<MoodEntryEntity?>

    @Query("SELECT * FROM mood_entries WHERE user_id = :userId ORDER BY timestamp DESC, date DESC")
    fun getAllMoodsLiveData(userId: Long): LiveData<List<MoodEntryEntity>>

    @Query("SELECT * FROM mood_entries WHERE user_id = :userId ORDER BY timestamp DESC, date DESC")
    suspend fun getAllMoods(userId: Long): List<MoodEntryEntity>

    @Query("SELECT * FROM mood_entries WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    suspend fun getMoodsBetween(userId: Long, startDate: String, endDate: String): List<MoodEntryEntity>

    @Query("SELECT * FROM mood_entries WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    fun getMoodsBetweenLiveData(userId: Long, startDate: String, endDate: String): LiveData<List<MoodEntryEntity>>

    @Query("SELECT COUNT(*) FROM mood_entries WHERE user_id = :userId")
    suspend fun getTotalMoodCheckInCount(userId: Long): Int

    @Query("DELETE FROM mood_entries WHERE user_id = :userId AND is_demo = 1")
    suspend fun deleteDemoMoods(userId: Long): Int

    @Query("SELECT * FROM mood_entries WHERE user_id = :userId ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun getLatestMood(userId: Long): MoodEntryEntity?

    @Query("SELECT COUNT(*) FROM mood_entries WHERE user_id = :userId AND is_demo = 1")
    suspend fun getDemoMoodCount(userId: Long): Int

    @Query("SELECT COUNT(*) FROM mood_entries WHERE user_id = :userId AND date = :date")
    suspend fun getMoodCountForDate(userId: Long, date: String): Int
}
