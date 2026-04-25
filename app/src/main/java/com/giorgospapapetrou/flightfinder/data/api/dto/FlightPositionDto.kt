package com.giorgospapapetrou.flightfinder.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FlightPositionDto(
    val t: String,
    val lat: Double? = null,
    val lon: Double? = null,
    val alt: Int? = null,
    val spd: Int? = null,
    val hdg: Int? = null,
    val vr: Int? = null,
)

@Serializable
data class FlightPositionsResponseDto(
    @SerialName("flight_id") val flightId: Int,
    val positions: List<FlightPositionDto>,
)