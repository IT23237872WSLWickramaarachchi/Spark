package com.example.spark

import android.app.Application
import com.example.spark.notifications.NotificationHelper
import com.example.spark.util.PreferenceHelper

class SparkApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Apply saved dark mode theme before any Activity inflates
        PreferenceHelper(this).applyTheme()

        // Initialize notification channels
        NotificationHelper.createNotificationChannel(this)
    }
}
