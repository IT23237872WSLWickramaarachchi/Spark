package com.example.spark.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.spark.ui.components.SparkChip
import com.example.spark.ui.components.SparkChipVariant
import com.example.spark.ui.components.SparkCircleToggle
import com.example.spark.ui.components.SparkGhostButton
import com.example.spark.ui.components.SparkPrimaryButton
import com.example.spark.ui.components.SparkSecondaryButton
import com.example.spark.ui.components.SparkTextField

/**
 * Infrastructure Preview screen demonstrating:
 * - Design tokens: Color, Typography, Shape, Spacing, Elevation
 * - Card level 1 soft shadow
 * - Primary, Secondary, Ghost Buttons
 * - Focused/Unfocused SparkTextField
 * - SparkCircleToggle (interactive checkmark)
 * - SparkChip pill variants
 */
@Preview(name = "Spark Design Infrastructure Preview", showBackground = true, device = "id:pixel_7")
@Composable
fun SparkInfrastructurePreview() {
    SparkTheme {
        var sampleText by remember { mutableStateOf("") }
        var isChecked by remember { mutableStateOf(true) }
        var selectedChip by remember { mutableStateOf("Mindful") }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SparkTheme.colorScheme.background // #FDF8FF
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = SparkTheme.spacing.screenMargin)
                    .padding(vertical = SparkTheme.spacing.sectionGap),
                verticalArrangement = Arrangement.spacedBy(SparkTheme.spacing.gutterStack)
            ) {
                // Display Hero Header
                Text(
                    text = "Spark System",
                    style = SparkTheme.typography.displayHero,
                    color = OnSurface
                )

                Text(
                    text = "Small habits, better days.",
                    style = SparkTheme.typography.bodyLg,
                    color = TextMuted
                )

                // Level 1 Card with softShadow & 24dp radius
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .cardElevation(borderRadius = 24.dp)
                        .background(
                            color = CardWhite,
                            shape = SparkTheme.shapes.card
                        )
                        .padding(
                            horizontal = SparkTheme.spacing.cardPaddingH,
                            vertical = SparkTheme.spacing.cardPaddingV
                        )
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(SparkTheme.spacing.gutterStack)
                    ) {
                        Text(
                            text = "Sample Elevated Card",
                            style = SparkTheme.typography.headlineMd,
                            color = TextDark
                        )

                        Text(
                            text = "This card uses Level 1 soft ambient shadow (8dp Y, 24dp blur, 6% opacity) with 24dp corner radii.",
                            style = SparkTheme.typography.bodySm,
                            color = OnSurfaceVariant
                        )

                        // Sample TextField
                        SparkTextField(
                            value = sampleText,
                            onValueChange = { sampleText = it },
                            placeholder = "Enter routine name..."
                        )

                        // Habit Circle Toggle row
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SparkCircleToggle(
                                checked = isChecked,
                                onCheckedChange = { isChecked = it }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isChecked) "Morning Meditation (Completed)" else "Morning Meditation (Pending)",
                                style = SparkTheme.typography.bodyMd,
                                color = if (isChecked) OnSurface else TextMuted
                            )
                        }

                        // Chips Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SparkChip(
                                label = "Mindful",
                                variant = SparkChipVariant.Primary,
                                isSelected = selectedChip == "Mindful",
                                onClick = { selectedChip = "Mindful" }
                            )
                            SparkChip(
                                label = "Happy",
                                variant = SparkChipVariant.Accent,
                                isSelected = selectedChip == "Happy",
                                onClick = { selectedChip = "Happy" }
                            )
                            SparkChip(
                                label = "Done",
                                variant = SparkChipVariant.Success,
                                isSelected = selectedChip == "Done",
                                onClick = { selectedChip = "Done" }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Primary Button (56dp, lg radius)
                SparkPrimaryButton(
                    text = "Primary Button (Log In)",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )

                // Secondary Button (Accent pink)
                SparkSecondaryButton(
                    text = "Secondary Button (Complete Streak)",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )

                // Ghost Button
                SparkGhostButton(
                    text = "Ghost Action (Cancel)",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
