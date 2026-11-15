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
 * The app's palette (docs/visual/ledger-design-v5.html): Pleos roles, filled with cool gray tints
 * instead of pure white and black, and one blue [accent]. Screens read these instead of Material roles.
 */
@Immutable
data class PleosColors(
    /** The app surface. No cards sit on it; groups are split by [outline] rules. */
    val background: Color,
    /** A raised chip on [surfaceHigh], e.g. the selected Box tab. */
    val surface: Color,
    /** The one sunken well: the home action, tab tracks, selected rows. */
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
    /** Rules between rows and sections. */
    val outline: Color,
    /** Quiet bars in charts: past months, the empty part of a track. */
    val track: Color,
    /**
     * The key color, meaning "my car now" only: the current odometer on the care ruler, this month
     * in the ledger table, a record the car just detected. Never on controls or titles.
     */
    val accent: Color,
    /** informative_active: the switch's on state, as the Pleos component draws it. */
    val active: Color,
    /** informative_positive. */
    val positive: Color,
    /** informative_negative: overdue, danger. */
    val negative: Color,
    val onAccent: Color,
    /** Toast: dark background, white text, in both themes. */
    val toast: Color,
    val onToast: Color,
)

val LightPleosColors = PleosColors(
    background = Color(0xFFF5F7F9),
    surface = Color(0xFFFDFEFF),
    surfaceHigh = Color(0xFFE8ECF1),
    textPrimary = Color(0xFF1E232A),
    textSecondary = Color(0xFF545C66),
    textTertiary = Color(0xFF616973),
    buttonBasic = Color(0xFFE8ECF1),
    buttonBasicPressed = Color(0xFFDBE1E8),
    onButtonBasic = Color(0xFF1E232A),
    buttonFilled = Color(0xFF1E232A),
    buttonFilledPressed = Color(0xFF343B45),
    onButtonFilled = Color(0xFFF5F7F9),
    outline = Color(0xFFDBE1E8),
    track = Color(0xFFC6CDD6),
    accent = Color(0xFF1F5FD1),
    active = ActiveLight,
    positive = PositiveLight,
    negative = NegativeLight,
    onAccent = StaticLight,
    toast = BasicLight700,
    onToast = StaticLight,
)

val DarkPleosColors = PleosColors(
    background = Color(0xFF15181D),
    surface = Color(0xFF2E343C),
    surfaceHigh = Color(0xFF22272E),
    textPrimary = Color(0xFFE9EDF2),
    textSecondary = Color(0xFFAEB6C0),
    textTertiary = Color(0xFF959DA8),
    buttonBasic = Color(0xFF22272E),
    buttonBasicPressed = Color(0xFF2E343C),
    onButtonBasic = Color(0xFFE9EDF2),
    buttonFilled = Color(0xFFE9EDF2),
    buttonFilledPressed = Color(0xFFC9D0D8),
    onButtonFilled = Color(0xFF15181D),
    outline = Color(0xFF2E343C),
    track = Color(0xFF3A414B),
    accent = Color(0xFF7FA6FF),
    active = ActiveDark,
    positive = PositiveDark,
    negative = NegativeDark,
    onAccent = StaticLight,
    toast = BasicLight700,
    onToast = StaticLight,
)
