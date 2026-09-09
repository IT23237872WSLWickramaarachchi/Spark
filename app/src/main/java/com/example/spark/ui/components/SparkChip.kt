package com.example.spark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.spark.ui.theme.AccentPink
import com.example.spark.ui.theme.Primary
import com.example.spark.ui.theme.Secondary
import com.example.spark.ui.theme.SparkTheme
import com.example.spark.ui.theme.SuccessGreen
import com.example.spark.ui.theme.Tertiary
import com.example.spark.ui.theme.TextDark
import com.example.spark.ui.theme.pressScale

enum class SparkChipVariant {
    Primary,
    Accent,
    Success
}

/**
 * SparkChip:
 * Pill-shaped tag container for mood tags, categories, and status badges.
 * Features 10% opacity primary or accent background variants.
 */
@Composable
fun SparkChip(
    label: String,
    modifier: Modifier = Modifier,
    variant: SparkChipVariant = SparkChipVariant.Primary,
    isSelected: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val (backgroundColor, textColor) = when (variant) {
        SparkChipVariant.Primary -> {
            if (isSelected) {
                Primary to Color.White
            } else {
                Primary.copy(alpha = 0.10f) to Primary
            }
        }
        SparkChipVariant.Accent -> {
            if (isSelected) {
                AccentPink to TextDark
            } else {
                AccentPink.copy(alpha = 0.22f) to Secondary
            }
        }
        SparkChipVariant.Success -> {
            if (isSelected) {
                SuccessGreen to Color.White
            } else {
                SuccessGreen.copy(alpha = 0.16f) to Tertiary
            }
        }
    }

    val clickModifier = if (onClick != null) {
        Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .pressScale(interactionSource)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(SparkTheme.shapes.chip)
            .background(backgroundColor)
            .then(
                if (isSelected && variant == SparkChipVariant.Accent) {
                    Modifier.border(1.dp, Secondary.copy(alpha = 0.3f), SparkTheme.shapes.chip)
                } else {
                    Modifier
                }
            )
            .then(clickModifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = SparkTheme.typography.labelMd.copy(
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
            )
        }
    }
}
