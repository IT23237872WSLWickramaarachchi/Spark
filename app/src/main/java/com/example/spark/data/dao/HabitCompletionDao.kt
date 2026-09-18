package com.example.spark.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.spark.data.entity.HabitCompletionEntity

@Dao
interface HabitCompletionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(completion: HabitCompletionEntity): Long

    @Query("DELETE FROM habit_completions WHERE habit_id = :habitId AND completion_date = :date")
    suspend fun deleteCompletion(habitId: Long, date: String)

    @Query("SELECT * FROM habit_completions WHERE habit_id = :habitId AND completion_date = :date LIMIT 1")
    suspend fun getCompletion(habitId: Long, date: String): HabitCompletionEntity?

    @Query("SELECT * FROM habit_completions WHERE habit_id = :habitId AND completion_date = :date LIMIT 1")
    fun getCompletionLiveData(habitId: Long, date: String): LiveData<HabitCompletionEntity?>

    @Query("SELECT * FROM habit_completions WHERE habit_id = :habitId ORDER BY completion_date DESC")
    suspend fun getCompletionsForHabit(habitId: Long): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE habit_id = :habitId ORDER BY completion_date DESC")
    fun getCompletionsForHabitLiveData(habitId: Long): LiveData<List<HabitCompletionEntity>>

    @Query("""
        SELECT hc.* FROM habit_completions hc
        INNER JOIN habits h ON hc.habit_id = h.id
        WHERE h.user_id = :userId AND hc.completion_date = :date
    """)
    fun getCompletionsForUserAndDateLiveData(userId: Long, date: String): LiveData<List<HabitCompletionEntity>>

    @Query("""
        SELECT hc.* FROM habit_completions hc
        INNER JOIN habits h ON hc.habit_id = h.id
        WHERE h.user_id = :userId AND hc.completion_date = :date
    """)
    suspend fun getCompletionsForUserAndDate(userId: Long, date: String): List<HabitCompletionEntity>

    @Query("""
        SELECT hc.* FROM habit_completions hc
        INNER JOIN habits h ON hc.habit_id = h.id
        WHERE h.user_id = :userId AND hc.completion_date BETWEEN :startDate AND :endDate
        ORDER BY hc.completion_date ASC
    """)
    suspend fun getCompletionsForUserBetween(userId: Long, startDate: String, endDate: String): List<HabitCompletionEntity>

    @Query("""
        SELECT hc.* FROM habit_completions hc
        INNER JOIN habits h ON hc.habit_id = h.id
        WHERE h.user_id = :userId AND hc.completion_date BETWEEN :startDate AND :endDate
        ORDER BY hc.completion_date ASC
    """)
    fun getCompletionsForUserBetweenLiveData(userId: Long, startDate: String, endDate: String): LiveData<List<HabitCompletionEntity>>

    @Query("SELECT COUNT(*) FROM habit_completions WHERE habit_id = :habitId AND completed = 1")
    suspend fun getTotalCompletedCountForHabit(habitId: Long): Int

    @Query("""
        DELETE FROM habit_completions
        WHERE is_demo = 1 AND habit_id IN (SELECT id FROM habits WHERE user_id = :userId)
    """)
    suspend fun deleteDemoCompletions(userId: Long): Int

    @Query("""
        SELECT COUNT(*) FROM habit_completions
        WHERE is_demo = 1 AND habit_id IN (SELECT id FROM habits WHERE user_id = :userId)
    """)
    suspend fun getDemoCompletionCount(userId: Long): Int
}
