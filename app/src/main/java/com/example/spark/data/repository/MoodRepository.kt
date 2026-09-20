package com.example.spark.data.repository

import androidx.lifecycle.LiveData
import com.example.spark.data.dao.MoodEntryDao
import com.example.spark.data.entity.MoodEntryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MoodRepository(private val moodEntryDao: MoodEntryDao) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)

    /**
     * Saves a NEW mood check-in. Always inserts a new row (supports multiple per day).
     */
    suspend fun saveMood(
        userId: Long,
        date: String,
        moodLevel: Int,
        note: String?,
        tags: String = "",
        isDemo: Boolean = false
    ): Long {
        val now = System.currentTimeMillis()
        val entry = MoodEntryEntity(
            userId = userId,
            date = date,
            moodLevel = moodLevel,
            note = note,
            tags = tags,
            isDemo = isDemo,
            timestamp = now,
            time = timeFormat.format(Date(now))
        )
        return moodEntryDao.insertNew(entry)
    }

    suspend fun deleteDemoMoods(userId: Long): Int {
        return moodEntryDao.deleteDemoMoods(userId)
    }

    suspend fun getDemoMoodCount(userId: Long): Int {
        return moodEntryDao.getDemoMoodCount(userId)
    }

    suspend fun insertDemoMood(userId: Long, date: String, level: Int, note: String?, tags: String): Long {
        return saveMood(userId, date, level, note, tags = tags, isDemo = true)
    }

    suspend fun getMoodForDate(userId: Long, date: String): MoodEntryEntity? {
        return moodEntryDao.getMoodForDate(userId, date)
    }

    suspend fun getMoodsForDate(userId: Long, date: String): List<MoodEntryEntity> {
        return moodEntryDao.getMoodsForDate(userId, date)
    }

    fun getMoodForDateLiveData(userId: Long, date: String): LiveData<MoodEntryEntity?> {
        return moodEntryDao.getMoodForDateLiveData(userId, date)
    }

    fun getLatestMoodForDateLiveData(userId: Long, date: String): LiveData<MoodEntryEntity?> {
        return moodEntryDao.getLatestMoodForDateLiveData(userId, date)
    }

    fun getMoodsForDateLiveData(userId: Long, date: String): LiveData<List<MoodEntryEntity>> {
        return moodEntryDao.getMoodsForDateLiveData(userId, date)
    }

    suspend fun getMoodsBetween(userId: Long, startDate: String, endDate: String): List<MoodEntryEntity> {
        return moodEntryDao.getMoodsBetween(userId, startDate, endDate)
    }

    fun getMoodsBetweenLiveData(userId: Long, startDate: String, endDate: String): LiveData<List<MoodEntryEntity>> {
        return moodEntryDao.getMoodsBetweenLiveData(userId, startDate, endDate)
    }

    fun getAllMoodsLiveData(userId: Long): LiveData<List<MoodEntryEntity>> {
        return moodEntryDao.getAllMoodsLiveData(userId)
    }

    suspend fun getMoodCountForDate(userId: Long, date: String): Int {
        return moodEntryDao.getMoodCountForDate(userId, date)
    }
}
