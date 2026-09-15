package com.example.spark.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.spark.data.local.dao.HabitDao
import com.example.spark.data.local.dao.MoodDao
import com.example.spark.data.local.dao.UserDao
import com.example.spark.data.local.entity.HabitEntity
import com.example.spark.data.local.entity.HabitLogEntity
import com.example.spark.data.local.entity.MoodEntryEntity
import com.example.spark.data.local.entity.UserEntity
import com.example.spark.data.local.entity.UserPrefsEntity

/**
 * The single Room database for the Spark app.
 *
 * Manages five entities with relational foreign keys:
 * - [UserEntity] 1—* [HabitEntity] 1—* [HabitLogEntity]
 * - [UserEntity] 1—* [MoodEntryEntity]
 * - [UserEntity] 1—1 [UserPrefsEntity]
 *
 * Uses a thread-safe singleton pattern via double-checked locking.
 */
@Database(
    entities = [
        UserEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        MoodEntryEntity::class,
        UserPrefsEntity::class,
        com.example.spark.data.local.entity.TodoEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SparkDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun habitDao(): HabitDao
    abstract fun moodDao(): MoodDao
    abstract fun todoDao(): com.example.spark.data.local.dao.TodoDao

    companion object {

        @Volatile
        private var INSTANCE: SparkDatabase? = null

        /**
         * Returns the singleton instance of [SparkDatabase].
         * Creates the database on first access using [Room.databaseBuilder].
         */
        fun getInstance(context: Context): SparkDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SparkDatabase::class.java,
                    "spark_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
