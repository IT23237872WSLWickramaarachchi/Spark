package com.example.spark.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spark.data.entity.HabitCompletionEntity
import com.example.spark.data.entity.HabitEntity
import com.example.spark.data.entity.MoodEntryEntity
import com.example.spark.data.repository.HabitRepository
import com.example.spark.data.repository.MoodRepository
import com.example.spark.model.HabitItemUiModel
import com.example.spark.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardViewModel(
    private val habitRepository: HabitRepository,
    private val moodRepository: MoodRepository,
    private val userId: Long
) : ViewModel() {

    private val today = DateUtils.getTodayDateString()

    private val activeHabitsLiveData = habitRepository.getActiveHabitsLiveData(userId)
    private val completionsLiveData = habitRepository.getCompletionsForUserAndDateLiveData(userId, today)

    val habitItems = MediatorLiveData<List<HabitItemUiModel>>()

    private val _streakDays = MutableLiveData(0)
    val streakDays: LiveData<Int> = _streakDays

    val todayMood: LiveData<MoodEntryEntity?> = moodRepository.getMoodForDateLiveData(userId, today)

    init {
        habitItems.addSource(activeHabitsLiveData) { habits ->
            combineHabitsAndCompletions(habits, completionsLiveData.value)
        }
        habitItems.addSource(completionsLiveData) { completions ->
            combineHabitsAndCompletions(activeHabitsLiveData.value, completions)
        }

        refreshStreak()
    }

    private fun combineHabitsAndCompletions(
        habits: List<HabitEntity>?,
        completions: List<HabitCompletionEntity>?
    ) {
        if (habits == null) {
            habitItems.value = emptyList()
            return
        }

        val completedSet = completions?.filter { it.completed }?.map { it.habitId }?.toSet() ?: emptySet()
        val list = habits.map { habit ->
            HabitItemUiModel.from(habit, isCompletedToday = completedSet.contains(habit.id))
        }
        habitItems.value = list
    }

    fun toggleHabit(habitId: Long) {
        viewModelScope.launch {
            habitRepository.toggleHabitCompletion(habitId, today)
            refreshStreak()
        }
    }

    fun refreshStreak() {
        viewModelScope.launch {
            val streak = withContext(Dispatchers.IO) {
                habitRepository.calculateUserOverallStreak(userId)
            }
            _streakDays.postValue(streak)
        }
    }

    fun saveMood(level: Int, note: String?) {
        viewModelScope.launch {
            moodRepository.saveMood(userId, today, level, note)
        }
    }

    fun addNewHabit(name: String, category: String, hour: Int = 8, minute: Int = 0) {
        viewModelScope.launch {
            val entity = HabitEntity(
                userId = userId,
                name = name,
                category = category,
                frequency = "DAILY",
                reminderEnabled = true,
                reminderHour = hour,
                reminderMinute = minute
            )
            habitRepository.insertHabit(entity)
        }
    }


    class Factory(
        private val habitRepository: HabitRepository,
        private val moodRepository: MoodRepository,
        private val userId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                return DashboardViewModel(habitRepository, moodRepository, userId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
