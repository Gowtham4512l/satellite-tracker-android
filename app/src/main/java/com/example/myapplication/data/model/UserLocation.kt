package com.example.myapplication.data.model

data class UserLocation(
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    val isManual: Boolean = false
)
