package com.example.spark.model

import com.example.spark.R

data class CategoryInfo(
    val title: String,
    val iconResId: Int,
    val isPrimary: Boolean = false
)

object HabitCategories {
    val ALL: List<CategoryInfo> = listOf(
        CategoryInfo("Health", R.drawable.ic_water_drop, isPrimary = true),
        CategoryInfo("Fitness", R.drawable.ic_walk, isPrimary = true),
        CategoryInfo("Mindfulness", R.drawable.ic_meditation, isPrimary = true),
        CategoryInfo("Productivity", R.drawable.ic_check_circle, isPrimary = true),
        CategoryInfo("Study", R.drawable.ic_book, isPrimary = true),
        CategoryInfo("Work", R.drawable.ic_clock),
        CategoryInfo("Sleep", R.drawable.ic_moon),
        CategoryInfo("Nutrition", R.drawable.ic_streak_flame),
        CategoryInfo("Hydration", R.drawable.ic_water_drop),
        CategoryInfo("Reading", R.drawable.ic_book),
        CategoryInfo("Self-care", R.drawable.ic_meditation),
        CategoryInfo("Social", R.drawable.ic_person),
        CategoryInfo("Finance", R.drawable.ic_chart),
        CategoryInfo("Creativity", R.drawable.ic_flare),
        CategoryInfo("Home", R.drawable.ic_home),
        CategoryInfo("Personal", R.drawable.ic_person)
    )

    val PRIMARY: List<CategoryInfo> = ALL.filter { it.isPrimary }

    fun getIconForCategory(categoryTitle: String): Int {
        val found = ALL.find { it.title.equals(categoryTitle, ignoreCase = true) }
        if (found != null) return found.iconResId
        if (categoryTitle.equals("Routine", ignoreCase = true)) return R.drawable.ic_moon
        return R.drawable.ic_water_drop
    }

    fun getAllTitles(): List<String> = ALL.map { it.title }
}
