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
import com.example.spark.util.DateUtils
import com.example.spark.util.StatisticsCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DayRateUiModel(
    val dayLetter: String,
    val date: String,
    val rate: Float,
    val isToday: Boolean
)

data class MoodPointUiModel(
    val xIndex: Float,
    val moodLevel: Float,
    val date: String
)

data class TagCountUiModel(
    val name: String,
    val count: Int
)

data class StatisticsUiState(
    val isWeek: Boolean = true,
    val completionPercentage: Int = 0,
    val dailyRates: List<DayRateUiModel> = emptyList(),
    val moodTrendPoints: List<MoodPointUiModel> = emptyList(),
    val moodAverage: Double? = null,
    val moodTrendLabel: String = "No Data",
    val moodCount: Int = 0,
    val moodProgressiveNote: String = "",
    val topMoodTags: List<TagCountUiModel> = emptyList(),
    val habitMoodInsight: String = "",
    val currentStreak: Int = 0,
    val todayCompletedCount: Int = 0,
    val todayTotalCount: Int = 0
)

class StatisticsViewModel(
    private val habitRepository: HabitRepository,
    private val moodRepository: MoodRepository,
    private val userId: Long
) : ViewModel() {

    private val _isWeekSelected = MutableLiveData(true)
    val isWeekSelected: LiveData<Boolean> = _isWeekSelected

    private val _uiState = MediatorLiveData<StatisticsUiState>()
    val uiState: LiveData<StatisticsUiState> = _uiState

    private var currentActiveHabits: List<HabitEntity> = emptyList()
    private var currentCompletions: List<HabitCompletionEntity> = emptyList()
    private var currentMoods: List<MoodEntryEntity> = emptyList()
    private var currentStreakValue: Int = 0

    private var activeHabitsSource: LiveData<List<HabitEntity>>? = null
    private var completionsSource: LiveData<List<HabitCompletionEntity>>? = null
    private var moodsSource: LiveData<List<MoodEntryEntity>>? = null

    init {
        _uiState.value = StatisticsUiState()
        setupDataSources()
        refreshStreak()
    }

    fun setRange(isWeek: Boolean) {
        if (_isWeekSelected.value == isWeek) return
        _isWeekSelected.value = isWeek
        setupDataSources()
    }

    private fun setupDataSources() {
        val isWeek = _isWeekSelected.value ?: true
        val dates = if (isWeek) {
            DateUtils.getCurrentWeekDateStrings()
        } else {
            DateUtils.getPast30Days()
        }

        val startDate = dates.first()
        val endDate = dates.last()

        // Remove previous sources to avoid duplicate emissions
        completionsSource?.let { _uiState.removeSource(it) }
        moodsSource?.let { _uiState.removeSource(it) }

        if (activeHabitsSource == null) {
            val habitsLd = habitRepository.getActiveHabitsLiveData(userId)
            activeHabitsSource = habitsLd
            _uiState.addSource(habitsLd) { habits ->
                currentActiveHabits = habits ?: emptyList()
                recalculateState()
            }
        }

        val compLd = habitRepository.getCompletionsForUserBetweenLiveData(userId, startDate, endDate)
        completionsSource = compLd
        _uiState.addSource(compLd) { completions ->
            currentCompletions = completions ?: emptyList()
            recalculateState()
        }

        val moodLd = moodRepository.getMoodsBetweenLiveData(userId, startDate, endDate)
        moodsSource = moodLd
        _uiState.addSource(moodLd) { moods ->
            currentMoods = moods ?: emptyList()
            recalculateState()
        }

        recalculateState()
    }

    fun refreshStreak() {
        viewModelScope.launch {
            val streak = withContext(Dispatchers.IO) {
                habitRepository.calculateUserOverallStreak(userId)
            }
            currentStreakValue = streak
            recalculateState()
        }
    }

    private fun recalculateState() {
        val isWeek = _isWeekSelected.value ?: true
        val today = DateUtils.getTodayDateString()
        val dates = if (isWeek) {
            DateUtils.getCurrentWeekDateStrings()
        } else {
            DateUtils.getPast30Days()
        }

        // 1. Overall completion percentage for the period
        val overallRate = StatisticsCalculator.calculatePeriodSuccessRate(
            dates,
            currentActiveHabits,
            currentCompletions
        )

        // 2. Daily completion rates
        val rawDailyRates = StatisticsCalculator.computeDailyRates(
            dates,
            currentActiveHabits,
            currentCompletions
        )

        val dailyModels = dates.mapIndexed { index, dateStr ->
            DayRateUiModel(
                dayLetter = DateUtils.getDayOfWeekLetter(dateStr),
                date = dateStr,
                rate = rawDailyRates.getOrElse(index) { 0.0 }.toFloat(),
                isToday = (dateStr == today)
            )
        }

        // 3. Mood trend points and label
        // Map moods by date
        val moodsByDate = currentMoods.associateBy { it.date }
        val moodPoints = mutableListOf<MoodPointUiModel>()
        var pointIndex = 0f
        dates.forEach { dateStr ->
            val mood = moodsByDate[dateStr]
            if (mood != null) {
                moodPoints.add(
                    MoodPointUiModel(
                        xIndex = pointIndex,
                        moodLevel = mood.moodLevel.toFloat(),
                        date = dateStr
                    )
                )
            }
            pointIndex += 1f
        }

        val moodAvg = StatisticsCalculator.calculateMoodAverage(currentMoods)
        val trendLabel = StatisticsCalculator.getMoodTrendLabel(moodAvg)
        val progressiveNote = StatisticsCalculator.getMoodProgressiveNote(currentMoods.size)
        val topTags = StatisticsCalculator.calculateTopMoodTags(currentMoods)
            .take(5)
            .map { TagCountUiModel(it.first, it.second) }
        val habitMoodInsight = StatisticsCalculator.calculateHabitMoodInsight(
            dates,
            currentActiveHabits,
            currentCompletions,
            currentMoods
        )

        // 4. Today's rate
        val (todayComp, todayTotal) = StatisticsCalculator.calculateTodayRate(
            today,
            currentActiveHabits,
            currentCompletions
        )

        _uiState.value = StatisticsUiState(
            isWeek = isWeek,
            completionPercentage = overallRate,
            dailyRates = dailyModels,
            moodTrendPoints = moodPoints,
            moodAverage = moodAvg,
            moodTrendLabel = trendLabel,
            moodCount = currentMoods.size,
            moodProgressiveNote = progressiveNote,
            topMoodTags = topTags,
            habitMoodInsight = habitMoodInsight,
            currentStreak = currentStreakValue,
            todayCompletedCount = todayComp,
            todayTotalCount = todayTotal
        )
    }

    class Factory(
        private val habitRepository: HabitRepository,
        private val moodRepository: MoodRepository,
        private val userId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
                return StatisticsViewModel(habitRepository, moodRepository, userId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
