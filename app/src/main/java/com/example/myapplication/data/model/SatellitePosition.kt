package com.example.myapplication.data.model

data class SatellitePosition(
    val azimuth: Double?,
    val elevation: Double?,
    val timestamp: Long?,
    val satName: String? = null,
    val satLatitude: Double? = null,
    val satLongitude: Double? = null,
    val satAltitude: Double? = null,
    val eclipsed: Boolean? = null
)
