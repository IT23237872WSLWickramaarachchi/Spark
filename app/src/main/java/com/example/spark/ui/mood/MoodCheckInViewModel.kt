package com.example.spark.ui.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spark.data.local.entity.MoodEntryEntity
import com.example.spark.data.repository.MoodRepository
import com.example.spark.di.ServiceLocator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Form state for the daily mood check-in.
 * [selectedMoodIndex] is 0-indexed (0 to 4) representing moods 1 (Difficult) through 5 (Radiant).
 * [isSaveEnabled] indicates whether a mood rating has been selected.
 */
data class MoodFormState(
    val selectedMoodIndex: Int? = null,
    val note: String = "",
    val isSaveEnabled: Boolean = selectedMoodIndex != null
)

/**
 * ViewModel managing daily mood check-in form state and actions.
 *
 * Implements shame-free skip: [skip] dismisses without database writes,
 * leaving streaks and habit metrics untouched.
 */
class MoodCheckInViewModel(
    private val moodRepository: MoodRepository = ServiceLocator.moodRepository!!,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _formState = MutableStateFlow(MoodFormState())
    val formState: StateFlow<MoodFormState> = _formState.asStateFlow()

    private val _dismissEvent = MutableSharedFlow<Unit>()
    val dismissEvent: SharedFlow<Unit> = _dismissEvent.asSharedFlow()

    fun onMoodSelect(index: Int) {
        _formState.update { current ->
            current.copy(
                selectedMoodIndex = index,
                isSaveEnabled = true
            )
        }
    }

    fun onNoteChange(note: String) {
        _formState.update { current ->
            current.copy(note = note)
        }
    }

    fun saveMood(userId: Long = 1L) {
        val state = _formState.value
        val index = state.selectedMoodIndex ?: return
        val moodScore = (index + 1).coerceIn(1, 5)

        viewModelScope.launch {
            moodRepository.insertMoodEntry(
                MoodEntryEntity(
                    userId = userId,
                    date = todayProvider(),
                    moodScore = moodScore,
                    note = state.note.trim().takeIf { it.isNotBlank() }
                )
            )
            _dismissEvent.emit(Unit)
        }
    }

    /**
     * Shame-free skip: dismisses the dialog without writing to the database.
     * No habit, streak, or completion records are modified or penalized.
     */
    fun skip() {
        viewModelScope.launch {
            _dismissEvent.emit(Unit)
        }
    }

    class Factory(
        private val moodRepository: MoodRepository,
        private val todayProvider: () -> LocalDate = { LocalDate.now() }
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MoodCheckInViewModel::class.java)) {
                return MoodCheckInViewModel(moodRepository, todayProvider) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
