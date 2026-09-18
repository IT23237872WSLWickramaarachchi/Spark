package com.example.spark.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.spark.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = AppDatabase.getDatabase(context)

            CoroutineScope(Dispatchers.IO).launch {
                val habits = db.habitDao().getAllActiveReminders()
                for (habit in habits) {
                    HabitReminderScheduler.scheduleReminder(context, habit)
                }
                MoodReminderScheduler.scheduleMoodReminder(context)
            }
        }
    }
}
