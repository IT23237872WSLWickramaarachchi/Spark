package com.example.spark.model

import android.graphics.Color

data class HabitColorOption(
    val key: String,
    val name: String,
    val hexLight: String,
    val hexDark: String,
    val isPrimary: Boolean = false
) {
    fun getColorInt(isDarkMode: Boolean = false): Int {
        return Color.parseColor(if (isDarkMode) hexDark else hexLight)
    }
}

object HabitColors {
    val ALL: List<HabitColorOption> = listOf(
        HabitColorOption("purple", "Purple", "#52559C", "#787CCC", isPrimary = true),
        HabitColorOption("lavender", "Lavender", "#8475B2", "#A699D4", isPrimary = true),
        HabitColorOption("pink", "Pink", "#D46A8C", "#E88DA9", isPrimary = true),
        HabitColorOption("rose", "Rose", "#8E5B6E", "#B0798D", isPrimary = true),
        HabitColorOption("red", "Red", "#C24141", "#E06262"),
        HabitColorOption("orange", "Orange", "#D96B27", "#F0894B"),
        HabitColorOption("amber", "Amber", "#D9822B", "#F5A24E"),
        HabitColorOption("yellow", "Yellow", "#C79A16", "#E3B634"),
        HabitColorOption("green", "Green", "#2E7D56", "#4CA376", isPrimary = true),
        HabitColorOption("mint", "Mint", "#3BA378", "#6FCF97", isPrimary = true),
        HabitColorOption("teal", "Teal", "#268C8C", "#47B0B0", isPrimary = true),
        HabitColorOption("cyan", "Cyan", "#208EA3", "#43B3C9"),
        HabitColorOption("blue", "Blue", "#3B71CA", "#6293E3", isPrimary = true),
        HabitColorOption("indigo", "Indigo", "#3F51B5", "#6B7CE6")
    )

    val PRIMARY: List<HabitColorOption> = ALL.filter { it.isPrimary }

    fun getColorByKeyOrHex(value: String?, isDarkMode: Boolean = false): Int {
        if (value.isNullOrBlank()) {
            return Color.parseColor(if (isDarkMode) "#787CCC" else "#52559C")
        }
        val foundByKey = ALL.find { it.key.equals(value, ignoreCase = true) }
        if (foundByKey != null) {
            return foundByKey.getColorInt(isDarkMode)
        }
        val foundByHex = ALL.find {
            it.hexLight.equals(value, ignoreCase = true) || it.hexDark.equals(value, ignoreCase = true)
        }
        if (foundByHex != null) {
            return foundByHex.getColorInt(isDarkMode)
        }
        return try {
            Color.parseColor(value)
        } catch (e: Exception) {
            Color.parseColor(if (isDarkMode) "#787CCC" else "#52559C")
        }
    }

    fun getKeyForValue(value: String?): String {
        if (value.isNullOrBlank()) return "purple"
        val found = ALL.find {
            it.key.equals(value, ignoreCase = true) ||
                    it.hexLight.equals(value, ignoreCase = true) ||
                    it.hexDark.equals(value, ignoreCase = true)
        }
        return found?.key ?: "purple"
    }

    fun getHexForTag(value: String?): String {
        if (value.isNullOrBlank()) return "#52559C"
        val found = ALL.find { it.key.equals(value, ignoreCase = true) }
        if (found != null) return found.hexLight
        if (value.startsWith("#")) return value
        return "#52559C"
    }
}
