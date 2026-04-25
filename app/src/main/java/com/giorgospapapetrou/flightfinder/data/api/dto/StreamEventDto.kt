package com.giorgospapapetrou.flightfinder.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * The envelope every WebSocket message arrives in.
 * The 'data' field's shape depends on 'type'.
 */
@Serializable
data class StreamEventDto(
    val type: String,
    val data: JsonElement? = null,
)

/** Payload for type="aircraft_removed" */
@Serializable
data class AircraftRemovedDto(
    val icao: String,
)