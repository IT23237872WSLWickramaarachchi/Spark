package com.example.spark.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.spark.ui.dashboard.DashboardFragment
import com.example.spark.ui.settings.SettingsFragment
import com.example.spark.ui.statistics.StatisticsFragment

/**
 * Adapter for the main ViewPager2 that hosts the three primary screens.
 * Position 0: Dashboard, Position 1: Statistics, Position 2: Settings.
 */
class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    companion object {
        const val PAGE_DASHBOARD = 0
        const val PAGE_STATISTICS = 1
        const val PAGE_SETTINGS = 2
        const val PAGE_COUNT = 3
    }

    override fun getItemCount(): Int = PAGE_COUNT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            PAGE_DASHBOARD -> DashboardFragment.newInstance()
            PAGE_STATISTICS -> StatisticsFragment.newInstance()
            PAGE_SETTINGS -> SettingsFragment.newInstance()
            else -> DashboardFragment.newInstance()
        }
    }
}
