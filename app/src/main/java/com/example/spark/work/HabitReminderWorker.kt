package com.example.spark.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.spark.MainActivity
import com.example.spark.R
import com.example.spark.SparkApplication
import com.example.spark.data.local.entity.HabitEntity
import com.example.spark.data.repository.HabitRepository
import com.example.spark.data.repository.UserRepository
import com.example.spark.di.ServiceLocator
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

/**
 * Periodic CoroutineWorker that checks for incomplete habits whose reminderTime
 * matches the current scheduling window, and posts notifications accordingly.
 *
 * Honors the user's notification settings (returns Result.success() early if disabled).
 */
open class HabitReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userRepository = getUserRepository()

        // Respect the Settings screen's Notifications toggle
        val prefs = userRepository.getUserPrefs()
        if (!shouldRunReminders(prefs)) {
            return Result.success()
        }

        val habitRepository = getHabitRepository()
        val today = LocalDate.now()
        val now = LocalTime.now()

        val allHabits = habitRepository.getAllHabits().first()
        val todayLogs = habitRepository.getTodayLogs(today).first()

        val dueIncompleteHabits = filterDueIncompleteHabits(allHabits, todayLogs, today, now)

        for (habit in dueIncompleteHabits) {
            sendNotification(habit)
        }

        return Result.success()
    }

    protected open fun sendNotification(habit: HabitEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val notificationManager = NotificationManagerCompat.from(applicationContext)
        if (!notificationManager.areNotificationsEnabled()) {
            return
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_HABIT_ID, habit.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            habit.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_streak_flame)
            .setContentTitle(habit.title)
            .setContentText("Gentle reminder: time for '${habit.title}'")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(habit.id.toInt(), notification)
    }

    protected open fun getHabitRepository(): HabitRepository {
        val app = applicationContext as? SparkApplication
        return app?.habitRepository ?: ServiceLocator.provideHabitRepository(applicationContext)
    }

    protected open fun getUserRepository(): UserRepository {
        val app = applicationContext as? SparkApplication
        return app?.userRepository ?: ServiceLocator.provideUserRepository(applicationContext)
    }

    companion object {
        const val WORK_NAME = "habit_reminder_work"
        const val CHANNEL_ID = "habit_reminders"
        const val CHANNEL_NAME = "Habit Reminders"
        const val SCHEDULING_WINDOW_MINUTES = 15L
        const val EXTRA_HABIT_ID = "EXTRA_HABIT_ID"

        /**
         * Returns true if user preferences allow notifications, defaulting to true if no row exists.
         */
        fun shouldRunReminders(prefs: com.example.spark.data.local.entity.UserPrefsEntity?): Boolean {
            return prefs?.notificationsEnabled ?: true
        }

        /**
         * Filters active habits for those that are not completed today and have a reminderTime
         * falling within the scheduling window.
         */
        fun filterDueIncompleteHabits(
            allHabits: List<HabitEntity>,
            todayLogs: List<com.example.spark.data.local.entity.HabitLogEntity>,
            today: LocalDate,
            currentTime: LocalTime,
            windowMinutes: Long = SCHEDULING_WINDOW_MINUTES
        ): List<HabitEntity> {
            val completedHabitIds = todayLogs
                .filter { it.isCompleted && it.completedDate == today }
                .map { it.habitId }
                .toSet()

            return allHabits.filter { habit ->
                !habit.isArchived &&
                    !completedHabitIds.contains(habit.id) &&
                    habit.reminderTime != null &&
                    isReminderMatching(habit.reminderTime, currentTime, windowMinutes)
            }
        }

        /**
         * Checks if [reminderTime] falls within the scheduling window around [currentTime].
         * Returns true if the reminder was due within the last [windowMinutes] or within
         * the next 5 minutes (to account for WorkManager execution drift).
         */
        fun isReminderMatching(
            reminderTime: LocalTime,
            currentTime: LocalTime,
            windowMinutes: Long = SCHEDULING_WINDOW_MINUTES
        ): Boolean {
            var diffMinutes = Duration.between(reminderTime, currentTime).toMinutes()
            if (diffMinutes < -720) diffMinutes += 1440
            if (diffMinutes > 720) diffMinutes -= 1440
            return diffMinutes in -5..windowMinutes
        }
    }
}
