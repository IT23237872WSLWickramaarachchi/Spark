package com.example.spark.util

import com.example.spark.data.entity.HabitCompletionEntity
import com.example.spark.data.entity.HabitEntity
import com.example.spark.data.entity.MoodEntryEntity
import kotlin.math.roundToInt

object StatisticsCalculator {

    /**
     * Calculates the daily success rate as a percentage (0.0 to 100.0).
     */
    fun calculateDailySuccessRate(completedCount: Int, totalApplicableCount: Int): Double {
        if (totalApplicableCount <= 0) return 0.0
        val rate = (completedCount.toDouble() / totalApplicableCount.toDouble()) * 100.0
        return rate.coerceIn(0.0, 100.0)
    }

    /**
     * Calculates overall period success rate percentage across all dates in the range.
     * completed applicable occurrences / total applicable occurrences.
     */
    fun calculatePeriodSuccessRate(
        dates: List<String>,
        activeHabits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>
    ): Int {
        if (dates.isEmpty() || activeHabits.isEmpty()) return 0

        val totalApplicableOccurrences = dates.size * activeHabits.size
        if (totalApplicableOccurrences <= 0) return 0

        val activeHabitIds = activeHabits.map { it.id }.toSet()
        val datesSet = dates.toSet()

        // Count completed occurrences that match active habits and fall within the date range
        val completedOccurrences = completions.count { completion ->
            completion.completed &&
                    activeHabitIds.contains(completion.habitId) &&
                    datesSet.contains(completion.completionDate)
        }

        val percentage = (completedOccurrences.toDouble() / totalApplicableOccurrences.toDouble()) * 100.0
        return percentage.roundToInt().coerceIn(0, 100)
    }

    /**
     * Computes daily completion rate percentages for each date in [dates].
     */
    fun computeDailyRates(
        dates: List<String>,
        activeHabits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>
    ): List<Double> {
        if (activeHabits.isEmpty()) {
            return dates.map { 0.0 }
        }

        val activeHabitIds = activeHabits.map { it.id }.toSet()
        val totalApplicable = activeHabits.size

        // Group valid completions by date
        val completionsByDate = completions
            .filter { it.completed && activeHabitIds.contains(it.habitId) }
            .groupBy { it.completionDate }

        return dates.map { date ->
            val count = completionsByDate[date]?.size ?: 0
            calculateDailySuccessRate(count, totalApplicable)
        }
    }

    /**
     * Calculates today's rate: (completed habits today, total applicable habits today).
     */
    fun calculateTodayRate(
        todayDate: String,
        activeHabits: List<HabitEntity>,
        todayCompletions: List<HabitCompletionEntity>
    ): Pair<Int, Int> {
        val total = activeHabits.size
        if (total == 0) return Pair(0, 0)

        val activeHabitIds = activeHabits.map { it.id }.toSet()
        val completedCount = todayCompletions.count {
            it.completed && it.completionDate == todayDate && activeHabitIds.contains(it.habitId)
        }
        return Pair(completedCount, total)
    }

    /**
     * Calculates the mean of mood levels (1 to 5) in the selected period.
     */
    fun calculateMoodAverage(moods: List<MoodEntryEntity>): Double? {
        if (moods.isEmpty()) return null
        return moods.map { it.moodLevel }.average()
    }

    /**
     * Translates the average emotional state into a concise summary label.
     */
    fun getMoodTrendLabel(averageMood: Double?): String {
        return when {
            averageMood == null -> "No Data"
            averageMood >= 3.5 -> "Positive"
            averageMood >= 2.5 -> "Neutral"
            else -> "Low"
        }
    }

    /**
     * Returns a progressive empty/maturity status note based on the number of logged mood entries.
     */
    fun getMoodProgressiveNote(count: Int): String {
        return when (count) {
            0 -> "No moods logged yet. Check in today to start seeing your emotional patterns."
            1 -> "Just getting started (1 entry logged). Check in a few more days to reveal trends."
            2 -> "Just getting started (2 entries logged). Check in a few more days to reveal trends."
            in 3..6 -> "Early trend preview ($count entries logged). Keep checking in to unlock deeper insights."
            else -> ""
        }
    }

    /**
     * Aggregates the frequency of mood tags in the given list of mood entries.
     * Returns a list of (TagName, DayCount) sorted descending.
     */
    fun calculateTopMoodTags(moods: List<MoodEntryEntity>): List<Pair<String, Int>> {
        val tagCounts = mutableMapOf<String, Int>()
        moods.forEach { entry ->
            val tags = entry.getTagsList()
            tags.forEach { tag ->
                tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
            }
        }
        return tagCounts.entries
            .sortedByDescending { it.value }
            .map { Pair(it.key, it.value) }
    }

    /**
     * Calculates descriptive correlation between habit completions and logged mood levels.
     */
    fun calculateHabitMoodInsight(
        dates: List<String>,
        activeHabits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        moods: List<MoodEntryEntity>
    ): String {
        if (moods.size < 3) {
            return "Complete habits and log moods for a few more days to reveal connections."
        }

        val activeHabitIds = activeHabits.map { it.id }.toSet()
        val completedDates = completions
            .filter { it.completed && activeHabitIds.contains(it.habitId) }
            .map { it.completionDate }
            .toSet()

        val completedDayMoods = mutableListOf<Int>()
        val nonCompletedDayMoods = mutableListOf<Int>()

        moods.forEach { mood ->
            if (completedDates.contains(mood.date)) {
                completedDayMoods.add(mood.moodLevel)
            } else {
                nonCompletedDayMoods.add(mood.moodLevel)
            }
        }

        if (completedDayMoods.isEmpty() && nonCompletedDayMoods.isEmpty()) {
            return "Complete habits and log moods for a few more days to reveal connections."
        }

        if (completedDayMoods.isNotEmpty() && nonCompletedDayMoods.isNotEmpty()) {
            val compAvg = completedDayMoods.average()
            val nonCompAvg = nonCompletedDayMoods.average()
            val compAvgStr = String.format(java.util.Locale.US, "%.1f", compAvg)
            val nonCompAvgStr = String.format(java.util.Locale.US, "%.1f", nonCompAvg)
            return if (compAvg >= nonCompAvg) {
                "On days you completed habits, your mood averaged $compAvgStr compared to $nonCompAvgStr on other days."
            } else {
                "On days you completed habits, your mood averaged $compAvgStr (vs $nonCompAvgStr overall)."
            }
        }

        if (completedDayMoods.isNotEmpty()) {
            val compAvg = completedDayMoods.average()
            val compAvgStr = String.format(java.util.Locale.US, "%.1f", compAvg)
            return "On days you completed habits, your mood averaged $compAvgStr. Keep up the steady momentum!"
        }

        return "Log both habits and moods to see how your daily practices support your well-being."
    }
}
