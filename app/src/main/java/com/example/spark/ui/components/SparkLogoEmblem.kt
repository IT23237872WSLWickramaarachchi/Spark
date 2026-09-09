package com.example.spark.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Custom Spark logo emblem based on the reference design:
 * Features a dynamic checkmark paired with two radiant sparkle stars.
 */
@Composable
fun SparkLogoEmblem(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = size.minDimension * 0.12f

        // Draw checkmark path matching reference image proportions
        val checkPath = Path().apply {
            moveTo(w * 0.28f, h * 0.50f)
            lineTo(w * 0.47f, h * 0.74f)
            lineTo(w * 0.82f, h * 0.32f)
        }
        drawPath(
            path = checkPath,
            color = tint,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw left sparkle star (smaller)
        drawSparkle(
            center = Offset(w * 0.36f, h * 0.26f),
            radius = size.minDimension * 0.085f,
            color = tint
        )

        // Draw right sparkle star (larger)
        drawSparkle(
            center = Offset(w * 0.63f, h * 0.19f),
            radius = size.minDimension * 0.125f,
            color = tint
        )
    }
}

private fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x, center.y, center.x + radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + radius)
        quadraticTo(center.x, center.y, center.x - radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path = path, color = color)
}
