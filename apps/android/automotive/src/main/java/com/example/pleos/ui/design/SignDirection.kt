package com.example.pleos.ui.design

import com.example.pleos.domain.model.Member
import com.example.pleos.domain.model.Riding
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * The arrow for something at ([lat], [lng]) as seen from [me]: its bearing against my heading,
 * rounded to one of six directions. Ahead when I have no position or heading, since the sign
 * is still about the road in front.
 */
fun arrowToward(me: Member?, lat: Double, lng: Double): SignArrow {
    val fromLat = me?.latitude ?: return SignArrow.AHEAD
    val fromLng = me.longitude ?: return SignArrow.AHEAD
    val heading = me.headingDeg ?: return SignArrow.AHEAD
    val bearing = bearingDeg(fromLat, fromLng, lat, lng)
    val relative = ((bearing - heading + 540) % 360) - 180 // -180..180, right positive
    return when {
        relative > 157.5 || relative < -157.5 -> SignArrow.BACK
        relative > 67.5 -> SignArrow.SHARP_RIGHT
        relative > 22.5 -> SignArrow.RIGHT
        relative < -67.5 -> SignArrow.SHARP_LEFT
        relative < -22.5 -> SignArrow.LEFT
        else -> SignArrow.AHEAD
    }
}

private fun bearingDeg(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val p1 = Math.toRadians(lat1)
    val p2 = Math.toRadians(lat2)
    val dl = Math.toRadians(lng2 - lng1)
    val y = sin(dl) * cos(p2)
    val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
    return (Math.toDegrees(atan2(y, x)) + 360) % 360
}

/** The arrow for a ride's destination; ahead when the ride has no position for it. */
fun Riding.destinationArrow(me: Member?): SignArrow {
    val lat = destinationLatitude ?: return SignArrow.AHEAD
    val lng = destinationLongitude ?: return SignArrow.AHEAD
    return arrowToward(me, lat, lng)
}
