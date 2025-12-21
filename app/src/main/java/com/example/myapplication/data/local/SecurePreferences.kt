package com.example.myapplication.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import timber.log.Timber

/**
 * Secure storage for sensitive app settings using EncryptedSharedPreferences
 */
class SecurePreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "satellite_tracker_secure_prefs"
        private const val KEY_API_KEY = "n2yo_api_key"
        private const val KEY_BLE_MAC = "ble_device_mac"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
    }

    private val masterKeyAlias: String = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Timber.e(e, "Failed to create EncryptedSharedPreferences, falling back to regular")
        // Fallback to regular SharedPreferences if encryption fails
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Save N2YO API key
     */
    fun saveApiKey(apiKey: String) {
        sharedPreferences.edit().putString(KEY_API_KEY, apiKey).apply()
        Timber.d("API key saved securely")
    }

    /**
     * Get N2YO API key
     */
    fun getApiKey(): String? {
        return sharedPreferences.getString(KEY_API_KEY, null)
    }

    /**
     * Save BLE device MAC address
     */
    fun saveBleMacAddress(macAddress: String) {
        sharedPreferences.edit().putString(KEY_BLE_MAC, macAddress).apply()
        Timber.d("BLE MAC address saved securely")
    }

    /**
     * Get BLE device MAC address
     */
    fun getBleMacAddress(): String? {
        return sharedPreferences.getString(KEY_BLE_MAC, null)
    }

    /**
     * Check if this is the first launch
     */
    fun isFirstLaunch(): Boolean {
        val isFirst = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
        if (isFirst) {
            // Mark as not first launch after checking
            sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
        }
        return isFirst
    }

    /**
     * Clear all stored settings (for testing or reset)
     */
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
        Timber.d("All secure preferences cleared")
    }

    /**
     * Check if API key is configured
     */
    fun hasApiKey(): Boolean {
        return !getApiKey().isNullOrBlank()
    }

    /**
     * Check if BLE MAC address is configured
     */
    fun hasBleMacAddress(): Boolean {
        return !getBleMacAddress().isNullOrBlank()
    }
}
