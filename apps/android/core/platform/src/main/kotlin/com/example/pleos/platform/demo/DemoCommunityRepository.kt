package com.example.pleos.platform.demo

import com.example.pleos.domain.model.Member
import com.example.pleos.domain.model.RankEntry
import com.example.pleos.domain.model.RankPeriod
import com.example.pleos.domain.model.RankScope
import com.example.pleos.domain.model.Ranking
import com.example.pleos.domain.model.ReportType
import com.example.pleos.domain.model.RideMembership
import com.example.pleos.domain.model.Riding
import com.example.pleos.domain.model.RoadReport
import com.example.pleos.domain.repository.CommunityRepository
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory community for the demo until the server exists. Neighbors, one invitation, a ranking
 * and two road reports are seeded so every home state can be shown on the emulator.
 */
@Singleton
class DemoCommunityRepository @Inject constructor(private val clock: Clock) : CommunityRepository {

    private val meState = MutableStateFlow(Member("me", "민수", "minsu_ioniq", "아이오닉 5", isEv = true, latitude = 37.62, longitude = 128.33, headingDeg = 70.0))
    private val jieun = Member("n1", "지은", "jieun_ev6", "EV6", isEv = true, latitude = 37.66, longitude = 128.52)
    private val hyunwoo = Member("n2", "현우", "hyunwoo", "아이오닉 5", isEv = true, latitude = 37.58, longitude = 128.14)
    private val seoyeon = Member("n3", "서연", "seoyeon_k5", "K5", isEv = false)
    private val junho = Member("n4", "준호", "junho_drive", "아이오닉 6", isEv = true)
    private val host = Member("n5", "도윤", "doyun_club", "아이오닉 5", isEv = true, latitude = 37.71, longitude = 128.72)
    private val haneul = Member("t1", "하늘", "haneul", "아이오닉 5", true)
    private val seojin = Member("t2", "서진", "seojin", "아이오닉 5", true)
    private val yuna = Member("t3", "유나", "yuna", "아이오닉 5", true)
    private val minji = Member("t4", "민지", "minji_ev3", "EV3", true)
    private val taeyang = Member("t5", "태양", "taeyang_5", "아이오닉 5", true)
    private val sua = Member("t6", "수아", "sua_drive", "아이오닉 5", true)
    private val jiho = Member("t7", "지호", "jiho_ev", "아이오닉 5", true)
    private val eunwoo = Member("t8", "은우", "eunwoo", "아이오닉 5", true)
    private val chaewon = Member("t9", "채원", "chaewon_6", "아이오닉 6", true)

    override val me: Flow<Member> = meState
    override val neighbors: Flow<List<Member>> = MutableStateFlow(listOf(jieun, hyunwoo, seoyeon, junho, host))

    private val ridingState = MutableStateFlow(
        listOf(
            Riding(
                id = "r1",
                name = "도윤의 라이딩",
                host = host,
                destinationName = "강릉 경포해변",
                joined = listOf(host, jieun, hyunwoo),
                away = listOf(seoyeon),
                membership = RideMembership.INVITED,
                invitedAt = clock.instant().minus(Duration.ofMinutes(5)),
                destinationLatitude = 37.80,
                destinationLongitude = 128.90,
            ),
        ),
    )
    override val ridings: Flow<List<Riding>> = combine(ridingState, meState) { list, me ->
        list.map { r -> r.copy(joined = r.joined.map { if (it.id == me.id) me else it }) }
    }

    override val ranking: Flow<Ranking> = ranking(RankScope.MODEL, RankPeriod.WEEK)

    override fun ranking(scope: RankScope, period: RankPeriod): Flow<Ranking> = meState.map { me ->
        val week = period == RankPeriod.WEEK
        when (scope) {
            RankScope.MODEL -> Ranking(
                scopeName = me.carModel,
                myRank = if (week) 214 else 238,
                myKmPerUnit = if (week) 6.0 else 5.8,
                previousRank = if (week) 252 else 231,
                leaders = listOf(
                    RankEntry(1, haneul, if (week) 7.4 else 7.2),
                    RankEntry(2, if (week) host else seojin, if (week) 7.3 else 7.1),
                    RankEntry(3, if (week) hyunwoo else host, if (week) 7.1 else 7.0),
                    RankEntry(4, if (week) seojin else yuna, if (week) 7.0 else 6.9),
                    RankEntry(5, if (week) yuna else hyunwoo, if (week) 6.9 else 6.8),
                    RankEntry(6, taeyang, if (week) 6.9 else 6.7),
                    RankEntry(7, if (week) sua else jiho, if (week) 6.8 else 6.7),
                    RankEntry(8, if (week) jiho else sua, if (week) 6.8 else 6.6),
                    RankEntry(9, eunwoo, if (week) 6.7 else 6.5),
                    RankEntry(10, minji, if (week) 6.7 else 6.5),
                ).moved(if (week) listOf(0, 2, -1, 4, -3, 1, 0, -2, 6, 0) else listOf(1, 0, 2, -2, -4, 0, 3, -1, 0, 2)),
                hardAccelerations = if (week) 3 else 14,
                hardBrakings = if (week) 5 else 19,
                ecoModeShare = if (week) 0.62 else 0.57,
            )
            RankScope.REGION -> Ranking(
                scopeName = "서울",
                myRank = if (week) 41 else 47,
                myKmPerUnit = if (week) 6.0 else 5.8,
                previousRank = if (week) 48 else 44,
                leaders = listOf(
                    RankEntry(1, junho, if (week) 7.9 else 7.6),
                    RankEntry(2, minji, if (week) 7.6 else 7.5),
                    RankEntry(3, haneul, if (week) 7.4 else 7.2),
                    RankEntry(4, jieun, if (week) 7.2 else 7.1),
                    RankEntry(5, if (week) host else seojin, if (week) 7.1 else 7.0),
                    RankEntry(6, chaewon, if (week) 7.0 else 6.9),
                    RankEntry(7, if (week) seojin else host, if (week) 7.0 else 6.8),
                    RankEntry(8, taeyang, if (week) 6.9 else 6.8),
                    RankEntry(9, sua, if (week) 6.8 else 6.7),
                    RankEntry(10, yuna, if (week) 6.8 else 6.6),
                ).moved(if (week) listOf(3, 0, -1, 5, -2, 0, 2, -3, 1, 0) else listOf(0, 4, -2, 1, -3, 2, 0, 0, -1, 3)),
                hardAccelerations = if (week) 3 else 14,
                hardBrakings = if (week) 5 else 19,
                ecoModeShare = if (week) 0.62 else 0.57,
            )
            // Neighbors driving an EV, so the numbers compare; K5 is on km/L.
            RankScope.NEIGHBORS -> Ranking(
                scopeName = "이웃",
                myRank = if (week) 4 else 3,
                myKmPerUnit = if (week) 6.0 else 5.8,
                previousRank = if (week) 5 else 3,
                leaders = (if (week) listOf(junho to 7.9, host to 7.3, hyunwoo to 7.1, me to 6.0, jieun to 5.7)
                else listOf(junho to 7.6, hyunwoo to 6.9, me to 5.8, host to 5.6, jieun to 5.5))
                    .mapIndexed { i, (m, v) -> RankEntry(i + 1, m, v) }
                    .moved(if (week) listOf(0, 1, -2, 1, 0) else listOf(0, 0, 1, -1, 0)),
                hardAccelerations = if (week) 3 else 14,
                hardBrakings = if (week) 5 else 19,
                ecoModeShare = if (week) 0.62 else 0.57,
            )
        }
    }

    private val routeActive = MutableStateFlow(false)
    private val reports = MutableStateFlow(
        listOf(
            RoadReport("p1", ReportType.ACCIDENT, clock.instant().minus(Duration.ofMinutes(3)), 37.79, 128.91, aheadKm = 2.4),
            RoadReport("p2", ReportType.SPEED_CAMERA, clock.instant().minus(Duration.ofDays(1)), 37.80, 128.90, aheadKm = 7.8),
            // Around the car (Pyeongchang), off the route: what the map shows before a route starts.
            RoadReport("p3", ReportType.CONSTRUCTION, clock.instant().minus(Duration.ofHours(2)), 37.64, 128.36),
            RoadReport("p4", ReportType.CONGESTION, clock.instant().minus(Duration.ofMinutes(12)), 37.60, 128.30),
            RoadReport("p5", ReportType.DISASTER, clock.instant().minus(Duration.ofHours(5)), 37.66, 128.29),
        ),
    )
    override val reportsOnRoute: Flow<List<RoadReport>> = combine(routeActive, reports) { active, list ->
        if (active) list.filter { it.aheadKm != null }.sortedBy { it.aheadKm } else emptyList()
    }
    override val reportsNearby: Flow<List<RoadReport>> = reports.map { list -> list.sortedByDescending { it.reportedAt } }

    override suspend fun setNickname(nickname: String) = meState.update { it.copy(nickname = nickname) }

    override suspend fun createRiding(name: String, destinationName: String?, inviteeIds: List<String>): String {
        val id = "r${ridingState.value.size + 1}"
        val me = meState.value
        val invited = listOf(jieun, hyunwoo, seoyeon, junho, host).filter { it.id in inviteeIds }
        ridingState.update { list ->
            list.map { if (it.membership == RideMembership.IN) it.left(me) else it } +
                Riding(id, name, me, destinationName, joined = listOf(me), away = invited, membership = RideMembership.IN)
        }
        return id
    }

    override suspend fun join(ridingId: String) {
        val me = meState.value
        ridingState.update { list ->
            list.map {
                when {
                    it.id == ridingId -> it.copy(membership = RideMembership.IN, joined = it.joined.filterNot { m -> m.id == me.id } + me, away = it.away.filterNot { m -> m.id == me.id })
                    it.membership == RideMembership.IN -> it.left(me)
                    else -> it
                }
            }
        }
    }

    override suspend fun leave(ridingId: String) {
        val me = meState.value
        ridingState.update { list ->
            list.mapNotNull {
                if (it.id != ridingId) return@mapNotNull it
                val left = it.left(me)
                // The last one out ends the ride.
                if (left.joined.isEmpty()) null else left
            }
        }
    }

    override suspend fun decline(ridingId: String) = ridingState.update { list -> list.filterNot { it.id == ridingId } }

    override suspend fun report(type: ReportType) {
        val me = meState.value
        reports.update { it + RoadReport("p${it.size + 1}", type, clock.instant(), me.latitude ?: 0.0, me.longitude ?: 0.0) }
    }

    override fun setRouteActive(active: Boolean) {
        routeActive.value = active
    }

    private fun Riding.left(me: Member) = copy(
        membership = RideMembership.LEFT,
        joined = joined.filterNot { it.id == me.id },
        away = away.filterNot { it.id == me.id } + me,
    )
}

private fun List<RankEntry>.moved(changes: List<Int>) = mapIndexed { i, e -> e.copy(change = changes[i]) }
