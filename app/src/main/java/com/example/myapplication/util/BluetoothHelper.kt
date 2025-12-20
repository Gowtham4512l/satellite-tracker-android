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
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    companion object {
        // Nordic UART Service UUIDs
        val SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val RX_CHAR_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        val TX_CHAR_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")

        const val DEVICE_MAC = "48:31:B7:C1:FF:7D"
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Timber.d("Connected to GATT server")
                    _connectionState.value = ConnectionState.CONNECTED
                    // Discover services
                    gatt?.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    Timber.d("Disconnected from GATT server")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    rxCharacteristic = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Timber.d("Services discovered")
                // Find the Nordic UART Service
                val service = gatt?.getService(SERVICE_UUID)
                if (service != null) {
                    rxCharacteristic = service.getCharacteristic(RX_CHAR_UUID)
                    Timber.d("RX Characteristic found: ${rxCharacteristic != null}")
                } else {
                    Timber.e("Nordic UART Service not found")
                    _connectionState.value = ConnectionState.ERROR
                }
            } else {
                Timber.e("Service discovery failed with status: $status")
                _connectionState.value = ConnectionState.ERROR
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
     * Connect to the IoT device using the hardcoded MAC address
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(): Boolean {
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
            val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(DEVICE_MAC)
            Timber.d("Connecting to device: ${device.address}")

            _connectionState.value = ConnectionState.CONNECTING

            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }

            return true
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect to device")
            _connectionState.value = ConnectionState.ERROR
            return false
        }
    }

    /**
     * Disconnect from the IoT device
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        rxCharacteristic = null
        _connectionState.value = ConnectionState.DISCONNECTED
        Timber.d("Disconnected from device")
    }

    /**
     * Send satellite data to IoT device
     * Format: AAA,EEE\n where AAA is azimuth (000-360) and EEE is elevation (00-90)
     */
    @SuppressLint("MissingPermission")
    fun sendData(azimuth: Double, elevation: Double): Boolean {
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
     * EEE: 2-digit elevation (00-90)
     */
    private fun formatData(azimuth: Double, elevation: Double): String {
        val azimuthInt = azimuth.toInt().coerceIn(0, 360)
        val elevationInt = elevation.toInt().coerceIn(0, 90)
        return "%03d,%02d\n".format(azimuthInt, elevationInt)
    }

    /**
     * Check if Bluetooth is available and enabled
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }
}
