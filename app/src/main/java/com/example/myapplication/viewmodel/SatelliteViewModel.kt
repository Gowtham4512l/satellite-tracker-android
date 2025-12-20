package com.example.myapplication.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.SatellitePosition
import com.example.myapplication.data.model.UserLocation
import com.example.myapplication.data.repository.SatelliteRepository
import com.example.myapplication.util.LocationHelper
import com.example.myapplication.util.NetworkHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SatelliteUiState(
    val noradId: String = "",
    val currentPosition: SatellitePosition? = null,
    val userLocation: UserLocation? = null,
    val isTracking: Boolean = false,
    val error: String? = null,
    val isLoading: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    val useManualLocation: Boolean = false
)

class SatelliteViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SatelliteUiState())
    val uiState: StateFlow<SatelliteUiState> = _uiState.asStateFlow()

    private var trackingJob: Job? = null
    private var locationHelper: LocationHelper? = null
    private var repository: SatelliteRepository? = null

    fun initialize(context: Context) {
        locationHelper = LocationHelper(context)
        val networkHelper = NetworkHelper(context)
        repository = SatelliteRepository(networkHelper = networkHelper)

        _uiState.update {
            it.copy(
                locationPermissionGranted = locationHelper?.hasLocationPermission() == true
            )
        }
    }

    fun onNoradIdChange(id: String) {
        _uiState.update { it.copy(noradId = id, error = null) }
    }

    fun onPermissionGranted() {
        _uiState.update { it.copy(locationPermissionGranted = true) }
    }

    fun onManualLocationChange(latitude: Double, longitude: Double, altitude: Double) {
        val manualLocation = UserLocation(
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            isManual = true
        )
        _uiState.update {
            it.copy(
                userLocation = manualLocation,
                useManualLocation = true,
                error = null
            )
        }
    }

    fun toggleManualLocation(enabled: Boolean) {
        _uiState.update { it.copy(useManualLocation = enabled) }
        if (!enabled) {
            // Switch back to GPS
            fetchGpsLocation()
        }
    }

    fun fetchGpsLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val helper = locationHelper
            if (helper == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "LocationHelper not initialized"
                    )
                }
                return@launch
            }

            val result = helper.getCurrentLocation()
            result.fold(
                onSuccess = { location ->
                    _uiState.update {
                        it.copy(
                            userLocation = location,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to get location: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun startTracking() {
        val noradId = _uiState.value.noradId.toIntOrNull()
        val location = _uiState.value.userLocation

        if (noradId == null || noradId <= 0) {
            _uiState.update { it.copy(error = "Please enter a valid NORAD ID (must be greater than 0)") }
            return
        }

        if (location == null) {
            _uiState.update { it.copy(error = "Location not available. Please enable GPS or enter manual location.") }
            return
        }

        // Validate that location has required coordinates
        if (location.latitude == null || location.longitude == null) {
            _uiState.update { it.copy(error = "Location coordinates are incomplete. Please try again.") }
            return
        }

        _uiState.update { it.copy(isTracking = true, error = null) }

        // Cancel existing job if any
        trackingJob?.cancel()

        // Start polling every 5 seconds
        trackingJob = viewModelScope.launch {
            while (true) {
                // Use current location from state to handle location updates during tracking
                val currentLocation = _uiState.value.userLocation
                if (currentLocation != null && currentLocation.latitude != null && currentLocation.longitude != null) {
                    fetchSatellitePosition(noradId, currentLocation)
                }
                delay(5000) // 5 seconds
            }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        _uiState.update { it.copy(isTracking = false) }
    }

    private suspend fun fetchSatellitePosition(noradId: Int, location: UserLocation) {
        // Validate location has non-null coordinates before API call
        if (location.latitude == null || location.longitude == null) {
            _uiState.update { it.copy(error = "Invalid location coordinates") }
            return
        }

        val repo = repository
        if (repo == null) {
            _uiState.update { it.copy(error = "Repository not initialized") }
            return
        }

        val result = repo.getSatellitePosition(noradId, location)

        result.fold(
            onSuccess = { position ->
                _uiState.update {
                    it.copy(
                        currentPosition = position,
                        error = null
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        error = "API Error: ${error.message}"
                    )
                }
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        trackingJob?.cancel()
    }
}
