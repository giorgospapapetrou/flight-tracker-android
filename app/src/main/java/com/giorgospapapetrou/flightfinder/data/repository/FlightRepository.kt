package com.giorgospapapetrou.flightfinder.data.repository

import com.giorgospapapetrou.flightfinder.data.api.FlightApi
import com.giorgospapapetrou.flightfinder.data.api.dto.FlightPositionDto
import com.giorgospapapetrou.flightfinder.data.api.dto.FlightSummaryDto
import com.giorgospapapetrou.flightfinder.domain.model.FlightDetail
import com.giorgospapapetrou.flightfinder.domain.model.FlightPosition
import com.giorgospapapetrou.flightfinder.domain.model.FlightSummary
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlightRepository @Inject constructor(
    private val api: FlightApi,
) {
    suspend fun fetchFlights(): List<FlightSummary> {
        return api.getFlights().flights.map { it.toDomain() }
    }

    suspend fun fetchFlightDetail(flightId: Int): FlightDetail {
        // Get the summary from the list (cheap; could be cached later).
        val summary = fetchFlights().firstOrNull { it.id == flightId }
            ?: throw IllegalArgumentException("Flight $flightId not found")

        val response = api.getFlightPositions(flightId)
        val positions = response.positions.map { it.toDomain() }
        return FlightDetail(summary = summary, positions = positions)
    }

    private fun FlightSummaryDto.toDomain(): FlightSummary = FlightSummary(
        id = id,
        aircraftIcao = aircraftIcao,
        aircraftRegistration = aircraftRegistration,
        aircraftType = aircraftType,
        callsign = callsign,
        startedAt = Instant.parse(startedAt),
        endedAt = endedAt?.let { Instant.parse(it) },
        lastPositionAt = lastPositionAt?.let { Instant.parse(it) },
        positionCount = positionCount,
        positionCountWithCoords = positionCountWithCoords,
        maxAltitudeFt = maxAltitudeFt,
        minAltitudeFt = minAltitudeFt,
    )

    private fun FlightPositionDto.toDomain(): FlightPosition = FlightPosition(
        timestamp = Instant.parse(t),
        lat = lat,
        lon = lon,
        altitudeFt = alt,
        groundSpeedKt = spd,
        headingDeg = hdg,
        verticalRateFpm = vr,
    )
}