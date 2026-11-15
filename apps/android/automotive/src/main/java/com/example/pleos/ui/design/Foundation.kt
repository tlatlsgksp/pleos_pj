package com.example.pleos.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.pleos.ui.theme.Pleos
import com.example.pleos.ui.theme.Suit

// The app's own layout rules on top of the Pleos components (docs/visual/design-foundation.html).
// Screens pick a text role, a spacing step and a container from here; they never write sizes.

/** Smallest height of a tappable list row, for a gloved finger. */
val ROW_MIN_HEIGHT = 96.dp

/** Width of a leading odometer column ("124,845 km" at the label size). */
val ODOMETER_COLUMN_WIDTH = 150.dp

/** Corner radius of the one well on a screen and of panels (12 to 16 keeps them calm at car scale). */
val CARD_RADIUS = 16.dp

/** Corner radius of a selected row's well. */
val ROW_RADIUS = 12.dp

/** Spacing steps (design-foundation 4). 4 is for drawings only, like a progress bar. */
object Space {
    /** Title to its description, icon to text. */
    val xs: Dp = 8.dp

    /** Between elements inside a card, between buttons. */
    val s: Dp = 16.dp

    /** Between cards, section title to its first card, between columns. */
    val m: Dp = 24.dp

    /** Screen edge, inside a card. */
    val l: Dp = 32.dp

    /** Between sections. */
    val xl: Dp = 48.dp
}

/**
 * Large roles are tracked in a little (-0.01 to -0.03 em): Korean set large reads loose at zero.
 * Tabular figures only where numbers stack in a column ([PleosText.value], [PleosText.figure],
 * [PleosText.labelFigure]); in running text they space "11월" apart.
 */
private fun role(size: TextUnit, lineHeight: TextUnit, weight: FontWeight, tracking: Float = 0f, tabular: Boolean = false) = TextStyle(
    fontFamily = Suit,
    fontWeight = weight,
    fontSize = size,
    lineHeight = lineHeight,
    letterSpacing = tracking.em,
    fontFeatureSettings = if (tabular) "tnum" else null,
)

/** Text roles (design-foundation 2). Sizes are the Pleos type scale (UI_DESIGN_GUIDE 4). */
object PleosText {
    /** Title Medium Bold: the top bar. */
    val pageTitle = role(36.sp, 44.sp, FontWeight.ExtraBold, -0.02f)

    /** Title Small Bold: a section above cards, always outside them. */
    val sectionTitle = role(32.sp, 40.sp, FontWeight.Bold, -0.01f)

    /** A statement that answers the screen's question in a sentence, e.g. this month's spending. */
    val sentence = role(44.sp, 60.sp, FontWeight.ExtraBold, -0.025f)

    /** Headline Small Bold: numbers read at a glance. */
    val value = role(48.sp, 60.sp, FontWeight.ExtraBold, -0.03f, tabular = true)

    /** Body Large Bold: one card's name or the situation of an action card. */
    val cardTitle = role(30.sp, 38.sp, FontWeight.Bold, -0.01f)

    /** Body Medium Bold: the name of a list row. */
    val itemTitle = role(28.sp, 36.sp, FontWeight.Bold)

    /** Body Small: descriptions and second lines. The smallest body text. */
    val body = role(26.sp, 34.sp, FontWeight.Normal)

    /** Label Medium: a label above a value, dates, sources. Not for information needed while driving. */
    val label = role(24.sp, 28.sp, FontWeight.Medium)

    /** [itemTitle] with tabular figures: amounts in a list column. */
    val figure = role(28.sp, 36.sp, FontWeight.Bold, tabular = true)

    /** [label] with tabular figures: odometer readings in a list column. */
    val labelFigure = role(24.sp, 28.sp, FontWeight.Medium, tabular = true)

    /** Label Medium Bold in a semantic color: one status word. */
    val status = role(24.sp, 28.sp, FontWeight.Bold)
}

/** Shown when a value is unknown (design-foundation 3). */
const val UNKNOWN = "–"

/**
 * The space Pleos gives the app (docs/visual/layout-variants.html): next to the driving view the
 * app gets two thirds of the screen (TWO), or one third when two apps share it (ONE).
 */
enum class WindowWidth { ONE, TWO }

data class WindowInfo(val width: WindowWidth, val isLow: Boolean)

/** Below this the app has one third of the screen (628 to 840); above, two thirds (1256 to 1700). */
const val TWO_COLUMN_MIN_WIDTH_DP = 1000

/** L-12.9, W and WF windows are 800 to 860 tall; there low-priority blocks collapse to one line. */
private const val LOW_HEIGHT_MAX_DP = 860

/** Screen frame for a tab: edge padding and the window class for the content. */
@Composable
fun AdaptiveScreen(modifier: Modifier = Modifier, content: @Composable (WindowInfo) -> Unit) {
    BoxWithConstraints(modifier.fillMaxSize().padding(horizontal = Space.l)) {
        val info = WindowInfo(
            width = if (maxWidth.value >= TWO_COLUMN_MIN_WIDTH_DP) WindowWidth.TWO else WindowWidth.ONE,
            isLow = maxHeight.value <= LOW_HEIGHT_MAX_DP,
        )
        content(info)
    }
}

/** Two columns for the TWO window: [leftWeight] : [rightWeight], each scrolling on its own when [scroll]. */
@Composable
fun TwoColumns(
    leftWeight: Float,
    rightWeight: Float,
    left: @Composable ColumnScope.() -> Unit,
    right: @Composable ColumnScope.() -> Unit,
    scroll: Boolean = true,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Space.m), modifier = Modifier.fillMaxSize()) {
        PaneColumn(Modifier.weight(leftWeight), scroll, left)
        PaneColumn(Modifier.weight(rightWeight), scroll, right)
    }
}

/** One column of sections; the ONE window uses it for the whole screen. */
@Composable
fun PaneColumn(modifier: Modifier = Modifier, scroll: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(bottom = Space.l),
        content = content,
    )
}

/**
 * A section: title outside the cards, an optional Text button on the right, then the content.
 * Sections are [Space.xl] apart; the title is [Space.m] above its first card.
 */
@Composable
fun Section(
    title: String,
    modifier: Modifier = Modifier,
    first: Boolean = false,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(top = if (first) 0.dp else Space.xl), verticalArrangement = Arrangement.spacedBy(Space.m)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = PleosText.sectionTitle, color = Pleos.colors.textPrimary, modifier = Modifier.weight(1f))
            if (action != null && onAction != null) PleosTextButton(action, onAction)
        }
        content()
    }
}

/** Text button (UI_DESIGN_GUIDE 8): blue label and "›", for going to another screen or tab. */
@Composable
fun PleosTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Text(
        "$text ›",
        style = PleosText.itemTitle,
        color = Pleos.colors.textPrimary,
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.xs, vertical = Space.s),
    )
}

/** Back link for a detail shown in place of its list in the one-third window: "‹" and where it returns. */
@Composable
fun PleosBackLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Text(
        "‹ $text",
        style = PleosText.itemTitle,
        color = Pleos.colors.textPrimary,
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.xs, vertical = Space.s),
    )
}

/** A label–value line inside a detail card. */
@Composable
fun FactRow(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        HorizontalDivider(color = Pleos.colors.outline, thickness = RULE)
        Row(
            Modifier.fillMaxWidth().padding(vertical = Space.s),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = PleosText.label, color = Pleos.colors.textTertiary, modifier = Modifier.weight(1f))
            Text(value, style = PleosText.body, color = Pleos.colors.textPrimary)
        }
    }
}

/**
 * A list: rows split by rules, straight on the surface (no card). It bleeds [Space.s] past the
 * column on both sides so row text lines up with the section title while a selected row's well
 * still has room around it.
 */
@Composable
fun ListCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.fillMaxWidth().bleed(Space.s), content = content)
}

/** Rule between [ListRow]s; not before the first. */
@Composable
fun ListDivider() {
    HorizontalDivider(color = Pleos.colors.outline, thickness = RULE, modifier = Modifier.padding(horizontal = Space.s))
}

/** Thickness of every rule. */
val RULE = 2.dp

/** Widens the content by [amount] on both sides, past its parent's padding. */
fun Modifier.bleed(amount: Dp): Modifier = layout { measurable, constraints ->
    val extra = amount.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = (constraints.minWidth + extra).coerceAtLeast(0),
            maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else constraints.maxWidth,
        ),
    )
    layout(placeable.width - extra, placeable.height) { placeable.place(-extra / 2, 0) }
}

/**
 * One row of a list card: title and an optional second line on the left, [trailing] on the right.
 * At least 96 tall so a gloved finger can hit it. [selected] tints the row for list-detail.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val c = Pleos.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ROW_MIN_HEIGHT)
            .clip(RoundedCornerShape(ROW_RADIUS))
            .background(if (selected) c.surfaceHigh else Color.Transparent)
            .then(if (onClick != null) Modifier.clickable(interaction, indication = null, onClick = onClick) else Modifier)
            .padding(horizontal = Space.s, vertical = Space.m),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(title, style = PleosText.itemTitle, color = c.textPrimary)
            detail?.let { Text(it, style = PleosText.body, color = c.textSecondary) }
        }
        trailing()
    }
}

/** A rounded progress bar; past 1.0 it stays full and the color says overdue. */
@Composable
fun ProgressTrack(ratio: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Pleos.colors.track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(ratio.coerceIn(0f, 1f))
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color),
        )
    }
}

/** A color chip, e.g. the mood light color. [rgb] is 0xRRGGBB. */
@Composable
fun ColorSwatch(rgb: Int, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF000000 or rgb.toLong()))
            .border(2.dp, Pleos.colors.outline, RoundedCornerShape(12.dp)),
    )
}

/** Placeholder in an empty detail column. */
@Composable
fun EmptyDetail(text: String, modifier: Modifier = Modifier) {
    Text(text, style = PleosText.body, color = Pleos.colors.textSecondary, modifier = modifier.fillMaxWidth())
}

/**
 * A card or detail title with its status word at the end of the same line. The status never sits
 * above the title as a kicker (design-foundation 3).
 */
@Composable
fun TitleWithStatus(
    title: String,
    status: String?,
    statusColor: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = PleosText.cardTitle,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        Text(title, style = style, color = Pleos.colors.textPrimary, modifier = Modifier.weight(1f))
        status?.let { StatusLabel(it, statusColor) }
    }
}

/**
 * Battery or tank level before and after a charge or refuel: what was there in a light tint, what
 * was added in the signal color. Levels are percentages 0..100.
 */
@Composable
fun LevelMeter(before: Double, after: Double, startLabel: String, middleLabel: String, endLabel: String, modifier: Modifier = Modifier) {
    val c = Pleos.colors
    val from = (before / 100).toFloat().coerceIn(0f, 1f)
    val to = (after / 100).toFloat().coerceIn(from, 1f)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(c.surfaceHigh),
        ) {
            if (from > 0f) Box(Modifier.weight(from).fillMaxSize().background(c.track))
            if (to > from) Box(Modifier.weight(to - from).fillMaxSize().background(c.textPrimary))
            if (to < 1f) Box(Modifier.weight(1f - to).fillMaxSize())
        }
        Row(Modifier.fillMaxWidth()) {
            Text(startLabel, style = PleosText.label, color = c.textSecondary, modifier = Modifier.weight(1f))
            Text(middleLabel, style = PleosText.label, color = c.textSecondary)
            Text(endLabel, style = PleosText.label, color = c.textSecondary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
    }
}
