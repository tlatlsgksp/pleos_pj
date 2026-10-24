package com.example.pleos.domain.gateway

import com.example.pleos.domain.model.Capability
import com.example.pleos.domain.model.DataSourceMode
import com.example.pleos.domain.model.VehicleFeature
import com.example.pleos.domain.model.VehicleSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Vehicle state from the Pleos Vehicle SDK. Implementations live in core/platform and
 * must not leak SDK types; capability checks and listener-vs-polling choices stay inside.
 */
interface VehicleGateway {
    /** Starts with the latest known snapshot and emits on every change. */
    val snapshot: Flow<VehicleSnapshot>

    /** Which source currently feeds [snapshot]. DEMO also when no real source is bound. */
    val activeSource: Flow<DataSourceMode>

    suspend fun capabilities(): Map<VehicleFeature, Capability>

    /** Vehicle identification number, or null when unsupported. Never send it raw to the server. */
    suspend fun vin(): String?
}
