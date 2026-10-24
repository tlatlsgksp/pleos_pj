package com.example.pleos.platform.di

import com.example.pleos.domain.gateway.CarPropertySource
import com.example.pleos.domain.gateway.DemoScenarioController
import com.example.pleos.domain.gateway.VehicleGateway
import com.example.pleos.domain.repository.SettingsRepository
import com.example.pleos.platform.demo.DemoScenarioPlayer
import com.example.pleos.platform.fake.FakeCarPropertySource
import com.example.pleos.platform.fake.FakeVehicleGateway
import com.example.pleos.platform.switching.SwitchingCarPropertySource
import com.example.pleos.platform.switching.SwitchingVehicleGateway
import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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

/** Scope that outlives any screen; used by the demo scenario player. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class DemoScope

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PlatformBindings {

    // Real implementations are optional so the app runs on the Fake sources until the SDK wrappers exist.
    @BindsOptionalOf
    @RealSource
    abstract fun realVehicleGateway(): VehicleGateway

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
    fun vehicleGateway(
        settings: SettingsRepository,
        @RealSource real: Optional<VehicleGateway>,
        fake: FakeVehicleGateway,
    ): VehicleGateway = SwitchingVehicleGateway(settings.dataSourceMode, real.orElse(null), fake)

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
