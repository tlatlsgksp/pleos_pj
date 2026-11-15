package com.example.pleos.domain.gateway

import com.example.pleos.domain.model.LocationFix
import kotlinx.coroutines.flow.Flow

/** Vehicle location. Emits null while no fix is available. */
interface LocationSource {
    val location: Flow<LocationFix?>

    suspend fun isSupported(): Boolean
}
