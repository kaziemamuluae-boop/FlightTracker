package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_flights")
data class FavoriteFlightEntity(
    @PrimaryKey val flightCode: String, // flight_iata or flight_icao
    val flightIata: String?,
    val flightIcao: String?,
    val airlineName: String?,
    val airlineIata: String?,
    val depIata: String?,
    val arrIata: String?,
    val lastStatus: String?,
    val regNumber: String?,
    val addedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_airports")
data class FavoriteAirportEntity(
    @PrimaryKey val iataOrIcao: String,
    val name: String,
    val iataCode: String?,
    val icaoCode: String?,
    val city: String?,
    val countryCode: String?,
    val lat: Double?,
    val lng: Double?,
    val timezone: String?,
    val addedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_airlines")
data class FavoriteAirlineEntity(
    @PrimaryKey val code: String,
    val name: String,
    val iataCode: String?,
    val icaoCode: String?,
    val countryCode: String?,
    val totalAircrafts: Int?,
    val addedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_aircraft")
data class FavoriteAircraftEntity(
    @PrimaryKey val regNumber: String,
    val hex: String?,
    val model: String?,
    val manufacturer: String?,
    val aircraftIcao: String?,
    val airlineIata: String?,
    val builtYear: Int?,
    val addedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val id: String, // e.g. "FLIGHT:AA6" or "AIRPORT:JFK"
    val query: String,
    val title: String,
    val subtitle: String,
    val category: String, // FLIGHT, AIRPORT, AIRLINE, AIRCRAFT, QUERY
    val timestampEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_airports")
data class CachedAirportEntity(
    @PrimaryKey val code: String,
    val name: String,
    val iataCode: String?,
    val icaoCode: String?,
    val city: String?,
    val countryCode: String?,
    val lat: Double?,
    val lng: Double?,
    val timezone: String?,
    val elevation: Int?,
    val cachedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_airlines")
data class CachedAirlineEntity(
    @PrimaryKey val code: String,
    val name: String,
    val iataCode: String?,
    val icaoCode: String?,
    val callsign: String?,
    val countryCode: String?,
    val isScheduled: Int?,
    val isPassenger: Int?,
    val isCargo: Int?,
    val totalAircrafts: Int?,
    val averageFleetAge: Double?,
    val cachedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "flight_alerts")
data class FlightAlertEntity(
    @PrimaryKey val flightCode: String,
    val airlineName: String?,
    val depIata: String?,
    val arrIata: String?,
    val enabled: Boolean = true,
    val notifyDeparture: Boolean = true,
    val notifyTakeoff: Boolean = true,
    val notifyArrival: Boolean = true,
    val notifyLanded: Boolean = true,
    val notifyDelayed: Boolean = true,
    val notifyCancelled: Boolean = true,
    val notifyGateChange: Boolean = true,
    val notifyTerminalChange: Boolean = true,
    val notifyStatusChange: Boolean = true,
    val lastStatus: String? = null,
    val lastDepGate: String? = null,
    val lastDepTerminal: String? = null,
    val lastArrGate: String? = null,
    val lastArrTerminal: String? = null,
    val lastDelayedMin: Int? = null,
    val lastNotifiedHash: String? = null,
    val webhookListenerId: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "position_snapshots",
    indices = [Index(value = ["flightKey", "timestampEpochSec"], unique = true)]
)
data class PositionSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val flightKey: String,
    val lat: Double,
    val lng: Double,
    val altMeters: Double?,
    val speedKmh: Double?,
    val headingDeg: Double?,
    val timestampEpochSec: Long
)
