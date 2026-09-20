package com.example.spark

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.spark.data.AppDatabase
import com.example.spark.data.repository.MoodRepository
import com.example.spark.util.DateUtils
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests verifying that multiple mood check-ins can be
 * stored on the same day after the Room schema v3→v4 migration.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SparkMoodMultipleCheckInsTest {

    private lateinit var db: AppDatabase
    private lateinit var moodRepository: MoodRepository
    private val testUserId = 1L
    private val today = DateUtils.getTodayDateString()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.getDatabase(context)
        moodRepository = MoodRepository(db.moodEntryDao())
    }

    @After
    fun teardown() {
        // Clean up test entries
        runBlocking {
            val entries = moodRepository.getMoodsForDate(testUserId, today)
            entries.filter { it.note?.startsWith("[TEST]") == true }.forEach {
                db.moodEntryDao().delete(it)
            }
        }
    }

    @Test
    fun canInsertMultipleMoodsOnSameDay() = runBlocking {
        // Get baseline count
        val initialCount = db.moodEntryDao().getMoodCountForDate(testUserId, today)

        // Insert 3 mood entries for today
        moodRepository.saveMood(testUserId, today, 3, "[TEST] Morning check-in", tags = "Calm,Focused")
        moodRepository.saveMood(testUserId, today, 4, "[TEST] Afternoon check-in", tags = "Energetic")
        moodRepository.saveMood(testUserId, today, 5, "[TEST] Evening check-in", tags = "Grateful,Relaxed")

        // Verify count increased by 3
        val newCount = db.moodEntryDao().getMoodCountForDate(testUserId, today)
        assertEquals("Should have 3 more mood entries", initialCount + 3, newCount)
    }

    @Test
    fun latestMoodIsReturnedFirst() = runBlocking {
        // Insert two entries with different levels
        moodRepository.saveMood(testUserId, today, 2, "[TEST] Earlier check-in")
        Thread.sleep(50) // Ensure different timestamp
        moodRepository.saveMood(testUserId, today, 5, "[TEST] Latest check-in")

        // Get latest mood for today
        val latest = moodRepository.getMoodForDate(testUserId, today)
        assertNotNull("Latest mood should not be null", latest)
        assertEquals("Latest mood should be level 5", 5, latest!!.moodLevel)
    }

    @Test
    fun allMoodsForDateAreReturned() = runBlocking {
        val initialEntries = moodRepository.getMoodsForDate(testUserId, today)
        val initialCount = initialEntries.size

        // Insert 2 new entries
        moodRepository.saveMood(testUserId, today, 3, "[TEST] Check-in A", tags = "Tired")
        moodRepository.saveMood(testUserId, today, 4, "[TEST] Check-in B", tags = "Focused")

        val allEntries = moodRepository.getMoodsForDate(testUserId, today)
        assertTrue("Should have at least 2 more entries", allEntries.size >= initialCount + 2)
    }

    @Test
    fun timestampAndTimeArePopulated() = runBlocking {
        moodRepository.saveMood(testUserId, today, 4, "[TEST] Timestamp test")

        val latest = moodRepository.getMoodForDate(testUserId, today)
        assertNotNull("Entry should exist", latest)
        assertTrue("Timestamp should be positive", latest!!.timestamp > 0)
        assertTrue("Time should not be empty", latest.time.isNotEmpty())
        assertTrue("Time should contain colon (HH:mm format)", latest.time.contains(":"))
    }

    @Test
    fun moodsBetweenReturnsAllEntries() = runBlocking {
        // Insert entries for today
        moodRepository.saveMood(testUserId, today, 3, "[TEST] Between test A")
        moodRepository.saveMood(testUserId, today, 5, "[TEST] Between test B")

        val entries = moodRepository.getMoodsBetween(testUserId, today, today)
        assertTrue("getMoodsBetween should return multiple entries for today", entries.size >= 2)
    }
}
