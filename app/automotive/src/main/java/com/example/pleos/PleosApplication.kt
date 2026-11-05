package com.example.pleos

import android.app.Application
import com.example.pleos.di.ApplicationScope
import com.example.pleos.domain.drive.VehicleMonitor
import com.example.pleos.domain.route.ChargeStopAdvisor
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class PleosApplication : Application() {

    @Inject lateinit var vehicleMonitor: VehicleMonitor

    @Inject lateinit var chargeStopAdvisor: ChargeStopAdvisor

    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // Sessions and charge drafts are recorded while the process lives, whichever screen is open.
        // A foreground service that survives the UI is a later step (research notes 11.2).
        appScope.launch { vehicleMonitor.run() }
        appScope.launch { chargeStopAdvisor.run() }
    }
}
