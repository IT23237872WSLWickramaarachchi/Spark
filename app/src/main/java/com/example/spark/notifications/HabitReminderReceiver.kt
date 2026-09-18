package com.example.spark.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.spark.R
import com.example.spark.data.AppDatabase
import com.example.spark.data.repository.HabitRepository
import com.example.spark.ui.habits.HabitDetailsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(HabitReminderScheduler.EXTRA_HABIT_ID, -1L)
        val habitName = intent.getStringExtra(HabitReminderScheduler.EXTRA_HABIT_NAME) ?: "Your Habit"

        if (habitId <= 0L) return

        NotificationHelper.createNotificationChannel(context)

        val detailIntent = Intent(context, HabitDetailsActivity::class.java).apply {
            putExtra(HabitDetailsActivity.EXTRA_HABIT_ID, habitId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(
            context,
            habitId.toInt(),
            detailIntent,
            flags
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_HABIT_REMINDERS)
            .setSmallIcon(R.drawable.ic_flare)
            .setContentTitle("Time for $habitName")
            .setContentText("Take a gentle moment to complete your habit today.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(habitId.toInt(), notification)
        } catch (e: SecurityException) {
            // Android 13+ permission not granted
        }

        // Reschedule for next day if active
        val db = AppDatabase.getDatabase(context)
        val repository = HabitRepository(db.habitDao(), db.habitCompletionDao())
        CoroutineScope(Dispatchers.IO).launch {
            val habit = repository.getHabitById(habitId)
            if (habit != null && habit.reminderEnabled && !habit.isArchived) {
                HabitReminderScheduler.scheduleReminder(context, habit)
            }
        }
    }
}
