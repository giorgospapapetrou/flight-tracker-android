package com.giorgospapapetrou.flightfinder.data.repository

import com.giorgospapapetrou.flightfinder.data.api.FlightApi
import com.giorgospapapetrou.flightfinder.data.api.LiveStreamClient
import com.giorgospapapetrou.flightfinder.data.api.StreamEventEnvelope
import com.giorgospapapetrou.flightfinder.data.api.dto.AircraftDto
import com.giorgospapapetrou.flightfinder.domain.model.Aircraft
import com.giorgospapapetrou.flightfinder.domain.model.AircraftEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AircraftRepository @Inject constructor(
    private val api: FlightApi,
    private val liveStreamClient: LiveStreamClient,
) {
    suspend fun fetchCurrentAircraft(): List<Aircraft> {
        val response = api.getCurrentAircraft()
        return response.aircraft.map { it.toDomain() }
    }

    fun liveEvents(): Flow<AircraftEvent> = liveStreamClient.events().map { envelope ->
        when (envelope) {
            is StreamEventEnvelope.Connected -> AircraftEvent.Connected
            is StreamEventEnvelope.Disconnected -> AircraftEvent.Disconnected
            is StreamEventEnvelope.AircraftUpdate -> AircraftEvent.Update(envelope.dto.toDomain())
            is StreamEventEnvelope.AircraftRemoved -> AircraftEvent.Removed(envelope.icao)
        }
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