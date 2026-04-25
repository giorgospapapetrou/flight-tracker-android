package com.giorgospapapetrou.flightfinder.data.repository

import com.giorgospapapetrou.flightfinder.data.api.FlightApi
import com.giorgospapapetrou.flightfinder.data.api.dto.AircraftDto
import com.giorgospapapetrou.flightfinder.domain.model.Aircraft
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AircraftRepository @Inject constructor(
    private val api: FlightApi,
) {
    suspend fun fetchCurrentAircraft(): List<Aircraft> {
        val response = api.getCurrentAircraft()
        return response.aircraft.map { it.toDomain() }
    }

    private fun AircraftDto.toDomain(): Aircraft = Aircraft(
        icao = icao,
        callsign = callsign,
        registration = registration,
        aircraftType = aircraftType,
        lat = lat,
        lon = lon,
        altitudeFt = altitudeFt,
        groundSpeedKt = groundSpeedKt,
        headingDeg = headingDeg,
        verticalRateFpm = verticalRateFpm,
        onGround = onGround,
        lastPositionAt = lastPositionAt?.let { runCatching { Instant.parse(it) }.getOrNull() },
    )
}