package com.example.pleos.platform.switching

import com.example.pleos.domain.gateway.CarPropertySource
import com.example.pleos.domain.model.CarProperties
import com.example.pleos.domain.model.DataSourceMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/** Same switching rule as [SwitchingVehicleGateway], for AAOS car properties. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SwitchingCarPropertySource(
    requestedMode: Flow<DataSourceMode>,
    private val real: CarPropertySource?,
    private val fake: CarPropertySource,
) : CarPropertySource {

    val isRealAvailable: Boolean = real != null

    override val properties: Flow<CarProperties> = requestedMode
        .map { it == DataSourceMode.REAL && real != null }
        .distinctUntilChanged()
        .flatMapLatest { useReal -> if (useReal) checkNotNull(real).properties else fake.properties }
}
