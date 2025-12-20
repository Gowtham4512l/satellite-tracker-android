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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

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
        // Use applicationContext to avoid memory leak (ViewModel outlives Activity)
        val appContext = context.applicationContext
        locationHelper = LocationHelper(appContext)
        val networkHelper = NetworkHelper(appContext)
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

    /**
     * Set error message in UI state
     */
    fun setError(message: String) {
        _uiState.update { it.copy(error = message) }
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

        // Enhanced NORAD ID validation with range checking
        when {
            noradId == null -> {
                _uiState.update { it.copy(error = "Please enter a NORAD ID") }
                return
            }

            noradId <= 0 -> {
                _uiState.update { it.copy(error = "NORAD ID must be greater than 0") }
                return
            }

            noradId > 99999 -> {
                _uiState.update { it.copy(error = "NORAD ID must be less than 100000") }
                return
            }
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

        // Start polling every 5 seconds with error handling
        trackingJob = viewModelScope.launch {
            while (isActive) {  // Check cancellation instead of while(true)
                try {
                    // Use current location from state to handle location updates during tracking
                    val currentLocation = _uiState.value.userLocation
                    if (currentLocation != null &&
                        currentLocation.latitude != null &&
                        currentLocation.longitude != null
                    ) {
                        fetchSatellitePosition(noradId, currentLocation)
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // Propagate cancellation
                    throw e
                } catch (e: Exception) {
                    // Log error but continue tracking
                    Timber.e(e, "Error in tracking loop")
                    _uiState.update {
                        it.copy(error = "Tracking error: ${e.message}")
                    }
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
