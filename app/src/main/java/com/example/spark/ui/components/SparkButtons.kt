package com.example.spark.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spark.ui.theme.AccentPink
import com.example.spark.ui.theme.OnPrimary
import com.example.spark.ui.theme.Primary
import com.example.spark.ui.theme.SparkTheme
import com.example.spark.ui.theme.TextDark
import com.example.spark.ui.theme.activeElevation
import com.example.spark.ui.theme.pressScale

/**
 * SparkPrimaryButton:
 * - 56dp min height
 * - lg radius (16dp)
 * - Primary background (#52559C)
 * - White text
 * - Press scale transform (2% scale-down) and soft active elevation
 */
@Composable
fun SparkPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .pressScale(interactionSource)
            .activeElevation(borderRadius = 16.dp),
        enabled = enabled,
        shape = SparkTheme.shapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            contentColor = OnPrimary,
            disabledContainerColor = Primary.copy(alpha = 0.4f),
            disabledContentColor = OnPrimary.copy(alpha = 0.7f)
        ),
        contentPadding = PaddingValues(horizontal = SparkTheme.spacing.screenMargin, vertical = 16.dp),
        interactionSource = interactionSource,
        elevation = null
    ) {
        Text(
            text = text,
            style = SparkTheme.typography.headlineMd.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = OnPrimary
            )
        )
    }
}

/**
 * SparkSecondaryButton:
 * - 56dp min height
 * - lg radius (16dp)
 * - Accent Pink background (#F2A9BC)
 * - TextDark text (#2E2B3D)
 * - Press scale transform
 */
@Composable
fun SparkSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .pressScale(interactionSource),
        enabled = enabled,
        shape = SparkTheme.shapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentPink,
            contentColor = TextDark,
            disabledContainerColor = AccentPink.copy(alpha = 0.4f),
            disabledContentColor = TextDark.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(horizontal = SparkTheme.spacing.screenMargin, vertical = 16.dp),
        interactionSource = interactionSource,
        elevation = null
    ) {
        Text(
            text = text,
            style = SparkTheme.typography.headlineMd.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        )
    }
}

/**
 * SparkGhostButton:
 * - No background
 * - Primary text (#52559C)
 * - lg radius (16dp)
 * - Press scale transform
 */
@Composable
fun SparkGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .pressScale(interactionSource),
        enabled = enabled,
        shape = SparkTheme.shapes.button,
        colors = ButtonDefaults.textButtonColors(
            containerColor = Color.Transparent,
            contentColor = Primary,
            disabledContentColor = Primary.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        interactionSource = interactionSource
    ) {
        Text(
            text = text,
            style = SparkTheme.typography.headlineMd.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Primary
            )
        )
    }
}
