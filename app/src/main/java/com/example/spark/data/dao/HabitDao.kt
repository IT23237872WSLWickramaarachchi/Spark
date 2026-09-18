package com.example.spark.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.spark.data.entity.HabitEntity

@Dao
interface HabitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: Long)

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    suspend fun getHabitById(habitId: Long): HabitEntity?

    @Query("SELECT * FROM habits WHERE user_id = :userId AND is_archived = 0 ORDER BY created_at ASC")
    fun getActiveHabitsLiveData(userId: Long): LiveData<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE user_id = :userId AND is_archived = 0 ORDER BY created_at ASC")
    suspend fun getActiveHabits(userId: Long): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE user_id = :userId ORDER BY created_at DESC")
    fun getAllHabitsLiveData(userId: Long): LiveData<List<HabitEntity>>

    @Query("UPDATE habits SET is_archived = :archived WHERE id = :habitId")
    suspend fun setArchived(habitId: Long, archived: Boolean)

    @Query("UPDATE habits SET notes = :notes WHERE id = :habitId")
    suspend fun updateHabitNotes(habitId: Long, notes: String?)

    @Query("SELECT * FROM habits WHERE is_archived = 0 AND reminder_enabled = 1")
    suspend fun getAllActiveReminders(): List<HabitEntity>
}
