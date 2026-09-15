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
    @field:DrawableRes val iconRes: Int,
    @field:ColorInt val iconColor: Int,
    @field:ColorInt val iconBgColor: Int,
    val isCompleted: Boolean
)
