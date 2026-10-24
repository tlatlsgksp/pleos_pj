package com.example.pleos.platform.demo

import com.example.pleos.domain.model.CarProperties
import com.example.pleos.domain.model.DrivingMode
import com.example.pleos.domain.model.VehicleSnapshot
import com.example.pleos.domain.model.VehicleWarning
import com.example.pleos.platform.fake.FakeCarPropertySource
import com.example.pleos.platform.fake.FakeVehicleGateway

/** One step of a demo: wait [delayMillis] after the previous step, then mutate the fakes. */
data class DemoStep(
    val label: String,
    val delayMillis: Long,
    val apply: (vehicle: FakeVehicleGateway, car: FakeCarPropertySource) -> Unit,
)

/** An ordered, timed script that drives the Fake gateways so the core flows can be shown on demand. */
data class DemoScenario(
    val name: String,
    val initialVehicle: VehicleSnapshot,
    val initialCar: CarProperties,
    val steps: List<DemoStep>,
) {
    companion object {
        /** 주유 → 주행 → 엔진오일 경고 → 정비소 도착(시동 off) → 재시동(경고 해제). */
        fun default(): DemoScenario = DemoScenario(
            name = "core-flow",
            initialVehicle = VehicleSnapshot(
                fuelPercent = 18,
                instantFuelEconomyKmPerL = 11.2,
                currentFuelEconomyKmPerL = 11.8,
                tripSinceRefillKm = 412.0,
                cumulativeDistanceKm = 23_410.0,
                drivingMode = DrivingMode.NORMAL,
                warnings = emptySet(),
                airbagDeployed = false,
            ),
            initialCar = CarProperties(
                speedKmh = 0.0,
                ignitionOn = true,
                rangeRemainingKm = null,
                fuelCapacityL = 60.0,
            ),
            steps = listOf(
                DemoStep("주유: 연료 95%, 주유 후 거리 0km", delayMillis = 3_000) { vehicle, _ ->
                    vehicle.refuel(toPercent = 95)
                },
                DemoStep("주행: 24km, 에코 모드", delayMillis = 4_000) { vehicle, car ->
                    vehicle.setDrivingMode(DrivingMode.ECO)
                    vehicle.drive(km = 24.0, fuelPercentUsed = 4)
                    vehicle.setFuelEconomy(instantKmPerL = 14.3, currentKmPerL = 12.1)
                    car.setSpeedKmh(62.0)
                },
                DemoStep("엔진오일 경고등 점등", delayMillis = 4_000) { vehicle, _ ->
                    vehicle.addWarning(VehicleWarning.ENGINE_OIL)
                },
                DemoStep("정비소 도착: 정차, 시동 off", delayMillis = 4_000) { _, car ->
                    car.setSpeedKmh(0.0)
                    car.setIgnitionOn(false)
                },
                DemoStep("재시동: 경고등 소등", delayMillis = 3_000) { vehicle, car ->
                    car.setIgnitionOn(true)
                    vehicle.clearWarning(VehicleWarning.ENGINE_OIL)
                },
            ),
        )
    }
}
