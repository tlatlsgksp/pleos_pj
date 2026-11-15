package com.example.pleos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Mapped to Pleos tokens: background_primary, surface_*, button_filled_enabled, text_dark_*, informative_*.
private val LightColorScheme = lightColorScheme(
    primary = BasicLight600,
    onPrimary = BasicLight00,
    secondary = ActiveLight,
    onSecondary = BasicLight00,
    tertiary = PositiveLight,
    onTertiary = BasicLight00,
    error = NegativeLight,
    onError = BasicLight00,
    background = BasicLight50,
    onBackground = BasicLight900,
    surface = BasicLight00,
    onSurface = BasicLight900,
    surfaceVariant = BasicLight100,
    onSurfaceVariant = BasicLight500,
    surfaceContainer = BasicLight100,
    outline = BasicLight300,
    outlineVariant = BasicLight200,
    scrim = Color.Black,
)

private val DarkColorScheme = darkColorScheme(
    primary = BasicDark600,
    onPrimary = BasicDark00,
    secondary = ActiveDark,
    onSecondary = BasicDark00,
    tertiary = PositiveDark,
    onTertiary = BasicDark00,
    error = NegativeDark,
    onError = BasicDark00,
    background = BasicDark50,
    onBackground = BasicDark900,
    surface = BasicDark50,
    onSurface = BasicDark900,
    surfaceVariant = BasicDark100,
    onSurfaceVariant = BasicDark500,
    surfaceContainer = BasicDark100,
    outline = BasicDark300,
    outlineVariant = BasicDark200,
    scrim = Color.Black,
)

@Composable
fun PleosTheme(
    // The vehicle switches Light/Dark automatically with Screen Mode Auto.
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
