package com.example.myapplication.data.remote

import com.example.myapplication.data.model.N2YOResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface N2YOApiService {
    /**
     * Get satellite positions
     * @param id NORAD satellite ID
     * @param observerLat Observer's latitude
     * @param observerLng Observer's longitude
     * @param observerAlt Observer's altitude in meters
     * @param seconds Number of seconds for predictions (typically 1 for single position)
     * @param apiKey N2YO API key
     */
    @GET("positions/{id}/{observer_lat}/{observer_lng}/{observer_alt}/{seconds}")
    suspend fun getSatellitePositions(
        @Path("id") id: Int,
        @Path("observer_lat") observerLat: Double,
        @Path("observer_lng") observerLng: Double,
        @Path("observer_alt") observerAlt: Double,
        @Path("seconds") seconds: Int,
        @Query("apiKey") apiKey: String
    ): Response<N2YOResponse>
}
