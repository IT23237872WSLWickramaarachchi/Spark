package com.example.spark.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spark.ui.components.SparkLogoEmblem
import com.example.spark.ui.theme.CardWhite
import com.example.spark.ui.theme.DividerGrey
import com.example.spark.ui.theme.ScriptFontFamily
import com.example.spark.ui.theme.SparkOnPrimary
import com.example.spark.ui.theme.SparkOnSurface
import com.example.spark.ui.theme.SparkOnSurfaceVariant
import com.example.spark.ui.theme.SparkOutline
import com.example.spark.ui.theme.SparkOutlineVariant
import com.example.spark.ui.theme.SparkPrimary
import com.example.spark.ui.theme.SparkPrimaryContainer
import com.example.spark.ui.theme.SparkPrimaryFixedVariant
import com.example.spark.ui.theme.SparkSecondary
import com.example.spark.ui.theme.SparkSurfaceContainerLow
import com.example.spark.ui.theme.SparkTheme

/**
 * WelcomeScreen: Calm, mindful authentication screen following Spark design system.
 *
 * Stateless composable displaying:
 * - Full-bleed top-to-bottom purple gradient with subtle ambient glowing circles.
 * - Upper third with rounded logo badge icon, "Welcome back" script headline, and subtitle.
 * - White rounded card anchored to the bottom half containing email/password inputs,
 *   forgot password link, primary Log In button, "or" divider, and register prompt.
 */
@Composable
fun WelcomeScreen(
    email: String,
    password: String,
    passwordVisible: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    // Gradient background colors mapped to Spark primary and secondary tokens
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            SparkPrimary,               // #52559C (Primary)
            SparkPrimaryContainer,      // #6B6EB6 (Primary Container)
            SparkPrimaryFixedVariant,   // #3C3F85 (Deep Purple)
            SparkSecondary              // #874D5E (Secondary warm tone)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
    ) {
        // Decorative ambient glow circles for tactile depth (PRD section 4.1 & 5.4)
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .padding(top = 20.dp, end = 0.dp)
                .blur(40.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x33FFB5C8), // Soft pink ambient reflection
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterStart)
                .blur(50.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x286B6EB6),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .imePadding()
        ) {
            // Upper Third: Brand Welcome Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Rounded logo badge icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(
                            width = 1.5.dp,
                            color = Color.White.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(22.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    SparkLogoEmblem(
                        modifier = Modifier.size(44.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // "Welcome back" in cursive/script style matching reference wordmark
                Text(
                    text = "Welcome back",
                    fontFamily = ScriptFontFamily,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle in lighter weight
                Text(
                    text = "Continue your journey with Spark",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            // Bottom Half: White Rounded Card anchored to the bottom
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = CardWhite,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 28.dp, bottom = 20.dp)
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Email TextField
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Email address",
                                color = SparkOutline,
                                fontSize = 15.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = "Email Icon",
                                tint = SparkPrimary
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SparkPrimary,
                            unfocusedBorderColor = SparkOutlineVariant,
                            focusedContainerColor = SparkSurfaceContainerLow,
                            unfocusedContainerColor = SparkSurfaceContainerLow,
                            focusedTextColor = SparkOnSurface,
                            unfocusedTextColor = SparkOnSurface,
                            cursorColor = SparkPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password TextField
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Password",
                                color = SparkOutline,
                                fontSize = 15.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = "Password Icon",
                                tint = SparkPrimary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = onTogglePasswordVisibility) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = SparkOutline
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                onLoginClick()
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SparkPrimary,
                            unfocusedBorderColor = SparkOutlineVariant,
                            focusedContainerColor = SparkSurfaceContainerLow,
                            unfocusedContainerColor = SparkSurfaceContainerLow,
                            focusedTextColor = SparkOnSurface,
                            unfocusedTextColor = SparkOnSurface,
                            cursorColor = SparkPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Right-aligned "Forgot password?" text link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Forgot password?",
                            color = SparkPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onForgotPasswordClick
                                )
                                .padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Full-width primary filled button labeled "Log In" (56dp height per PRD)
                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SparkPrimary,
                            contentColor = SparkOnPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 6.dp
                        )
                    ) {
                        Text(
                            text = "Log In",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SparkOnPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // "or" Divider Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = DividerGrey
                        )
                        Text(
                            text = "or",
                            fontSize = 13.sp,
                            color = SparkOutline,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 1.dp,
                            color = DividerGrey
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Bottom text row: "Don't have an account? Register"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Don't have an account? ",
                            fontSize = 14.sp,
                            color = SparkOnSurfaceVariant
                        )
                        Text(
                            text = "Register",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SparkPrimary,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onRegisterClick
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "WelcomeScreen Light Preview", showBackground = true, device = "id:pixel_7")
@Composable
fun WelcomeScreenPreview() {
    SparkTheme {
        var email by rememberSaveable { mutableStateOf("lonindu@spark.app") }
        var password by rememberSaveable { mutableStateOf("mindful2026") }
        var passwordVisible by rememberSaveable { mutableStateOf(false) }

        WelcomeScreen(
            email = email,
            password = password,
            passwordVisible = passwordVisible,
            onEmailChange = { email = it },
            onPasswordChange = { password = it },
            onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
            onLoginClick = {},
            onForgotPasswordClick = {},
            onRegisterClick = {}
        )
    }
}
