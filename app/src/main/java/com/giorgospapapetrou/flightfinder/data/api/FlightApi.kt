package com.giorgospapapetrou.flightfinder.data.api

import com.giorgospapapetrou.flightfinder.data.api.dto.CurrentAircraftResponseDto
import com.giorgospapapetrou.flightfinder.data.api.dto.FlightPositionsResponseDto
import com.giorgospapapetrou.flightfinder.data.api.dto.FlightsResponseDto
import com.giorgospapapetrou.flightfinder.data.api.dto.HealthResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FlightApi {
    @GET("api/v1/health")
    suspend fun getHealth(): HealthResponseDto

    @GET("api/v1/aircraft/current")
    suspend fun getCurrentAircraft(): CurrentAircraftResponseDto

    @GET("api/v1/flights")
    suspend fun getFlights(
        @Query("date") date: String? = null,
    ): FlightsResponseDto

    @GET("api/v1/flights/{flight_id}/positions")
    suspend fun getFlightPositions(
        @Path("flight_id") flightId: Int,
    ): FlightPositionsResponseDto
}