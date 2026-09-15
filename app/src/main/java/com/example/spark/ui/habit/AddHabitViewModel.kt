package com.example.spark.ui.habit

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spark.data.local.entity.HabitEntity
import com.example.spark.data.repository.HabitRepository
import com.example.spark.di.ServiceLocator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.Locale

/**
 * Form state for creating or editing a habit.
 */
data class AddHabitFormState(
    val name: String = "",
    val category: String = "Health",
    val frequency: String = "Daily",
    val reminderTime: String = "08:00",
    val colorTag: String = "#52559C",
    val isSaveEnabled: Boolean = false
)

/**
 * ViewModel for [AddHabitBottomSheetFragment].
 * Manages form state reactively and persists newly created or updated habits to [HabitRepository].
 */
class AddHabitViewModel(
    private val habitRepository: HabitRepository = ServiceLocator.habitRepository!!,
    savedStateHandle: SavedStateHandle? = null,
    private val currentUserIdProvider: () -> Long = { ServiceLocator.sessionManager?.currentUserId ?: 1L }
) : ViewModel() {

    val habitId: String? = savedStateHandle?.get<String>("habitId")
        ?: savedStateHandle?.get<Long>("habitId")?.toString()

    private val _formState = MutableStateFlow(AddHabitFormState())
    val formState: StateFlow<AddHabitFormState> = _formState.asStateFlow()

    private val _habitSaved = MutableSharedFlow<Unit>()
    val habitSaved: SharedFlow<Unit> = _habitSaved.asSharedFlow()

    init {
        if (!habitId.isNullOrBlank()) {
            viewModelScope.launch {
                habitRepository.getHabitById(habitId).collect { habit ->
                    if (habit != null) {
                        val timeStr = habit.reminderTime?.let {
                            String.format(Locale.ROOT, "%02d:%02d", it.hour, it.minute)
                        } ?: "08:00"
                        _formState.update { current ->
                            current.copy(
                                name = habit.title,
                                category = habit.category,
                                frequency = habit.frequency,
                                reminderTime = timeStr,
                                colorTag = habit.colorHex,
                                isSaveEnabled = habit.title.isNotBlank()
                            )
                        }
                    }
                }
            }
        }
    }

    fun onNameChange(name: String) {
        _formState.update { current ->
            current.copy(
                name = name,
                isSaveEnabled = name.isNotBlank()
            )
        }
    }

    fun onCategorySelect(category: String) {
        _formState.update { current ->
            current.copy(
                category = category,
                isSaveEnabled = current.name.isNotBlank()
            )
        }
    }

    fun onFrequencySelect(frequency: String) {
        _formState.update { current ->
            current.copy(
                frequency = frequency,
                isSaveEnabled = current.name.isNotBlank()
            )
        }
    }

    fun onReminderTimeSelect(reminderTime: String) {
        _formState.update { current ->
            current.copy(
                reminderTime = reminderTime,
                isSaveEnabled = current.name.isNotBlank()
            )
        }
    }

    fun onColorSelect(colorTag: String) {
        _formState.update { current ->
            current.copy(
                colorTag = colorTag,
                isSaveEnabled = current.name.isNotBlank()
            )
        }
    }

    fun onColorTagSelect(colorTag: String) = onColorSelect(colorTag)

    fun saveHabit(userId: Long = currentUserIdProvider()) {
        val state = _formState.value
        if (!state.isSaveEnabled) return

        val parsedTime = runCatching {
            LocalTime.parse(state.reminderTime)
        }.getOrNull() ?: LocalTime.of(8, 0)

        val targetDays = if (state.frequency.equals("Daily", ignoreCase = true)) 7 else 3

        viewModelScope.launch {
            val existingId = habitId?.toLongOrNull()
            if (existingId != null && existingId > 0L) {
                val updated = HabitEntity(
                    id = existingId,
                    userId = userId,
                    title = state.name.trim(),
                    category = state.category,
                    frequency = state.frequency,
                    targetDaysPerWeek = targetDays,
                    reminderTime = parsedTime,
                    colorHex = state.colorTag
                )
                habitRepository.updateHabit(updated)
            } else {
                val newHabit = HabitEntity(
                    userId = userId,
                    title = state.name.trim(),
                    category = state.category,
                    frequency = state.frequency,
                    targetDaysPerWeek = targetDays,
                    reminderTime = parsedTime,
                    colorHex = state.colorTag
                )
                habitRepository.insertHabit(newHabit)
            }
            _habitSaved.emit(Unit)
        }
    }

    class Factory(
        private val habitRepository: HabitRepository,
        defaultArgs: Bundle? = null,
        private val currentUserIdProvider: () -> Long = { ServiceLocator.sessionManager?.currentUserId ?: 1L }
    ) : ViewModelProvider.Factory {
        private val handle: SavedStateHandle? = defaultArgs?.let { bundle ->
            val map = mutableMapOf<String, Any?>()
            bundle.keySet().forEach { key ->
                map[key] = bundle.get(key)
            }
            SavedStateHandle(map)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddHabitViewModel(habitRepository, handle, currentUserIdProvider) as T
        }
    }
}
