package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.local.SecurePreferences
import timber.log.Timber

/**
 * Repository for managing app settings (API key, BLE MAC address, etc.)
 */
class SettingsRepository(context: Context) {

    private val securePreferences = SecurePreferences(context)

    companion object {
        // N2YO API key format: alphanumeric, typically 13-20 characters
        private val API_KEY_REGEX = Regex("^[A-Za-z0-9-_]{10,30}$")
        
        // BLE MAC address format: XX:XX:XX:XX:XX:XX (hex)
        private val MAC_ADDRESS_REGEX = Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")
    }

    /**
     * Save N2YO API key
     */
    fun saveApiKey(apiKey: String): Result<Unit> {
        val trimmed = apiKey.trim()
        
        return if (validateApiKey(trimmed)) {
            securePreferences.saveApiKey(trimmed)
            Timber.d("API key saved successfully")
            Result.success(Unit)
        } else {
            Timber.w("Invalid API key format")
            Result.failure(IllegalArgumentException("Invalid API key format. Expected alphanumeric string (10-30 characters)"))
        }
    }

    /**
     * Get N2YO API key
     */
    fun getApiKey(): String? {
        return securePreferences.getApiKey()
    }

    /**
     * Save BLE device MAC address
     */
    fun saveBleMacAddress(macAddress: String): Result<Unit> {
        val trimmed = macAddress.trim().uppercase()
        
        return if (validateMacAddress(trimmed)) {
            securePreferences.saveBleMacAddress(trimmed)
            Timber.d("BLE MAC address saved successfully")
            Result.success(Unit)
        } else {
            Timber.w("Invalid MAC address format")
            Result.failure(IllegalArgumentException("Invalid MAC address format. Expected format: XX:XX:XX:XX:XX:XX"))
        }
    }

    /**
     * Get BLE device MAC address
     */
    fun getBleMacAddress(): String? {
        return securePreferences.getBleMacAddress()
    }

    /**
     * Check if this is the first launch
     */
    fun isFirstLaunch(): Boolean {
        return securePreferences.isFirstLaunch()
    }

    /**
     * Check if API key is configured
     */
    fun hasApiKey(): Boolean {
        return securePreferences.hasApiKey()
    }

    /**
     * Check if BLE MAC address is configured
     */
    fun hasBleMacAddress(): Boolean {
        return securePreferences.hasBleMacAddress()
    }

    /**
     * Validate API key format
     */
    fun validateApiKey(apiKey: String): Boolean {
        return apiKey.isNotBlank() && API_KEY_REGEX.matches(apiKey)
    }

    /**
     * Validate MAC address format
     */
    fun validateMacAddress(macAddress: String): Boolean {
        return macAddress.isNotBlank() && MAC_ADDRESS_REGEX.matches(macAddress)
    }

    /**
     * Clear all settings (for testing or reset)
     */
    fun clearAllSettings() {
        securePreferences.clearAll()
        Timber.d("All settings cleared")
    }
}
