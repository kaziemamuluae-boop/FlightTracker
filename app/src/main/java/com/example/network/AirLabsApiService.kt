package com.example.network

import com.example.model.AirLabsResponse
import com.example.model.AirlineDto
import com.example.model.AirportDto
import com.example.model.FleetAircraftDto
import com.example.model.FlightDetailDto
import com.example.model.FlightDto
import com.example.model.ListenResponseDto
import com.example.model.NearbyResponseDto
import com.example.model.RouteDto
import com.example.model.ScheduleDto
import com.example.model.SuggestResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AirLabsApiService {

    @GET("flights")
    suspend fun getLiveFlights(
        @Query("bbox") bbox: String? = null,
        @Query("zoom") zoom: Int? = null,
        @Query("airline_iata") airlineIata: String? = null,
        @Query("airline_icao") airlineIcao: String? = null,
        @Query("flight_iata") flightIata: String? = null,
        @Query("flight_icao") flightIcao: String? = null,
        @Query("dep_iata") depIata: String? = null,
        @Query("arr_iata") arrIata: String? = null,
        @Query("reg_number") regNumber: String? = null,
        @Query("flag") flag: String? = null,
        @Query("_fields") fields: String = DEFAULT_FLIGHT_FIELDS
    ): Response<AirLabsResponse<List<FlightDto>>>

    @GET("flight")
    suspend fun getFlightDetails(
        @Query("flight_iata") flightIata: String? = null,
        @Query("flight_icao") flightIcao: String? = null
    ): Response<AirLabsResponse<FlightDetailDto>>

    @GET("schedules")
    suspend fun getSchedules(
        @Query("dep_iata") depIata: String? = null,
        @Query("dep_icao") depIcao: String? = null,
        @Query("arr_iata") arrIata: String? = null,
        @Query("arr_icao") arrIcao: String? = null,
        @Query("airline_iata") airlineIata: String? = null,
        @Query("flight_iata") flightIata: String? = null,
        @Query("flight_icao") flightIcao: String? = null,
        @Query("_fields") fields: String = DEFAULT_SCHEDULE_FIELDS
    ): Response<AirLabsResponse<List<ScheduleDto>>>

    @GET("airports")
    suspend fun getAirports(
        @Query("iata_code") iataCode: String? = null,
        @Query("icao_code") icaoCode: String? = null,
        @Query("city_code") cityCode: String? = null,
        @Query("country_code") countryCode: String? = null,
        @Query("_fields") fields: String = DEFAULT_AIRPORT_FIELDS
    ): Response<AirLabsResponse<List<AirportDto>>>

    @GET("airlines")
    suspend fun getAirlines(
        @Query("iata_code") iataCode: String? = null,
        @Query("icao_code") icaoCode: String? = null,
        @Query("country_code") countryCode: String? = null,
        @Query("_fields") fields: String = DEFAULT_AIRLINE_FIELDS
    ): Response<AirLabsResponse<List<AirlineDto>>>

    @GET("fleets")
    suspend fun getFleets(
        @Query("airline_iata") airlineIata: String? = null,
        @Query("airline_icao") airlineIcao: String? = null,
        @Query("reg_number") regNumber: String? = null,
        @Query("hex") hex: String? = null,
        @Query("icao") aircraftIcao: String? = null,
        @Query("_fields") fields: String = DEFAULT_FLEET_FIELDS
    ): Response<AirLabsResponse<List<FleetAircraftDto>>>

    @GET("routes")
    suspend fun getRoutes(
        @Query("airline_iata") airlineIata: String? = null,
        @Query("airline_icao") airlineIcao: String? = null,
        @Query("dep_iata") depIata: String? = null,
        @Query("arr_iata") arrIata: String? = null,
        @Query("flight_iata") flightIata: String? = null
    ): Response<AirLabsResponse<List<RouteDto>>>

    @GET("nearby")
    suspend fun getNearby(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("distance") distanceKm: Int = 150
    ): Response<AirLabsResponse<NearbyResponseDto>>

    @GET("suggest")
    suspend fun getSuggestions(
        @Query("q") query: String
    ): Response<AirLabsResponse<SuggestResponseDto>>

    @GET("listen")
    suspend fun registerFlightAlertWebhook(
        @Query("webhook_url") webhookUrl: String,
        @Query("flight_iata") flightIata: String? = null,
        @Query("flight_icao") flightIcao: String? = null,
        @Query("_fields") fields: String = "dep_terminal,dep_gate,dep_estimated,arr_terminal,arr_gate,status,dep_delayed,arr_delayed"
    ): Response<AirLabsResponse<ListenResponseDto>>

    companion object {
        const val DEFAULT_BASE_URL = "https://airlabs.co/api/v9/"

        const val DEFAULT_FLIGHT_FIELDS =
            "hex,reg_number,flag,lat,lng,alt,dir,speed,v_speed,squawk,flight_number,flight_icao,flight_iata,airline_icao,airline_iata,aircraft_icao,dep_icao,dep_iata,arr_icao,arr_iata,updated,status"

        const val DEFAULT_SCHEDULE_FIELDS =
            "airline_iata,airline_icao,flight_iata,flight_icao,flight_number,dep_iata,dep_icao,dep_terminal,dep_gate,dep_time,dep_time_utc,dep_estimated,dep_estimated_utc,dep_actual,dep_actual_utc,arr_iata,arr_icao,arr_terminal,arr_gate,arr_baggage,arr_time,arr_time_utc,arr_estimated,arr_estimated_utc,arr_actual,arr_actual_utc,cs_airline_iata,cs_flight_iata,status,duration,delayed,dep_delayed,arr_delayed"

        const val DEFAULT_AIRPORT_FIELDS =
            "name,iata_code,icao_code,lat,lng,city,city_code,country_code,timezone,elevation,gmt,hub"

        const val DEFAULT_AIRLINE_FIELDS =
            "name,iata_code,icao_code,callsign,country_code,is_scheduled,is_passenger,is_cargo,total_aircrafts,average_fleet_age"

        const val DEFAULT_FLEET_FIELDS =
            "hex,reg_number,flag,airline_iata,airline_icao,iata,icao,model,manufacturer,engine,engine_count,built,age,msn"
    }
}
