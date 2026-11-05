package com.example.pleos.ui.testing

import com.example.pleos.domain.gateway.DemoScenarioController
import com.example.pleos.domain.model.EnergyType
import com.example.pleos.domain.model.Vehicle
import com.example.pleos.domain.model.VehicleSnapshot
import com.example.pleos.domain.testing.InMemoryCareRepository
import com.example.pleos.domain.testing.InMemoryDriveSessionRepository
import com.example.pleos.domain.testing.InMemoryRecordRepository
import com.example.pleos.domain.testing.InMemorySettingsRepository
import com.example.pleos.domain.testing.InMemoryVehicleRepository
import com.example.pleos.domain.testing.MutableCarPropertySource
import com.example.pleos.domain.testing.MutableClock
import com.example.pleos.domain.testing.MutableVehicleGateway
import com.example.pleos.domain.testing.SequentialIds
import com.example.pleos.domain.usecase.ConfirmDraftUseCase
import com.example.pleos.domain.usecase.DismissDraftUseCase
import com.example.pleos.domain.usecase.ObserveCareItemsUseCase
import com.example.pleos.domain.usecase.ObserveDashboardUseCase
import com.example.pleos.domain.usecase.ObservePendingDraftUseCase
import com.example.pleos.domain.usecase.RecordMaintenanceUseCase
import com.example.pleos.domain.usecase.StampVehicleUseCase
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** The in-memory app the ViewModel tests run against: one EV at 12,345 km in demo mode. */
class TestApp {
    val clock = MutableClock(Instant.parse("2026-11-05T03:00:00Z"))
    val vehicle = Vehicle("v1", EnergyType.EV, "vin", null, 77.4, 12_000.0, clock.now)
    val gateway = MutableVehicleGateway(
        VehicleSnapshot(cumulativeDistanceKm = 12_345.0, batteryPercent = 64.4, rangeRemainingKm = 310.0),
    )
    val car = MutableCarPropertySource()
    val vehicles = InMemoryVehicleRepository(vehicle)
    val records = InMemoryRecordRepository()
    val care = InMemoryCareRepository()
    val sessions = InMemoryDriveSessionRepository()
    val ids = SequentialIds()
    val demo = FakeDemoController()
    val settings = InMemorySettingsRepository()

    val observeDashboard get() = ObserveDashboardUseCase(gateway, car)
    val observeCare get() = ObserveCareItemsUseCase(vehicles, care, gateway, clock)
    val observePendingDraft get() = ObservePendingDraftUseCase(vehicles, records)
    val recordMaintenance get() = RecordMaintenanceUseCase(vehicles, records, care, StampVehicleUseCase(gateway, sessions, clock), ids, clock)
    val confirmDraft get() = ConfirmDraftUseCase(records)
    val dismissDraft get() = DismissDraftUseCase(records)
}

class FakeDemoController : DemoScenarioController {
    val playing = MutableStateFlow(false)
    var playCalls = 0
    override val isPlaying: Flow<Boolean> = playing
    override fun play() { playCalls++; playing.value = true }
    override fun stop() { playing.value = false }
}
