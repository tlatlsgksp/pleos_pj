package com.example.pleos.platform.switching

import com.example.pleos.domain.gateway.VehicleGateway
import com.example.pleos.domain.model.Capability
import com.example.pleos.domain.model.DataSourceMode
import com.example.pleos.domain.model.VehicleFeature
import com.example.pleos.domain.model.VehicleSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Routes to the real or the Fake gateway based on the requested [DataSourceMode].
 * Falls back to the Fake when no real implementation is bound, and reports that through [activeSource].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SwitchingVehicleGateway(
    requestedMode: Flow<DataSourceMode>,
    private val real: VehicleGateway?,
    private val fake: VehicleGateway,
) : VehicleGateway {

    val isRealAvailable: Boolean = real != null

    override val activeSource: Flow<DataSourceMode> = requestedMode
        .map { if (it == DataSourceMode.REAL && real != null) DataSourceMode.REAL else DataSourceMode.DEMO }
        .distinctUntilChanged()

    override val snapshot: Flow<VehicleSnapshot> = activeSource.flatMapLatest { current(it).snapshot }

    override suspend fun capabilities(): Map<VehicleFeature, Capability> = current(activeSource.first()).capabilities()

    override suspend fun vin(): String? = current(activeSource.first()).vin()

    private fun current(source: DataSourceMode): VehicleGateway =
        if (source == DataSourceMode.REAL) checkNotNull(real) else fake
}
