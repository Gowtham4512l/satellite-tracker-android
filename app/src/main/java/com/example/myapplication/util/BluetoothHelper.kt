package com.example.myapplication.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

class BluetoothHelper(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var bluetoothGatt: BluetoothGatt? = null
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    
    // Coroutine scope for timeout management
    private val scope = CoroutineScope(Dispatchers.Main)
    private var connectionTimeoutJob: Job? = null
    private var isIntentionalDisconnect = false

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    // Current device MAC address (configurable)
    private var currentDeviceMac: String? = null

    companion object {
        // Nordic UART Service UUIDs
        val SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val RX_CHAR_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        val TX_CHAR_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
        
        // Connection timeout in milliseconds (15 seconds)
        private const val CONNECTION_TIMEOUT_MS = 15_000L
        
        // MAC address validation regex
        private val MAC_ADDRESS_REGEX = Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")
        
        /**
         * Validate MAC address format
         */
        fun isValidMacAddress(macAddress: String): Boolean {
            return MAC_ADDRESS_REGEX.matches(macAddress.trim())
        }
    }

    /**
     * Check if required Bluetooth permissions are granted
     */
    private fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires BLUETOOTH_CONNECT permission
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 11 and below - permissions are granted at install time
            true
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Timber.d("Connected to GATT server")
                    // Cancel timeout since connection succeeded
                    connectionTimeoutJob?.cancel()
                    _connectionState.value = ConnectionState.CONNECTED
                    // Discover services
                    gatt?.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    // Cancel timeout job
                    connectionTimeoutJob?.cancel()
                    
                    // Check if this was an intentional disconnect or a connection failure
                    if (isIntentionalDisconnect) {
                        Timber.d("Intentionally disconnected from GATT server")
                        _connectionState.value = ConnectionState.DISCONNECTED
                        isIntentionalDisconnect = false
                    } else if (status != BluetoothGatt.GATT_SUCCESS) {
                        // Connection failed (status != 0 means error)
                        Timber.e("Connection failed with status: $status")
                        _connectionState.value = ConnectionState.ERROR
                    } else {
                        // Unexpected disconnect
                        Timber.w("Unexpectedly disconnected from GATT server")
                        _connectionState.value = ConnectionState.DISCONNECTED
                    }
                    rxCharacteristic = null
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.d("Services discovered successfully")
                
                // Find the Nordic UART Service
                val service = gatt?.getService(SERVICE_UUID)
                if (service != null) {
                    rxCharacteristic = service.getCharacteristic(RX_CHAR_UUID)
                    
                    if (rxCharacteristic != null) {
                        Timber.d("UART characteristics found")
                        _connectionState.value = ConnectionState.CONNECTED
                    } else {
                        Timber.e("UART characteristics not found")
                        _connectionState.value = ConnectionState.ERROR
                        // Clean up GATT resources on characteristic discovery failure
                        gatt?.disconnect()
                        gatt?.close()
                        bluetoothGatt = null
                    }
                } else {
                    Timber.e("UART service not found")
                    _connectionState.value = ConnectionState.ERROR
                    // Clean up GATT resources on service discovery failure
                    gatt?.disconnect()
                    gatt?.close()
                    bluetoothGatt = null
                }
            } else {
                Timber.e("Service discovery failed with status: $status")
                _connectionState.value = ConnectionState.ERROR
                // Clean up GATT resources on service discovery failure
                gatt?.disconnect()
                gatt?.close()
                bluetoothGatt = null
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.d("Data written successfully")
            } else {
                Timber.e("Write failed with status: $status")
            }
        }
    }

    /**
     * Connect to the IoT device using the provided MAC address
     * @param deviceMacAddress BLE device MAC address (format: XX:XX:XX:XX:XX:XX)
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceMacAddress: String? = null): Boolean {
        // Use provided MAC or fall back to stored MAC
        val macToUse = deviceMacAddress ?: currentDeviceMac
        
        if (macToUse.isNullOrBlank()) {
            Timber.e("No MAC address provided")
            _connectionState.value = ConnectionState.ERROR
            return false
        }
        
        // Validate MAC address format
        if (!isValidMacAddress(macToUse)) {
            Timber.e("Invalid MAC address format: $macToUse")
            _connectionState.value = ConnectionState.ERROR
            return false
        }
        
        // Store the MAC address for future use
        currentDeviceMac = macToUse
        
        // Check permissions first
        if (!hasBluetoothPermissions()) {
            Timber.e("Bluetooth permissions not granted")
            _connectionState.value = ConnectionState.ERROR
            return false
        }

        if (bluetoothAdapter == null) {
            Timber.e("Bluetooth adapter not available")
            _connectionState.value = ConnectionState.ERROR
            return false
        }

        if (!bluetoothAdapter.isEnabled) {
            Timber.e("Bluetooth is not enabled")
            _connectionState.value = ConnectionState.ERROR
            return false
        }

        try {
            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(macToUse)
            Timber.d("Connecting to device: ${device.address}")

            _connectionState.value = ConnectionState.CONNECTING

            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }
            
            // Start connection timeout
            connectionTimeoutJob?.cancel()
            connectionTimeoutJob = scope.launch {
                delay(CONNECTION_TIMEOUT_MS)
                // If we reach here, connection timed out
                if (_connectionState.value == ConnectionState.CONNECTING) {
                    Timber.e("Connection timeout after ${CONNECTION_TIMEOUT_MS}ms")
                    _connectionState.value = ConnectionState.ERROR
                    bluetoothGatt?.disconnect()
                    bluetoothGatt?.close()
                    bluetoothGatt = null
                }
            }

            return true
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect to device")
            connectionTimeoutJob?.cancel()
            _connectionState.value = ConnectionState.ERROR
            return false
        }
    }
    
    /**
     * Get the currently configured MAC address
     */
    fun getCurrentMacAddress(): String? {
        return currentDeviceMac
    }
    
    /**
     * Set the MAC address without connecting
     */
    fun setMacAddress(macAddress: String) {
        if (isValidMacAddress(macAddress)) {
            currentDeviceMac = macAddress
            Timber.d("MAC address set to: $macAddress")
        } else {
            Timber.w("Invalid MAC address format: $macAddress")
        }
    }

    /**
     * Disconnect from the IoT device
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        if (!hasBluetoothPermissions()) {
            Timber.w("Cannot disconnect: Bluetooth permissions not granted")
            return
        }
        
        // Cancel any pending timeout
        connectionTimeoutJob?.cancel()
        
        // Set flag to indicate this is an intentional disconnect
        isIntentionalDisconnect = true
        
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        rxCharacteristic = null
        
        // State will be updated by onConnectionStateChange callback
        Timber.d("Disconnecting from device...")
    }

    /**
     * Send azimuth and elevation data to the connected device
     */
    @SuppressLint("MissingPermission")
    fun sendData(azimuth: Double, elevation: Double): Boolean {
        // Check permission before sending (user might revoke during session)
        if (!hasBluetoothPermissions()) {
            Timber.e("Bluetooth permission revoked - cannot send data")
            _connectionState.value = ConnectionState.ERROR
            return false
        }
        
        if (_connectionState.value != ConnectionState.CONNECTED) {
            Timber.w("Cannot send data: not connected")
            return false
        }

        if (rxCharacteristic == null) {
            Timber.e("RX Characteristic not available")
            return false
        }

        try {
            val data = formatData(azimuth, elevation)
            Timber.d("Sending data: $data")

            val bytes = data.toByteArray(Charsets.UTF_8)

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGatt?.writeCharacteristic(
                    rxCharacteristic!!,
                    bytes,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                ) == BluetoothGatt.GATT_SUCCESS
            } else {
                @Suppress("DEPRECATION")
                rxCharacteristic!!.value = bytes
                @Suppress("DEPRECATION")
                bluetoothGatt?.writeCharacteristic(rxCharacteristic!!) ?: false
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to send data")
            return false
        }
    }

    /**
     * Format azimuth and elevation into the required format: AAA,EEE\n
     * AAA: 3-digit azimuth (000-360)
     * EEE: 2-digit elevation with sign at end (-90 to +90, e.g., "10-", "45+", "00+")
     */
    private fun formatData(azimuth: Double, elevation: Double): String {
        val azimuthInt = azimuth.toInt().coerceIn(0, 360)
        val elevationInt = elevation.toInt().coerceIn(-90, 90)
        
        // Format elevation with sign at end: 45+, 10-, 00+
        val elevationAbs = kotlin.math.abs(elevationInt)
        val sign = if (elevationInt >= 0) "+" else "-"
        val elevationStr = "%02d%s".format(elevationAbs, sign)
        
        return "%03d,%s\n".format(azimuthInt, elevationStr)
    }

    /**
     * Check if Bluetooth is available and enabled
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }
    
    /**
     * Clean up resources - call this when BluetoothHelper is no longer needed
     */
    fun cleanup() {
        connectionTimeoutJob?.cancel()
        scope.cancel()
        disconnect()
    }
}
