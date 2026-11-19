package com.example.utils

import com.example.model.AltitudeUnit
import com.example.model.DistanceUnit
import com.example.model.FlightDto
import com.example.model.FlightFilter
import com.example.model.SpeedUnit
import com.example.model.TimeFormatMode
import com.example.model.TimezoneMode
import com.example.model.UserSettings
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object AviationFormatters {

    fun formatAltitude(altMeters: Double?, unit: AltitudeUnit): String {
        if (altMeters == null) return "N/A"
        return when (unit) {
            AltitudeUnit.FT -> "${String.format(Locale.US, "%,d", (altMeters * 3.28084).toInt())} ft"
            AltitudeUnit.M -> "${String.format(Locale.US, "%,d", altMeters.toInt())} m"
        }
    }

    fun formatSpeed(speedKmh: Double?, unit: SpeedUnit): String {
        if (speedKmh == null) return "N/A"
        return when (unit) {
            SpeedUnit.KTS -> "${(speedKmh * 0.539957).toInt()} kts"
            SpeedUnit.KMH -> "${speedKmh.toInt()} km/h"
        }
    }

    fun formatDistance(distanceKm: Double?, unit: DistanceUnit): String {
        if (distanceKm == null) return "N/A"
        return when (unit) {
            DistanceUnit.KM -> "${String.format(Locale.US, "%,.0f", distanceKm)} km"
            DistanceUnit.MI -> "${String.format(Locale.US, "%,.0f", distanceKm * 0.621371)} mi"
        }
    }

    fun formatHeading(dir: Double?): String {
        if (dir == null) return "N/A"
        val normalized = ((dir % 360.0) + 360.0) % 360.0
        val cardinal = when {
            normalized >= 337.5 || normalized < 22.5 -> "N"
            normalized < 67.5 -> "NE"
            normalized < 112.5 -> "E"
            normalized < 157.5 -> "SE"
            normalized < 202.5 -> "S"
            normalized < 247.5 -> "SW"
            normalized < 292.5 -> "W"
            else -> "NW"
        }
        return "${normalized.toInt()}° $cardinal"
    }

    fun formatCoordinates(lat: Double?, lng: Double?): String {
        if (lat == null || lng == null) return "Unavailable"
        val latDir = if (lat >= 0) "N" else "S"
        val lngDir = if (lng >= 0) "E" else "W"
        return String.format(
            Locale.US,
            "%.3f°%s, %.3f°%s",
            kotlin.math.abs(lat), latDir,
            kotlin.math.abs(lng), lngDir
        )
    }

    fun formatTimestamp(epochSeconds: Long?, settings: UserSettings): String {
        if (epochSeconds == null || epochSeconds <= 0L) return "Unknown"
        val zone = if (settings.timezoneMode == TimezoneMode.UTC) ZoneOffset.UTC else ZoneId.systemDefault()
        val pattern = if (settings.timeFormat == TimeFormatMode.H24) "HH:mm:ss" else "hh:mm:ss a"
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.US)
        val dt = Instant.ofEpochSecond(epochSeconds).atZone(zone)
        val suffix = if (settings.timezoneMode == TimezoneMode.UTC) " UTC" else ""
        return dt.format(formatter) + suffix
    }

    fun formatTimestampShortMs(epochMs: Long?, settings: UserSettings): String {
        if (epochMs == null || epochMs <= 0L) return "Never"
        val zone = if (settings.timezoneMode == TimezoneMode.UTC) ZoneOffset.UTC else ZoneId.systemDefault()
        val pattern = if (settings.timeFormat == TimeFormatMode.H24) "HH:mm" else "hh:mm a"
        val dt = Instant.ofEpochMilli(epochMs).atZone(zone)
        return dt.format(DateTimeFormatter.ofPattern(pattern, Locale.US))
    }

    fun formatScheduleTime(localTime: String?, utcTime: String?, settings: UserSettings): String {
        val raw = if (settings.timezoneMode == TimezoneMode.UTC) {
            utcTime ?: localTime
        } else {
            localTime ?: utcTime
        } ?: return "—"

        val suffix = if (settings.timezoneMode == TimezoneMode.UTC && utcTime != null) " UTC" else ""
        return try {
            val parsed = LocalDateTime.parse(raw.take(16), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            val pattern = if (settings.timeFormat == TimeFormatMode.H24) "HH:mm" else "hh:mm a"
            parsed.format(DateTimeFormatter.ofPattern(pattern, Locale.US)) + suffix
        } catch (_: Exception) {
            raw
        }
    }

    fun formatDuration(minutes: Int?): String {
        if (minutes == null || minutes <= 0) return "—"
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    fun isPositionStale(updatedEpochSec: Long?, thresholdSeconds: Long = 300): Boolean {
        if (updatedEpochSec == null || updatedEpochSec <= 0L) return true
        val nowSec = System.currentTimeMillis() / 1000L
        return (nowSec - updatedEpochSec) > thresholdSeconds
    }

    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Generates intermediate geodesic coordinates along the great circle between two points.
     */
    fun greatCircleArc(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
        steps: Int = 36
    ): List<Pair<Double, Double>> {
        val phi1 = Math.toRadians(lat1)
        val lam1 = Math.toRadians(lon1)
        val phi2 = Math.toRadians(lat2)
        val lam2 = Math.toRadians(lon2)
        val d = 2 * kotlin.math.asin(
            sqrt(
                sin((phi2 - phi1) / 2).pow(2) +
                    cos(phi1) * cos(phi2) * sin((lam2 - lam1) / 2).pow(2)
            )
        )
        if (d < 1e-6) return listOf(lat1 to lon1, lat2 to lon2)
        val points = ArrayList<Pair<Double, Double>>(steps + 1)
        for (i in 0..steps) {
            val f = i.toDouble() / steps
            val a = sin((1 - f) * d) / sin(d)
            val b = sin(f * d) / sin(d)
            val x = a * cos(phi1) * cos(lam1) + b * cos(phi2) * cos(lam2)
            val y = a * cos(phi1) * sin(lam1) + b * cos(phi2) * sin(lam2)
            val z = a * sin(phi1) + b * sin(phi2)
            val lat = Math.toDegrees(atan2(z, sqrt(x * x + y * y)))
            val lon = Math.toDegrees(atan2(y, x))
            points.add(lat to lon)
        }
        return points
    }

    fun wmoWeatherDescription(code: Int?): Pair<String, String?> {
        return when (code) {
            0 -> "Clear Sky" to null
            1, 2 -> "Partly Cloudy" to null
            3 -> "Overcast" to null
            45, 48 -> "Fog / Mist" to "Low visibility conditions reported near airfield."
            51, 53, 55 -> "Drizzle" to null
            61, 63 -> "Rain" to "Wet runway advisory."
            65 -> "Heavy Rain" to "Heavy precipitation may reduce visibility and braking action."
            71, 73, 75, 77 -> "Snow" to "Winter weather / de-icing operations possible."
            80, 81, 82 -> "Rain Showers" to "Convective showers in vicinity."
            95, 96, 99 -> "Thunderstorm" to "Severe thunderstorm warning in airport area."
            else -> "Unavailable" to null
        }
    }

    fun statusDisplay(status: String?): String {
        return when (status?.lowercase(Locale.US)) {
            "en-route", "active" -> "In Air"
            "scheduled" -> "Scheduled"
            "landed" -> "Landed"
            "delayed" -> "Delayed"
            "cancelled", "canceled" -> "Cancelled"
            else -> status?.replaceFirstChar { it.uppercaseChar() } ?: "Unknown"
        }
    }

    fun matchesFilter(flight: FlightDto, filter: FlightFilter): Boolean {
        val status = flight.status?.lowercase(Locale.US).orEmpty()
        return when (filter) {
            FlightFilter.ALL -> true
            FlightFilter.IN_AIR -> status == "en-route" || status == "active" || (flight.altMeters ?: 0.0) > 100.0
            FlightFilter.SCHEDULED -> status == "scheduled"
            FlightFilter.LANDED -> status == "landed"
            FlightFilter.DELAYED -> status == "delayed"
            FlightFilter.CANCELLED -> status == "cancelled" || status == "canceled"
            FlightFilter.DOMESTIC -> {
                val depCountry = icaoCountryPrefix(flight.depIcao)
                val arrCountry = icaoCountryPrefix(flight.arrIcao)
                depCountry != null && arrCountry != null && depCountry == arrCountry
            }
            FlightFilter.INTERNATIONAL -> {
                val depCountry = icaoCountryPrefix(flight.depIcao)
                val arrCountry = icaoCountryPrefix(flight.arrIcao)
                depCountry != null && arrCountry != null && depCountry != arrCountry
            }
        }
    }

    private fun icaoCountryPrefix(icao: String?): String? {
        if (icao.isNullOrBlank()) return null
        val upper = icao.trim().uppercase(Locale.US)
        return when (upper.firstOrNull()) {
            'K', 'C', 'Y', 'Z', 'U' -> upper.take(1)
            else -> upper.take(2)
        }
    }
}
