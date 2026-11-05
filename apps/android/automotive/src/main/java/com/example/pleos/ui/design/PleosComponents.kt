package com.example.pleos.ui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.pleos.R
import com.example.pleos.ui.theme.Pleos

// Pleos Connect components (docs/UI_DESIGN_GUIDE.md 7 and 8). Sizes the guide leaves to the Figma
// assets are marked [기본값] and chosen for a glove-friendly touch area on a 160 dpi display.

/** ivi motion easing and short2 duration (UI_DESIGN_GUIDE 7). */
private val PleosEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private const val PRESS_MS = 150

/** Screen padding inside the app window. [기본값] */
val ScreenPadding: Dp = 32.dp

/** Gap between sibling cards. [기본값] */
val CardGap: Dp = 24.dp

/** Touch feedback from the guide: shrink to 95% around the center while pressed. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource, enabled: Boolean = true): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.95f else 1f,
        animationSpec = tween(PRESS_MS, easing = PleosEasing),
        label = "pressScale",
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
}

enum class ButtonStyle {
    /** Light gray fill: the default action. */
    Basic,

    /** Dark fill, white label: the one emphasised action. Sits on the left of a pair. */
    Filled,
}

enum class ButtonSize(val height: Dp, val horizontalPadding: Dp) {
    // [기본값] heights; the guide defines L/M/S but keeps the numbers in Figma.
    Large(96.dp, 48.dp),
    Medium(80.dp, 40.dp),
    Small(64.dp, 32.dp),
}

/**
 * Basic button. Pressed darkens the fill and shrinks to 95%; disabled is outline only, as the
 * guide draws it, so an outline-only button always means "not available".
 */
@Composable
fun PleosButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Basic,
    size: ButtonSize = ButtonSize.Medium,
    enabled: Boolean = true,
) {
    val c = Pleos.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill = when {
        !enabled -> Color.Transparent
        style == ButtonStyle.Filled -> if (pressed) c.buttonFilledPressed else c.buttonFilled
        else -> if (pressed) c.buttonBasicPressed else c.buttonBasic
    }
    val label = when {
        !enabled -> c.textTertiary
        style == ButtonStyle.Filled -> c.onButtonFilled
        else -> c.onButtonBasic
    }
    val animatedFill by animateColorAsState(fill, tween(PRESS_MS, easing = PleosEasing), label = "buttonFill")
    val shape = RoundedCornerShape(size.height / 4)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .pressScale(interaction, enabled)
            .heightIn(min = size.height)
            .clip(shape)
            .background(animatedFill)
            .then(if (enabled) Modifier else Modifier.border(2.dp, c.outline, shape))
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = size.horizontalPadding),
    ) {
        Text(
            text = text,
            color = label,
            style = when (size) {
                ButtonSize.Large -> MaterialTheme.typography.bodyLarge
                ButtonSize.Medium -> MaterialTheme.typography.bodyMedium
                ButtonSize.Small -> MaterialTheme.typography.labelMedium
            }.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A surface_basic card. Clickable cards get the same press feedback as buttons. */
@Composable
fun PleosCard(
    modifier: Modifier = Modifier,
    color: Color = Pleos.colors.surface,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(28.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        verticalArrangement = verticalArrangement,
        modifier = modifier
            .then(if (onClick != null) Modifier.pressScale(interaction) else Modifier)
            .clip(RoundedCornerShape(28.dp))
            .background(color)
            .then(if (onClick != null) Modifier.clickable(interaction, indication = null, onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/**
 * Page Navigation (UI_DESIGN_GUIDE 8). Depth type when [onBack] is set: back button and title.
 * Main type otherwise: title with actions on the right.
 */
@Composable
fun PageNavigation(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = Pleos.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().heightIn(min = 96.dp),
    ) {
        if (onBack != null) {
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .pressScale(interaction)
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    // Plain icon button: background only while pressed.
                    .background(if (pressed) c.buttonBasicPressed else Color.Transparent)
                    .clickable(interaction, indication = null, role = Role.Button, onClick = onBack),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = stringResource(R.string.common_back),
                    tint = c.textPrimary,
                    modifier = Modifier.size(48.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = c.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

/**
 * Toast (UI_DESIGN_GUIDE 8): bottom center of the app window, 24 from the edges, at most 792 wide
 * with 40/24 padding, dark with white centered text, two lines at most.
 */
@Composable
fun PleosToastHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier = modifier) { data ->
        val c = Pleos.colors
        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.BottomCenter) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .widthIn(max = 792.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(c.toast)
                    .padding(horizontal = 40.dp, vertical = 24.dp),
            ) {
                Text(
                    text = data.visuals.message,
                    color = c.onToast,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A short status word in a semantic color, e.g. "곧 교체". */
@Composable
fun StatusLabel(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(text, color = color, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), modifier = modifier)
}

/** Label above a large value, the glanceable unit of the home screen. */
@Composable
fun ValueBlock(label: String, value: String, modifier: Modifier = Modifier, valueStyle: TextStyle = MaterialTheme.typography.headlineSmall) {
    val c = Pleos.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
        Text(value, style = valueStyle, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Wide app windows (landscape 1274 and up) get two panes; portrait (628 to 840) gets one. */
const val TWO_PANE_MIN_WIDTH_DP = 1100

@Composable
fun BoxScope.CenteredMessage(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = Pleos.colors.textSecondary,
        modifier = Modifier.align(Alignment.Center).padding(ScreenPadding),
    )
}
