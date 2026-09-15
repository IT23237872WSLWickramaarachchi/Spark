package com.example.spark.ui.habitdetail

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.example.spark.data.repository.HabitRepository
import com.example.spark.di.ServiceLocator
import com.example.spark.domain.usecase.CalculateStreakUseCase
import com.example.spark.ui.custom.DayStatus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * ViewModel for [HabitDetailFragment].
 * Manages habit metrics, streaks, 7-day strip, notes, and habit deletion.
 */
class HabitDetailViewModel(
    private val habitRepository: HabitRepository = ServiceLocator.habitRepository!!,
    savedStateHandle: SavedStateHandle,
    private val streakUseCase: CalculateStreakUseCase = CalculateStreakUseCase(),
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(5000),
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    val habitId: String = savedStateHandle.get<String>("habitId")
        ?: savedStateHandle.get<Long>("habitId")?.toString().orEmpty()

    private val _habitDeleted = MutableSharedFlow<Unit>()
    val habitDeleted: SharedFlow<Unit> = _habitDeleted.asSharedFlow()

    val uiState: StateFlow<HabitDetailUiState> = combine(
        habitRepository.getHabitById(habitId),
        habitRepository.getLogsForHabit(habitId)
    ) { habit, logs ->
        if (habit == null) {
            HabitDetailUiState(habitId = habitId, isLoading = false)
        } else {
            val today = todayProvider()
            val streakResult = streakUseCase.execute(logs, today)
            val currentStreak = streakResult.currentStreak
            val bestStreak = streakResult.bestStreak

            // Map the last 7 habit log entity rows (Monday to Sunday of the current week)
            val monday = today.with(DayOfWeek.MONDAY)
            val weeklyStatuses = (0..6).map { i ->
                val date = monday.plusDays(i.toLong())
                val isDone = logs.any { it.completedDate == date && it.isCompleted }
                when {
                    isDone -> DayStatus.COMPLETED
                    date == today -> DayStatus.TODAY
                    date.isAfter(today) -> DayStatus.FUTURE
                    else -> DayStatus.MISSED
                }
            }

            HabitDetailUiState(
                habitId = habitId,
                title = habit.title,
                category = habit.category,
                frequency = habit.frequency,
                currentStreak = currentStreak,
                bestStreak = bestStreak,
                weeklyStatuses = weeklyStatuses,
                notes = habit.notes.orEmpty(),
                habit = habit,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = sharingStarted,
        initialValue = HabitDetailUiState.initial()
    )

    fun deleteHabit() {
        if (habitId.isBlank()) return
        viewModelScope.launch {
            habitRepository.deleteHabit(habitId)
            _habitDeleted.emit(Unit)
        }
    }

    fun updateNotes(notes: String) {
        val habit = uiState.value.habit ?: return
        viewModelScope.launch {
            habitRepository.updateHabit(habit.copy(notes = notes))
        }
    }

    class Factory(
        private val habitRepository: HabitRepository,
        owner: SavedStateRegistryOwner,
        defaultArgs: Bundle? = null
    ) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
            return HabitDetailViewModel(habitRepository, handle) as T
        }
    }
}
