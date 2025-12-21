package com.example.myapplication.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.SatellitePosition
import com.example.myapplication.data.model.UserLocation
import com.example.myapplication.data.repository.SatelliteRepository
import com.example.myapplication.util.BluetoothHelper
import com.example.myapplication.util.ConnectionState
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
    val useManualLocation: Boolean = false,
    val bleConnected: Boolean = false,
    val bleConnectionState: ConnectionState = ConnectionState.DISCONNECTED
)

class SatelliteViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SatelliteUiState())
    val uiState: StateFlow<SatelliteUiState> = _uiState.asStateFlow()

    private var trackingJob: Job? = null
    private var locationHelper: LocationHelper? = null
    private var repository: SatelliteRepository? = null
    private var bluetoothHelper: BluetoothHelper? = null
    private var settingsRepository: com.example.myapplication.data.repository.SettingsRepository? =
        null
    private var errorDismissJob: Job? = null
    
    companion object {
        private const val ERROR_DISMISS_DELAY_MS = 5_000L // 5 seconds
    }

    fun initialize(context: Context) {
        // Use applicationContext to avoid memory leak (ViewModel outlives Activity)
        val appContext = context.applicationContext
        locationHelper = LocationHelper(appContext)
        val networkHelper = NetworkHelper(appContext)

        // Initialize settings repository
        settingsRepository =
            com.example.myapplication.data.repository.SettingsRepository(appContext)

        // Load API key from settings
        val apiKey = settingsRepository?.getApiKey() ?: ""
        repository = SatelliteRepository(networkHelper = networkHelper, apiKey = apiKey)

        // Initialize Bluetooth helper
        bluetoothHelper = BluetoothHelper(appContext)

        // Load and set BLE MAC address if configured
        val bleMac = settingsRepository?.getBleMacAddress()
        if (!bleMac.isNullOrBlank()) {
            bluetoothHelper?.setMacAddress(bleMac)
        }

        _uiState.update {
            it.copy(
                locationPermissionGranted = locationHelper?.hasLocationPermission() == true
            )
        }

        // Observe BLE connection state
        viewModelScope.launch {
            bluetoothHelper?.connectionState?.collect { state ->
                _uiState.update {
                    it.copy(
                        bleConnected = state == ConnectionState.CONNECTED,
                        bleConnectionState = state
                    )
                }
            }
        }
    }

    /**
     * Reload settings (call this when returning from settings screen)
     */
    fun reloadSettings() {
        val apiKey = settingsRepository?.getApiKey() ?: ""
        repository?.updateApiKey(apiKey)

        val bleMac = settingsRepository?.getBleMacAddress()
        if (!bleMac.isNullOrBlank()) {
            bluetoothHelper?.setMacAddress(bleMac)
        }

        Timber.d("Settings reloaded")
    }

    fun onNoradIdChange(id: String) {
        _uiState.update { it.copy(noradId = id, error = null) }
    }

    fun onPermissionGranted() {
        _uiState.update { it.copy(locationPermissionGranted = true) }
    }

    /**
     * Set error message in UI state with auto-dismiss after 10 seconds
     */
    fun setError(message: String) {
        _uiState.update { it.copy(error = message) }
        
        // Cancel any existing error dismiss job
        errorDismissJob?.cancel()
        
        // Schedule error dismissal after 10 seconds
        errorDismissJob = viewModelScope.launch {
            delay(ERROR_DISMISS_DELAY_MS)
            _uiState.update { it.copy(error = null) }
            Timber.d("Error message auto-dismissed")
        }
    }
    
    /**
     * Manually clear error message
     */
    fun clearError() {
        errorDismissJob?.cancel()
        _uiState.update { it.copy(error = null) }
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
                        error = "Location helper not initialized"
                    )
                }
                return@launch
            }
            
            // Check permission before attempting to get location
            if (!helper.hasLocationPermission()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        locationPermissionGranted = false,
                        error = "Location permission required. Please grant location access."
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
                setError("Please enter a NORAD ID")
                return
            }

            noradId <= 0 -> {
                setError("NORAD ID must be greater than 0")
                return
            }

            noradId > 99999 -> {
                setError("NORAD ID must be less than 100000")
                return
            }
        }

        if (location == null) {
            setError("Location not available. Please enable GPS or enter manual location.")
            return
        }

        // Validate that location has required coordinates
        if (location.latitude == null || location.longitude == null) {
            setError("Location coordinates are incomplete. Please try again.")
            return
        }

        _uiState.update { it.copy(isTracking = true, error = null) }

        // Cancel existing job if any
        trackingJob?.cancel()

        // Start polling every 5 seconds with error handling
        trackingJob = viewModelScope.launch {
            while (isActive) {  // Check cancellation instead of while(true)
                try {
                    // Check if location permission is still granted (user might revoke during tracking)
                    if (locationHelper?.hasLocationPermission() == false) {
                        Timber.w("Location permission revoked during tracking")
                        setError("Location permission was revoked. Please grant permission to continue tracking.")
                        stopTracking()
                        break
                    }
                    
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
                } catch (e: SecurityException) {
                    // Permission revoked
                    Timber.e(e, "Security exception in tracking loop - permission likely revoked")
                    setError("Permission error: ${e.message}")
                    stopTracking()
                    break
                } catch (e: Exception) {
                    // Log error but continue tracking
                    Timber.e(e, "Error in tracking loop")
                    setError("Tracking error: ${e.message}")
                }
                delay(5000) // 5 seconds
            }
        }
    }

    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        _uiState.update { 
            it.copy(
                isTracking = false,
                currentPosition = null  // Clear old position data
            ) 
        }
    }

    private suspend fun fetchSatellitePosition(noradId: Int, location: UserLocation) {
        // Validate location has non-null coordinates before API call
        if (location.latitude == null || location.longitude == null) {
            setError("Invalid location coordinates")
            return
        }

        val repo = repository
        if (repo == null) {
            setError("Repository not initialized")
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

                // Auto-send to BLE if connected
                if (_uiState.value.bleConnected) {
                    bluetoothHelper?.sendData(
                        position.azimuth ?: 0.0,
                        position.elevation ?: 0.0
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        currentPosition = null,  // Clear position data on error
                        error = "API Error: ${error.message}"
                    )
                }
                
                // Auto-dismiss error after 5 seconds
                viewModelScope.launch {
                    delay(5000)
                    _uiState.update { it.copy(error = null) }
                }
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        trackingJob?.cancel()
        bluetoothHelper?.cleanup()
    }

    /**
     * Connect to the IoT device via BLE
     */
    fun connectBluetooth() {
        val bleMac = settingsRepository?.getBleMacAddress()
        if (bleMac.isNullOrBlank()) {
            setError("BLE MAC address not configured. Please set it in settings.")
            return
        }
        bluetoothHelper?.connectToDevice(bleMac)
    }

    /**
     * Disconnect from the IoT device
     */
    fun disconnectBluetooth() {
        bluetoothHelper?.disconnect()
    }

    /**
     * Check if Bluetooth is available
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothHelper?.isBluetoothAvailable() ?: false
    }
}
