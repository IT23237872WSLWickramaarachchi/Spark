package com.example.spark.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private const val DATE_PATTERN = "yyyy-MM-dd"

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat(DATE_PATTERN, Locale.getDefault())
        return sdf.format(Date())
    }

    fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat(DATE_PATTERN, Locale.getDefault())
        return sdf.format(date)
    }

    fun parseDate(dateString: String): Date? {
        return try {
            val sdf = SimpleDateFormat(DATE_PATTERN, Locale.getDefault())
            sdf.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    fun formatTime12Hour(hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    fun getCurrentWeekDateStrings(): List<String> {
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        val sdf = SimpleDateFormat(DATE_PATTERN, Locale.getDefault())
        val days = mutableListOf<String>()
        for (i in 0 until 7) {
            days.add(sdf.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return days
    }

    fun getDayOfWeekLetter(dateString: String): String {
        val date = parseDate(dateString) ?: return ""
        val calendar = Calendar.getInstance().apply { time = date }
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "M"
            Calendar.TUESDAY -> "T"
            Calendar.WEDNESDAY -> "W"
            Calendar.THURSDAY -> "T"
            Calendar.FRIDAY -> "F"
            Calendar.SATURDAY -> "S"
            Calendar.SUNDAY -> "S"
            else -> ""
        }
    }

    fun getPast30Days(): List<String> {
        val sdf = SimpleDateFormat(DATE_PATTERN, Locale.getDefault())
        val calendar = Calendar.getInstance()
        val dates = mutableListOf<String>()
        for (i in 0 until 30) {
            dates.add(0, sdf.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        return dates
    }
}
