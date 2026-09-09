package com.example.spark.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SparkLightColorScheme = lightColorScheme(
    primary = SparkPrimary,
    onPrimary = SparkOnPrimary,
    primaryContainer = SparkPrimaryContainer,
    onPrimaryContainer = SparkOnPrimaryContainer,
    inversePrimary = SparkInversePrimary,
    secondary = SparkSecondary,
    onSecondary = SparkOnSecondary,
    secondaryContainer = SparkSecondaryContainer,
    onSecondaryContainer = SparkOnSecondaryContainer,
    tertiary = SparkTertiary,
    onTertiary = SparkOnTertiary,
    tertiaryContainer = SparkTertiaryContainer,
    onTertiaryContainer = SparkOnTertiaryContainer,
    background = SparkBackground,
    onBackground = SparkOnBackground,
    surface = SparkSurface,
    onSurface = SparkOnSurface,
    surfaceVariant = SparkSurfaceVariant,
    onSurfaceVariant = SparkOnSurfaceVariant,
    surfaceTint = SparkSurfaceTint,
    inverseSurface = SparkInverseSurface,
    inverseOnSurface = SparkInverseOnSurface,
    error = SparkError,
    onError = SparkOnError,
    errorContainer = SparkErrorContainer,
    onErrorContainer = SparkOnErrorContainer,
    outline = SparkOutline,
    outlineVariant = SparkOutlineVariant,
)

@Composable
fun SparkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SparkLightColorScheme,
        typography = SparkTypography,
        content = content
    )
}
