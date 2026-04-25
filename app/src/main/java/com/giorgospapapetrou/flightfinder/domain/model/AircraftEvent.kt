package com.giorgospapapetrou.flightfinder.domain.model

/**
 * Events emitted by the live stream.
 * The repository translates raw WebSocket messages into one of these.
 */
sealed interface AircraftEvent {
    data class Update(val aircraft: Aircraft) : AircraftEvent
    data class Removed(val icao: String) : AircraftEvent
    data object Connected : AircraftEvent
    data object Disconnected : AircraftEvent
}