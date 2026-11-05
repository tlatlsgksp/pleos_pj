package com.example.pleos.platform.di

import android.content.Context
import com.example.pleos.domain.gateway.CarPropertySource
import com.example.pleos.domain.gateway.ChargerSource
import com.example.pleos.domain.gateway.DemoScenarioController
import com.example.pleos.domain.gateway.NavigationGateway
import com.example.pleos.domain.gateway.VehicleGateway
import com.example.pleos.domain.repository.SettingsRepository
import com.example.pleos.platform.demo.DemoScenarioPlayer
import com.example.pleos.platform.demo.BundledChargerSource
import com.example.pleos.platform.fake.FakeCarPropertySource
import com.example.pleos.platform.fake.FakeNavigationGateway
import com.example.pleos.platform.fake.FakeVehicleGateway
import com.example.pleos.platform.pleos.PleosVehicleGateway
import com.example.pleos.platform.pleos.PleosNavigationGateway
import com.example.pleos.platform.switching.SwitchingCarPropertySource
import com.example.pleos.platform.switching.SwitchingNavigationGateway
import com.example.pleos.platform.switching.SwitchingVehicleGateway
import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Optional
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Marks the real (Pleos SDK / AAOS) implementation of a gateway. Absent until one is bound. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class RealSource

/** Scope that outlives any screen; used by the demo scenario player and to share the vehicle signal. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class DemoScope

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PlatformBindings {

    // The AAOS car-property source is optional until it is written; the Fake fills in until then.
    @BindsOptionalOf
    @RealSource
    abstract fun realCarPropertySource(): CarPropertySource

    @Binds
    abstract fun demoScenarioController(player: DemoScenarioPlayer): DemoScenarioController
}

@Module
@InstallIn(SingletonComponent::class)
internal object PlatformModule {

    @Provides
    @Singleton
    fun fakeVehicleGateway(): FakeVehicleGateway = FakeVehicleGateway()

    @Provides
    @Singleton
    fun fakeCarPropertySource(): FakeCarPropertySource = FakeCarPropertySource()

    @Provides
    @Singleton
    fun fakeNavigationGateway(): FakeNavigationGateway = FakeNavigationGateway()

    /** NaviHelper where the Pleos navigation is installed; the demo navigation elsewhere. */
    @Provides
    @Singleton
    fun navigationGateway(
        @ApplicationContext context: Context,
        settings: SettingsRepository,
        fake: FakeNavigationGateway,
    ): NavigationGateway {
        val real = if (PleosNavigationGateway.isAvailable(context)) PleosNavigationGateway(context) else null
        return SwitchingNavigationGateway(settings.dataSourceMode, real, fake)
    }

    /** Demo list for both modes until the backend serves chargers. */
    @Provides
    @Singleton
    fun chargerSource(): ChargerSource = BundledChargerSource()

    /** The Pleos SDK gateway exists only where the Pleos vehicle service is installed; elsewhere the demo runs. */
    @Provides
    @Singleton
    fun vehicleGateway(
        @ApplicationContext context: Context,
        settings: SettingsRepository,
        fake: FakeVehicleGateway,
        @DemoScope scope: CoroutineScope,
    ): VehicleGateway {
        val real = if (PleosVehicleGateway.isAvailable(context)) PleosVehicleGateway(context, scope) else null
        return SwitchingVehicleGateway(settings.dataSourceMode, real, fake)
    }

    @Provides
    @Singleton
    fun carPropertySource(
        settings: SettingsRepository,
        @RealSource real: Optional<CarPropertySource>,
        fake: FakeCarPropertySource,
    ): CarPropertySource = SwitchingCarPropertySource(settings.dataSourceMode, real.orElse(null), fake)

    @Provides
    @Singleton
    @DemoScope
    fun demoScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
