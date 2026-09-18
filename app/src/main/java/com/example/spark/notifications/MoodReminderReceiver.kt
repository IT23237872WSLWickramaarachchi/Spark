package com.example.spark.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.spark.MainActivity
import com.example.spark.R
import com.example.spark.data.AppDatabase
import com.example.spark.util.DateUtils
import com.example.spark.util.PreferenceHelper
import com.example.spark.util.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MoodReminderReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_ID_MOOD = 2001
        const val EXTRA_OPEN_MOOD_DIALOG = "extra_open_mood_dialog"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prefHelper = PreferenceHelper(context)
        if (!prefHelper.isNotificationsMasterEnabled) {
            return
        }

        NotificationHelper.createNotificationChannel(context)

        val sessionManager = SessionManager(context)
        val userId = sessionManager.getCurrentUserId()
        if (userId <= 0) {
            // User not logged in, reschedule for tomorrow
            MoodReminderScheduler.scheduleMoodReminder(context)
            return
        }

        val db = AppDatabase.getDatabase(context)
        val today = DateUtils.getTodayDateString()

        CoroutineScope(Dispatchers.IO).launch {
            // If today's mood is already recorded, suppress notification
            val existingMood = db.moodEntryDao().getMoodForDate(userId, today)
            if (existingMood == null) {
                val latestMood = db.moodEntryDao().getLatestMood(userId)
                showMoodNotification(context, latestMood?.moodLevel)
            }

            // Reschedule for next day
            MoodReminderScheduler.scheduleMoodReminder(context)
        }
    }

    private fun showMoodNotification(context: Context, lastMoodLevel: Int?) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_MOOD_DIALOG, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_MOOD,
            launchIntent,
            flags
        )

        val (notifTitle, notifText) = com.example.spark.util.MoodMessageProvider.getReminderNotificationContent(lastMoodLevel)

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_MOOD_REMINDERS)
            .setSmallIcon(R.drawable.ic_flare)
            .setContentTitle(notifTitle)
            .setContentText(notifText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_MOOD, notification)
        } catch (e: SecurityException) {
            // Notification permission not granted on Android 13+
        }
    }
}
