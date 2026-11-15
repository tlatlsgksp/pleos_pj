package com.example.pleos.domain.eco

/** How the current driving affects efficiency. Ordered from good to bad. */
enum class EcoLevel { GOOD, CAUTION, BAD }

/** Why the level is not GOOD. */
enum class EcoReason { HARD_ACCELERATION, HARD_BRAKING, HIGH_SPEED }

data class EcoVerdict(val level: EcoLevel, val reason: EcoReason? = null) {
    companion object {
        val GOOD = EcoVerdict(EcoLevel.GOOD)
    }
}

/**
 * Judges one speed change. Uses only vehicle speed, which every car reports, so it works the same
 * on fuel and EV cars and does not need the (often unsupported) instant fuel economy signal.
 */
object EcoDriveRules {

    /** [기본값] km/h gained per second. 6 is a brisk start, 10 (about 0.28 g) is a hard one. */
    const val CAUTION_ACCEL_KMH_PER_S = 6.0
    const val BAD_ACCEL_KMH_PER_S = 10.0

    /** [기본값] km/h lost per second. Hard braking throws away the energy just spent. */
    const val CAUTION_BRAKE_KMH_PER_S = 8.0
    const val BAD_BRAKE_KMH_PER_S = 12.0

    /** [기본값] Air drag grows with the square of speed; past these, consumption climbs fast. */
    const val CAUTION_SPEED_KMH = 100.0
    const val BAD_SPEED_KMH = 120.0

    /** Below this gap the speed change is sensor noise, not driving. */
    private const val MIN_INTERVAL_S = 0.2

    fun judge(previousKmh: Double?, currentKmh: Double, intervalSeconds: Double?): EcoVerdict {
        val change = if (previousKmh != null && intervalSeconds != null && intervalSeconds >= MIN_INTERVAL_S) {
            (currentKmh - previousKmh) / intervalSeconds
        } else {
            0.0
        }
        val verdicts = listOf(
            when {
                change >= BAD_ACCEL_KMH_PER_S -> EcoVerdict(EcoLevel.BAD, EcoReason.HARD_ACCELERATION)
                change >= CAUTION_ACCEL_KMH_PER_S -> EcoVerdict(EcoLevel.CAUTION, EcoReason.HARD_ACCELERATION)
                -change >= BAD_BRAKE_KMH_PER_S -> EcoVerdict(EcoLevel.BAD, EcoReason.HARD_BRAKING)
                -change >= CAUTION_BRAKE_KMH_PER_S -> EcoVerdict(EcoLevel.CAUTION, EcoReason.HARD_BRAKING)
                else -> EcoVerdict.GOOD
            },
            when {
                currentKmh >= BAD_SPEED_KMH -> EcoVerdict(EcoLevel.BAD, EcoReason.HIGH_SPEED)
                currentKmh >= CAUTION_SPEED_KMH -> EcoVerdict(EcoLevel.CAUTION, EcoReason.HIGH_SPEED)
                else -> EcoVerdict.GOOD
            },
        )
        return verdicts.maxBy { it.level }
    }
}
