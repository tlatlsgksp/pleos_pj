package com.example.pleos.ui.design

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.getValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.Popup
import com.example.pleos.R
import com.example.pleos.ui.theme.Pleos

// The canon world (.impeccable/surfaces/apps-android-automotive.md): grey ground, white cards, ink
// buttons, and one kick point, real road guide signs for things that lie on the road ahead.

/** A white card on the grey ground. Clickable cards shrink a little while pressed. */
@Composable
fun HomeCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .then(if (onClick != null) Modifier.pressScale(interaction, enabled) else Modifier)
            .clip(RoundedCornerShape(CARD_RADIUS))
            .background(Pleos.colors.surface)
            .then(if (onClick != null) Modifier.clickable(interaction, indication = null, enabled = enabled, onClick = onClick) else Modifier)
            .alpha(if (enabled) 1f else 0.45f)
            .padding(Space.m),
        content = content,
    )
}

enum class SignKind { DESTINATION, WAYPOINT }
/** Where the sign's subject lies relative to the car's heading; the arrow is rotated by [degrees]. */
enum class SignArrow(val degrees: Float) { AHEAD(0f), RIGHT(45f), SHARP_RIGHT(90f), BACK(180f), SHARP_LEFT(-90f), LEFT(-45f) }

/**
 * A road guide sign: colored plate, white inner border, bold arrow. Only for something on the road
 * ahead: a ride's destination (green), a station to stop at (blue), a report on the route (its
 * [reportTone], through [ReportSign]).
 */
@Composable
fun RoadSign(text: String, kind: SignKind, modifier: Modifier = Modifier, arrow: SignArrow = SignArrow.AHEAD) {
    val c = Pleos.colors
    val (fill, ink) = when (kind) {
        SignKind.DESTINATION -> c.signGreen to c.onSign
        SignKind.WAYPOINT -> c.signBlue to c.onSign
    }
    RoadSignPlate(text, fill, ink, modifier, arrow)
}

@Composable
internal fun RoadSignPlate(text: String, fill: Color, ink: Color, modifier: Modifier, arrow: SignArrow?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
            .padding(3.dp)
            .border(3.dp, ink, RoundedCornerShape(9.dp))
            .padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
    ) {
        if (arrow != null) {
            Icon(
                painterResource(R.drawable.ic_arrow_up),
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(30.dp).rotate(arrow.degrees),
            )
        }
        Text(text, style = PleosText.status.copy(fontSize = PleosText.itemTitle.fontSize), color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Profile colors: deep, muted tones that sit on the cool gray ground and carry white initials. A member keeps one color
 * everywhere (home, the ride room, the ranking) because it comes from their handle.
 */
val AvatarPalette = listOf(
    Color(0xFF4A6FA5), // slate blue
    Color(0xFF2F6F73), // deep teal
    Color(0xFF6B7A3A), // olive
    Color(0xFFB5603F), // terracotta
    Color(0xFF6E4C7A), // plum
    Color(0xFF5B6B7F), // steel
    Color(0xFFA68A2E), // mustard
    Color(0xFF8A3B4C), // wine
)

fun avatarColor(key: String): Color = AvatarPalette[Math.floorMod(key.hashCode(), AvatarPalette.size)]

/**
 * A round profile mark for a member, colored by [key] (their handle). [speaking] rings it; [dim]
 * means not in the room.
 */
@Composable
fun MemberAvatar(name: String, key: String, modifier: Modifier = Modifier, speaking: Boolean = false, dim: Boolean = false, large: Boolean = false) {
    val c = Pleos.colors
    val size = if (large) 96.dp else 72.dp
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size + 12.dp)
            .alpha(if (dim) 0.4f else 1f)
            .then(if (speaking) Modifier.border(4.dp, c.accent, CircleShape) else Modifier)
            .padding(6.dp)
            .clip(CircleShape)
            .background(avatarColor(key)),
    ) {
        Text(name.take(2), style = PleosText.label, color = Color.White, maxLines = 1)
    }
}

/**
 * A card with a picture: the image fills the top and fades into the card color by the middle, and
 * the content sits on the lower half (the Genesis home pattern). [overlay] rides on the image.
 */
@Composable
fun ImageCard(
    @DrawableRes image: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    imageAlpha: Float = 1f,
    /** Drawn in place of [image] when set, e.g. a live map. */
    art: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Pleos.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .then(if (onClick != null) Modifier.pressScale(interaction, enabled) else Modifier)
            .clip(RoundedCornerShape(CARD_RADIUS))
            .background(c.surface)
            .then(if (onClick != null) Modifier.clickable(interaction, indication = null, enabled = enabled, onClick = onClick) else Modifier)
            .alpha(if (enabled) 1f else 0.45f),
    ) {
        if (art != null) {
            Box(Modifier.fillMaxSize().alpha(imageAlpha), content = art)
        } else {
            Image(
                painterResource(image),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = imageAlpha,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // A faint ink veil, then the card color, both on a smoothstep from 20% to 62% of the height:
        // no visible band where the picture ends, and text stays legible on bright and dark pictures.
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops = fadeStops(c.textPrimary.copy(alpha = 0.05f), 0.12f, 0.42f))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(colorStops = fadeStops(c.surface, 0.20f, 0.62f))))
        overlay?.let { Box(Modifier.fillMaxSize().padding(Space.m), content = it) }
        Column(
            verticalArrangement = Arrangement.spacedBy(Space.s, Alignment.Bottom),
            modifier = Modifier.fillMaxSize().padding(Space.m),
            content = content,
        )
    }
}

/** Stops easing [color] in with a smoothstep between [from] and [to] (fractions of the height). */
private fun fadeStops(color: Color, from: Float, to: Float): Array<Pair<Float, Color>> {
    val ramp = (0..10).map { i ->
        val t = i / 10f
        (from + (to - from) * t) to color.copy(alpha = color.alpha * t * t * (3 - 2 * t))
    }
    return (listOf(0f to color.copy(alpha = 0f)) + ramp + listOf(1f to color)).toTypedArray()
}

/** One quick action on home: a picture card with the name and one line of state. Finishes in place. */
@Composable
fun QuickAction(
    title: String,
    detail: String,
    @DrawableRes image: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sign: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = Pleos.colors
    ImageCard(image, modifier, onClick = onClick, enabled = enabled, overlay = sign?.let { s -> { Box(Modifier.align(Alignment.TopStart)) { s() } } }) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = PleosText.sectionTitle, color = c.textPrimary, maxLines = 1)
                Text(detail, style = PleosText.body, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            trailing?.invoke()
        }
    }
}

data class MenuItem(val label: String, @DrawableRes val icon: Int)

/**
 * The app menu: a vertical stack floating over the content at the bottom-left, taking no layout
 * space. [expanded] false folds it to a handle on the left edge; tapping the handle opens it.
 */
@Composable
fun BoxScope.FloatingMenu(
    items: List<MenuItem>,
    selected: Int,
    expanded: Boolean,
    onSelect: (Int) -> Unit,
    onExpand: () -> Unit,
) {
    val c = Pleos.colors
    if (!expanded) {
        val interaction = remember { MutableInteractionSource() }
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = Space.l)
                .size(width = 48.dp, height = 140.dp)
                .clickable(interaction, indication = null, role = Role.Button, onClick = onExpand),
        ) {
            Box(Modifier.size(width = 14.dp, height = 112.dp).clip(RoundedCornerShape(topEnd = 7.dp, bottomEnd = 7.dp)).background(c.buttonFilled))
        }
        return
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = Space.s, bottom = Space.s)
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(c.surface.copy(alpha = 0.96f))
            .padding(8.dp),
    ) {
        items.forEachIndexed { index, item ->
            val on = index == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                modifier = Modifier
                    .size(width = 104.dp, height = 96.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (on) c.buttonFilled else Color.Transparent)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(index) }),
            ) {
                Icon(painterResource(item.icon), contentDescription = null, tint = if (on) c.onButtonFilled else c.textSecondary, modifier = Modifier.size(36.dp))
                Text(item.label, style = PleosText.label, color = if (on) c.onButtonFilled else c.textSecondary, maxLines = 1)
            }
        }
    }
}

/**
 * The full-width banner that drops from the top of the app window: a route report, an invitation,
 * a detected charge. Leading content, one sentence, up to two buttons.
 */
@Composable
fun BoxScope.TopBanner(
    visible: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    onClick: (() -> Unit)? = null,
    leading: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = Pleos.colors
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(260)) { -it },
        exit = slideOutVertically(tween(200)) { -it },
        modifier = modifier.align(Alignment.TopCenter),
    ) {
        val interaction = remember { MutableInteractionSource() }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 112.dp)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(c.toast)
                .then(if (onClick != null) Modifier.clickable(interaction, indication = null, onClick = onClick) else Modifier)
                .padding(horizontal = Space.l, vertical = Space.s),
        ) {
            leading()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = PleosText.cardTitle, color = c.onToast, maxLines = 1, overflow = TextOverflow.Ellipsis)
                detail?.let { Text(it, style = PleosText.label, color = c.onToast.copy(alpha = 0.8f), maxLines = 1) }
            }
            actions()
        }
    }
}

/** A light button that reads on the dark banner. */
@Composable
fun BannerButton(text: String, onClick: () -> Unit, primary: Boolean = false) {
    val c = Pleos.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = 72.dp)
            .width(if (primary) 160.dp else 140.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (primary) c.surface else c.onToast.copy(alpha = 0.16f))
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick),
    ) {
        Text(text, style = PleosText.itemTitle, color = if (primary) c.textPrimary else c.onToast)
    }
}

/**
 * Press-to-talk: records while held. The biggest control of a ride, placed at the driver's side.
 * Disabled (grey) when not in the room.
 */
@Composable
fun TalkButton(label: String, talking: Boolean, enabled: Boolean, onTalkingChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val c = Pleos.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        modifier = modifier
            .heightIn(min = 120.dp)
            .width(300.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                when {
                    !enabled -> c.buttonBasic
                    talking -> c.negative
                    else -> c.buttonFilled
                },
            )
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            onTalkingChange(true)
                            tryAwaitRelease()
                            onTalkingChange(false)
                        })
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Icon(painterResource(R.drawable.ic_mic), contentDescription = null, tint = if (enabled) c.onButtonFilled else c.textTertiary, modifier = Modifier.size(40.dp))
        Text(label, style = PleosText.cardTitle, color = if (enabled) c.onButtonFilled else c.textTertiary)
    }
}

/** A thin vertical rule between groups in a bar. */
@Composable
fun BarDivider() {
    Box(Modifier.size(width = 2.dp, height = 56.dp).background(Pleos.colors.outline))
}

/** A floating panel over a map: white, rounded, softly shadowed. */
@Composable
fun FloatingPanel(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s),
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(Pleos.colors.surface)
            .padding(Space.s),
        content = content,
    )
}

/** Stand-in for the pairing QR until the server issues real codes. */
@Composable
fun QrPlaceholder() {
    val c = Pleos.colors
    Column(Modifier.size(240.dp).clip(RoundedCornerShape(16.dp)).background(c.surface).border(2.dp, c.outline, RoundedCornerShape(16.dp)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(6) { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(6) { col -> Box(Modifier.size(26.dp).background(if ((r * 7 + col * 3) % 4 < 2) c.textPrimary else c.surface)) }
            }
        }
    }
}

/**
 * The app's navigation as its own card: a vertical rail on the left in the two-thirds window, a
 * row at the bottom in the one-third window. It takes its own space, so it never covers content.
 */
@Composable
fun NavCard(
    items: List<MenuItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    vertical: Boolean,
    modifier: Modifier = Modifier,
    /** How many of the last items are pushed to the far end, away from the destinations (settings). */
    trailingCount: Int = 0,
) {
    val lead = items.size - trailingCount
    val c = Pleos.colors
    val item: @Composable (Int, MenuItem, Modifier) -> Unit = { index, it, m ->
        val on = index == selected
        val fill by animateColorAsState(if (on) c.buttonFilled else c.surface, tween(240), label = "navFill")
        val ink by animateColorAsState(if (on) c.onButtonFilled else c.textSecondary, tween(240), label = "navInk")
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            modifier = m
                .clip(RoundedCornerShape(18.dp))
                .background(fill)
                .selectable(selected = on, role = Role.Tab, onClick = { onSelect(index) }),
        ) {
            Icon(painterResource(it.icon), contentDescription = null, tint = ink, modifier = Modifier.size(40.dp))
            Text(it.label, style = PleosText.label, color = ink, maxLines = 1)
        }
    }
    if (vertical) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.width(128.dp).clip(RoundedCornerShape(CARD_RADIUS)).background(c.surface).padding(10.dp),
        ) {
            items.take(lead).forEachIndexed { i, it -> item(i, it, Modifier.fillMaxWidth().heightIn(min = 112.dp)) }
            if (trailingCount > 0) {
                Spacer(Modifier.weight(1f))
                items.drop(lead).forEachIndexed { i, it -> item(lead + i, it, Modifier.fillMaxWidth().heightIn(min = 112.dp)) }
            }
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(CARD_RADIUS)).background(c.surface).padding(8.dp),
        ) {
            items.take(lead).forEachIndexed { i, it -> item(i, it, Modifier.weight(1f).heightIn(min = 96.dp)) }
            if (trailingCount > 0) {
                BarDivider()
                items.drop(lead).forEachIndexed { i, it -> item(lead + i, it, Modifier.weight(1f).heightIn(min = 96.dp)) }
            }
        }
    }
}

/**
 * A sheet that rises from the bottom over a scrim covering the whole app window, menu included:
 * full width inside the side padding, top corners rounded, a round close button at the top left,
 * nearest the driver. Tapping the scrim or pressing back also closes it.
 */
@Composable
fun BottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Pleos.colors
    val shown = remember { MutableTransitionState(false) }.apply { targetState = true }
    val scrim = remember { MutableInteractionSource() }
    val block = remember { MutableInteractionSource() }
    Popup(alignment = Alignment.BottomCenter, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true, dismissOnBackPress = true)) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxSize()
                .clickable(scrim, indication = null, onClick = onDismiss),
        ) {
            AnimatedVisibility(shown, enter = fadeIn(tween(200))) {
                Box(Modifier.fillMaxSize().background(c.buttonFilled.copy(alpha = 0.4f)))
            }
            AnimatedVisibility(shown, enter = slideInVertically(tween(260)) { it }) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Space.m),
                    modifier = Modifier
                        .padding(horizontal = Space.l)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(c.surface)
                        .clickable(block, indication = null) {}
                        .padding(start = Space.xl, end = Space.xl, top = Space.l, bottom = Space.xl),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                        PleosIconButton(R.drawable.ic_close, stringResource(R.string.common_close), onDismiss)
                        Text(title, style = PleosText.pageTitle, color = c.textPrimary)
                    }
                    content()
                }
            }
        }
    }
}

/** One big choice in a sheet: icon (on its [plate], if any) above its label, the whole tile tappable. */
@Composable
fun ModalChoice(label: String, @DrawableRes icon: Int, onClick: () -> Unit, modifier: Modifier = Modifier, plate: Color? = null, ink: Color? = null) {
    val c = Pleos.colors
    val interaction = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.s, Alignment.CenterVertically),
        modifier = modifier
            .pressScale(interaction)
            .heightIn(min = 200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(c.surfaceHigh)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(Space.m),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(112.dp).clip(RoundedCornerShape(24.dp)).then(if (plate != null) Modifier.background(plate) else Modifier),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = ink ?: c.textPrimary, modifier = Modifier.size(72.dp))
        }
        Text(label, style = PleosText.cardTitle, color = c.textPrimary, maxLines = 1)
    }
}

/** A big color choice for the mood light; the current one carries a thick ink ring. */
@Composable
fun BigSwatch(rgb: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Pleos.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction)
            .size(152.dp)
            .clip(RoundedCornerShape(28.dp))
            .then(if (selected) Modifier.border(6.dp, c.textPrimary, RoundedCornerShape(28.dp)) else Modifier)
            .padding(if (selected) 12.dp else 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF000000 or rgb.toLong()))
            .border(2.dp, c.outline, RoundedCornerShape(20.dp))
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick),
    )
}

/** Gold, silver and bronze for the top three; null below. */
fun podiumTone(rank: Int): Color? = when (rank) {
    1 -> Color(0xFFC99A1E)
    2 -> Color(0xFF8E96A1)
    3 -> Color(0xFFB0703A)
    else -> null
}

/** A leaderboard position: the top three are filled medals with white numbers, the rest an outlined circle. */
@Composable
fun RankBadge(rank: Int, modifier: Modifier = Modifier) {
    val c = Pleos.colors
    val medal = podiumTone(rank)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(64.dp)
            .clip(CircleShape)
            .then(if (medal != null) Modifier.background(medal) else Modifier.border(3.dp, c.outline, CircleShape)),
    ) {
        Text("$rank", style = PleosText.itemTitle.copy(fontFeatureSettings = "tnum"), color = if (medal != null) Color.White else c.textSecondary, maxLines = 1)
    }
}

/** A rank change as a chip: trend arrow and places on a faint green (up) or red (down) tint. */
@Composable
fun TrendChip(change: Int, modifier: Modifier = Modifier, large: Boolean = false) {
    if (change == 0) return
    val c = Pleos.colors
    val tone = if (change > 0) c.signGreen else c.negative
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(CircleShape)
            .background(tone.copy(alpha = 0.1f))
            .padding(horizontal = if (large) 16.dp else 12.dp, vertical = if (large) 8.dp else 4.dp),
    ) {
        Icon(
            painterResource(if (change > 0) R.drawable.ic_trend_up else R.drawable.ic_trend_down),
            contentDescription = null,
            tint = tone,
            modifier = Modifier.size(if (large) 36.dp else 28.dp),
        )
        Text("${kotlin.math.abs(change)}", style = (if (large) PleosText.cardTitle else PleosText.status).copy(fontFeatureSettings = "tnum"), color = tone, maxLines = 1)
    }
}

/** A number with its unit set smaller and quieter beside it, on one baseline: "214 위", "6.0 km/kWh". */
@Composable
fun NumberUnit(number: String, unit: String, numberStyle: TextStyle, unitStyle: TextStyle, modifier: Modifier = Modifier) {
    val c = Pleos.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(number, style = numberStyle.copy(fontFeatureSettings = "tnum"), color = c.textPrimary, maxLines = 1, modifier = Modifier.alignByBaseline())
        Text(unit, style = unitStyle, color = c.textSecondary, maxLines = 1, modifier = Modifier.alignByBaseline())
    }
}

/** Marks the driver's own row wherever it appears: a blue outline. */
@Composable
fun Modifier.mineMark(mine: Boolean): Modifier = if (mine) border(4.dp, Pleos.colors.signBlue, RoundedCornerShape(ROW_RADIUS)) else this

/** A group ride I am in, marked on home: a green sign plate on the picture and a green outline on the card. */
@Composable
fun LiveBadge(text: String, modifier: Modifier = Modifier) {
    val c = Pleos.colors
    RoadSignPlate(text, c.signGreen, c.onSign, modifier, arrow = null)
}

@Composable
fun Modifier.liveOutline(on: Boolean): Modifier = if (on) border(4.dp, Pleos.colors.signGreen, RoundedCornerShape(CARD_RADIUS)) else this
