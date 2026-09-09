package com.example.spark.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ============================================================================
// Spark Shapes (Exact tokens and semantic component radii)
// ============================================================================

data class SparkShapes(
    val sm: CornerBasedShape = RoundedCornerShape(4.dp),        // 0.25rem
    val default: CornerBasedShape = RoundedCornerShape(8.dp),   // 0.5rem
    val md: CornerBasedShape = RoundedCornerShape(12.dp),       // 0.75rem
    val lg: CornerBasedShape = RoundedCornerShape(16.dp),       // 1rem (buttons, inputs)
    val xl: CornerBasedShape = RoundedCornerShape(24.dp),       // 1.5rem (cards, bottom sheets)
    val full: CornerBasedShape = RoundedCornerShape(percent = 50), // 9999px (chips, badges)

    // Semantic component mappings according to Spark Style Guide & PRD:
    // Cards & bottom sheets use 24dp (cloud feel)
    val card: CornerBasedShape = RoundedCornerShape(24.dp),
    val bottomSheet: CornerBasedShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    // Interactive elements (buttons, text fields) use 16dp
    val button: CornerBasedShape = RoundedCornerShape(16.dp),
    val input: CornerBasedShape = RoundedCornerShape(16.dp),
    // Secondary inputs / dialogs
    val dialog: CornerBasedShape = RoundedCornerShape(24.dp),
    // Small elements (chips, badges, pills) use full rounded
    val chip: CornerBasedShape = RoundedCornerShape(percent = 50)
)

val DefaultSparkShapes = SparkShapes()

// ============================================================================
// Material 3 Shapes Mapping
// ============================================================================

val MaterialShapes = Shapes(
    extraSmall = DefaultSparkShapes.sm,   // 4.dp
    small = DefaultSparkShapes.default,    // 8.dp
    medium = DefaultSparkShapes.md,       // 12.dp
    large = DefaultSparkShapes.lg,        // 16.dp (buttons, inputs)
    extraLarge = DefaultSparkShapes.xl    // 24.dp (cards, bottom sheets)
)
