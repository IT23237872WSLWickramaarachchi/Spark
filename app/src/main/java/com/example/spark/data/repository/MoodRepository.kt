package com.example.spark.data.repository

import androidx.lifecycle.LiveData
import com.example.spark.data.dao.MoodEntryDao
import com.example.spark.data.entity.MoodEntryEntity

class MoodRepository(private val moodEntryDao: MoodEntryDao) {

    suspend fun saveMood(
        userId: Long,
        date: String,
        moodLevel: Int,
        note: String?,
        tags: String = "",
        isDemo: Boolean = false
    ): Long {
        val existing = moodEntryDao.getMoodForDate(userId, date)
        val entry = if (existing != null) {
            existing.copy(
                moodLevel = moodLevel,
                note = note,
                tags = tags,
                isDemo = if (existing.isDemo && !isDemo) false else (existing.isDemo || isDemo),
                updatedAt = System.currentTimeMillis()
            )
        } else {
            MoodEntryEntity(
                userId = userId,
                date = date,
                moodLevel = moodLevel,
                note = note,
                tags = tags,
                isDemo = isDemo
            )
        }
        return moodEntryDao.insertOrUpdate(entry)
    }

    suspend fun deleteDemoMoods(userId: Long): Int {
        return moodEntryDao.deleteDemoMoods(userId)
    }

    suspend fun getDemoMoodCount(userId: Long): Int {
        return moodEntryDao.getDemoMoodCount(userId)
    }

    suspend fun insertDemoMood(userId: Long, date: String, level: Int, note: String?, tags: String): Long {
        val existing = moodEntryDao.getMoodForDate(userId, date)
        if (existing == null) {
            return saveMood(userId, date, level, note, tags = tags, isDemo = true)
        }
        return 0L
    }

    suspend fun getMoodForDate(userId: Long, date: String): MoodEntryEntity? {
        return moodEntryDao.getMoodForDate(userId, date)
    }

    fun getMoodForDateLiveData(userId: Long, date: String): LiveData<MoodEntryEntity?> {
        return moodEntryDao.getMoodForDateLiveData(userId, date)
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
}
