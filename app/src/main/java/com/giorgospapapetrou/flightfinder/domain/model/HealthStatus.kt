package com.giorgospapapetrou.flightfinder.domain.model

/** Health snapshot from the backend. */
data class HealthStatus(
    val ok: Boolean,
    val aircraftCount: Int,
)