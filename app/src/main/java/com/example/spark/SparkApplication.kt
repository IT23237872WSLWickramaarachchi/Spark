package com.example.spark

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.spark.data.repository.HabitRepository
import com.example.spark.data.repository.MoodRepository
import com.example.spark.data.repository.UserRepository
import com.example.spark.di.ServiceLocator
import com.example.spark.work.HabitReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Application class for Spark.
 * Exposes repository instances for manual constructor injection or property access,
 * initializes NotificationChannels, and schedules periodic HabitReminderWorker.
 */
class SparkApplication : Application() {

    private val appScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
            android.util.Log.e("SparkApplication", "Uncaught coroutine error", throwable)
        }
    )

    val habitRepository: HabitRepository
        get() = ServiceLocator.provideHabitRepository(this)

    val moodRepository: MoodRepository
        get() = ServiceLocator.provideMoodRepository(this)

    val userRepository: UserRepository
        get() = ServiceLocator.provideUserRepository(this)

    val sessionManager: com.example.spark.data.local.SessionManager
        get() = ServiceLocator.provideSessionManager(this)

    val todoRepository: com.example.spark.data.repository.TodoRepository
        get() = ServiceLocator.provideTodoRepository(this)

    override fun onCreate() {
        super.onCreate()
        // Eagerly initialize all ServiceLocator repositories
        ServiceLocator.provideHabitRepository(this)
        ServiceLocator.provideMoodRepository(this)
        ServiceLocator.provideUserRepository(this)
        ServiceLocator.provideTodoRepository(this)
        ServiceLocator.provideSessionManager(this)

        seedDefaultUserIfNecessary()
        applySavedNightMode()
        createNotificationChannels()
        scheduleHabitReminderWorker()
    }

    private fun seedDefaultUserIfNecessary() {
        appScope.launch {
            val user = userRepository.getUserByEmail("alex@example.com")
            if (user == null) {
                userRepository.register(
                    com.example.spark.data.local.entity.UserEntity(
                        id = 1L,
                        name = "Alex",
                        email = "alex@example.com",
                        passwordHash = com.example.spark.data.local.PasswordHasher.hash("password123")
                    )
                )
                userRepository.updatePrefs(com.example.spark.data.local.entity.UserPrefsEntity(userId = 1L))
            }
        }
    }

    private fun applySavedNightMode() {
        appScope.launch {
            val prefs = userRepository.getUserPrefs()
            if (prefs != null) {
                withContext(Dispatchers.Main) {
                    AppCompatDelegate.setDefaultNightMode(
                        if (prefs.darkModeEnabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                    )
                }
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                HabitReminderWorker.CHANNEL_ID,
                HabitReminderWorker.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle reminders to nurture your daily mindful habits"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun scheduleHabitReminderWorker() {
        val reminderWorkRequest = PeriodicWorkRequestBuilder<HabitReminderWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            HabitReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderWorkRequest
        )
    }
}
