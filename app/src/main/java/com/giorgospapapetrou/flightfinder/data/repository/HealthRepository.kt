package com.giorgospapapetrou.flightfinder.data.repository

import com.giorgospapapetrou.flightfinder.data.api.FlightApi
import com.giorgospapapetrou.flightfinder.domain.model.HealthStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepository @Inject constructor(
    private val api: FlightApi,
) {
    suspend fun fetchHealth(): HealthStatus {
        val dto = api.getHealth()
        return HealthStatus(
            ok = dto.status == "ok",
            aircraftCount = dto.aircraftCurrentlyTracked,
        )
    }
}