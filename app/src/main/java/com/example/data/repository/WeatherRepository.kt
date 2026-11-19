package com.example.data.repository

import com.example.model.WeatherReport
import com.example.network.OpenMeteoApiService
import com.example.utils.AviationFormatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class WeatherRepository(private val weatherApi: OpenMeteoApiService) {

    private data class CachedWeather(val report: WeatherReport, val timestampMs: Long)
    private val cache = ConcurrentHashMap<String, CachedWeather>()

    suspend fun getAirportWeather(
        airportCode: String,
        lat: Double,
        lng: Double
    ): Result<WeatherReport> = withContext(Dispatchers.IO) {
        val key = airportCode.uppercase()
        val now = System.currentTimeMillis()
        cache[key]?.let {
            if (now - it.timestampMs < 15 * 60_000L) {
                return@withContext Result.success(it.report)
            }
        }
        try {
            val response = weatherApi.getCurrentWeather(latitude = lat, longitude = lng)
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Weather HTTP ${response.code()}"))
            }
            val current = response.body()?.current
                ?: return@withContext Result.failure(Exception("Weather data unavailable"))
            val (condition, baseAdvisory) = AviationFormatters.wmoWeatherDescription(current.weatherCode)
            val windAdvisory = if ((current.windSpeedKmh ?: 0.0) >= 55.0) {
                "Strong surface winds (${current.windSpeedKmh?.toInt()} km/h)."
            } else null
            val visAdvisory = if ((current.visibilityMeters ?: 10000.0) < 1600.0) {
                "Low airfield visibility (<1.6 km)."
            } else null
            val combinedAdvisory = listOfNotNull(baseAdvisory, windAdvisory, visAdvisory)
                .joinToString(" ")
                .ifBlank { null }

            val report = WeatherReport(
                airportCode = key,
                temperatureC = current.temperatureC,
                conditionLabel = condition,
                weatherCode = current.weatherCode,
                windSpeedKmh = current.windSpeedKmh,
                windDirectionDeg = current.windDirectionDeg,
                visibilityMeters = current.visibilityMeters,
                humidityPercent = current.humidityPercent,
                rainMm = current.rainMm,
                advisoryWarning = combinedAdvisory,
                observedAt = current.time
            )
            cache[key] = CachedWeather(report, now)
            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
