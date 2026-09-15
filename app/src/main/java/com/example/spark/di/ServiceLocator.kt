package com.example.spark.di

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.example.spark.data.local.SparkDatabase
import com.example.spark.data.repository.HabitRepository
import com.example.spark.data.repository.MoodRepository
import com.example.spark.data.repository.UserRepository

/**
 * A lightweight ServiceLocator providing singleton instances of repositories.
 * Enables manual dependency injection without external frameworks (Hilt/Koin).
 */
object ServiceLocator {

    private var database: SparkDatabase? = null

    @Volatile
    var habitRepository: HabitRepository? = null
        @VisibleForTesting set

    @Volatile
    var moodRepository: MoodRepository? = null
        @VisibleForTesting set

    @Volatile
    var userRepository: UserRepository? = null
        @VisibleForTesting set

    @Volatile
    var todoRepository: com.example.spark.data.repository.TodoRepository? = null
        @VisibleForTesting set

    @Volatile
    var sessionManager: com.example.spark.data.local.SessionManager? = null
        @VisibleForTesting set

    fun provideSessionManager(context: Context): com.example.spark.data.local.SessionManager {
        return sessionManager ?: synchronized(this) {
            sessionManager ?: com.example.spark.data.local.SessionManager(context.applicationContext).also { sessionManager = it }
        }
    }

    fun provideHabitRepository(context: Context): HabitRepository {
        return habitRepository ?: synchronized(this) {
            habitRepository ?: createHabitRepository(context).also { habitRepository = it }
        }
    }

    fun provideMoodRepository(context: Context): MoodRepository {
        return moodRepository ?: synchronized(this) {
            moodRepository ?: createMoodRepository(context).also { moodRepository = it }
        }
    }

    fun provideUserRepository(context: Context): UserRepository {
        return userRepository ?: synchronized(this) {
            userRepository ?: createUserRepository(context).also { userRepository = it }
        }
    }

    fun provideTodoRepository(context: Context): com.example.spark.data.repository.TodoRepository {
        return todoRepository ?: synchronized(this) {
            todoRepository ?: createTodoRepository(context).also { todoRepository = it }
        }
    }

    private fun getDatabase(context: Context): SparkDatabase {
        return database ?: synchronized(this) {
            database ?: SparkDatabase.getInstance(context).also { database = it }
        }
    }

    private fun createHabitRepository(context: Context): HabitRepository {
        return HabitRepository(getDatabase(context).habitDao())
    }

    private fun createMoodRepository(context: Context): MoodRepository {
        return MoodRepository(getDatabase(context).moodDao())
    }

    private fun createUserRepository(context: Context): UserRepository {
        return UserRepository(getDatabase(context).userDao())
    }

    private fun createTodoRepository(context: Context): com.example.spark.data.repository.TodoRepository {
        return com.example.spark.data.repository.TodoRepository(getDatabase(context).todoDao())
    }

    @VisibleForTesting
    fun resetRepository() {
        database = null
        habitRepository = null
        moodRepository = null
        userRepository = null
        todoRepository = null
        sessionManager = null
    }
}
