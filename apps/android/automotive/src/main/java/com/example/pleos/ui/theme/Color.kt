package com.example.pleos.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Pleos Connect grayscale tokens (docs/UI_DESIGN_GUIDE.md 3). Values invert between Light and Dark.
val BasicLight00 = Color(0xFFFFFFFF)
val BasicLight50 = Color(0xFFF7F8FA)
val BasicLight100 = Color(0xFFEDEEF2)
val BasicLight200 = Color(0xFFE0E1E6)
val BasicLight300 = Color(0xFFC7C9CE)
val BasicLight400 = Color(0xFFA4A8AE)
val BasicLight500 = Color(0xFF686A72)
val BasicLight600 = Color(0xFF44464E)
val BasicLight700 = Color(0xFF313236)
val BasicLight800 = Color(0xFF1D1E21)
val BasicLight900 = Color(0xFF131417)

val BasicDark00 = Color(0xFF131417)
val BasicDark50 = Color(0xFF1D1E21)
val BasicDark100 = Color(0xFF313236)
val BasicDark200 = Color(0xFF44464E)
val BasicDark300 = Color(0xFF686A72)
val BasicDark400 = Color(0xFFA4A8AE)
val BasicDark500 = Color(0xFFC7C9CE)
val BasicDark600 = Color(0xFFE0E1E6)
val BasicDark700 = Color(0xFFEDEEF2)
val BasicDark800 = Color(0xFFF7F8FA)
val BasicDark900 = Color(0xFFFFFFFF)

// Static tokens: the same in both themes.
private val StaticDark = Color(0xFF131417)
private val StaticLight = Color(0xFFFFFFFF)

// Informative tokens. Regulation colors (telltales) are deliberately absent: app UI must not use them.
val ActiveLight = Color(0xFF02C265)
val ActiveDark = Color(0xFF00E074)
val PositiveLight = Color(0xFF0064FF)
val PositiveDark = Color(0xFF0082FF)
val NegativeLight = Color(0xFFFE3D16)
val NegativeDark = Color(0xFFFF4C28)

/**
 * The app's palette (.impeccable/surfaces/apps-android-automotive.md): the Pleos grayscale roles
 * played straight, plus road-sign colors used only for things that lie on the road ahead.
 * Screens read these instead of Material roles.
 */
@Immutable
data class PleosColors(
    /** The app ground behind the cards. */
    val background: Color,
    /** Cards on the ground. */
    val surface: Color,
    /** A well inside a card: tab tracks, selected rows, icon tiles. */
    val surfaceHigh: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    /** button_basic: the light, default button. */
    val buttonBasic: Color,
    val buttonBasicPressed: Color,
    val onButtonBasic: Color,
    /** button_filled: the ink button, the one emphasised action. */
    val buttonFilled: Color,
    val buttonFilledPressed: Color,
    val onButtonFilled: Color,
    /** Rules between rows. */
    val outline: Color,
    /** Quiet bars in charts and the empty part of a track. */
    val track: Color,
    /** Selection and "my car now" marks (the speaking member, my row in a ranking). */
    val accent: Color,
    /** informative_active: the switch's on state, as the Pleos component draws it. */
    val active: Color,
    /** informative_positive. */
    val positive: Color,
    /** informative_negative: danger. */
    val negative: Color,
    val onAccent: Color,
    /** Road guide signs: destinations (green), waypoints and stations (blue), reports (amber). */
    val signGreen: Color,
    val signBlue: Color,
    val signAmber: Color,
    /** Text and border on green and blue signs. */
    val onSign: Color,
    /** Text and border on the amber sign. */
    val onSignAmber: Color,
    /** Toast: dark background, white text, in both themes. */
    val toast: Color,
    val onToast: Color,
)

val LightPleosColors = PleosColors(
    background = Color(0xFFF5F6F8),
    surface = BasicLight00,
    surfaceHigh = BasicLight100,
    textPrimary = BasicLight800,
    textSecondary = Color(0xFF5C5F67),
    textTertiary = Color(0xFF6E7078),
    buttonBasic = BasicLight100,
    buttonBasicPressed = BasicLight200,
    onButtonBasic = BasicLight800,
    buttonFilled = BasicLight800,
    buttonFilledPressed = BasicLight700,
    onButtonFilled = StaticLight,
    outline = BasicLight200,
    track = BasicLight300,
    accent = BasicLight800,
    active = ActiveLight,
    positive = PositiveLight,
    negative = NegativeLight,
    onAccent = StaticLight,
    signGreen = Color(0xFF0B6B45),
    signBlue = Color(0xFF1C5BA8),
    signAmber = Color(0xFFF2C230),
    onSign = StaticLight,
    onSignAmber = BasicLight800,
    toast = BasicLight700,
    onToast = StaticLight,
)

val DarkPleosColors = PleosColors(
    background = BasicDark00,
    surface = BasicDark50,
    surfaceHigh = BasicDark100,
    textPrimary = BasicDark900,
    textSecondary = BasicDark500,
    textTertiary = BasicDark400,
    buttonBasic = BasicDark100,
    buttonBasicPressed = BasicDark200,
    onButtonBasic = BasicDark900,
    buttonFilled = BasicDark800,
    buttonFilledPressed = BasicDark600,
    onButtonFilled = BasicDark00,
    outline = BasicDark100,
    track = BasicDark200,
    accent = BasicDark900,
    active = ActiveDark,
    positive = PositiveDark,
    negative = NegativeDark,
    onAccent = StaticLight,
    signGreen = Color(0xFF0E7A50),
    signBlue = Color(0xFF2266BA),
    signAmber = Color(0xFFF2C230),
    onSign = StaticLight,
    onSignAmber = BasicLight800,
    toast = BasicLight700,
    onToast = StaticLight,
)
