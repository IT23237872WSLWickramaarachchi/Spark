package com.example.spark.util

import android.content.Context
import com.example.spark.data.AppDatabase
import com.example.spark.data.entity.HabitEntity
import com.example.spark.data.entity.MoodEntryEntity
import com.example.spark.data.repository.HabitRepository
import com.example.spark.data.repository.MoodRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object DemoDataGenerator {

    private data class DemoMoodTemplate(
        val daysAgo: Int,
        val level: Int,
        val tags: List<String>,
        val note: String?
    )

    private val TEMPLATES = listOf(
        DemoMoodTemplate(13, 3, listOf("Calm"), "Easy start to the week"),
        DemoMoodTemplate(12, 4, listOf("Focused", "Productive"), "Accomplished a lot of reading"),
        DemoMoodTemplate(11, 4, listOf("Energetic", "Motivated"), "Morning walk and good breakfast"),
        DemoMoodTemplate(10, 5, listOf("Grateful", "Calm"), "Wonderful sunny morning"),
        DemoMoodTemplate(9, 2, listOf("Tired"), "Late night, needed some rest"),
        DemoMoodTemplate(8, 3, listOf("Calm"), "Quiet evening"),
        DemoMoodTemplate(7, 4, listOf("Focused", "Relaxed"), "Read a full chapter and meditated"),
        DemoMoodTemplate(6, 5, listOf("Excited", "Energetic"), "Crushed my routine today!"),
        DemoMoodTemplate(5, 4, listOf("Calm", "Grateful"), "Peaceful morning coffee"),
        DemoMoodTemplate(4, 2, listOf("Stressed"), "Lots of tasks, took a deep breath"),
        DemoMoodTemplate(3, 3, listOf("Tired", "Relaxed"), "Rest day, taking things gentle"),
        DemoMoodTemplate(2, 4, listOf("Focused", "Productive"), "Steady progress and clarity"),
        DemoMoodTemplate(1, 5, listOf("Calm", "Energetic"), "Great energy and momentum"),
        DemoMoodTemplate(0, 4, listOf("Calm", "Focused"), "Mindful daily check-in")
    )

    suspend fun generateDemoHistory(
        context: Context,
        userId: Long
    ): Pair<Int, Int> {
        val db = AppDatabase.getDatabase(context)
        val moodRepo = MoodRepository(db.moodEntryDao())
        val habitRepo = HabitRepository(db.habitDao(), db.habitCompletionDao())

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var moodsInserted = 0
        var completionsInserted = 0

        // 1. Ensure user has at least 2 habits so completion charts light up
        val habits = db.habitDao().getActiveHabits(userId)
        val activeHabits = if (habits.isEmpty()) {
            val h1 = HabitEntity(
                userId = userId,
                name = "Morning Meditation",
                category = "Mindfulness",
                frequency = "DAILY",
                colorTag = "#6B5B95"
            )
            val h2 = HabitEntity(
                userId = userId,
                name = "Reading",
                category = "Learning",
                frequency = "DAILY",
                colorTag = "#4ECDC4"
            )
            val id1 = db.habitDao().insertHabit(h1)
            val id2 = db.habitDao().insertHabit(h2)
            listOf(h1.copy(id = id1), h2.copy(id = id2))
        } else {
            habits
        }

        // 2. Insert 14 days of demo moods and completions
        for (template in TEMPLATES) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -template.daysAgo)
            }
            val dateStr = sdf.format(cal.time)

            // Mood
            val tagsJoined = MoodEntryEntity.joinTags(template.tags)
            val insertedMoodId = moodRepo.insertDemoMood(
                userId = userId,
                date = dateStr,
                level = template.level,
                note = template.note,
                tags = tagsJoined
            )
            if (insertedMoodId > 0) moodsInserted++

            // Completions (complete on ~75% of days, higher on positive mood days)
            val shouldComplete = template.level >= 3 || (template.daysAgo % 2 == 0)
            if (shouldComplete) {
                activeHabits.forEach { habit ->
                    val cId = habitRepo.insertDemoCompletion(habit.id, dateStr)
                    if (cId > 0) completionsInserted++
                }
            }
        }

        return Pair(moodsInserted, completionsInserted)
    }

    suspend fun clearDemoHistory(context: Context, userId: Long): Pair<Int, Int> {
        val db = AppDatabase.getDatabase(context)
        val moodRepo = MoodRepository(db.moodEntryDao())
        val habitRepo = HabitRepository(db.habitDao(), db.habitCompletionDao())

        val moodsCleared = moodRepo.deleteDemoMoods(userId)
        val completionsCleared = habitRepo.deleteDemoCompletions(userId)
        return Pair(moodsCleared, completionsCleared)
    }

    suspend fun getDemoCounts(context: Context, userId: Long): Pair<Int, Int> {
        val db = AppDatabase.getDatabase(context)
        val moodRepo = MoodRepository(db.moodEntryDao())
        val habitRepo = HabitRepository(db.habitDao(), db.habitCompletionDao())

        val moodCount = moodRepo.getDemoMoodCount(userId)
        val completionCount = habitRepo.getDemoCompletionCount(userId)
        return Pair(moodCount, completionCount)
    }
}
