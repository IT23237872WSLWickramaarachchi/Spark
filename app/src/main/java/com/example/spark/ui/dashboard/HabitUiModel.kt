package com.example.spark.ui.dashboard

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes

/**
 * UI representation of a habit row item on the Dashboard.
 */
data class HabitUiModel(
    val id: Long,
    val title: String,
    val subtitle: String,
    val category: String,
    @DrawableRes val iconRes: Int,
    @ColorInt val iconColor: Int,
    @ColorInt val iconBgColor: Int,
    val isCompleted: Boolean
)
