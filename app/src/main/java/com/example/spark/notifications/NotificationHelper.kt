package com.example.spark.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {

    const val CHANNEL_HABIT_REMINDERS = "spark_habit_reminders"
    private const val CHANNEL_NAME = "Habit Reminders"
    private const val CHANNEL_DESC = "Mindful daily and weekly reminders for habits"

    const val CHANNEL_MOOD_REMINDERS = "spark_mood_reminders"
    private const val CHANNEL_MOOD_NAME = "Daily Mood Reminders"
    private const val CHANNEL_MOOD_DESC = "Daily reminders to record your mood"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val habitChannel = NotificationChannel(CHANNEL_HABIT_REMINDERS, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            manager.createNotificationChannel(habitChannel)

            val moodChannel = NotificationChannel(CHANNEL_MOOD_REMINDERS, CHANNEL_MOOD_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = CHANNEL_MOOD_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            manager.createNotificationChannel(moodChannel)
        }
    }
}
