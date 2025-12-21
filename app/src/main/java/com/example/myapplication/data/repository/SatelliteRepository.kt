package com.example.myapplication.data.repository

import com.example.myapplication.BuildConfig
import com.example.myapplication.data.model.SatellitePosition
import com.example.myapplication.data.model.UserLocation
import com.example.myapplication.data.remote.N2YOApiService
import com.example.myapplication.data.remote.RetrofitClient
import com.example.myapplication.util.NetworkHelper
import kotlinx.coroutines.delay
import org.json.JSONObject
import timber.log.Timber

class SatelliteRepository(
    private val apiService: N2YOApiService = RetrofitClient.apiService,
    private val networkHelper: NetworkHelper? = null,
    private var apiKey: String = ""
) {
    private val maxRetries = 3
    private val initialDelayMs = 1000L
    
    /**
     * Update the API key (useful when user changes it in settings)
     */
    fun updateApiKey(newApiKey: String) {
        apiKey = newApiKey
        Timber.d("API key updated")
    }
    
    /**
     * Check if API key is configured
     */
    fun hasApiKey(): Boolean {
        return apiKey.isNotBlank()
    }

    /**
     * Fetch satellite position from N2YO API
     * Returns Result for error handling
     */
    suspend fun getSatellitePosition(
        noradId: Int,
        location: UserLocation
    ): Result<SatellitePosition> {
        // Check if API key is configured
        if (apiKey.isBlank()) {
            Timber.e("API key not configured")
            return Result.failure(Exception("API key not configured. Please set your N2YO API key in settings."))
        }
        
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
                    
                    // Check if response contains an error field (API returns 200 even for errors)
                    if (!body.error.isNullOrBlank()) {
                        val errorMessage = body.error
                        Timber.e("API returned error in 200 response: $errorMessage")
                        
                        val userMessage = when {
                            errorMessage.contains("Invalid API Key", ignoreCase = true) -> 
                                "Invalid API Key! Please check your N2YO API key in settings."
                            errorMessage.contains("rate limit", ignoreCase = true) -> 
                                "API rate limit exceeded. Please try again later."
                            else -> "API Error: $errorMessage"
                        }
                        
                        return Result.failure(Exception(userMessage))
                    }
                    
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
                                satName = body.info?.satName ?: "Satellite $noradId",
                                satLatitude = position.satLatitude,
                                satLongitude = position.satLongitude,
                                satAltitude = position.satAltitude,
                                eclipsed = position.eclipsed
                            )
                        )
                    } else {
                        Timber.w("No position data in response")
                        return Result.failure(Exception("No position data available. Please check the NORAD ID."))
                    }
                } else {
                    // Try to parse error response body
                    val errorMessage = try {
                        val errorBody = response.errorBody()?.string()
                        if (!errorBody.isNullOrBlank()) {
                            val json = JSONObject(errorBody)
                            when {
                                json.has("error") -> json.getString("error")
                                else -> response.message()
                            }
                        } else {
                            response.message()
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to parse error body")
                        response.message()
                    }
                    
                    Timber.e("API error: ${response.code()} - $errorMessage")
                    
                    // Provide user-friendly error messages
                    val userMessage = when {
                        errorMessage.contains("Invalid API Key", ignoreCase = true) -> 
                            "Invalid API Key! Please check your N2YO API key in settings."
                        response.code() == 401 -> 
                            "Authentication failed. Please verify your API key in settings."
                        response.code() == 429 -> 
                            "API rate limit exceeded. Please try again later."
                        response.code() >= 500 -> 
                            "N2YO server error. Please try again later."
                        else -> "API Error: $errorMessage"
                    }
                    
                    lastException = Exception(userMessage)
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
