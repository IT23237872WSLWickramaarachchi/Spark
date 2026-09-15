package com.example.spark.data.repository

import com.example.spark.data.local.dao.HabitDao
import com.example.spark.data.local.entity.HabitEntity
import com.example.spark.data.local.entity.HabitLogEntity
import com.example.spark.data.local.entity.HabitWithLogs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Repository wrapping [HabitDao] exposing suspend functions and Flows only.
 * No business logic, no UI concerns. Every suspend function executes on [ioDispatcher].
 */
class HabitRepository(
    private val habitDao: HabitDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Reactive stream of all active habits.
     */
    fun getAllHabits(): Flow<List<HabitEntity>> = habitDao.getAllHabits()

    /**
     * Inserts a new habit into the database.
     */
    suspend fun insertHabit(habit: HabitEntity): Long = withContext(ioDispatcher) {
        habitDao.insertHabit(habit)
    }

    /**
     * Updates an existing habit.
     */
    suspend fun updateHabit(habit: HabitEntity): Unit = withContext(ioDispatcher) {
        habitDao.updateHabit(habit)
    }

    /**
     * Deletes a habit by String ID.
     */
    suspend fun deleteHabit(habitId: String): Unit = withContext(ioDispatcher) {
        val id = habitId.toLongOrNull() ?: return@withContext
        habitDao.deleteHabitById(id)
    }

    suspend fun deleteHabit(habit: HabitEntity): Unit = withContext(ioDispatcher) {
        habitDao.deleteHabit(habit)
    }

    suspend fun deleteHabit(habitId: Long): Unit = withContext(ioDispatcher) {
        habitDao.deleteHabitById(habitId)
    }

    /**
     * Reactive stream of a habit by String ID.
     */
    fun getHabitById(habitId: String): Flow<HabitEntity?> {
        val id = habitId.toLongOrNull() ?: return flowOf(null)
        return habitDao.getHabitByIdFlow(id)
    }

    /**
     * Reactive stream of completion logs for a specific habit.
     */
    fun getLogsForHabit(habitId: String): Flow<List<HabitLogEntity>> {
        val id = habitId.toLongOrNull() ?: return flowOf(emptyList())
        return habitDao.getLogsForHabit(id)
    }

    fun getLogsForHabit(habitId: Long): Flow<List<HabitLogEntity>> {
        return habitDao.getLogsForHabit(habitId)
    }

    /**
     * Upserts into [HabitLogEntity] using OnConflictStrategy.REPLACE at the DAO level
     * so toggling twice on the same day updates the row without creating duplicate entries.
     */
    suspend fun logCompletion(
        habitId: String,
        date: LocalDate,
        completed: Boolean
    ): Unit = withContext(ioDispatcher) {
        val id = habitId.toLongOrNull() ?: return@withContext
        val existing = habitDao.getLogForDate(id, date)
        val log = HabitLogEntity(
            id = existing?.id ?: 0L,
            habitId = id,
            completedDate = date,
            isCompleted = completed
        )
        habitDao.insertLog(log)
    }

    /**
     * Convenience method to toggle completion state for a given date.
     */
    suspend fun logHabitCompletion(
        habitId: String,
        date: LocalDate = LocalDate.now()
    ): Unit = withContext(ioDispatcher) {
        val id = habitId.toLongOrNull() ?: return@withContext
        val existing = habitDao.getLogForDate(id, date)
        val isCurrentlyCompleted = existing?.isCompleted == true
        logCompletion(habitId, date, !isCurrentlyCompleted)
    }

    suspend fun logHabitCompletion(
        habitId: Long,
        date: LocalDate = LocalDate.now()
    ): Unit = logHabitCompletion(habitId.toString(), date)

    /**
     * Reactive stream of completion logs for a given date across all habits.
     */
    fun getTodayLogs(date: LocalDate): Flow<List<HabitLogEntity>> = habitDao.getTodayLogs(date)

    fun getAllLogs(): Flow<List<HabitLogEntity>> = habitDao.getAllLogs()

    fun getLogsInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<HabitLogEntity>> =
        habitDao.getLogsInRange(startDate, endDate)

    fun getHabitWithLogs(habitId: Long): Flow<HabitWithLogs?> = habitDao.getHabitWithLogs(habitId)

    fun getAllHabitsWithLogs(userId: Long = 1L): Flow<List<HabitWithLogs>> = habitDao.getAllHabitsWithLogs(userId)
}
