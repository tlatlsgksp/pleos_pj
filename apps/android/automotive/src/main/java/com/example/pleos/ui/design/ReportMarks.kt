package com.example.pleos.ui.design

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.pleos.R
import com.example.pleos.domain.model.ReportType
import com.example.pleos.ui.theme.Pleos

/** The plate color of a report and the ink that sits on it. */
data class ReportTone(val fill: Color, val ink: Color)

/**
 * How urgent a report looks, the same everywhere it appears (sheet, home, map, banner): danger in
 * red, slowdowns in amber, enforcement in blue.
 */
@Composable
fun reportTone(type: ReportType): ReportTone {
    val c = Pleos.colors
    return when (type) {
        ReportType.ACCIDENT, ReportType.DISASTER -> ReportTone(c.negative, c.onSign)
        ReportType.CONSTRUCTION, ReportType.CONGESTION -> ReportTone(c.signAmber, c.onSignAmber)
        ReportType.SPEED_CAMERA -> ReportTone(c.signBlue, c.onSign)
    }
}

/** The drawn icon for a report type. */
@DrawableRes
fun ReportType.icon(): Int = when (this) {
    ReportType.ACCIDENT -> R.drawable.ic_report_accident
    ReportType.CONSTRUCTION -> R.drawable.ic_report_construction
    ReportType.CONGESTION -> R.drawable.ic_report_congestion
    ReportType.SPEED_CAMERA -> R.drawable.ic_report_camera
    ReportType.DISASTER -> R.drawable.ic_report_disaster
}

/** A report on the road ahead, as a road sign in the report's tone. */
@Composable
fun ReportSign(type: ReportType, text: String, modifier: Modifier = Modifier, arrow: SignArrow = SignArrow.AHEAD) {
    val tone = reportTone(type)
    RoadSignPlate(text, tone.fill, tone.ink, modifier, arrow)
}
