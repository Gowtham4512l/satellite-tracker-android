package com.example.myapplication.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.example.myapplication.data.model.UserLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

class LocationHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get current device location
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Result<UserLocation> {
        return try {
            val location: Location? = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()

            if (location != null) {
                Result.success(
                    UserLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = location.altitude,
                        isManual = false
                    )
                )
            } else {
                Result.failure(Exception("Unable to retrieve location"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
