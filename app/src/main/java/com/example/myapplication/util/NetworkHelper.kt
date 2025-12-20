package com.example.myapplication.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import timber.log.Timber

class NetworkHelper(private val context: Context) {

    /**
     * Check if network is available
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        if (connectivityManager == null) {
            Timber.w("ConnectivityManager not available")
            return false
        }

        val network = connectivityManager.activeNetwork ?: run {
            Timber.d("No active network")
            return false
        }

        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: run {
            Timber.d("No network capabilities")
            return false
        }

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        Timber.d("Network available: $hasInternet")
        return hasInternet
    }

    /**
     * Get network type description
     */
    fun getNetworkType(): String {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return "Unknown"

        val network = connectivityManager.activeNetwork ?: return "No Network"
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Unknown"

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Unknown"
        }
    }
}