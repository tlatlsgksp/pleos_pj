package com.example.pleos.domain.model

/** One speech-recognition result. Partial results arrive with isFinal = false. */
data class Transcript(
    val text: String,
    val isFinal: Boolean,
)

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val headingDeg: Double? = null,
    val timestampMillis: Long,
)
