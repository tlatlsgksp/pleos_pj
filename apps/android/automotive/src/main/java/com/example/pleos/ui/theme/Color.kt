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
 * Pleos system tokens the screens use by name (UI_DESIGN_GUIDE 3, "앱에서 자주 쓰는 시스템 토큰").
 * Screens read these instead of Material roles so the mapping to the guide stays visible.
 */
@Immutable
data class PleosColors(
    /** background_primary */
    val background: Color,
    /** surface_basic: cards on the background. */
    val surface: Color,
    /** surface_high: a card inside a card, list rows. */
    val surfaceHigh: Color,
    /** text_dark_primary / secondary / tertiary */
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    /** button_basic: the light, default button. */
    val buttonBasic: Color,
    val buttonBasicPressed: Color,
    val onButtonBasic: Color,
    /** button_filled: the dark, emphasised button. */
    val buttonFilled: Color,
    val buttonFilledPressed: Color,
    val onButtonFilled: Color,
    val outline: Color,
    /** informative_active: on, healthy. */
    val active: Color,
    /** informative_positive: positive, in progress. */
    val positive: Color,
    /** informative_negative: error, danger. */
    val negative: Color,
    val onAccent: Color,
    /** Toast: dark background, white text, in both themes. */
    val toast: Color,
    val onToast: Color,
)

val LightPleosColors = PleosColors(
    background = BasicLight50,
    surface = BasicLight00,
    surfaceHigh = BasicLight100,
    textPrimary = StaticDark,
    textSecondary = StaticDark.copy(alpha = 0.84f),
    textTertiary = StaticDark.copy(alpha = 0.64f),
    buttonBasic = BasicLight100,
    buttonBasicPressed = BasicLight200,
    onButtonBasic = StaticDark,
    buttonFilled = BasicLight600,
    buttonFilledPressed = BasicLight700,
    onButtonFilled = StaticLight,
    outline = BasicLight300,
    active = ActiveLight,
    positive = PositiveLight,
    negative = NegativeLight,
    onAccent = StaticLight,
    toast = BasicLight700,
    onToast = StaticLight,
)

val DarkPleosColors = PleosColors(
    background = BasicDark50,
    surface = BasicDark100,
    surfaceHigh = BasicDark200,
    textPrimary = StaticLight,
    textSecondary = StaticLight.copy(alpha = 0.84f),
    textTertiary = StaticLight.copy(alpha = 0.64f),
    buttonBasic = BasicDark200,
    buttonBasicPressed = BasicDark300,
    onButtonBasic = StaticLight,
    buttonFilled = BasicDark600,
    buttonFilledPressed = BasicDark500,
    onButtonFilled = StaticDark,
    outline = BasicDark300,
    active = ActiveDark,
    positive = PositiveDark,
    negative = NegativeDark,
    onAccent = StaticLight,
    toast = BasicLight700,
    onToast = StaticLight,
)
