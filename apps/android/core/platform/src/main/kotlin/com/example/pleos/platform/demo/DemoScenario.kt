package com.example.pleos.platform.demo

import com.example.pleos.domain.model.CarProperties
import com.example.pleos.domain.model.DrivingMode
import com.example.pleos.domain.model.Gear
import com.example.pleos.domain.model.VehicleSnapshot
import com.example.pleos.domain.model.VehicleWarning
import com.example.pleos.platform.fake.FakeCarPropertySource
import com.example.pleos.platform.fake.FakeNavigationGateway
import com.example.pleos.platform.fake.FakeVehicleGateway

/** One step of a demo: wait [delayMillis] after the previous step, then mutate the fakes. */
data class DemoStep(
    val label: String,
    val delayMillis: Long,
    val apply: (vehicle: FakeVehicleGateway, car: FakeCarPropertySource, nav: FakeNavigationGateway) -> Unit,
)

/** An ordered, timed script that drives the Fake gateways so the core flows can be shown on demand. */
data class DemoScenario(
    val name: String,
    val initialVehicle: VehicleSnapshot,
    val initialCar: CarProperties,
    val steps: List<DemoStep>,
) {
    companion object {
        private const val EV_CAPACITY_KWH = 77.4

        /** [기본값] km per kWh used to keep the demo range plausible. */
        private const val EV_KM_PER_KWH = 5.0

        fun default(): DemoScenario = evCoreFlow()

        /**
         * The EV flows the video shows, on the same odometer the emulator console starts from:
         * a long route on a low battery (a charger is offered as a waypoint), drive, park, charge
         * (a draft appears), drive again, then a time skip that pushes the care items towards
         * their interval.
         */
        fun evCoreFlow(): DemoScenario = DemoScenario(
            name = "ev-core-flow",
            initialVehicle = VehicleSnapshot(
                cumulativeDistanceKm = 12_345.0,
                drivingMode = DrivingMode.NORMAL,
                warnings = emptySet(),
                airbagDeployed = false,
                gear = Gear.PARK,
                brakePedalPressed = false,
                hvacOn = false,
                rangeRemainingKm = 18.0 * EV_KM_PER_KWH,
                batteryEnergyKwh = 18.0,
                batteryCapacityKwh = EV_CAPACITY_KWH,
                batteryPercent = 18.0 / EV_CAPACITY_KWH * 100,
                chargePortOpen = false,
            ),
            initialCar = CarProperties(speedKmh = 0.0, ignitionOn = true),
            steps = listOf(
                DemoStep("목적지 설정: 대전 160km, 주행 가능 90km", delayMillis = 3_000) { _, _, nav ->
                    nav.startRoute("대전", 160.0)
                },
                DemoStep("출발: 기어 D", delayMillis = 3_000) { vehicle, car, _ ->
                    vehicle.setGear(Gear.DRIVE)
                    vehicle.setHvacOn(true)
                    car.setSpeedKmh(40.0)
                },
                DemoStep("주행: 25km", delayMillis = 4_000) { vehicle, car, _ ->
                    vehicle.driveEv(km = 25.0, kwhUsed = 4.5)
                    vehicle.syncRange()
                    car.setSpeedKmh(62.0)
                },
                DemoStep("충전소 도착: 기어 P", delayMillis = 4_000) { vehicle, car, _ ->
                    car.setSpeedKmh(0.0)
                    vehicle.setGear(Gear.PARK)
                    vehicle.setHvacOn(false)
                },
                DemoStep("충전구 열림", delayMillis = 3_000) { vehicle, _, _ ->
                    vehicle.setChargePortOpen(true)
                },
                DemoStep("충전 중: 배터리 +20kWh", delayMillis = 3_000) { vehicle, _, _ ->
                    vehicle.setBatteryEnergyKwh((vehicle.current.batteryEnergyKwh ?: 0.0) + 20.0)
                    vehicle.syncRange()
                },
                DemoStep("충전 중: 배터리 +20kWh", delayMillis = 3_000) { vehicle, _, _ ->
                    vehicle.setBatteryEnergyKwh((vehicle.current.batteryEnergyKwh ?: 0.0) + 20.0)
                    vehicle.syncRange()
                },
                DemoStep("충전구 닫힘: 충전 기록 초안", delayMillis = 3_000) { vehicle, _, _ ->
                    vehicle.setChargePortOpen(false)
                },
                DemoStep("다시 출발: 기어 D", delayMillis = 4_000) { vehicle, car, _ ->
                    vehicle.setGear(Gear.DRIVE)
                    car.setSpeedKmh(50.0)
                },
                DemoStep("몇 달 뒤: 주행거리 +12,000km", delayMillis = 4_000) { vehicle, _, _ ->
                    vehicle.driveEv(km = 12_000.0, kwhUsed = 0.0)
                },
            ),
        )

        private fun FakeVehicleGateway.syncRange() =
            setRangeRemainingKm((current.batteryEnergyKwh ?: 0.0) * EV_KM_PER_KWH)

        /** 주유 → 주행 → 엔진오일 경고 → 정비소 도착(시동 off) → 재시동(경고 해제). */
        fun fuelCoreFlow(): DemoScenario = DemoScenario(
            name = "fuel-core-flow",
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
                DemoStep("주유: 연료 95%, 주유 후 거리 0km", delayMillis = 3_000) { vehicle, _, _ ->
                    vehicle.refuel(toPercent = 95)
                },
                DemoStep("주행: 24km, 에코 모드", delayMillis = 4_000) { vehicle, car, _ ->
                    vehicle.setDrivingMode(DrivingMode.ECO)
                    vehicle.drive(km = 24.0, fuelPercentUsed = 4)
                    vehicle.setFuelEconomy(instantKmPerL = 14.3, currentKmPerL = 12.1)
                    car.setSpeedKmh(62.0)
                },
                DemoStep("엔진오일 경고등 점등", delayMillis = 4_000) { vehicle, _, _ ->
                    vehicle.addWarning(VehicleWarning.ENGINE_OIL)
                },
                DemoStep("정비소 도착: 정차, 시동 off", delayMillis = 4_000) { _, car, _ ->
                    car.setSpeedKmh(0.0)
                    car.setIgnitionOn(false)
                },
                DemoStep("재시동: 경고등 소등", delayMillis = 3_000) { vehicle, car, _ ->
                    car.setIgnitionOn(true)
                    vehicle.clearWarning(VehicleWarning.ENGINE_OIL)
                },
            ),
        )
    }
}
