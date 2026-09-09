package com.example.spark.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.spark.ui.theme.CardWhite
import com.example.spark.ui.theme.OnSurface
import com.example.spark.ui.theme.OutlineVariant
import com.example.spark.ui.theme.Primary
import com.example.spark.ui.theme.SparkTheme
import com.example.spark.ui.theme.SurfaceContainerLow
import com.example.spark.ui.theme.TextMuted
import com.example.spark.ui.theme.softShadow

/**
 * SparkTextField:
 * - Card White background (#FFFFFF)
 * - 1dp OutlineVariant border (#C7C5D2) when unfocused
 * - 2dp Primary (#52559C) focus ring with soft ambient glow shadow on focus
 * - 16dp rounded corners (SparkTheme.shapes.input)
 */
@Composable
fun SparkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val isFocused by interactionSource.collectIsFocusedAsState()

    val glowModifier = if (isFocused) {
        Modifier.softShadow(
            color = Primary,
            alpha = 0.14f,
            borderRadius = 16.dp,
            shadowRadius = 8.dp,
            offsetY = 2.dp
        )
    } else {
        Modifier
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .then(glowModifier),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = SparkTheme.typography.bodyMd.copy(color = OnSurface),
        placeholder = placeholder?.let {
            {
                Text(
                    text = it,
                    style = SparkTheme.typography.bodyMd.copy(color = TextMuted)
                )
            }
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        shape = SparkTheme.shapes.input,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CardWhite,
            unfocusedContainerColor = CardWhite,
            disabledContainerColor = SurfaceContainerLow,
            focusedBorderColor = Primary,
            unfocusedBorderColor = OutlineVariant,
            cursorColor = Primary,
            focusedTextColor = OnSurface,
            unfocusedTextColor = OnSurface
        )
    )
}
