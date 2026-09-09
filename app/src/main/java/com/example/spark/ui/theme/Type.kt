package com.example.spark.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.spark.R

// ============================================================================
// Google Fonts Provider Setup
// ============================================================================

val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val plusJakartaSansFont = GoogleFont("Plus Jakarta Sans")
val nunitoSansFont = GoogleFont("Nunito Sans")

val PlusJakartaSansFamily = FontFamily(
    Font(googleFont = plusJakartaSansFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = plusJakartaSansFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = plusJakartaSansFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = plusJakartaSansFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

val NunitoSansFamily = FontFamily(
    Font(googleFont = nunitoSansFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = nunitoSansFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = nunitoSansFont, fontProvider = fontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = nunitoSansFont, fontProvider = fontProvider, weight = FontWeight.Bold)
)

// Handwritten cursive font family matching Spark brand display wordmark
val ScriptFontFamily = FontFamily.Cursive

// ============================================================================
// Custom SparkTypography data object (exact names matching PRD & tokens)
// ============================================================================

data class SparkTypography(
    val displayHero: TextStyle = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold, // 700
        lineHeight = 40.sp,
        letterSpacing = (-0.02).em
    ),
    val headlineLg: TextStyle = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold, // 700
        lineHeight = 32.sp
    ),
    val headlineMd: TextStyle = TextStyle(
        fontFamily = PlusJakartaSansFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold, // 600
        lineHeight = 28.sp
    ),
    val bodyLg: TextStyle = TextStyle(
        fontFamily = NunitoSansFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.Normal, // 400
        lineHeight = 26.sp
    ),
    val bodyMd: TextStyle = TextStyle(
        fontFamily = NunitoSansFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal, // 400
        lineHeight = 24.sp
    ),
    val bodySm: TextStyle = TextStyle(
        fontFamily = NunitoSansFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal, // 400
        lineHeight = 20.sp
    ),
    val labelBold: TextStyle = TextStyle(
        fontFamily = NunitoSansFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold, // 700
        lineHeight = 18.sp,
        letterSpacing = 0.05.em
    ),
    val labelMd: TextStyle = TextStyle(
        fontFamily = NunitoSansFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold, // 600
        lineHeight = 16.sp
    )
)

val DefaultSparkTypography = SparkTypography()

// ============================================================================
// Material 3 Typography Mapping
// ============================================================================

val MaterialTypography = Typography(
    displayLarge = DefaultSparkTypography.displayHero,
    headlineLarge = DefaultSparkTypography.headlineLg,
    headlineMedium = DefaultSparkTypography.headlineMd,
    titleLarge = DefaultSparkTypography.headlineMd,
    titleMedium = DefaultSparkTypography.bodyLg.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = DefaultSparkTypography.bodyMd.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = DefaultSparkTypography.bodyLg,
    bodyMedium = DefaultSparkTypography.bodyMd,
    bodySmall = DefaultSparkTypography.bodySm,
    labelLarge = DefaultSparkTypography.labelBold,
    labelMedium = DefaultSparkTypography.labelMd,
    labelSmall = DefaultSparkTypography.labelMd.copy(fontSize = 11.sp, lineHeight = 14.sp)
)
