package com.example.spark.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.spark.ui.theme.OutlineVariant
import com.example.spark.ui.theme.SuccessGreen
import com.example.spark.ui.theme.pressScale

/**
 * SparkCircleToggle:
 * Habit-completion checkbox replacement.
 * - Unchecked: 2dp outlined circle in OutlineVariant (#C7C5D2).
 * - Checked: Smoothly fills with Success Green (#6FCF97) with animated scaling white checkmark.
 */
@Composable
fun SparkCircleToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val bgColor by animateColorAsState(
        targetValue = if (checked) SuccessGreen else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "circleToggleBg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (checked) SuccessGreen else OutlineVariant,
        animationSpec = tween(durationMillis = 200),
        label = "circleToggleBorder"
    )

    val checkScale by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "checkScale"
    )

    Box(
        modifier = modifier
            .semantics { role = Role.Checkbox }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(4.dp) // Accessibility padding
            .pressScale(interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(bgColor)
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checkScale > 0.05f) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = if (checked) "Completed" else "Incomplete",
                    tint = Color.White,
                    modifier = Modifier
                        .size(size * 0.65f)
                        .scale(checkScale)
                )
            }
        }
    }
}
