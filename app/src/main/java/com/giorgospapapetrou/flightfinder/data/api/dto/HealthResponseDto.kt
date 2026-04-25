package com.giorgospapapetrou.flightfinder.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponseDto(
    val status: String,
    @SerialName("aircraft_currently_tracked")
    val aircraftCurrentlyTracked: Int,
)