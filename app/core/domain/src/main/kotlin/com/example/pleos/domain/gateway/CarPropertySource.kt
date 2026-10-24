package com.example.pleos.domain.gateway

import com.example.pleos.domain.model.CarProperties
import kotlinx.coroutines.flow.Flow

/** AAOS CarPropertyManager values (speed, ignition, range, tank capacity). Separate from the Pleos SDK. */
interface CarPropertySource {
    val properties: Flow<CarProperties>
}
