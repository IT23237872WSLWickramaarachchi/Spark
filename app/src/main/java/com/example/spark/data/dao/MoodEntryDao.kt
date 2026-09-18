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

    @Update
    suspend fun update(mood: MoodEntryEntity)

    @Delete
    suspend fun delete(mood: MoodEntryEntity)

    @Query("SELECT * FROM mood_entries WHERE user_id = :userId AND date = :date LIMIT 1")
    suspend fun getMoodForDate(userId: Long, date: String): MoodEntryEntity?

    @Query("SELECT * FROM mood_entries WHERE user_id = :userId AND date = :date LIMIT 1")
    fun getMoodForDateLiveData(userId: Long, date: String): LiveData<MoodEntryEntity?>

    @Query("SELECT * FROM mood_entries WHERE user_id = :userId ORDER BY date DESC")
    fun getAllMoodsLiveData(userId: Long): LiveData<List<MoodEntryEntity>>

    @Query("SELECT * FROM mood_entries WHERE user_id = :userId ORDER BY date DESC")
    suspend fun getAllMoods(userId: Long): List<MoodEntryEntity>

    @Query("SELECT * FROM mood_entries WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getMoodsBetween(userId: Long, startDate: String, endDate: String): List<MoodEntryEntity>

    @Query("SELECT * FROM mood_entries WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getMoodsBetweenLiveData(userId: Long, startDate: String, endDate: String): LiveData<List<MoodEntryEntity>>

    @Query("SELECT COUNT(*) FROM mood_entries WHERE user_id = :userId")
    suspend fun getTotalMoodCheckInCount(userId: Long): Int

    @Query("DELETE FROM mood_entries WHERE user_id = :userId AND is_demo = 1")
    suspend fun deleteDemoMoods(userId: Long): Int

    @Query("SELECT * FROM mood_entries WHERE user_id = :userId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestMood(userId: Long): MoodEntryEntity?

    @Query("SELECT COUNT(*) FROM mood_entries WHERE user_id = :userId AND is_demo = 1")
    suspend fun getDemoMoodCount(userId: Long): Int
}
