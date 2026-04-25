package com.giorgospapapetrou.flightfinder.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FlightSummaryDto(
    val id: Int,
    @SerialName("aircraft_icao") val aircraftIcao: String,
    @SerialName("aircraft_registration") val aircraftRegistration: String? = null,
    @SerialName("aircraft_type") val aircraftType: String? = null,
    val callsign: String? = null,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("last_position_at") val lastPositionAt: String? = null,
    @SerialName("position_count") val positionCount: Int,
    @SerialName("position_count_with_coords") val positionCountWithCoords: Int = 0,
    @SerialName("max_altitude_ft") val maxAltitudeFt: Int? = null,
    @SerialName("min_altitude_ft") val minAltitudeFt: Int? = null,
)

@Serializable
data class FlightsResponseDto(
    val flights: List<FlightSummaryDto>,
)