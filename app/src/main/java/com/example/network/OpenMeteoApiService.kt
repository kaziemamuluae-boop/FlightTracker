package com.example.network

import com.example.model.OpenMeteoResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApiService {
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String =
            "temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m,visibility,rain,weather_code",
        @Query("timezone") timezone: String = "auto"
    ): Response<OpenMeteoResponse>

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"
    }
}
