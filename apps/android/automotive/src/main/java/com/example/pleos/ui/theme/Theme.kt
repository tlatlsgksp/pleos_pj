package com.example.pleos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Every Material color role is mapped to a Pleos token, including the surface containers and
 * the *Container roles. Left unset they fall back to Material's purple baseline, which is what
 * tinted the cards lavender before.
 */
private fun scheme(p: PleosColors, dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p.buttonFilled,
        onPrimary = p.onButtonFilled,
        primaryContainer = p.buttonBasic,
        onPrimaryContainer = p.onButtonBasic,
        secondary = p.active,
        onSecondary = p.onAccent,
        secondaryContainer = p.surfaceHigh,
        onSecondaryContainer = p.textPrimary,
        tertiary = p.positive,
        onTertiary = p.onAccent,
        tertiaryContainer = p.surfaceHigh,
        onTertiaryContainer = p.textPrimary,
        error = p.negative,
        onError = p.onAccent,
        errorContainer = p.surfaceHigh,
        onErrorContainer = p.negative,
        background = p.background,
        onBackground = p.textPrimary,
        surface = p.surface,
        onSurface = p.textPrimary,
        surfaceVariant = p.surfaceHigh,
        onSurfaceVariant = p.textTertiary,
        surfaceTint = Color.Transparent,
        surfaceBright = p.surface,
        surfaceDim = p.background,
        surfaceContainerLowest = p.surface,
        surfaceContainerLow = p.surface,
        surfaceContainer = p.surface,
        surfaceContainerHigh = p.surfaceHigh,
        surfaceContainerHighest = p.surfaceHigh,
        inverseSurface = p.toast,
        inverseOnSurface = p.onToast,
        outline = p.outline,
        outlineVariant = p.outline,
        scrim = Color.Black,
    )
}

private val LocalPleosColors = staticCompositionLocalOf { LightPleosColors }

/** Pleos tokens of the current theme. */
object Pleos {
    val colors: PleosColors
        @Composable @ReadOnlyComposable get() = LocalPleosColors.current
}

@Composable
fun PleosTheme(
    // The vehicle switches Light/Dark automatically with Screen Mode Auto.
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkPleosColors else LightPleosColors
    CompositionLocalProvider(LocalPleosColors provides colors) {
        MaterialTheme(
            colorScheme = scheme(colors, darkTheme),
            typography = Typography,
            content = content,
        )
    }
}
