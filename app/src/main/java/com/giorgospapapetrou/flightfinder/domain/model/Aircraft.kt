package com.giorgospapapetrou.flightfinder.domain.model

import java.time.Instant

data class Aircraft(
    val icao: String,
    val callsign: String?,
    val registration: String?,
    val aircraftType: String?,
    val lat: Double?,
    val lon: Double?,
    val altitudeFt: Int?,
    val groundSpeedKt: Int?,
    val headingDeg: Int?,
    val verticalRateFpm: Int?,
    val onGround: Boolean,
    val lastPositionAt: Instant?,
) {
    /** True when this aircraft has a usable position to draw on the map. */
    val hasPosition: Boolean get() = lat != null && lon != null
}