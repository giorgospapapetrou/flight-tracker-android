package com.giorgospapapetrou.flightfinder.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AircraftDto(
    val icao: String,
    val callsign: String? = null,
    val registration: String? = null,
    @SerialName("aircraft_type") val aircraftType: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    @SerialName("altitude_ft") val altitudeFt: Int? = null,
    @SerialName("ground_speed_kt") val groundSpeedKt: Int? = null,
    @SerialName("heading_deg") val headingDeg: Int? = null,
    @SerialName("vertical_rate_fpm") val verticalRateFpm: Int? = null,
    @SerialName("on_ground") val onGround: Boolean = false,
    @SerialName("last_position_at") val lastPositionAt: String? = null,
)

@Serializable
data class CurrentAircraftResponseDto(
    val aircraft: List<AircraftDto>,
    @SerialName("server_time") val serverTime: String,
)