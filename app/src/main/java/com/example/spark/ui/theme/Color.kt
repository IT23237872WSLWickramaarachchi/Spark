package com.example.spark.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ============================================================================
// Spark Design Token Colors (Mapped directly from design-tokens YAML)
// ============================================================================

// Primary
val Primary = Color(0xFF52559C)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFF6B6EB6)
val OnPrimaryContainer = Color(0xFFFFFBFF)
val InversePrimary = Color(0xFFC0C1FF)

// Secondary
val Secondary = Color(0xFF874D5E)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFFFB5C8)
val OnSecondaryContainer = Color(0xFF7C4354)

// Tertiary
val Tertiary = Color(0xFF006A3F)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFF1C8554)
val OnTertiaryContainer = Color(0xFFF6FFF5)

// Error
val Error = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF93000A)

// Fixed Variants
val PrimaryFixed = Color(0xFFE1E0FF)
val PrimaryFixedDim = Color(0xFFC0C1FF)
val OnPrimaryFixed = Color(0xFF0E0F58)
val OnPrimaryFixedVariant = Color(0xFF3C3F85)

val SecondaryFixed = Color(0xFFFFD9E1)
val SecondaryFixedDim = Color(0xFFFCB2C5)
val OnSecondaryFixed = Color(0xFF370B1B)
val OnSecondaryFixedVariant = Color(0xFF6C3646)

val TertiaryFixed = Color(0xFF95F7BB)
val TertiaryFixedDim = Color(0xFF7ADAA1)
val OnTertiaryFixed = Color(0xFF002110)
val OnTertiaryFixedVariant = Color(0xFF005230)

// Surface & Background
val Surface = Color(0xFFFDF8FF)
val SurfaceDim = Color(0xFFDDD7EF)
val SurfaceBright = Color(0xFFFDF8FF)
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFF7F1FF)
val SurfaceContainer = Color(0xFFF1EBFF)
val SurfaceContainerHigh = Color(0xFFEBE5FD)
val SurfaceContainerHighest = Color(0xFFE6DFF7)

val OnSurface = Color(0xFF1C192A)
val OnSurfaceVariant = Color(0xFF464650)
val InverseSurface = Color(0xFF312E40)
val InverseOnSurface = Color(0xFFF4EEFF)

val Outline = Color(0xFF777681)
val OutlineVariant = Color(0xFFC7C5D2)
val SurfaceTint = Color(0xFF54579E)

val Background = Color(0xFFFDF8FF)
val OnBackground = Color(0xFF1C192A)
val SurfaceVariant = Color(0xFFE6DFF7)

// Brand constants & helpers from Spark PRD / Design System
val BackgroundCream = Color(0xFFF8F5F1)
val CardWhite = Color(0xFFFFFFFF)
val TextDark = Color(0xFF2E2B3D)
val TextMuted = Color(0xFF9C99AC)
val SuccessGreen = Color(0xFF6FCF97)
val DividerGrey = Color(0xFFECE9F1)
val DeepPurple = Color(0xFF5D5FA6)
val AccentPink = Color(0xFFF2A9BC)

// Backwards-compatible aliases with Spark prefix
val SparkPrimary = Primary
val SparkOnPrimary = OnPrimary
val SparkPrimaryContainer = PrimaryContainer
val SparkOnPrimaryContainer = OnPrimaryContainer
val SparkInversePrimary = InversePrimary
val SparkSecondary = Secondary
val SparkOnSecondary = OnSecondary
val SparkSecondaryContainer = SecondaryContainer
val SparkOnSecondaryContainer = OnSecondaryContainer
val SparkTertiary = Tertiary
val SparkOnTertiary = OnTertiary
val SparkTertiaryContainer = TertiaryContainer
val SparkOnTertiaryContainer = OnTertiaryContainer
val SparkError = Error
val SparkOnError = OnError
val SparkErrorContainer = ErrorContainer
val SparkOnErrorContainer = OnErrorContainer
val SparkPrimaryFixed = PrimaryFixed
val SparkPrimaryFixedDim = PrimaryFixedDim
val SparkOnPrimaryFixed = OnPrimaryFixed
val SparkOnPrimaryFixedVariant = OnPrimaryFixedVariant
val SparkPrimaryFixedVariant = OnPrimaryFixedVariant
val SparkSecondaryFixed = SecondaryFixed
val SparkSecondaryFixedDim = SecondaryFixedDim
val SparkOnSecondaryFixed = OnSecondaryFixed
val SparkOnSecondaryFixedVariant = OnSecondaryFixedVariant
val SparkTertiaryFixed = TertiaryFixed
val SparkTertiaryFixedDim = TertiaryFixedDim
val SparkOnTertiaryFixed = OnTertiaryFixed
val SparkOnTertiaryFixedVariant = OnTertiaryFixedVariant
val SparkSurface = Surface
val SparkSurfaceDim = SurfaceDim
val SparkSurfaceBright = SurfaceBright
val SparkSurfaceContainerLowest = SurfaceContainerLowest
val SparkSurfaceContainerLow = SurfaceContainerLow
val SparkSurfaceContainer = SurfaceContainer
val SparkSurfaceContainerHigh = SurfaceContainerHigh
val SparkSurfaceContainerHighest = SurfaceContainerHighest
val SparkOnSurface = OnSurface
val SparkOnSurfaceVariant = OnSurfaceVariant
val SparkInverseSurface = InverseSurface
val SparkInverseOnSurface = InverseOnSurface
val SparkOutline = Outline
val SparkOutlineVariant = OutlineVariant
val SparkSurfaceTint = SurfaceTint
val SparkBackground = Background
val SparkOnBackground = OnBackground
val SparkSurfaceVariant = SurfaceVariant

// ============================================================================
// Material 3 Color Schemes
// ============================================================================

val SparkLightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    inversePrimary = InversePrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceTint = SurfaceTint,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    outline = Outline,
    outlineVariant = OutlineVariant,
    surfaceBright = SurfaceBright,
    surfaceDim = SurfaceDim,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainerLowest = SurfaceContainerLowest
)

// TODO: no dark values provided in DESIGN.md — derive or request from Stitch comment, don't invent dark values.
val SparkDarkColorScheme: ColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    inversePrimary = InversePrimary,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceTint = SurfaceTint,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    outline = Outline,
    outlineVariant = OutlineVariant,
    surfaceBright = SurfaceBright,
    surfaceDim = SurfaceDim,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainerLowest = SurfaceContainerLowest
)
