package com.example.spark.data.repository

import androidx.lifecycle.LiveData
import com.example.spark.data.dao.HabitCompletionDao
import com.example.spark.data.dao.HabitDao
import com.example.spark.data.entity.HabitCompletionEntity
import com.example.spark.data.entity.HabitEntity
import com.example.spark.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HabitRepository(
    private val habitDao: HabitDao,
    private val completionDao: HabitCompletionDao
) {

    suspend fun insertHabit(habit: HabitEntity): Long {
        return habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: HabitEntity) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habitId: Long) {
        habitDao.deleteHabitById(habitId)
    }

    suspend fun getHabitById(habitId: Long): HabitEntity? {
        return habitDao.getHabitById(habitId)
    }

    fun getActiveHabitsLiveData(userId: Long): LiveData<List<HabitEntity>> {
        return habitDao.getActiveHabitsLiveData(userId)
    }

    suspend fun getActiveHabits(userId: Long): List<HabitEntity> {
        return habitDao.getActiveHabits(userId)
    }

    fun getCompletionsForUserAndDateLiveData(userId: Long, date: String): LiveData<List<HabitCompletionEntity>> {
        return completionDao.getCompletionsForUserAndDateLiveData(userId, date)
    }

    suspend fun getCompletionsForUserAndDate(userId: Long, date: String): List<HabitCompletionEntity> {
        return completionDao.getCompletionsForUserAndDate(userId, date)
    }

    fun getCompletionsForUserBetweenLiveData(userId: Long, startDate: String, endDate: String): LiveData<List<HabitCompletionEntity>> {
        return completionDao.getCompletionsForUserBetweenLiveData(userId, startDate, endDate)
    }

    suspend fun getCompletionsForUserBetween(userId: Long, startDate: String, endDate: String): List<HabitCompletionEntity> {
        return completionDao.getCompletionsForUserBetween(userId, startDate, endDate)
    }

    fun getCompletionsForHabitLiveData(habitId: Long): LiveData<List<HabitCompletionEntity>> {
        return completionDao.getCompletionsForHabitLiveData(habitId)
    }

    suspend fun getCompletionsForHabit(habitId: Long): List<HabitCompletionEntity> {
        return completionDao.getCompletionsForHabit(habitId)
    }

    suspend fun toggleHabitCompletion(habitId: Long, date: String): Boolean {
        val existing = completionDao.getCompletion(habitId, date)
        return if (existing != null && existing.completed) {
            completionDao.deleteCompletion(habitId, date)
            false
        } else {
            val completion = HabitCompletionEntity(
                habitId = habitId,
                completionDate = date,
                completed = true
            )
            completionDao.insertOrUpdate(completion)
            true
        }
    }

    suspend fun isCompletedOn(habitId: Long, date: String): Boolean {
        val completion = completionDao.getCompletion(habitId, date)
        return completion != null && completion.completed
    }

    /**
     * Calculates the current consecutive streak for a habit (days for daily, weeks for weekly)
     * ending today/yesterday or this/last week.
     */
    suspend fun calculateCurrentStreak(habitId: Long): Int {
        val habit = habitDao.getHabitById(habitId)
        val completions = completionDao.getCompletionsForHabit(habitId)
        val completedDates = completions.filter { it.completed }.map { it.completionDate }.toSet()
        if (completedDates.isEmpty()) return 0

        val isWeekly = habit?.frequency.equals("WEEKLY", ignoreCase = true)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        if (isWeekly) {
            val cal = Calendar.getInstance()
            cal.firstDayOfWeek = Calendar.MONDAY

            fun isWeekCompleted(c: Calendar): Boolean {
                val tempCal = c.clone() as Calendar
                tempCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                for (i in 0 until 7) {
                    val dStr = sdf.format(tempCal.time)
                    if (completedDates.contains(dStr)) return true
                    tempCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                return false
            }

            var streak = 0
            val thisWeekCompleted = isWeekCompleted(cal)
            if (!thisWeekCompleted) {
                cal.add(Calendar.WEEK_OF_YEAR, -1)
                if (!isWeekCompleted(cal)) {
                    return 0
                }
            }

            while (isWeekCompleted(cal)) {
                streak++
                cal.add(Calendar.WEEK_OF_YEAR, -1)
            }
            return streak
        } else {
            val cal = Calendar.getInstance()
            var streak = 0
            val todayStr = sdf.format(cal.time)

            if (!completedDates.contains(todayStr)) {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val yesterdayStr = sdf.format(cal.time)
                if (!completedDates.contains(yesterdayStr)) {
                    return 0
                }
            }

            while (true) {
                val dateStr = sdf.format(cal.time)
                if (completedDates.contains(dateStr)) {
                    streak++
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
            return streak
        }
    }

    /**
     * Calculates overall user current streak across all active habits.
     */
    suspend fun calculateUserOverallStreak(userId: Long): Int {
        val habits = habitDao.getActiveHabits(userId)
        if (habits.isEmpty()) return 0
        var maxStreak = 0
        for (habit in habits) {
            val s = calculateCurrentStreak(habit.id)
            if (s > maxStreak) {
                maxStreak = s
            }
        }
        return maxStreak
    }

    suspend fun updateHabitNotes(habitId: Long, notes: String?) {
        habitDao.updateHabitNotes(habitId, notes)
    }

    /**
     * Calculates the longest all-time consecutive streak for a habit (days for daily, weeks for weekly).
     */
    suspend fun calculateBestStreak(habitId: Long): Int {
        val habit = habitDao.getHabitById(habitId)
        val completions = completionDao.getCompletionsForHabit(habitId)
        val isWeekly = habit?.frequency.equals("WEEKLY", ignoreCase = true)

        val completedDates = completions.filter { it.completed }
            .mapNotNull { DateUtils.parseDate(it.completionDate) }
            .distinct()
            .sorted()

        if (completedDates.isEmpty()) return 0

        if (isWeekly) {
            val cal = Calendar.getInstance()
            cal.firstDayOfWeek = Calendar.MONDAY

            var bestStreak = 1
            var currentStreak = 1
            val calPrev = Calendar.getInstance()
            val calCurr = Calendar.getInstance()
            calPrev.firstDayOfWeek = Calendar.MONDAY
            calCurr.firstDayOfWeek = Calendar.MONDAY

            for (i in 1 until completedDates.size) {
                calPrev.time = completedDates[i - 1]
                calCurr.time = completedDates[i]

                val prevWeek = calPrev.get(Calendar.YEAR) * 100 + calPrev.get(Calendar.WEEK_OF_YEAR)
                val currWeek = calCurr.get(Calendar.YEAR) * 100 + calCurr.get(Calendar.WEEK_OF_YEAR)
                if (prevWeek == currWeek) continue

                calPrev.add(Calendar.WEEK_OF_YEAR, 1)
                val isNextWeek = calPrev.get(Calendar.YEAR) == calCurr.get(Calendar.YEAR) &&
                        calPrev.get(Calendar.WEEK_OF_YEAR) == calCurr.get(Calendar.WEEK_OF_YEAR)

                if (isNextWeek) {
                    currentStreak++
                    if (currentStreak > bestStreak) {
                        bestStreak = currentStreak
                    }
                } else {
                    currentStreak = 1
                }
            }
            val activeCurrentStreak = calculateCurrentStreak(habitId)
            return maxOf(bestStreak, activeCurrentStreak)
        } else {
            var bestStreak = 1
            var currentStreak = 1
            val calPrev = Calendar.getInstance()
            val calCurr = Calendar.getInstance()

            for (i in 1 until completedDates.size) {
                calPrev.time = completedDates[i - 1]
                calCurr.time = completedDates[i]

                calPrev.add(Calendar.DAY_OF_YEAR, 1)
                val isNextDay = calPrev.get(Calendar.YEAR) == calCurr.get(Calendar.YEAR) &&
                        calPrev.get(Calendar.DAY_OF_YEAR) == calCurr.get(Calendar.DAY_OF_YEAR)

                if (isNextDay) {
                    currentStreak++
                    if (currentStreak > bestStreak) {
                        bestStreak = currentStreak
                    }
                } else {
                    currentStreak = 1
                }
            }
            val activeCurrentStreak = calculateCurrentStreak(habitId)
            return maxOf(bestStreak, activeCurrentStreak)
        }
    }

    suspend fun getAllActiveReminders(): List<HabitEntity> {
        return habitDao.getAllActiveReminders()
    }

    suspend fun insertCompletion(completion: HabitCompletionEntity): Long {
        return completionDao.insertOrUpdate(completion)
    }

    suspend fun deleteDemoCompletions(userId: Long): Int {
        return completionDao.deleteDemoCompletions(userId)
    }

    suspend fun getDemoCompletionCount(userId: Long): Int {
        return completionDao.getDemoCompletionCount(userId)
    }

    suspend fun insertDemoCompletion(habitId: Long, date: String): Long {
        val existing = completionDao.getCompletion(habitId, date)
        if (existing == null) {
            val completion = HabitCompletionEntity(
                habitId = habitId,
                completionDate = date,
                completed = true,
                isDemo = true
            )
            return completionDao.insertOrUpdate(completion)
        }
        return 0L
    }
}
