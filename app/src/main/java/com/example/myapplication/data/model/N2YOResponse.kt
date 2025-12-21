package com.example.myapplication.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class N2YOResponse(
    @field:Json(name = "info") val info: Info?,
    @field:Json(name = "positions") val positions: List<Position>?,
    @field:Json(name = "error") val error: String? // API returns error field even with 200 OK
)

@JsonClass(generateAdapter = true)
data class Info(
    @field:Json(name = "satid") val satId: Int?,
    @field:Json(name = "satname") val satName: String?,
    @field:Json(name = "transactionscount") val transactionsCount: Int?
)

@JsonClass(generateAdapter = true)
data class Position(
    @field:Json(name = "satlatitude") val satLatitude: Double?,
    @field:Json(name = "satlongitude") val satLongitude: Double?,
    @field:Json(name = "sataltitude") val satAltitude: Double?,
    @field:Json(name = "azimuth") val azimuth: Double?,
    @field:Json(name = "elevation") val elevation: Double?,
    @field:Json(name = "ra") val ra: Double?,
    @field:Json(name = "dec") val dec: Double?,
    @field:Json(name = "timestamp") val timestamp: Long?
)
