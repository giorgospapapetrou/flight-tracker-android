package com.giorgospapapetrou.flightfinder.data.api

import com.giorgospapapetrou.flightfinder.data.api.dto.HealthResponseDto
import retrofit2.http.GET

interface FlightApi {
    @GET("api/v1/health")
    suspend fun getHealth(): HealthResponseDto
}