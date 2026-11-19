package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "current") val current: OpenMeteoCurrentDto? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoCurrentDto(
    @Json(name = "time") val time: String? = null,
    @Json(name = "temperature_2m") val temperatureC: Double? = null,
    @Json(name = "relative_humidity_2m") val humidityPercent: Int? = null,
    @Json(name = "wind_speed_10m") val windSpeedKmh: Double? = null,
    @Json(name = "wind_direction_10m") val windDirectionDeg: Int? = null,
    @Json(name = "visibility") val visibilityMeters: Double? = null,
    @Json(name = "rain") val rainMm: Double? = null,
    @Json(name = "weather_code") val weatherCode: Int? = null
)

data class WeatherReport(
    val airportCode: String,
    val temperatureC: Double?,
    val conditionLabel: String,
    val weatherCode: Int?,
    val windSpeedKmh: Double?,
    val windDirectionDeg: Int?,
    val visibilityMeters: Double?,
    val humidityPercent: Int?,
    val rainMm: Double?,
    val advisoryWarning: String?,
    val observedAt: String?
)
