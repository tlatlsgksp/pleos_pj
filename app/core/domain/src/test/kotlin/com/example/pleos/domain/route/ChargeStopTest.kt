package com.example.pleos.domain.route

import com.example.pleos.domain.model.CarProperties
import com.example.pleos.domain.model.ChargerCandidate
import com.example.pleos.domain.model.NavEvent
import com.example.pleos.domain.model.Place
import com.example.pleos.domain.model.VehicleSnapshot
import com.example.pleos.domain.model.WaypointSlot
import com.example.pleos.domain.testing.ListChargerSource
import com.example.pleos.domain.testing.MutableCarPropertySource
import com.example.pleos.domain.testing.MutableVehicleGateway
import com.example.pleos.domain.testing.RecordingNavigationGateway
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChargeStopTest {

    // Around Seoul Station.
    private val seoulStation = Place("서울역", 37.5547, 126.9707)
    private val gangnam = Place("강남역", 37.4979, 127.0276)
    private val yeouido = Place("여의도", 37.5219, 126.9245)
    private val behind = Place("북쪽", 37.6000, 126.9707)

    @Test
    fun `suggests only with an active route and too little range`() {
        val route = RouteState(active = true, routeId = 1, remainingKm = 160.0)

        assertTrue(ChargeStopRules.shouldSuggest(90.0, route))
        assertFalse(ChargeStopRules.shouldSuggest(200.0, route))
        assertFalse(ChargeStopRules.shouldSuggest(10.0, route.copy(active = false)))
        // A short route still asks for 50 km.
        assertTrue(ChargeStopRules.shouldSuggest(40.0, route.copy(remainingKm = 5.0)))
        assertFalse(ChargeStopRules.shouldSuggest(null, route))
    }

    @Test
    fun `route state follows the navigation events`() {
        var s = RouteState()
        s = s.reduce(NavEvent.RouteStarted("대전", 160.0, 7_000))
        assertTrue(s.active)
        assertEquals(1, s.routeId)
        s = s.reduce(NavEvent.TripInfo(150.0, 6_000))
        assertEquals(150.0, s.remainingKm!!, 0.0)
        s = s.reduce(NavEvent.DestinationArrived)
        assertFalse(s.active)
        // Trip info for a route set before the app ran opens a new one.
        s = s.reduce(NavEvent.TripInfo(40.0, 2_000))
        assertTrue(s.active)
        assertEquals(2, s.routeId)
    }

    @Test
    fun `stations ahead and close come before nearer ones behind`() {
        val candidates = listOf(behind, gangnam, yeouido).map {
            ChargerCandidate(it, ChargeStopRules.distanceKm(37.5547, 126.9707, it.latitude, it.longitude), true)
        }

        val ranked = ChargeStopRules.rank(37.5547, 126.9707, headingDeg = 120.0, candidates)

        // Heading east-south-east. Yeouido (5.5 km, south-west) and the station 5 km north are
        // nearer but off the heading; Gangnam is 8 km ahead, inside the search radius.
        assertEquals("강남역", ranked.first().place.name)
        assertEquals(setOf("북쪽", "여의도"), ranked.drop(1).map { it.place.name }.toSet())
    }

    @Test
    fun `without a heading the nearest station wins`() {
        val candidates = listOf(gangnam, behind).map {
            ChargerCandidate(it, ChargeStopRules.distanceKm(37.5547, 126.9707, it.latitude, it.longitude), true)
        }

        assertEquals("북쪽", ChargeStopRules.rank(37.5547, 126.9707, null, candidates).first().place.name)
    }

    @Test
    fun `low battery on a long route offers a charger once, and accepting adds the first waypoint`() =
        runTest(UnconfinedTestDispatcher()) {
            val vehicle = MutableVehicleGateway(VehicleSnapshot(rangeRemainingKm = 90.0))
            val nav = RecordingNavigationGateway()
            val advisor = ChargeStopAdvisor(vehicle, MutableCarPropertySource(CarProperties()), nav, ListChargerSource(listOf(gangnam, behind)))
            backgroundScope.launch { advisor.run() }

            assertNull(advisor.suggestion.value)
            nav.emit(NavEvent.RouteStarted("대전", 160.0, null))

            val suggestion = advisor.suggestion.value!!
            assertEquals("강남역", suggestion.candidate.place.name)
            assertEquals(90.0, suggestion.rangeKm, 0.0)
            assertEquals("대전", suggestion.destinationName)

            assertTrue(advisor.accept())
            assertEquals(listOf(gangnam to WaypointSlot.FIRST), nav.waypoints)
            assertNull(advisor.suggestion.value)

            // Same route: no second offer even as the range keeps dropping.
            vehicle.update { it.copy(rangeRemainingKm = 60.0) }
            nav.emit(NavEvent.TripInfo(120.0, 5_000))
            assertNull(advisor.suggestion.value)
        }

    @Test
    fun `dismissing hides the offer until the next route, and enough range never offers`() =
        runTest(UnconfinedTestDispatcher()) {
            val vehicle = MutableVehicleGateway(VehicleSnapshot(rangeRemainingKm = 90.0))
            val nav = RecordingNavigationGateway()
            val advisor = ChargeStopAdvisor(vehicle, MutableCarPropertySource(CarProperties()), nav, ListChargerSource(listOf(gangnam)))
            backgroundScope.launch { advisor.run() }

            nav.emit(NavEvent.RouteStarted("대전", 160.0, null))
            advisor.dismiss()
            assertNull(advisor.suggestion.value)

            nav.emit(NavEvent.RouteCancelled)
            nav.emit(NavEvent.RouteStarted("부산", 400.0, null))
            assertEquals(2, advisor.suggestion.value!!.routeId)

            advisor.dismiss()
            vehicle.update { it.copy(rangeRemainingKm = 500.0) }
            nav.emit(NavEvent.RouteStarted("수원", 40.0, null))
            assertNull(advisor.suggestion.value)
            assertTrue(nav.waypoints.isEmpty())
        }
}
