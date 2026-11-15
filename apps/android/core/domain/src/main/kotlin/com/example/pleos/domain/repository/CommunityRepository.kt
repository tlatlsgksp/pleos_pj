package com.example.pleos.domain.repository

import com.example.pleos.domain.model.Member
import com.example.pleos.domain.model.RankPeriod
import com.example.pleos.domain.model.RankScope
import com.example.pleos.domain.model.Ranking
import com.example.pleos.domain.model.ReportType
import com.example.pleos.domain.model.Riding
import com.example.pleos.domain.model.RoadReport
import kotlinx.coroutines.flow.Flow

/**
 * The community the car takes part in: me, neighbors, group rides, the ranking and road reports.
 * The server is not built yet, so the app binds a demo implementation (labelled as such on screen).
 */
interface CommunityRepository {
    val me: Flow<Member>
    val neighbors: Flow<List<Member>>
    val ridings: Flow<List<Riding>>
    /** Same model, this week: the ranking home shows. */
    val ranking: Flow<Ranking>
    fun ranking(scope: RankScope, period: RankPeriod): Flow<Ranking>
    /** Reports lying on the current route, nearest first. Empty without a route. */
    val reportsOnRoute: Flow<List<RoadReport>>
    /** Reports around the car, newest first, route or not; the map shows these. */
    val reportsNearby: Flow<List<RoadReport>>

    suspend fun setNickname(nickname: String)
    suspend fun createRiding(name: String, destinationName: String?, inviteeIds: List<String>): String
    suspend fun join(ridingId: String)
    suspend fun leave(ridingId: String)
    suspend fun decline(ridingId: String)
    suspend fun report(type: ReportType)
    /** The navigation started or ended a route; reports are matched against it. */
    fun setRouteActive(active: Boolean)
}
