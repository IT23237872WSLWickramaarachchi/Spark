package com.example.spark.data.local

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Room TypeConverters for java.time types used across entities.
 *
 * Converts [LocalDate], [LocalTime], and [Instant] to/from their
 * String/Long representations for SQLite storage.
 */
class Converters {

    // --- LocalDate (ISO-8601 string: "2026-09-11") ---
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    // --- LocalTime (ISO-8601 string: "08:30") ---
    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? = time?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it) }

    // --- Instant (epoch milliseconds) ---
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }
}
