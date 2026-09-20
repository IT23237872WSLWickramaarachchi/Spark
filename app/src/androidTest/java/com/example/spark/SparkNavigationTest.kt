package com.example.spark

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.spark.data.entity.UserEntity
import com.example.spark.util.SessionManager
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests verifying ViewPager2 swipe navigation between
 * Dashboard, Statistics, and Settings, plus bottom navigation synchronization.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SparkNavigationTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUpClass() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val sessionManager = SessionManager(context)
            sessionManager.saveSession(
                UserEntity(
                    id = 1L,
                    email = "test@example.com",
                    fullName = "Test User",
                    passwordHash = "hash"
                )
            )
        }
    }

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun viewPager_isDisplayed() {
        onView(withId(R.id.viewPagerMain))
            .check(matches(isDisplayed()))
    }

    @Test
    fun bottomNav_homeIsActiveByDefault() {
        // The home indicator should be visible and active on launch
        onView(withId(R.id.indicatorHome))
            .check(matches(isDisplayed()))
    }

    @Test
    fun swipeLeft_navigatesToStatistics() {
        // Swipe left on ViewPager to go from Dashboard → Statistics
        onView(withId(R.id.viewPagerMain))
            .perform(swipeLeft())

        // Give the ViewPager animation time to settle
        Thread.sleep(500)

        // Stats indicator should now be active
        onView(withId(R.id.indicatorStats))
            .check(matches(isDisplayed()))
    }

    @Test
    fun swipeLeftTwice_navigatesToSettings() {
        // Swipe Dashboard → Statistics → Settings
        onView(withId(R.id.viewPagerMain))
            .perform(swipeLeft())
        Thread.sleep(500)

        onView(withId(R.id.viewPagerMain))
            .perform(swipeLeft())
        Thread.sleep(500)

        onView(withId(R.id.indicatorSettings))
            .check(matches(isDisplayed()))
    }

    @Test
    fun swipeRightFromStatistics_returnsToHome() {
        // Navigate to Statistics first
        onView(withId(R.id.viewPagerMain))
            .perform(swipeLeft())
        Thread.sleep(500)

        // Swipe right to return to Dashboard
        onView(withId(R.id.viewPagerMain))
            .perform(swipeRight())
        Thread.sleep(500)

        onView(withId(R.id.indicatorHome))
            .check(matches(isDisplayed()))
    }

    @Test
    fun bottomNav_tapStatsSwitchesPage() {
        // Tap the Statistics bottom nav item
        onView(withId(R.id.navItemStats))
            .perform(click())
        Thread.sleep(500)

        onView(withId(R.id.indicatorStats))
            .check(matches(isDisplayed()))
    }

    @Test
    fun bottomNav_tapSettingsSwitchesPage() {
        // Tap the Settings bottom nav item
        onView(withId(R.id.navItemSettings))
            .perform(click())
        Thread.sleep(500)

        onView(withId(R.id.indicatorSettings))
            .check(matches(isDisplayed()))
    }

    @Test
    fun bottomNav_tapHomeSwitchesBackFromSettings() {
        // Go to Settings first
        onView(withId(R.id.navItemSettings))
            .perform(click())
        Thread.sleep(500)

        // Tap Home to return
        onView(withId(R.id.navItemHome))
            .perform(click())
        Thread.sleep(500)

        onView(withId(R.id.indicatorHome))
            .check(matches(isDisplayed()))
    }

    @Test
    fun addButton_doesNotSwitchPage() {
        // Tap the Add (+) button — should open bottom sheet, not change page
        onView(withId(R.id.navItemAdd))
            .perform(click())
        Thread.sleep(500)

        // Verify Add Habit bottom sheet is displayed
        onView(withId(R.id.tvSheetTitle))
            .check(matches(isDisplayed()))

        // Dismiss the bottom sheet
        pressBack()
        Thread.sleep(500)

        // ViewPager should still be on Dashboard (page 0)
        onView(withId(R.id.indicatorHome))
            .check(matches(isDisplayed()))
    }

    @Test
    fun moodButton_doesNotSwitchPage() {
        // Tap the Mood button — should open dialog, not change page
        onView(withId(R.id.navItemMood))
            .perform(click())
        Thread.sleep(500)

        // Verify Mood Check-in dialog is displayed
        onView(withId(R.id.btnSaveMood))
            .check(matches(isDisplayed()))

        // Dismiss the dialog
        pressBack()
        Thread.sleep(500)

        // ViewPager should still be on Dashboard (page 0)
        onView(withId(R.id.indicatorHome))
            .check(matches(isDisplayed()))
    }
}
