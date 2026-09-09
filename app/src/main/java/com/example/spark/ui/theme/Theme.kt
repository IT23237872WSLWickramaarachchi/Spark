package com.example.spark.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

// ============================================================================
// Composition Locals for Spark Design System
// ============================================================================

val LocalSparkTypography = staticCompositionLocalOf { DefaultSparkTypography }
val LocalSparkShapes = staticCompositionLocalOf { DefaultSparkShapes }
val LocalSparkSpacing = staticCompositionLocalOf { DefaultSparkSpacing }

// ============================================================================
// SparkTheme Object Accessor (SparkTheme.typography, SparkTheme.shapes, etc.)
// ============================================================================

object SparkTheme {
    val typography: SparkTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalSparkTypography.current

    val shapes: SparkShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalSparkShapes.current

    val spacing: SparkSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSparkSpacing.current

    val colorScheme: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme
}

// ============================================================================
// SparkTheme Composable
// ============================================================================

@Composable
fun SparkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SparkDarkColorScheme else SparkLightColorScheme

    CompositionLocalProvider(
        LocalSparkTypography provides DefaultSparkTypography,
        LocalSparkShapes provides DefaultSparkShapes,
        LocalSparkSpacing provides DefaultSparkSpacing
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTypography,
            shapes = MaterialShapes,
            content = content
        )
    }
}
