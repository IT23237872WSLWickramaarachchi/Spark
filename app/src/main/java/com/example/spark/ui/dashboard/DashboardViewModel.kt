package com.example.spark.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.spark.data.local.SparkDatabase
import com.example.spark.data.local.entity.HabitEntity
import com.example.spark.data.local.entity.HabitLogEntity
import com.example.spark.data.local.entity.HabitWithLogs
import com.example.spark.domain.usecase.CalculateCompletionRateUseCase
import com.example.spark.domain.usecase.CalculateStreakUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel for the Dashboard screen.
 *
 * Exposes reactive [StateFlow]s for the habit list, daily completion
 * percentage, and current user's best streak — all powered by Room
 * [Flow] queries and domain use cases.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = SparkDatabase.getInstance(application)
    private val habitDao = db.habitDao()
    private val streakUseCase = CalculateStreakUseCase()
    private val completionRateUseCase = CalculateCompletionRateUseCase()

    /**
     * Currently logged-in user ID. In a full auth flow this would come
     * from a session manager; for now we default to 1L.
     */
    private val _currentUserId = MutableStateFlow(1L)
    val currentUserId: StateFlow<Long> = _currentUserId.asStateFlow()

    /** Reactive stream of all active habits with their logs. */
    val habitsWithLogs: StateFlow<List<HabitWithLogs>> = _currentUserId
        .flatMapLatest { userId ->
            if (userId > 0) habitDao.getAllHabitsWithLogs(userId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Today's completion percentage (0–100). */
    val todayCompletionPercent: StateFlow<Int> = habitsWithLogs
        .map { list -> completionRateUseCase.forDate(list, LocalDate.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Search/filter query for FR-10. */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Filter status for FR-10: null = All, true = Completed, false = Pending. */
    private val _filterCompleted = MutableStateFlow<Boolean?>(null)
    val filterCompleted: StateFlow<Boolean?> = _filterCompleted.asStateFlow()

    // ── Actions ─────────────────────────────────────────────────────────

    fun setUserId(userId: Long) {
        _currentUserId.value = userId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterCompleted(completed: Boolean?) {
        _filterCompleted.value = completed
    }

    /**
     * Toggles the completion state for a habit on a given date.
     * Inserts a log if not yet completed; deletes it if already completed.
     */
    fun toggleHabitCompletion(habitId: Long, date: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            val existingLog = habitDao.getLogForDate(habitId, date)
            if (existingLog != null) {
                habitDao.deleteLog(habitId, date)
            } else {
                habitDao.insertLog(
                    HabitLogEntity(
                        habitId = habitId,
                        completedDate = date,
                        isCompleted = true
                    )
                )
            }
        }
    }

    /**
     * Creates a new habit for the current user.
     */
    fun createHabit(title: String, category: String, frequency: String = "Daily") {
        viewModelScope.launch {
            habitDao.insertHabit(
                HabitEntity(
                    userId = _currentUserId.value,
                    title = title,
                    category = category,
                    frequency = frequency
                )
            )
        }
    }

    /**
     * Deletes a habit and all its logs (via FK cascade).
     */
    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            habitDao.deleteHabit(habit)
        }
    }

    /**
     * Calculates the streak for a specific habit.
     */
    fun calculateStreak(logs: List<HabitLogEntity>): CalculateStreakUseCase.StreakResult {
        return streakUseCase.execute(logs)
    }
}
