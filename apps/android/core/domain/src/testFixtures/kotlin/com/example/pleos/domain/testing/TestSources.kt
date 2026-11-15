package com.example.pleos.domain.testing

import com.example.pleos.domain.gateway.CarPropertySource
import com.example.pleos.domain.gateway.IdGenerator
import com.example.pleos.domain.gateway.VehicleGateway
import com.example.pleos.domain.model.Capability
import com.example.pleos.domain.model.CarProperties
import com.example.pleos.domain.model.DataSourceMode
import com.example.pleos.domain.model.VehicleFeature
import com.example.pleos.domain.model.VehicleSnapshot
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** A vehicle gateway the test drives by setting [state]. */
class MutableVehicleGateway(
    initial: VehicleSnapshot = VehicleSnapshot(),
    mode: DataSourceMode = DataSourceMode.DEMO,
    var vin: String? = "KMHTEST0000000001",
) : VehicleGateway {
    val state = MutableStateFlow(initial)
    val source = MutableStateFlow(mode)
    override val snapshot: Flow<VehicleSnapshot> = state
    override val activeSource: Flow<DataSourceMode> = source
    override suspend fun capabilities(): Map<VehicleFeature, Capability> = emptyMap()
    override suspend fun vin(): String? = vin

    fun update(transform: (VehicleSnapshot) -> VehicleSnapshot) {
        state.value = transform(state.value)
    }
}

class MutableCarPropertySource(initial: CarProperties = CarProperties(speedKmh = 0.0)) : CarPropertySource {
    val state = MutableStateFlow(initial)
    override val properties: Flow<CarProperties> = state
}

/** Ids "id-1", "id-2", ... */
class SequentialIds : IdGenerator {
    private var next = 1
    override fun newId(): String = "id-${next++}"
}

/** A clock the test moves by hand. */
class MutableClock(
    var now: Instant = Instant.parse("2026-11-05T00:00:00Z"),
    private val zoneId: ZoneId = ZoneId.of("Asia/Seoul"),
) : Clock() {
    override fun getZone(): ZoneId = zoneId
    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)
    override fun instant(): Instant = now

    fun advance(duration: Duration) {
        now = now.plus(duration)
    }
}
