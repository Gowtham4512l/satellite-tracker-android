package com.example.myapplication.data.repository

import com.example.myapplication.BuildConfig
import com.example.myapplication.data.model.SatellitePosition
import com.example.myapplication.data.model.UserLocation
import com.example.myapplication.data.remote.N2YOApiService
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.util.NetworkHelper
import kotlinx.coroutines.delay
import timber.log.Timber

class SatelliteRepository(
    private val apiService: N2YOApiService = RetrofitClient.apiService,
    private val networkHelper: NetworkHelper? = null
) {
    private val apiKey = BuildConfig.N2YO_API_KEY
    private val maxRetries = 3
    private val initialDelayMs = 1000L

    /**
     * Fetch satellite position from N2YO API
     * Returns Result for error handling
     */
    suspend fun getSatellitePosition(
        noradId: Int,
        location: UserLocation
    ): Result<SatellitePosition> {
        // Check network connectivity
        if (networkHelper?.isNetworkAvailable() == false) {
            Timber.w("No network connection available")
            return Result.failure(Exception("No network connection. Please check your internet and try again."))
        }

        // Validate location has required coordinates
        val lat = location.latitude
        val lng = location.longitude
        val alt = location.altitude ?: 0.0

        if (lat == null || lng == null) {
            Timber.e("Invalid location coordinates")
            return Result.failure(Exception("Invalid location: latitude and longitude are required"))
        }

        Timber.d("Fetching satellite position for NORAD ID: $noradId at ($lat, $lng, $alt)")

        // Retry logic with exponential backoff
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                val response = apiService.getSatellitePositions(
                    id = noradId,
                    observerLat = lat,
                    observerLng = lng,
                    observerAlt = alt,
                    seconds = 1,
                    apiKey = apiKey
                )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val position = body.positions?.firstOrNull()

                    if (position != null) {
                        // Debug logging for satellite name
                        Timber.d("API Response - info: ${body.info}")
                        Timber.d("API Response - satName: ${body.info?.satName}")
                        
                        Timber.d("Successfully fetched satellite position")
                        return Result.success(
                            SatellitePosition(
                                azimuth = position.azimuth,
                                elevation = position.elevation,
                                timestamp = position.timestamp,
                                satName = body.info?.satName ?: "Satellite $noradId"
                            )
                        )
                    } else {
                        Timber.w("No position data in response")
                        return Result.failure(Exception("No position data available"))
                    }
                } else {
                    Timber.e("API error: ${response.code()} - ${response.message()}")
                    lastException =
                        Exception("API Error: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Network request failed (attempt ${attempt + 1}/$maxRetries)")
                lastException = e

                // Don't delay after last attempt
                if (attempt < maxRetries - 1) {
                    val delayMs = initialDelayMs * (1 shl attempt) // Exponential backoff
                    Timber.d("Retrying in ${delayMs}ms...")
                    delay(delayMs)
                }
            }
        }

        return Result.failure(lastException ?: Exception("Unknown error"))
    }
}
