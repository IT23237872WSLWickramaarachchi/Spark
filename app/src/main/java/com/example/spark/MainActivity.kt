package com.example.spark

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewpager2.widget.ViewPager2
import com.example.spark.databinding.ActivityMainBinding
import com.example.spark.ui.adapter.MainPagerAdapter
import com.example.spark.ui.auth.LoginActivity
import com.example.spark.ui.habits.AddHabitBottomSheet
import com.example.spark.ui.mood.MoodCheckInDialog
import com.example.spark.util.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

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

        setupWindowInsets()
        setupViewPager()
        setupBottomNavigation()
        setupBackNavigation()

        // Handle mood dialog launch from notification
        if (savedInstanceState == null) {
            if (intent.getBooleanExtra(com.example.spark.notifications.MoodReminderReceiver.EXTRA_OPEN_MOOD_DIALOG, false)) {
                val dialog = MoodCheckInDialog.newInstance()
                dialog.show(supportFragmentManager, MoodCheckInDialog.TAG)
            }
        }
    }

    /**
     * Apply WindowInsetsCompat to the bottom navigation card so it never overlaps
     * with the system navigation bar (gesture pill or 3-button nav) on any device.
     */
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavCard) { view, insets ->
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updatePadding(bottom = navInsets.bottom)
            insets
        }
    }

    private fun setupViewPager() {
        val adapter = MainPagerAdapter(this)
        binding.viewPagerMain.adapter = adapter
        binding.viewPagerMain.offscreenPageLimit = 2 // Keep all 3 pages alive

        // Sync bottom nav indicator when user swipes between pages
        binding.viewPagerMain.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateNavIndicators(position)
            }
        })
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.viewPagerMain.currentItem != MainPagerAdapter.PAGE_DASHBOARD) {
                    binding.viewPagerMain.currentItem = MainPagerAdapter.PAGE_DASHBOARD
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.navItemHome.setOnClickListener {
            binding.viewPagerMain.currentItem = MainPagerAdapter.PAGE_DASHBOARD
        }

        binding.navItemStats.setOnClickListener {
            binding.viewPagerMain.currentItem = MainPagerAdapter.PAGE_STATISTICS
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
            binding.viewPagerMain.currentItem = MainPagerAdapter.PAGE_SETTINGS
        }
    }

    fun navigateToHome() {
        binding.viewPagerMain.currentItem = MainPagerAdapter.PAGE_DASHBOARD
    }

    fun navigateToSettings() {
        binding.viewPagerMain.currentItem = MainPagerAdapter.PAGE_SETTINGS
    }

    private fun updateNavIndicators(activePage: Int) {
        val activeColor = ContextCompat.getColor(this, R.color.nav_active_icon)
        val inactiveColor = ContextCompat.getColor(this, R.color.nav_icon_inactive)

        // Reset all destinations
        binding.indicatorHome.background = null
        binding.ivNavHome.imageTintList = ColorStateList.valueOf(inactiveColor)

        binding.indicatorStats.background = null
        binding.ivNavStats.imageTintList = ColorStateList.valueOf(inactiveColor)

        binding.indicatorSettings.background = null
        binding.ivNavSettings.imageTintList = ColorStateList.valueOf(inactiveColor)

        // Highlight selected destination
        when (activePage) {
            MainPagerAdapter.PAGE_DASHBOARD -> {
                binding.indicatorHome.setBackgroundResource(R.drawable.bg_nav_active_pill)
                binding.ivNavHome.imageTintList = ColorStateList.valueOf(activeColor)
            }
            MainPagerAdapter.PAGE_STATISTICS -> {
                binding.indicatorStats.setBackgroundResource(R.drawable.bg_nav_active_pill)
                binding.ivNavStats.imageTintList = ColorStateList.valueOf(activeColor)
            }
            MainPagerAdapter.PAGE_SETTINGS -> {
                binding.indicatorSettings.setBackgroundResource(R.drawable.bg_nav_active_pill)
                binding.ivNavSettings.imageTintList = ColorStateList.valueOf(activeColor)
            }
        }
    }
}
