package com.example.model

enum class AppThemeMode { DARK, LIGHT, SYSTEM }
enum class DistanceUnit { KM, MI }
enum class SpeedUnit { KTS, KMH }
enum class AltitudeUnit { FT, M }
enum class TimeFormatMode { H24, H12 }
enum class TimezoneMode { AIRPORT_LOCAL, UTC }
enum class MapLayerStyle { DARK_RADAR, NORMAL, SATELLITE, TERRAIN, GLOBE_3D }

data class UserSettings(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val speedUnit: SpeedUnit = SpeedUnit.KTS,
    val altitudeUnit: AltitudeUnit = AltitudeUnit.FT,
    val timeFormat: TimeFormatMode = TimeFormatMode.H24,
    val timezoneMode: TimezoneMode = TimezoneMode.AIRPORT_LOCAL,
    val mapLayerStyle: MapLayerStyle = MapLayerStyle.DARK_RADAR,
    val autoRefreshSeconds: Int = 30,
    val clusteringEnabled: Boolean = true,
    val showFlightLabels: Boolean = true,
    val languageCode: String = "en",
    val notifyDeparture: Boolean = true,
    val notifyTakeoff: Boolean = true,
    val notifyArrival: Boolean = true,
    val notifyLanded: Boolean = true,
    val notifyDelayed: Boolean = true,
    val notifyCancelled: Boolean = true,
    val notifyGateChange: Boolean = true,
    val notifyTerminalChange: Boolean = true,
    val notifyStatusChange: Boolean = true
)

data class ApiConfiguration(
    val useGateway: Boolean = false,
    val apiKey: String = "",
    val gatewayBaseUrl: String = "",
    val gatewayBearerToken: String = "",
    val webhookUrl: String = "",
    val lastTestedAtEpochMs: Long? = null,
    val lastConnectionSuccess: Boolean? = null,
    val lastStatusMessage: String = "Not tested",
    val planType: String? = null,
    val limitsByHour: Int? = null,
    val limitsByMonth: Int? = null,
    val limitsTotal: Int? = null,
    val keyExpiresAt: String? = null
) {
    val isConfigured: Boolean
        get() = if (useGateway) gatewayBaseUrl.isNotBlank() else apiKey.isNotBlank()
}

sealed class AirLabsError(val title: String, val message: String) {
    data object MissingApiKey : AirLabsError(
        "API Key Missing",
        "No AirLabs API key or backend gateway is configured. Open API Configuration to connect."
    )
    data class InvalidApiKey(val detail: String) : AirLabsError("Invalid API Key", detail)
    data class ExpiredApiKey(val detail: String) : AirLabsError("Expired API Key", detail)
    data class RateLimitExceeded(val detail: String) : AirLabsError("Request Limit Exceeded", detail)
    data class InvalidParameters(val detail: String) : AirLabsError("Invalid Parameters", detail)
    data class NotFound(val detail: String) : AirLabsError("No Data Found", detail)
    data class NetworkError(val detail: String) : AirLabsError("Network Error", detail)
    data class ServerError(val detail: String) : AirLabsError("Server Error", detail)
    data class ParseError(val detail: String) : AirLabsError("Data Parsing Error", detail)
}

sealed class ApiResult<out T> {
    data class Success<T>(
        val data: T,
        val meta: AirLabsRequestMeta? = null,
        val fromCache: Boolean = false,
        val fetchedAtEpochMs: Long = System.currentTimeMillis()
    ) : ApiResult<T>()

    data class Failure(val error: AirLabsError) : ApiResult<Nothing>()
}

enum class FlightFilter {
    ALL, IN_AIR, SCHEDULED, LANDED, DELAYED, CANCELLED, DOMESTIC, INTERNATIONAL
}

data class SuggestEntry(
    val title: String,
    val subtitle: String,
    val code: String,
    val kind: SuggestKind,
    val lat: Double? = null,
    val lng: Double? = null
)

enum class SuggestKind { AIRPORT, AIRLINE, CITY, FLIGHT }

data class PlaybackPoint(
    val lat: Double,
    val lng: Double,
    val altMeters: Double?,
    val speedKmh: Double?,
    val headingDeg: Double?,
    val timestampEpochSec: Long
)
