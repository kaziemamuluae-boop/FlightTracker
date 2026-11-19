package com.example.ui

import com.example.model.AirLabsError
import com.example.model.AirlineDto
import com.example.model.AirportDto
import com.example.model.FleetAircraftDto
import com.example.model.FlightDetailDto
import com.example.model.FlightDto
import com.example.model.FlightFilter
import com.example.model.NearbyAirportDto
import com.example.model.PlaybackPoint
import com.example.model.RouteDto
import com.example.model.ScheduleDto
import com.example.model.SuggestEntry
import com.example.model.WeatherReport

data class HomeUiState(
    val isLoading: Boolean = false,
    val liveFlightsSample: List<FlightDto> = emptyList(),
    val nearbyAirports: List<NearbyAirportDto> = emptyList(),
    val userLat: Double? = null,
    val userLng: Double? = null,
    val lastUpdatedMs: Long? = null,
    val error: AirLabsError? = null
)

data class LiveMapUiState(
    val isLoading: Boolean = false,
    val flights: List<FlightDto> = emptyList(),
    val selectedFlight: FlightDto? = null,
    val selectedRouteDep: AirportDto? = null,
    val selectedRouteArr: AirportDto? = null,
    val playbackPoints: List<PlaybackPoint> = emptyList(),
    val isPlayingBack: Boolean = false,
    val playbackIndex: Int = 0,
    val userLat: Double? = null,
    val userLng: Double? = null,
    val nearbyOnly: Boolean = false,
    val searchQuery: String = "",
    val focusLatLng: Pair<Double, Double>? = null,
    val multiTrackCodes: Set<String> = emptySet(),
    val isAutoRefreshEnabled: Boolean = true,
    val lastUpdatedMs: Long? = null,
    val error: AirLabsError? = null
)

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val suggestions: List<SuggestEntry> = emptyList(),
    val flightResults: List<FlightDto> = emptyList(),
    val activeFilter: FlightFilter = FlightFilter.ALL,
    val error: AirLabsError? = null
)

data class FlightDetailUiState(
    val flightCode: String = "",
    val isLoading: Boolean = false,
    val detail: FlightDetailDto? = null,
    val depAirport: AirportDto? = null,
    val arrAirport: AirportDto? = null,
    val depWeather: WeatherReport? = null,
    val arrWeather: WeatherReport? = null,
    val playbackPoints: List<PlaybackPoint> = emptyList(),
    val error: AirLabsError? = null
)

data class AirportUiState(
    val airportCode: String = "",
    val isLoading: Boolean = false,
    val airport: AirportDto? = null,
    val weather: WeatherReport? = null,
    val departures: List<ScheduleDto> = emptyList(),
    val arrivals: List<ScheduleDto> = emptyList(),
    val nearbyAirports: List<NearbyAirportDto> = emptyList(),
    val boardFilter: String = "",
    val showDelayedOnly: Boolean = false,
    val showCancelledOnly: Boolean = false,
    val lastUpdatedMs: Long? = null,
    val error: AirLabsError? = null
)

data class AirlineUiState(
    val airlineCode: String = "",
    val isLoading: Boolean = false,
    val airline: AirlineDto? = null,
    val fleet: List<FleetAircraftDto> = emptyList(),
    val routes: List<RouteDto> = emptyList(),
    val liveFlights: List<FlightDto> = emptyList(),
    val error: AirLabsError? = null
)

data class AircraftUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val results: List<FleetAircraftDto> = emptyList(),
    val livePosition: FlightDto? = null,
    val historySnapshots: List<PlaybackPoint> = emptyList(),
    val error: AirLabsError? = null
)

data class PopularAirportPreset(
    val iata: String,
    val name: String,
    val city: String,
    val country: String
)

val POPULAR_AIRPORTS = listOf(
    PopularAirportPreset("JFK", "John F. Kennedy Intl", "New York", "US"),
    PopularAirportPreset("LHR", "London Heathrow", "London", "GB"),
    PopularAirportPreset("DXB", "Dubai International", "Dubai", "AE"),
    PopularAirportPreset("SIN", "Singapore Changi", "Singapore", "SG"),
    PopularAirportPreset("HND", "Tokyo Haneda", "Tokyo", "JP"),
    PopularAirportPreset("FRA", "Frankfurt Airport", "Frankfurt", "DE")
)
