package com.example.pleos.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pleos.ui.theme.Pleos

// Drawings of the fuel and charge tab (docs/visual/ledger-design-v5.html): the odometer spine of
// records, bars inside the monthly table, filter chips, the amount field. Only [PleosColors.accent] marks "my car now"; everything else is ink and gray.

/** Width of the dot column of the spine. */
private val SPINE_DOT_COLUMN = 32.dp
private val SPINE_DOT = 20.dp

/** Small filters inside a tab: the selected one is ink, the rest sit in a well. */
@Composable
fun FilterChips(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Pleos.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
        labels.forEachIndexed { index, label ->
            val on = index == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .heightIn(min = 64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (on) c.textPrimary else c.surfaceHigh)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(horizontal = Space.m),
            ) {
                Text(label, style = PleosText.status, color = if (on) c.background else c.textSecondary, maxLines = 1)
            }
        }
    }
}

/**
 * One record on the spine: the odometer the car stamped, a dot, what and when, the amount. A
 * record the car just detected gets the accent dot and its [flag] under the amount.
 */
@Composable
fun SpineRow(
    odometer: String?,
    title: String,
    time: String?,
    amount: String,
    flag: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Pleos.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s),
        modifier = modifier
            .fillMaxWidth()
            .bleed(Space.s)
            .heightIn(min = ROW_MIN_HEIGHT)
            .clip(RoundedCornerShape(ROW_RADIUS))
            .background(if (selected) c.surfaceHigh else Color.Transparent)
            .clickable(interaction, indication = null, onClick = onClick)
            .padding(horizontal = Space.s, vertical = Space.s),
    ) {
        if (odometer != null) {
            Text(odometer, style = PleosText.labelFigure, color = c.textSecondary, textAlign = TextAlign.End, modifier = Modifier.width(ODOMETER_COLUMN_WIDTH))
        }
        Box(Modifier.width(SPINE_DOT_COLUMN), contentAlignment = Alignment.Center) {
            val pending = flag != null
            Box(
                Modifier
                    .size(SPINE_DOT)
                    .clip(CircleShape)
                    .background(if (pending) c.accent else c.background)
                    .border(4.dp, if (pending) c.accent else c.textPrimary, CircleShape),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = PleosText.itemTitle, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            time?.let { Text(it, style = PleosText.label, color = c.textTertiary, maxLines = 1) }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(amount, style = PleosText.figure, color = c.textPrimary, maxLines = 1)
            flag?.let { Text(it, style = PleosText.status, color = c.accent, maxLines = 1) }
        }
    }
}

/** The stretch of road between two records: the spine continues and says how far was driven. */
@Composable
fun SpineGap(text: String?, withOdometer: Boolean, modifier: Modifier = Modifier) {
    val c = Pleos.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s),
        modifier = modifier.fillMaxWidth().height(44.dp),
    ) {
        if (withOdometer) Box(Modifier.width(ODOMETER_COLUMN_WIDTH))
        Box(Modifier.width(SPINE_DOT_COLUMN).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(c.outline))
        }
        text?.let { Text(it, style = PleosText.label, color = c.textTertiary, maxLines = 1) }
    }
}

/** A bar inside a table cell, read next to its number. Only the current row is [highlighted]. */
@Composable
fun InlineBar(fraction: Float, highlighted: Boolean, modifier: Modifier = Modifier) {
    val c = Pleos.colors
    Box(modifier.width(160.dp).height(12.dp)) {
        if (fraction <= 0f) return@Box
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (highlighted) c.accent else c.track),
        )
    }
}

/** Shows typed digits as "25,952" while the value stays digits only. */
private object ThousandsSeparator : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val out = digits.reversed().chunked(3).joinToString(",").reversed()
        // Commas before original position i: those that sit left of it.
        fun commasBefore(i: Int) = ((digits.length - 1) / 3) - ((digits.length - i) / 3).coerceAtMost((digits.length - 1) / 3)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = offset + commasBefore(offset).coerceAtLeast(0)
            override fun transformedToOriginal(offset: Int) = out.take(offset).count { it != ',' }
        }
        return TransformedText(AnnotatedString(out), mapping)
    }
}

/**
 * The amount of a record as an input: ink outline, the number at the value size, the unit after
 * it. Digits only.
 */
@Composable
fun AmountField(value: String, onValueChange: (String) -> Unit, unit: String, modifier: Modifier = Modifier) {
    val c = Pleos.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
            .border(4.dp, c.textPrimary, RoundedCornerShape(24.dp))
            .padding(horizontal = Space.l),
    ) {
        BasicTextField(
            value = value,
            onValueChange = { text -> onValueChange(text.filter(Char::isDigit).take(9)) },
            textStyle = PleosText.value.copy(color = c.textPrimary),
            singleLine = true,
            cursorBrush = SolidColor(c.textPrimary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = ThousandsSeparator,
            modifier = Modifier.weight(1f),
        )
        Text(unit, style = PleosText.cardTitle, color = c.textSecondary)
    }
}
