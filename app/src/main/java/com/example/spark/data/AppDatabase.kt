package com.example.spark.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.spark.data.dao.HabitCompletionDao
import com.example.spark.data.dao.HabitDao
import com.example.spark.data.dao.MoodEntryDao
import com.example.spark.data.dao.UserDao
import com.example.spark.data.entity.HabitCompletionEntity
import com.example.spark.data.entity.HabitEntity
import com.example.spark.data.entity.MoodEntryEntity
import com.example.spark.data.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        MoodEntryEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun moodEntryDao(): MoodEntryDao

    companion object {
        private const val DATABASE_NAME = "spark_database.db"

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE mood_entries ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE mood_entries ADD COLUMN is_demo INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE habit_completions ADD COLUMN is_demo INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Migration v3 → v4: Multiple mood check-ins per day.
         * - Removes unique constraint on (user_id, date).
         * - Adds 'timestamp' (Long) and 'time' (String) columns.
         * - Preserves all existing data by copying to new table.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create new table without the unique(user_id, date) constraint
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS mood_entries_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        user_id INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        mood_level INTEGER NOT NULL,
                        note TEXT,
                        tags TEXT NOT NULL DEFAULT '',
                        is_demo INTEGER NOT NULL DEFAULT 0,
                        timestamp INTEGER NOT NULL DEFAULT 0,
                        time TEXT NOT NULL DEFAULT '',
                        created_at INTEGER NOT NULL DEFAULT 0,
                        updated_at INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Copy existing data, backfilling timestamp from created_at
                db.execSQL("""
                    INSERT INTO mood_entries_new (id, user_id, date, mood_level, note, tags, is_demo, timestamp, time, created_at, updated_at)
                    SELECT id, user_id, date, mood_level, note, tags, is_demo, created_at, '', created_at, updated_at
                    FROM mood_entries
                """.trimIndent())

                // Drop old table
                db.execSQL("DROP TABLE mood_entries")

                // Rename new table
                db.execSQL("ALTER TABLE mood_entries_new RENAME TO mood_entries")

                // Create index on user_id (non-unique)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mood_entries_user_id ON mood_entries (user_id)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
