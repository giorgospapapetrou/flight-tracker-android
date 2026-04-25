package com.giorgospapapetrou.flightfinder.domain.model

import java.time.Instant

data class FlightSummary(
    val id: Int,
    val aircraftIcao: String,
    val aircraftRegistration: String?,
    val aircraftType: String?,
    val callsign: String?,
    val startedAt: Instant,
    val endedAt: Instant?,
    val lastPositionAt: Instant?,
    val positionCount: Int,
    val positionCountWithCoords: Int,
    val maxAltitudeFt: Int?,
    val minAltitudeFt: Int?,
) {
    val displayName: String get() = callsign?.takeIf { it.isNotBlank() } ?: aircraftIcao

    /**
     * True only if the aircraft is currently transmitting (seen recently).
     * Backend may report ended_at=null for up to 10 minutes after silence,
     * so we use last_position_at to decide.
     */
    fun isLiveAt(now: Instant, freshSeconds: Long = 60L): Boolean {
        if (endedAt != null) return false
        val lastSeen = lastPositionAt ?: return false
        return now.epochSecond - lastSeen.epochSecond <= freshSeconds
    }

    /** When this flight ended (or was last heard from). */
    val effectiveEndAt: Instant? get() = endedAt ?: lastPositionAt
}