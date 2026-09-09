package com.example.spark.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================================
// Custom Soft Ambient Shadows (reproducing CSS-style offsetY, blur, and opacity)
// ============================================================================

/**
 * Custom soft shadow modifier using Android framework paint shadow layer,
 * producing soft Gaussian ambient shadows without harsh edges.
 */
fun Modifier.softShadow(
    color: Color,
    alpha: Float,
    borderRadius: Dp,
    shadowRadius: Dp,
    offsetY: Dp,
    offsetX: Dp = 0.dp
): Modifier = this.drawBehind {
    val shadowColor = color.copy(alpha = alpha).toArgb()
    val transparentColor = color.copy(alpha = 0f).toArgb()

    drawIntoCanvas { canvas ->
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = transparentColor
        frameworkPaint.setShadowLayer(
            shadowRadius.toPx(),
            offsetX.toPx(),
            offsetY.toPx(),
            shadowColor
        )
        canvas.drawRoundRect(
            0f,
            0f,
            size.width,
            size.height,
            borderRadius.toPx(),
            borderRadius.toPx(),
            paint
        )
    }
}

/**
 * Level 1 (Cards): 8dp Y-offset, 24dp blur radius, 6% opacity of onSurface (#1C192A).
 */
fun Modifier.cardElevation(
    borderRadius: Dp = 24.dp
): Modifier = softShadow(
    color = OnSurface,
    alpha = 0.06f,
    borderRadius = borderRadius,
    shadowRadius = 24.dp,
    offsetY = 8.dp
)

/**
 * Level 2 (Active/Floating): 12dp Y-offset, 32dp blur radius, 12% opacity of primary (#52559C).
 */
fun Modifier.activeElevation(
    borderRadius: Dp = 16.dp
): Modifier = softShadow(
    color = Primary,
    alpha = 0.12f,
    borderRadius = borderRadius,
    shadowRadius = 32.dp,
    offsetY = 12.dp
)

// ============================================================================
// Pressed State: 2% scale-down transform
// ============================================================================

/**
 * Reusable pressScale modifier providing a subtle 2% scale-down transform on press.
 */
fun Modifier.pressScale(
    interactionSource: InteractionSource? = null,
    targetScale: Float = 0.98f
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pressScaleAnimation"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
