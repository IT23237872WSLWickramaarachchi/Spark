package com.example.spark.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.spark.data.local.SparkDatabase
import com.example.spark.data.local.dao.HabitDao
import com.example.spark.data.local.dao.MoodDao
import com.example.spark.data.local.dao.UserDao
import com.example.spark.data.local.entity.HabitEntity
import com.example.spark.data.local.entity.HabitLogEntity
import com.example.spark.data.local.entity.MoodEntryEntity
import com.example.spark.data.local.entity.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDate

/**
 * Room integration tests running against an in-memory SQLite database.
 * Verifies relational integrity, cascade deletes, and boundary queries.
 */
@RunWith(AndroidJUnit4::class)
class SparkDatabaseTest {

    private lateinit var db: SparkDatabase
    private lateinit var userDao: UserDao
    private lateinit var habitDao: HabitDao
    private lateinit var moodDao: MoodDao

    private val testUser = UserEntity(
        id = 1L,
        name = "Sakith",
        email = "sakith@spark.com",
        passwordHash = "secure_hash_123"
    )

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SparkDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        userDao = db.userDao()
        habitDao = db.habitDao()
        moodDao = db.moodDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun habitDao_insertThenQuery_returnsCorrectRow() = runTest {
        // Insert foreign key parent user
        userDao.register(testUser)

        val habit = HabitEntity(
            id = 10L,
            userId = testUser.id,
            title = "Morning Meditation",
            category = "Mindfulness",
            frequency = "Daily"
        )
        val insertedId = habitDao.insertHabit(habit)
        assertEquals(10L, insertedId)

        val queried = habitDao.getHabitById(10L)
        assertNotNull("Queried habit should not be null", queried)
        assertEquals(10L, queried?.id)
        assertEquals(testUser.id, queried?.userId)
        assertEquals("Morning Meditation", queried?.title)
        assertEquals("Mindfulness", queried?.category)
        assertEquals("Daily", queried?.frequency)
    }

    @Test
    fun deletingHabitEntity_cascadesToDeleteHabitLogEntityChildren() = runTest {
        // Insert foreign key parent user
        userDao.register(testUser)

        // Insert habit
        val habit = HabitEntity(
            id = 20L,
            userId = testUser.id,
            title = "Drink Water",
            category = "Health"
        )
        habitDao.insertHabit(habit)

        // Insert multiple completion logs referencing habitId = 20L
        val today = LocalDate.of(2026, 9, 12)
        habitDao.insertLog(HabitLogEntity(id = 101L, habitId = 20L, completedDate = today, isCompleted = true))
        habitDao.insertLog(HabitLogEntity(id = 102L, habitId = 20L, completedDate = today.minusDays(1), isCompleted = true))
        habitDao.insertLog(HabitLogEntity(id = 103L, habitId = 20L, completedDate = today.minusDays(2), isCompleted = true))

        // Confirm logs exist before deletion
        val initialLogs = habitDao.getLogsForHabit(20L).first()
        assertEquals(3, initialLogs.size)

        // Delete the parent HabitEntity
        habitDao.deleteHabit(habit)

        // Confirm parent habit is deleted
        val queriedHabit = habitDao.getHabitById(20L)
        assertNull("HabitEntity should be deleted", queriedHabit)

        // Confirm @ForeignKey(onDelete = CASCADE) automatically removed child logs
        val remainingLogs = habitDao.getLogsForHabit(20L).first()
        assertTrue(
            "Deleting HabitEntity must cascade to delete its HabitLogEntity children",
            remainingLogs.isEmpty()
        )
    }

    @Test
    fun moodDao_dateRangeQuery_returnsOnlyEntriesWithinBounds_excludingAdjacentDates() = runTest {
        // Insert foreign key parent user
        userDao.register(testUser)

        val startDate = LocalDate.of(2026, 9, 5)
        val endDate = LocalDate.of(2026, 9, 10)

        // Boundary-adjacent: 1 day before start (MUST be excluded)
        moodDao.insertMoodEntry(
            MoodEntryEntity(id = 1L, userId = testUser.id, date = startDate.minusDays(1), moodScore = 1, note = "Before window")
        )
        // Exact start boundary (MUST be included)
        moodDao.insertMoodEntry(
            MoodEntryEntity(id = 2L, userId = testUser.id, date = startDate, moodScore = 3, note = "Start boundary")
        )
        // Inside range (MUST be included)
        moodDao.insertMoodEntry(
            MoodEntryEntity(id = 3L, userId = testUser.id, date = LocalDate.of(2026, 9, 7), moodScore = 4, note = "Mid window")
        )
        // Exact end boundary (MUST be included)
        moodDao.insertMoodEntry(
            MoodEntryEntity(id = 4L, userId = testUser.id, date = endDate, moodScore = 5, note = "End boundary")
        )
        // Boundary-adjacent: 1 day after end (MUST be excluded)
        moodDao.insertMoodEntry(
            MoodEntryEntity(id = 5L, userId = testUser.id, date = endDate.plusDays(1), moodScore = 2, note = "After window")
        )

        // Query the date range
        val inRange = moodDao.getMoodEntriesInRange(startDate, endDate).first()

        assertEquals("Should only include entries on or between startDate and endDate", 3, inRange.size)

        // Exclude boundary-adjacent dates outside the range
        assertTrue("Day before start range must be excluded", inRange.none { it.date == startDate.minusDays(1) })
        assertTrue("Day after end range must be excluded", inRange.none { it.date == endDate.plusDays(1) })

        // Include boundary dates and inner dates
        assertTrue("Start boundary date must be included", inRange.any { it.date == startDate && it.moodScore == 3 })
        assertTrue("Mid date must be included", inRange.any { it.date == LocalDate.of(2026, 9, 7) && it.moodScore == 4 })
        assertTrue("End boundary date must be included", inRange.any { it.date == endDate && it.moodScore == 5 })
    }
}
