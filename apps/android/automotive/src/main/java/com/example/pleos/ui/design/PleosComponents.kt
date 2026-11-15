package com.example.pleos.ui.design

import androidx.annotation.DrawableRes
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.draw.shadow
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

/**
 * A group of content. By default it is not a card: it sits straight on the surface with no fill
 * and no padding. Pass [color] for the one well a screen may have (the home action) or for danger;
 * a filled group gets [Space.l] padding. Clickable groups get the same press feedback as buttons.
 */
@Composable
fun PleosCard(
    modifier: Modifier = Modifier,
    color: Color = Color.Transparent,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = if (color == Color.Transparent) PaddingValues(0.dp) else PaddingValues(Space.l),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(Space.s),
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        verticalArrangement = verticalArrangement,
        modifier = modifier
            .then(if (onClick != null) Modifier.pressScale(interaction) else Modifier)
            .clip(RoundedCornerShape(CARD_RADIUS))
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
        // Same top margin as the main tabs, so a page title never touches the window edge.
        modifier = modifier.fillMaxWidth().padding(top = Space.l, bottom = Space.s).heightIn(min = 96.dp),
    ) {
        if (onBack != null) {
            PleosIconButton(R.drawable.ic_arrow_back, stringResource(R.string.common_back), onBack)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            style = PleosText.pageTitle,
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
    Text(text, color = color, style = PleosText.status, modifier = modifier)
}

/** Label above a large value, the glanceable unit of the home screen. */
@Composable
fun ValueBlock(label: String, value: String, modifier: Modifier = Modifier, valueStyle: TextStyle = PleosText.value) {
    val c = Pleos.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Text(label, style = PleosText.label, color = c.textTertiary)
        Text(value, style = valueStyle, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun BoxScope.CenteredMessage(text: String) {
    Text(
        text,
        style = PleosText.body,
        color = Pleos.colors.textSecondary,
        modifier = Modifier.align(Alignment.Center).padding(Space.l),
    )
}

/** A round icon button, gray at rest: back, close. Sized for a gloved thumb. */
@Composable
fun PleosIconButton(@DrawableRes icon: Int, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Pleos.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .pressScale(interaction)
            .size(ButtonSize.Large.height)
            .clip(CircleShape)
            .background(if (pressed) c.buttonBasicPressed else c.buttonBasic)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick),
    ) {
        Icon(painterResource(icon), contentDescription = contentDescription, tint = c.textPrimary, modifier = Modifier.size(48.dp))
    }
}

/**
 * A segmented control, as wide as its labels: the selected segment fills with ink and white text,
 * the others are gray text on the gray track. Selection changes cross-fade.
 */
@Composable
fun BoxTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Pleos.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(CARD_RADIUS + Space.xs))
            .background(c.surfaceHigh)
            .padding(Space.xs),
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            val fill by animateColorAsState(if (isSelected) c.buttonFilled else c.surfaceHigh, tween(240), label = "segment")
            val ink by animateColorAsState(if (isSelected) c.onButtonFilled else c.textSecondary, tween(240), label = "segmentText")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .heightIn(min = ButtonSize.Large.height)
                    .clip(RoundedCornerShape(CARD_RADIUS))
                    .background(fill)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(horizontal = Space.l),
            ) {
                Text(label, style = PleosText.itemTitle.copy(fontWeight = FontWeight.Bold), color = ink, maxLines = 1)
            }
        }
    }
}

/** Line tabs (UI_DESIGN_GUIDE 8, Tabs): left aligned, the selected tab is underlined. Filters inside a section. */
@Composable
fun LineTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Pleos.colors
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .heightIn(min = 64.dp)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(index) }),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                    color = if (isSelected) c.textPrimary else c.textTertiary,
                    maxLines = 1,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
                Box(
                    Modifier
                        .size(width = 48.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isSelected) c.textPrimary else Color.Transparent),
                )
            }
        }
    }
}

/** Switch (UI_DESIGN_GUIDE 8): rounded track, white knob; off is gray with the knob left, on is green with it right. */
@Composable
fun PleosSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = Pleos.colors
    val track by animateColorAsState(if (checked) c.active else c.buttonBasicPressed, tween(PRESS_MS, easing = PleosEasing), label = "switchTrack")
    Box(
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        modifier = modifier
            .size(width = 96.dp, height = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(track)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(6.dp),
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color.White))
    }
}
