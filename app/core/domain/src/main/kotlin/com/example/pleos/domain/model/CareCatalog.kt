package com.example.pleos.domain.model

import com.example.pleos.domain.model.EnergyType.EV
import com.example.pleos.domain.model.EnergyType.FUEL

/**
 * Built-in maintenance table. Intervals are the manufacturer manual's normal-condition values
 * collected in PLANNING_FOUNDATION 2.3. EV-only rows are defaults still to be checked against an
 * EV manual.
 */
object CareCatalog {
    private const val MANUAL = "제조사 정비 매뉴얼 일반 조건 (PLANNING_FOUNDATION 2.3)"
    private const val EV_DEFAULT = "[기본값] 전기차 매뉴얼 확인 필요"

    val items: List<CareItemDef> = listOf(
        CareItemDef("engine_oil", "엔진오일·오일필터", setOf(FUEL), 10_000, 12, MANUAL),
        CareItemDef("air_filter", "에어 필터", setOf(FUEL), 20_000, 12, MANUAL),
        CareItemDef("transmission_oil", "미션오일", setOf(FUEL), 40_000, 24, MANUAL),
        CareItemDef("coolant", "냉각수", setOf(FUEL), 80_000, 24, MANUAL),
        CareItemDef("spark_plug", "점화플러그", setOf(FUEL), 60_000, 36, MANUAL),
        CareItemDef("reducer_oil", "감속기 오일", setOf(EV), 60_000, null, EV_DEFAULT),
        CareItemDef("battery_coolant", "배터리 냉각수", setOf(EV), 60_000, 36, EV_DEFAULT),
        CareItemDef("cabin_filter", "에어컨 필터", setOf(FUEL, EV), 15_000, 12, MANUAL),
        CareItemDef("brake_fluid", "브레이크 오일", setOf(FUEL, EV), 30_000, 24, MANUAL),
        CareItemDef("brake_pad_front", "브레이크 패드(앞)", setOf(FUEL, EV), 30_000, null, MANUAL),
        CareItemDef("brake_pad_rear", "브레이크 패드(뒤)", setOf(FUEL, EV), 60_000, null, MANUAL),
        CareItemDef("tire_rotation", "타이어 위치교환", setOf(FUEL, EV), 10_000, 6, MANUAL),
        CareItemDef("tire", "타이어", setOf(FUEL, EV), 50_000, 60, MANUAL),
        CareItemDef("aux_battery", "보조 배터리(12V)", setOf(FUEL, EV), null, 36, MANUAL),
        CareItemDef("wiper", "와이퍼", setOf(FUEL, EV), null, 12, MANUAL),
    )

    private val byId: Map<String, CareItemDef> = items.associateBy { it.id }

    fun get(id: String): CareItemDef? = byId[id]

    fun forEnergyType(energyType: EnergyType): List<CareItemDef> = items.filter { energyType in it.energyTypes }
}
