package com.example.spark

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.spark.databinding.ActivityMainBinding
import com.example.spark.ui.auth.LoginActivity
import com.example.spark.ui.dashboard.DashboardFragment
import com.example.spark.ui.habits.AddHabitBottomSheet
import com.example.spark.ui.mood.MoodCheckInDialog
import com.example.spark.ui.settings.SettingsFragment
import com.example.spark.ui.statistics.StatisticsFragment
import com.example.spark.util.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    private enum class NavDestination {
        HOME,
        STATS,
        SETTINGS
    }

    private var currentDestination: NavDestination = NavDestination.HOME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Schedule configurable daily mood reminder
        com.example.spark.notifications.MoodReminderScheduler.scheduleMoodReminder(this)

        setupBottomNavigation()
        setupBackNavigation()

        if (savedInstanceState == null) {
            selectDestination(NavDestination.HOME, isInitial = true)
            if (intent.getBooleanExtra(com.example.spark.notifications.MoodReminderReceiver.EXTRA_OPEN_MOOD_DIALOG, false)) {
                val dialog = MoodCheckInDialog.newInstance()
                dialog.show(supportFragmentManager, MoodCheckInDialog.TAG)
            }
        } else {
            updateNavIndicators(currentDestination)
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentDestination != NavDestination.HOME) {
                    selectDestination(NavDestination.HOME)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.navItemHome.setOnClickListener {
            selectDestination(NavDestination.HOME)
        }

        binding.navItemStats.setOnClickListener {
            selectDestination(NavDestination.STATS)
        }

        binding.navItemAdd.setOnClickListener {
            val sheet = AddHabitBottomSheet.newInstance()
            sheet.show(supportFragmentManager, AddHabitBottomSheet.TAG)
        }

        binding.navItemMood.setOnClickListener {
            val dialog = MoodCheckInDialog.newInstance()
            dialog.show(supportFragmentManager, MoodCheckInDialog.TAG)
        }

        binding.navItemSettings.setOnClickListener {
            selectDestination(NavDestination.SETTINGS)
        }
    }

    private fun selectDestination(destination: NavDestination, isInitial: Boolean = false) {
        if (!isInitial && destination == currentDestination) {
            return
        }

        currentDestination = destination
        updateNavIndicators(destination)

        val fragment = when (destination) {
            NavDestination.HOME -> DashboardFragment.newInstance()
            NavDestination.STATS -> StatisticsFragment.newInstance()
            NavDestination.SETTINGS -> SettingsFragment.newInstance()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    fun navigateToHome() {
        selectDestination(NavDestination.HOME)
    }

    fun navigateToSettings() {
        selectDestination(NavDestination.SETTINGS)
    }

    private fun updateNavIndicators(active: NavDestination) {
        val activeColor = ContextCompat.getColor(this, R.color.on_primary)
        val inactiveColor = ContextCompat.getColor(this, R.color.on_surface_variant)

        // Reset all destinations
        binding.indicatorHome.background = null
        binding.ivNavHome.imageTintList = ColorStateList.valueOf(inactiveColor)

        binding.indicatorStats.background = null
        binding.ivNavStats.imageTintList = ColorStateList.valueOf(inactiveColor)

        binding.indicatorSettings.background = null
        binding.ivNavSettings.imageTintList = ColorStateList.valueOf(inactiveColor)

        // Highlight selected destination
        when (active) {
            NavDestination.HOME -> {
                binding.indicatorHome.setBackgroundResource(R.drawable.bg_nav_active_pill)
                binding.ivNavHome.imageTintList = ColorStateList.valueOf(activeColor)
            }
            NavDestination.STATS -> {
                binding.indicatorStats.setBackgroundResource(R.drawable.bg_nav_active_pill)
                binding.ivNavStats.imageTintList = ColorStateList.valueOf(activeColor)
            }
            NavDestination.SETTINGS -> {
                binding.indicatorSettings.setBackgroundResource(R.drawable.bg_nav_active_pill)
                binding.ivNavSettings.imageTintList = ColorStateList.valueOf(activeColor)
            }
        }
    }
}
