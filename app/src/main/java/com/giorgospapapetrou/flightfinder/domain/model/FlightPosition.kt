package com.giorgospapapetrou.flightfinder.domain.model

import java.time.Instant

data class FlightPosition(
    val timestamp: Instant,
    val lat: Double?,
    val lon: Double?,
    val altitudeFt: Int?,
    val groundSpeedKt: Int?,
    val headingDeg: Int?,
    val verticalRateFpm: Int?,
) {
    val hasPosition: Boolean get() = lat != null && lon != null
}

data class FlightDetail(
    val summary: FlightSummary,
    val positions: List<FlightPosition>,
)