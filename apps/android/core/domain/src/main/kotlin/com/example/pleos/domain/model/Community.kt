package com.example.pleos.domain.model

import java.time.Instant

/** A Moli driver as other drivers see them: nickname, handle and their car. */
data class Member(
    val id: String,
    val nickname: String,
    /** Profile id without the leading @. Neighbors add each other by it. */
    val handle: String,
    val carModel: String,
    val isEv: Boolean,
    /** Last shared position (car first, phone otherwise); null when the member is not sharing. */
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** Direction of travel in degrees clockwise from north; null when unknown or parked. */
    val headingDeg: Double? = null,
)

/** Where I stand in one group ride. A ride is an open room, like a voice channel. */
enum class RideMembership {
    /** Invited and not joined yet. */
    INVITED,
    /** Joined: sharing location and hearing the walkie-talkie. */
    IN,
    /** Joined before and left; the ride is still open, so I can join again. */
    LEFT,
    /** A neighbor's ride I was not invited to. */
    NOT_INVITED,
}

data class Riding(
    val id: String,
    val name: String,
    val host: Member,
    val destinationName: String?,
    /** In the room now. Includes me when [membership] is IN. */
    val joined: List<Member>,
    /** Invited or once joined, not in the room now. */
    val away: List<Member>,
    val membership: RideMembership,
    val invitedAt: Instant? = null,
    val destinationLatitude: Double? = null,
    val destinationLongitude: Double? = null,
)

/** [change]: places moved since the previous period, up positive. */
data class RankEntry(val rank: Int, val member: Member, val kmPerUnit: Double, val change: Int? = null)

/** Who I am ranked against: drivers of the same model, of my region, or my neighbors only. */
enum class RankScope { MODEL, REGION, NEIGHBORS }

enum class RankPeriod { WEEK, MONTH }

/** An efficiency ranking for one scope and period. km/kWh for an EV, km/L for a fuel car. */
data class Ranking(
    /** What the scope is called: the model ("아이오닉 5"), the region ("서울") or the neighbors. */
    val scopeName: String,
    val myRank: Int?,
    val myKmPerUnit: Double?,
    /** My rank in the previous period of the same length. */
    val previousRank: Int?,
    val leaders: List<RankEntry>,
    val hardAccelerations: Int,
    val hardBrakings: Int,
    val ecoModeShare: Double?,
)

enum class ReportType { ACCIDENT, CONSTRUCTION, CONGESTION, SPEED_CAMERA, DISASTER }

/** A road report from a driver, or an official traffic event from the navigation. */
data class RoadReport(
    val id: String,
    val type: ReportType,
    val reportedAt: Instant,
    val latitude: Double,
    val longitude: Double,
    /** Distance along my route, when it lies on it. */
    val aheadKm: Double? = null,
)
