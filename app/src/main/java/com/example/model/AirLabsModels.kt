package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AirLabsResponse<T>(
    @Json(name = "request") val request: AirLabsRequestMeta? = null,
    @Json(name = "response") val response: T? = null,
    @Json(name = "error") val error: AirLabsErrorDto? = null
)

@JsonClass(generateAdapter = true)
data class AirLabsErrorDto(
    @Json(name = "message") val message: String? = null,
    @Json(name = "code") val code: String? = null
)

@JsonClass(generateAdapter = true)
data class AirLabsRequestMeta(
    @Json(name = "lang") val lang: String? = null,
    @Json(name = "currency") val currency: String? = null,
    @Json(name = "time") val time: Double? = null,
    @Json(name = "id") val id: String? = null,
    @Json(name = "server") val server: String? = null,
    @Json(name = "host") val host: String? = null,
    @Json(name = "pid") val pid: Int? = null,
    @Json(name = "key") val key: AirLabsKeyMeta? = null,
    @Json(name = "params") val params: Map<String, Any?>? = null,
    @Json(name = "version") val version: Int? = null,
    @Json(name = "method") val method: String? = null
)

@JsonClass(generateAdapter = true)
data class AirLabsKeyMeta(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "api_key") val apiKey: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "expired") val expired: String? = null,
    @Json(name = "registered") val registered: String? = null,
    @Json(name = "limits_by_hour") val limitsByHour: Int? = null,
    @Json(name = "limits_by_minute") val limitsByMinute: Int? = null,
    @Json(name = "limits_by_month") val limitsByMonth: Int? = null,
    @Json(name = "limits_total") val limitsTotal: Int? = null
)

@JsonClass(generateAdapter = true)
data class FlightDto(
    @Json(name = "hex") val hex: String? = null,
    @Json(name = "reg_number") val regNumber: String? = null,
    @Json(name = "flag") val flag: String? = null,
    @Json(name = "lat") val lat: Double? = null,
    @Json(name = "lng") val lng: Double? = null,
    @Json(name = "alt") val altMeters: Double? = null,
    @Json(name = "dir") val dir: Double? = null,
    @Json(name = "speed") val speedKmh: Double? = null,
    @Json(name = "v_speed") val vSpeed: Double? = null,
    @Json(name = "squawk") val squawk: String? = null,
    @Json(name = "flight_number") val flightNumber: String? = null,
    @Json(name = "flight_icao") val flightIcao: String? = null,
    @Json(name = "flight_iata") val flightIata: String? = null,
    @Json(name = "airline_icao") val airlineIcao: String? = null,
    @Json(name = "airline_iata") val airlineIata: String? = null,
    @Json(name = "aircraft_icao") val aircraftIcao: String? = null,
    @Json(name = "dep_icao") val depIcao: String? = null,
    @Json(name = "dep_iata") val depIata: String? = null,
    @Json(name = "arr_icao") val arrIcao: String? = null,
    @Json(name = "arr_iata") val arrIata: String? = null,
    @Json(name = "updated") val updated: Long? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "type") val type: String? = null
)

@JsonClass(generateAdapter = true)
data class FlightDetailDto(
    @Json(name = "hex") val hex: String? = null,
    @Json(name = "reg_number") val regNumber: String? = null,
    @Json(name = "flag") val flag: String? = null,
    @Json(name = "lat") val lat: Double? = null,
    @Json(name = "lng") val lng: Double? = null,
    @Json(name = "alt") val altMeters: Double? = null,
    @Json(name = "dir") val dir: Double? = null,
    @Json(name = "speed") val speedKmh: Double? = null,
    @Json(name = "v_speed") val vSpeed: Double? = null,
    @Json(name = "squawk") val squawk: String? = null,
    @Json(name = "airline_iata") val airlineIata: String? = null,
    @Json(name = "airline_icao") val airlineIcao: String? = null,
    @Json(name = "airline_name") val airlineName: String? = null,
    @Json(name = "flight_iata") val flightIata: String? = null,
    @Json(name = "flight_icao") val flightIcao: String? = null,
    @Json(name = "flight_number") val flightNumber: String? = null,
    @Json(name = "aircraft_icao") val aircraftIcao: String? = null,
    @Json(name = "model") val model: String? = null,
    @Json(name = "manufacturer") val manufacturer: String? = null,
    @Json(name = "built") val built: Int? = null,
    @Json(name = "age") val age: Int? = null,
    @Json(name = "engine") val engine: String? = null,
    @Json(name = "engine_count") val engineCount: String? = null,
    @Json(name = "dep_iata") val depIata: String? = null,
    @Json(name = "dep_icao") val depIcao: String? = null,
    @Json(name = "dep_name") val depName: String? = null,
    @Json(name = "dep_city") val depCity: String? = null,
    @Json(name = "dep_country") val depCountry: String? = null,
    @Json(name = "dep_terminal") val depTerminal: String? = null,
    @Json(name = "dep_gate") val depGate: String? = null,
    @Json(name = "dep_time") val depTime: String? = null,
    @Json(name = "dep_time_utc") val depTimeUtc: String? = null,
    @Json(name = "dep_estimated") val depEstimated: String? = null,
    @Json(name = "dep_estimated_utc") val depEstimatedUtc: String? = null,
    @Json(name = "dep_actual") val depActual: String? = null,
    @Json(name = "dep_actual_utc") val depActualUtc: String? = null,
    @Json(name = "dep_delayed") val depDelayed: Int? = null,
    @Json(name = "arr_iata") val arrIata: String? = null,
    @Json(name = "arr_icao") val arrIcao: String? = null,
    @Json(name = "arr_name") val arrName: String? = null,
    @Json(name = "arr_city") val arrCity: String? = null,
    @Json(name = "arr_country") val arrCountry: String? = null,
    @Json(name = "arr_terminal") val arrTerminal: String? = null,
    @Json(name = "arr_gate") val arrGate: String? = null,
    @Json(name = "arr_baggage") val arrBaggage: String? = null,
    @Json(name = "arr_time") val arrTime: String? = null,
    @Json(name = "arr_time_utc") val arrTimeUtc: String? = null,
    @Json(name = "arr_estimated") val arrEstimated: String? = null,
    @Json(name = "arr_estimated_utc") val arrEstimatedUtc: String? = null,
    @Json(name = "arr_actual") val arrActual: String? = null,
    @Json(name = "arr_actual_utc") val arrActualUtc: String? = null,
    @Json(name = "arr_delayed") val arrDelayed: Int? = null,
    @Json(name = "cs_airline_iata") val csAirlineIata: String? = null,
    @Json(name = "cs_flight_iata") val csFlightIata: String? = null,
    @Json(name = "cs_flight_number") val csFlightNumber: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "duration") val duration: Int? = null,
    @Json(name = "delayed") val delayed: Int? = null,
    @Json(name = "updated") val updated: Long? = null
)

@JsonClass(generateAdapter = true)
data class ScheduleDto(
    @Json(name = "airline_iata") val airlineIata: String? = null,
    @Json(name = "airline_icao") val airlineIcao: String? = null,
    @Json(name = "flight_iata") val flightIata: String? = null,
    @Json(name = "flight_icao") val flightIcao: String? = null,
    @Json(name = "flight_number") val flightNumber: String? = null,
    @Json(name = "dep_iata") val depIata: String? = null,
    @Json(name = "dep_icao") val depIcao: String? = null,
    @Json(name = "dep_terminal") val depTerminal: String? = null,
    @Json(name = "dep_gate") val depGate: String? = null,
    @Json(name = "dep_time") val depTime: String? = null,
    @Json(name = "dep_time_utc") val depTimeUtc: String? = null,
    @Json(name = "dep_estimated") val depEstimated: String? = null,
    @Json(name = "dep_estimated_utc") val depEstimatedUtc: String? = null,
    @Json(name = "dep_actual") val depActual: String? = null,
    @Json(name = "dep_actual_utc") val depActualUtc: String? = null,
    @Json(name = "arr_iata") val arrIata: String? = null,
    @Json(name = "arr_icao") val arrIcao: String? = null,
    @Json(name = "arr_terminal") val arrTerminal: String? = null,
    @Json(name = "arr_gate") val arrGate: String? = null,
    @Json(name = "arr_baggage") val arrBaggage: String? = null,
    @Json(name = "arr_time") val arrTime: String? = null,
    @Json(name = "arr_time_utc") val arrTimeUtc: String? = null,
    @Json(name = "arr_estimated") val arrEstimated: String? = null,
    @Json(name = "arr_estimated_utc") val arrEstimatedUtc: String? = null,
    @Json(name = "arr_actual") val arrActual: String? = null,
    @Json(name = "arr_actual_utc") val arrActualUtc: String? = null,
    @Json(name = "cs_airline_iata") val csAirlineIata: String? = null,
    @Json(name = "cs_flight_iata") val csFlightIata: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "duration") val duration: Int? = null,
    @Json(name = "delayed") val delayed: Int? = null,
    @Json(name = "dep_delayed") val depDelayed: Int? = null,
    @Json(name = "arr_delayed") val arrDelayed: Int? = null
)

@JsonClass(generateAdapter = true)
data class AirportDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "iata_code") val iataCode: String? = null,
    @Json(name = "icao_code") val icaoCode: String? = null,
    @Json(name = "lat") val lat: Double? = null,
    @Json(name = "lng") val lng: Double? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "city_code") val cityCode: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "elevation") val elevation: Int? = null,
    @Json(name = "gmt") val gmt: String? = null,
    @Json(name = "hub") val hub: Int? = null
)

@JsonClass(generateAdapter = true)
data class AirlineDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "iata_code") val iataCode: String? = null,
    @Json(name = "icao_code") val icaoCode: String? = null,
    @Json(name = "callsign") val callsign: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    @Json(name = "is_scheduled") val isScheduled: Int? = null,
    @Json(name = "is_passenger") val isPassenger: Int? = null,
    @Json(name = "is_cargo") val isCargo: Int? = null,
    @Json(name = "total_aircrafts") val totalAircrafts: Int? = null,
    @Json(name = "average_fleet_age") val averageFleetAge: Double? = null
)

@JsonClass(generateAdapter = true)
data class FleetAircraftDto(
    @Json(name = "hex") val hex: String? = null,
    @Json(name = "reg_number") val regNumber: String? = null,
    @Json(name = "flag") val flag: String? = null,
    @Json(name = "airline_iata") val airlineIata: String? = null,
    @Json(name = "airline_icao") val airlineIcao: String? = null,
    @Json(name = "iata") val iata: String? = null,
    @Json(name = "icao") val icao: String? = null,
    @Json(name = "model") val model: String? = null,
    @Json(name = "manufacturer") val manufacturer: String? = null,
    @Json(name = "engine") val engine: String? = null,
    @Json(name = "engine_count") val engineCount: String? = null,
    @Json(name = "built") val built: Int? = null,
    @Json(name = "age") val age: Int? = null,
    @Json(name = "msn") val msn: String? = null
)

@JsonClass(generateAdapter = true)
data class RouteDto(
    @Json(name = "airline_iata") val airlineIata: String? = null,
    @Json(name = "airline_icao") val airlineIcao: String? = null,
    @Json(name = "flight_number") val flightNumber: String? = null,
    @Json(name = "flight_iata") val flightIata: String? = null,
    @Json(name = "flight_icao") val flightIcao: String? = null,
    @Json(name = "dep_iata") val depIata: String? = null,
    @Json(name = "dep_icao") val depIcao: String? = null,
    @Json(name = "dep_time") val depTime: String? = null,
    @Json(name = "dep_time_utc") val depTimeUtc: String? = null,
    @Json(name = "arr_iata") val arrIata: String? = null,
    @Json(name = "arr_icao") val arrIcao: String? = null,
    @Json(name = "arr_time") val arrTime: String? = null,
    @Json(name = "arr_time_utc") val arrTimeUtc: String? = null,
    @Json(name = "duration") val duration: Int? = null,
    @Json(name = "days") val days: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class NearbyResponseDto(
    @Json(name = "airports") val airports: List<NearbyAirportDto>? = null,
    @Json(name = "cities") val cities: List<NearbyCityDto>? = null
)

@JsonClass(generateAdapter = true)
data class NearbyAirportDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "iata_code") val iataCode: String? = null,
    @Json(name = "icao_code") val icaoCode: String? = null,
    @Json(name = "lat") val lat: Double? = null,
    @Json(name = "lng") val lng: Double? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    @Json(name = "distance") val distanceKm: Double? = null,
    @Json(name = "popularity") val popularity: Int? = null
)

@JsonClass(generateAdapter = true)
data class NearbyCityDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "city_code") val cityCode: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    @Json(name = "lat") val lat: Double? = null,
    @Json(name = "lng") val lng: Double? = null,
    @Json(name = "distance") val distanceKm: Double? = null
)

@JsonClass(generateAdapter = true)
data class SuggestResponseDto(
    @Json(name = "airports") val airports: List<AirportDto>? = null,
    @Json(name = "airlines") val airlines: List<AirlineDto>? = null,
    @Json(name = "cities") val cities: List<NearbyCityDto>? = null,
    @Json(name = "countries") val countries: List<SuggestCountryDto>? = null
)

@JsonClass(generateAdapter = true)
data class SuggestCountryDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "code") val code: String? = null
)

@JsonClass(generateAdapter = true)
data class ListenResponseDto(
    @Json(name = "listener_id") val listenerId: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null
)
