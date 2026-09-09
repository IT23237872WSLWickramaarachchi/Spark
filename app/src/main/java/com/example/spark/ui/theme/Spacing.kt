package com.example.spark.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================================
// Spark Spacing (Exact tokens & vertical rhythm rules)
// ============================================================================

data class SparkSpacing(
    val screenMargin: Dp = 24.dp,   // Fixed 24px horizontal margin for breathability
    val gutterStack: Dp = 16.dp,    // 16px standard vertical gap between stacked elements
    val sectionGap: Dp = 32.dp,     // 32px separation between larger logical sections
    val cardPaddingV: Dp = 20.dp,   // 20px card vertical padding
    val cardPaddingH: Dp = 20.dp,   // 20px card horizontal padding
    val insetBtn: Dp = 16.dp        // 16px button inset padding
)

val DefaultSparkSpacing = SparkSpacing()
