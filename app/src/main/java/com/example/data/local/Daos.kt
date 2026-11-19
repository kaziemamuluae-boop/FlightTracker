package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorite_flights ORDER BY addedAtEpochMs DESC")
    fun observeFavoriteFlights(): Flow<List<FavoriteFlightEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_flights WHERE flightCode = :code)")
    fun isFlightFavorite(code: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteFlight(item: FavoriteFlightEntity)

    @Query("DELETE FROM favorite_flights WHERE flightCode = :code")
    suspend fun deleteFavoriteFlight(code: String)

    @Query("SELECT * FROM favorite_airports ORDER BY addedAtEpochMs DESC")
    fun observeFavoriteAirports(): Flow<List<FavoriteAirportEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_airports WHERE iataOrIcao = :code)")
    fun isAirportFavorite(code: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteAirport(item: FavoriteAirportEntity)

    @Query("DELETE FROM favorite_airports WHERE iataOrIcao = :code")
    suspend fun deleteFavoriteAirport(code: String)

    @Query("SELECT * FROM favorite_airlines ORDER BY addedAtEpochMs DESC")
    fun observeFavoriteAirlines(): Flow<List<FavoriteAirlineEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_airlines WHERE code = :code)")
    fun isAirlineFavorite(code: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteAirline(item: FavoriteAirlineEntity)

    @Query("DELETE FROM favorite_airlines WHERE code = :code")
    suspend fun deleteFavoriteAirline(code: String)

    @Query("SELECT * FROM favorite_aircraft ORDER BY addedAtEpochMs DESC")
    fun observeFavoriteAircraft(): Flow<List<FavoriteAircraftEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_aircraft WHERE regNumber = :reg)")
    fun isAircraftFavorite(reg: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavoriteAircraft(item: FavoriteAircraftEntity)

    @Query("DELETE FROM favorite_aircraft WHERE regNumber = :reg")
    suspend fun deleteFavoriteAircraft(reg: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM search_history ORDER BY timestampEpochMs DESC LIMIT 50")
    fun observeHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHistory(item: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteHistoryItem(id: String)

    @Query("DELETE FROM search_history")
    suspend fun clearAllHistory()
}

@Dao
interface CacheDao {
    @Query("SELECT * FROM cached_airports WHERE code = :code LIMIT 1")
    suspend fun getAirportByCode(code: String): CachedAirportEntity?

    @Query(
        "SELECT * FROM cached_airports WHERE iataCode LIKE :q OR icaoCode LIKE :q OR name LIKE :q OR city LIKE :q LIMIT 30"
    )
    suspend fun searchCachedAirports(q: String): List<CachedAirportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAirports(items: List<CachedAirportEntity>)

    @Query("DELETE FROM cached_airports")
    suspend fun clearAirportCache()

    @Query("SELECT * FROM cached_airlines WHERE code = :code LIMIT 1")
    suspend fun getAirlineByCode(code: String): CachedAirlineEntity?

    @Query("SELECT * FROM cached_airlines WHERE iataCode LIKE :q OR icaoCode LIKE :q OR name LIKE :q LIMIT 30")
    suspend fun searchCachedAirlines(q: String): List<CachedAirlineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAirlines(items: List<CachedAirlineEntity>)

    @Query("DELETE FROM cached_airlines")
    suspend fun clearAirlineCache()

    @Query("SELECT COUNT(*) FROM cached_airports")
    suspend fun countCachedAirports(): Int

    @Query("SELECT COUNT(*) FROM cached_airlines")
    suspend fun countCachedAirlines(): Int
}

@Dao
interface AlertsAndPlaybackDao {
    @Query("SELECT * FROM flight_alerts ORDER BY createdAtEpochMs DESC")
    fun observeAlerts(): Flow<List<FlightAlertEntity>>

    @Query("SELECT * FROM flight_alerts WHERE enabled = 1")
    suspend fun getEnabledAlerts(): List<FlightAlertEntity>

    @Query("SELECT * FROM flight_alerts WHERE flightCode = :flightCode LIMIT 1")
    fun observeAlertForFlight(flightCode: String): Flow<FlightAlertEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlert(alert: FlightAlertEntity)

    @Query("DELETE FROM flight_alerts WHERE flightCode = :flightCode")
    suspend fun deleteAlert(flightCode: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSnapshot(snapshot: PositionSnapshotEntity)

    @Query("SELECT * FROM position_snapshots WHERE flightKey = :flightKey ORDER BY timestampEpochSec ASC")
    suspend fun getSnapshotsForFlight(flightKey: String): List<PositionSnapshotEntity>

    @Query("DELETE FROM position_snapshots WHERE timestampEpochSec < :olderThanEpochSec")
    suspend fun pruneOldSnapshots(olderThanEpochSec: Long)
}
