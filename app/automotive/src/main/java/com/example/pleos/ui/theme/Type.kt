package com.example.pleos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

// Pleos Connect font hierarchy (docs/UI_DESIGN_GUIDE.md §4). Asta Sans is not bundled yet.
private fun pleosStyle(size: TextUnit, lineHeight: TextUnit, weight: FontWeight) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = 0.sp
)

val Typography = Typography(
    displayLarge = pleosStyle(60.sp, 80.sp, FontWeight.Bold),
    displayMedium = pleosStyle(56.sp, 72.sp, FontWeight.Bold),
    displaySmall = pleosStyle(48.sp, 64.sp, FontWeight.Bold),
    headlineLarge = pleosStyle(40.sp, 52.sp, FontWeight.Bold),
    headlineMedium = pleosStyle(36.sp, 44.sp, FontWeight.Bold),
    headlineSmall = pleosStyle(32.sp, 40.sp, FontWeight.Bold),
    titleLarge = pleosStyle(40.sp, 52.sp, FontWeight.Normal),
    titleMedium = pleosStyle(36.sp, 44.sp, FontWeight.Normal),
    titleSmall = pleosStyle(32.sp, 40.sp, FontWeight.Normal),
    bodyLarge = pleosStyle(30.sp, 38.sp, FontWeight.Normal),
    bodyMedium = pleosStyle(28.sp, 36.sp, FontWeight.Normal),
    bodySmall = pleosStyle(26.sp, 34.sp, FontWeight.Normal),
    labelLarge = pleosStyle(24.sp, 28.sp, FontWeight.Medium),
    labelMedium = pleosStyle(24.sp, 28.sp, FontWeight.Normal),
    labelSmall = pleosStyle(20.sp, 24.sp, FontWeight.Normal)
)
