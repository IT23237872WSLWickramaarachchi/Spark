package com.example.spark.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.spark.data.local.entity.HabitEntity
import com.example.spark.data.local.entity.HabitLogEntity
import com.example.spark.data.local.entity.MoodEntryEntity
import com.example.spark.data.repository.HabitRepository
import com.example.spark.data.repository.MoodRepository
import com.example.spark.di.ServiceLocator
import com.example.spark.domain.usecase.CalculateCompletionRateUseCase
import com.example.spark.domain.usecase.CalculateStreakUseCase
import com.example.spark.ui.custom.DayStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * ViewModel for [StatsFragment].
 * Uses .flatMapLatest on the period selector to re-query the correct date range from both
 * [HabitRepository] and [MoodRepository], computes completion rates through [CalculateCompletionRateUseCase],
 * and smooths mood trends using a 7-day rolling average.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(
    private val habitRepository: HabitRepository = ServiceLocator.habitRepository!!,
    private val moodRepository: MoodRepository = ServiceLocator.moodRepository!!,
    private val streakUseCase: CalculateStreakUseCase = CalculateStreakUseCase(),
    private val completionRateUseCase: CalculateCompletionRateUseCase = CalculateCompletionRateUseCase(),
    sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(5000),
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _period = MutableStateFlow<Period>(Period.WEEK)
    val period: StateFlow<Period> = _period.asStateFlow()

    val uiState: StateFlow<StatsUiState> = _period.flatMapLatest { selectedPeriod ->
        val today = todayProvider()
        val startDate = when (selectedPeriod) {
            Period.WEEK -> today.with(DayOfWeek.MONDAY)
            Period.MONTH -> today.minusDays(29)
        }
        // Query mood entries with a 6-day lead-in to compute 7-day rolling average
        val moodLeadInStartDate = startDate.minusDays(6)

        combine(
            habitRepository.getAllHabits(),
            habitRepository.getLogsInRange(startDate, today),
            habitRepository.getLogsInRange(today.minusDays(90), today),
            moodRepository.getMoodEntriesInRange(moodLeadInStartDate, today)
        ) { habits, periodLogs, streakLogs, moods ->
            computeStats(selectedPeriod, habits, periodLogs, streakLogs, moods, today, startDate)
        }
    }.stateIn(
        scope = viewModelScope,
        started = sharingStarted,
        initialValue = StatsUiState.initial()
    )

    fun onPeriodChange(period: Period) {
        _period.value = period
    }

    fun setPeriod(period: Period) = onPeriodChange(period)

    private fun computeStats(
        period: Period,
        habits: List<HabitEntity>,
        periodLogs: List<HabitLogEntity>,
        allLogs: List<HabitLogEntity>,
        moods: List<MoodEntryEntity>,
        today: LocalDate,
        startDate: LocalDate
    ): StatsUiState {
        val activeHabits = habits.filter { !it.isArchived }
        val totalActiveHabits = activeHabits.size

        // Today's counts and rate
        val todayCompletedCount = periodLogs.count { it.completedDate == today && it.isCompleted }
        val todaysRate = completionRateUseCase.forDay(totalActiveHabits, todayCompletedCount)

        // Streak
        val streakResult = streakUseCase.execute(allLogs, today)
        val currentStreak = streakResult.currentStreak

        // Period Completion Rate
        val (completionPercent, completionSubtitle) = when (period) {
            Period.WEEK -> {
                val elapsedDays = maxOf(1, (ChronoUnit.DAYS.between(startDate, today) + 1).toInt())
                val rate = completionRateUseCase.forPeriod(totalActiveHabits, periodLogs, elapsedDays)
                rate to "This Week"
            }
            Period.MONTH -> {
                val rate = completionRateUseCase.forPeriod(totalActiveHabits, periodLogs, 30)
                rate to "Past 30 Days"
            }
        }

        // 7-day strip for Monday to Sunday of the current week
        val monday = today.with(DayOfWeek.MONDAY)
        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
        val dayStats = ArrayList<DayStat>(7)
        val dayLogStatus = (0..6).map { offset ->
            val date = monday.plusDays(offset.toLong())
            val completedOnDay = allLogs.count { it.completedDate == date && it.isCompleted }
            val isFullyCompleted = totalActiveHabits > 0 && completedOnDay >= totalActiveHabits
            val percent = if (totalActiveHabits > 0) {
                ((completedOnDay.toDouble() / totalActiveHabits) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }

            val status = when {
                date.isAfter(today) -> DayStatus.FUTURE
                date == today -> DayStatus.TODAY
                isFullyCompleted || percent >= 70 -> DayStatus.COMPLETED
                else -> DayStatus.MISSED
            }

            dayStats.add(
                DayStat(
                    date = date,
                    dayLabel = dayLabels[offset],
                    status = status,
                    completionPercent = percent,
                    isToday = (date == today)
                )
            )
            isFullyCompleted
        }

        // 7-day rolling average mood trend
        val (moodTrendPoints, moodPoints, moodTrendStatus, isTrendPositive) =
            computeSmoothedMoodTrend(period, moods, startDate, today)

        return StatsUiState(
            period = period,
            completionPercent = completionPercent,
            dayLogStatus = dayLogStatus,
            moodTrendPoints = moodTrendPoints,
            currentStreak = currentStreak,
            todaysRate = todaysRate,
            completionRatePercent = completionPercent,
            completionSubtitle = completionSubtitle,
            dayStats = dayStats,
            moodPoints = moodPoints,
            moodTrendStatus = moodTrendStatus,
            isTrendPositive = isTrendPositive,
            streakDays = currentStreak,
            todayCompletedCount = todayCompletedCount,
            todayTotalCount = totalActiveHabits
        )
    }

    /**
     * Computes a 7-day rolling average for mood entries across the period.
     * Each target day in the period calculates the mean mood score of entries in [day - 6 days, day].
     */
    private fun computeSmoothedMoodTrend(
        period: Period,
        moods: List<MoodEntryEntity>,
        startDate: LocalDate,
        today: LocalDate
    ): MoodTrendResult {
        val totalPointsCount = if (period == Period.WEEK) 7 else 30
        val targetDates = (0 until totalPointsCount).map { i ->
            if (period == Period.WEEK) {
                val monday = today.with(DayOfWeek.MONDAY)
                monday.plusDays(i.toLong())
            } else {
                today.minusDays((totalPointsCount - 1 - i).toLong())
            }
        }

        if (moods.isEmpty()) {
            val defaultScores = if (period == Period.WEEK) {
                listOf(3.0f, 3.5f, 3.2f, 3.8f, 4.0f, 3.7f, 4.1f)
            } else {
                List(30) { index -> 3.0f + (index % 5) * 0.2f }
            }
            val chartPoints = defaultScores.mapIndexed { idx, score ->
                MoodChartPoint(idx.toFloat(), score, targetDates[idx])
            }
            return MoodTrendResult(defaultScores, chartPoints, "Stable", true)
        }

        // Calculate 7-day rolling average for each target day
        val smoothedScores = targetDates.map { date ->
            val windowStart = date.minusDays(6)
            val windowEntries = moods.filter { !it.date.isBefore(windowStart) && !it.date.isAfter(date) }
            if (windowEntries.isNotEmpty()) {
                windowEntries.map { it.moodScore }.average().toFloat()
            } else {
                // Nearest or fallback score
                val closest = moods.minByOrNull { Math.abs(ChronoUnit.DAYS.between(it.date, date)) }
                closest?.moodScore?.toFloat() ?: 3.0f
            }
        }

        val chartPoints = smoothedScores.mapIndexed { idx, score ->
            MoodChartPoint(idx.toFloat(), score, targetDates[idx])
        }

        // Determine trend: compare first half vs second half average
        val halfSize = maxOf(1, smoothedScores.size / 2)
        val firstHalfAvg = smoothedScores.take(halfSize).average()
        val secondHalfAvg = smoothedScores.takeLast(halfSize).average()
        val diff = secondHalfAvg - firstHalfAvg

        val (status, isPositive) = when {
            diff > 0.15 -> "Positive" to true
            diff < -0.15 -> "Declining" to false
            else -> "Stable" to true
        }

        return MoodTrendResult(smoothedScores, chartPoints, status, isPositive)
    }

    private data class MoodTrendResult(
        val trendPoints: List<Float>,
        val chartPoints: List<MoodChartPoint>,
        val status: String,
        val isPositive: Boolean
    )

    class Factory(
        private val habitRepository: HabitRepository,
        private val moodRepository: MoodRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatsViewModel(habitRepository, moodRepository) as T
        }
    }
}
