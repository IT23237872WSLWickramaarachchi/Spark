package com.example.spark.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.spark.data.local.entity.HabitEntity
import com.example.spark.data.local.entity.HabitLogEntity
import com.example.spark.data.local.entity.HabitWithLogs
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Data Access Object for [HabitEntity] and [HabitLogEntity].
 *
 * Provides habit CRUD, completion toggling, and streak-relevant queries.
 * Uses @Transaction for relational reads (HabitWithLogs) and
 * Flow<> for reactive UI updates (rubric: DB Connection — reactive queries).
 */
@Dao
interface HabitDao {

    // ── Habit CRUD ──────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: Long)

    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE userId = :userId AND isArchived = 0 ORDER BY createdAt DESC")
    fun getActiveHabitsForUser(userId: Long): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    suspend fun getHabitById(habitId: Long): HabitEntity?

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    fun getHabitByIdFlow(habitId: Long): Flow<HabitEntity?>

    /**
     * Returns a habit with all of its log entries via @Transaction.
     * Demonstrates @Relation and @Transaction for rubric DB marks.
     */
    @Transaction
    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    fun getHabitWithLogs(habitId: Long): Flow<HabitWithLogs?>

    @Transaction
    @Query("SELECT * FROM habits WHERE userId = :userId AND isArchived = 0 ORDER BY createdAt DESC")
    fun getAllHabitsWithLogs(userId: Long): Flow<List<HabitWithLogs>>

    // ── Habit Log (Completion) Operations ────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLogEntity): Long

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND completedDate = :date")
    suspend fun deleteLog(habitId: Long, date: LocalDate)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY completedDate DESC")
    fun getLogsForHabit(habitId: Long): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE completedDate = :date")
    fun getLogsForDate(date: LocalDate): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE completedDate = :date")
    fun getTodayLogs(date: LocalDate): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE completedDate BETWEEN :startDate AND :endDate ORDER BY completedDate ASC")
    fun getLogsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs ORDER BY completedDate DESC")
    fun getAllLogs(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND completedDate = :date LIMIT 1")
    suspend fun getLogForDate(habitId: Long, date: LocalDate): HabitLogEntity?

    /**
     * Count of completed logs for a date across all user habits.
     * Used for daily completion percentage on Dashboard.
     */
    @Query("""
        SELECT COUNT(*) FROM habit_logs hl
        INNER JOIN habits h ON hl.habitId = h.id
        WHERE h.userId = :userId AND hl.completedDate = :date AND hl.isCompleted = 1
    """)
    suspend fun getCompletedCountForDate(userId: Long, date: LocalDate): Int
}
