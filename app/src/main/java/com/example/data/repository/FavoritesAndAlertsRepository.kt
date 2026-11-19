package com.example.data.repository

import com.example.data.local.AlertsAndPlaybackDao
import com.example.data.local.FavoriteAircraftEntity
import com.example.data.local.FavoriteAirlineEntity
import com.example.data.local.FavoriteAirportEntity
import com.example.data.local.FavoriteFlightEntity
import com.example.data.local.FavoritesDao
import com.example.data.local.FlightAlertEntity
import com.example.data.local.HistoryDao
import com.example.data.local.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

class FavoritesAndAlertsRepository(
    private val favoritesDao: FavoritesDao,
    private val historyDao: HistoryDao,
    private val alertsDao: AlertsAndPlaybackDao
) {
    val favoriteFlights: Flow<List<FavoriteFlightEntity>> = favoritesDao.observeFavoriteFlights()
    val favoriteAirports: Flow<List<FavoriteAirportEntity>> = favoritesDao.observeFavoriteAirports()
    val favoriteAirlines: Flow<List<FavoriteAirlineEntity>> = favoritesDao.observeFavoriteAirlines()
    val favoriteAircraft: Flow<List<FavoriteAircraftEntity>> = favoritesDao.observeFavoriteAircraft()
    val history: Flow<List<SearchHistoryEntity>> = historyDao.observeHistory()
    val alerts: Flow<List<FlightAlertEntity>> = alertsDao.observeAlerts()

    fun isFlightFavorite(code: String): Flow<Boolean> = favoritesDao.isFlightFavorite(code.uppercase())
    fun isAirportFavorite(code: String): Flow<Boolean> = favoritesDao.isAirportFavorite(code.uppercase())
    fun isAirlineFavorite(code: String): Flow<Boolean> = favoritesDao.isAirlineFavorite(code.uppercase())
    fun isAircraftFavorite(reg: String): Flow<Boolean> = favoritesDao.isAircraftFavorite(reg.uppercase())
    fun observeAlertForFlight(code: String): Flow<FlightAlertEntity?> = alertsDao.observeAlertForFlight(code.uppercase())

    suspend fun toggleFavoriteFlight(item: FavoriteFlightEntity, currentlyFav: Boolean) {
        if (currentlyFav) favoritesDao.deleteFavoriteFlight(item.flightCode.uppercase())
        else favoritesDao.insertFavoriteFlight(item.copy(flightCode = item.flightCode.uppercase()))
    }

    suspend fun removeFavoriteFlight(code: String) = favoritesDao.deleteFavoriteFlight(code.uppercase())

    suspend fun toggleFavoriteAirport(item: FavoriteAirportEntity, currentlyFav: Boolean) {
        if (currentlyFav) favoritesDao.deleteFavoriteAirport(item.iataOrIcao.uppercase())
        else favoritesDao.insertFavoriteAirport(item.copy(iataOrIcao = item.iataOrIcao.uppercase()))
    }

    suspend fun removeFavoriteAirport(code: String) = favoritesDao.deleteFavoriteAirport(code.uppercase())

    suspend fun toggleFavoriteAirline(item: FavoriteAirlineEntity, currentlyFav: Boolean) {
        if (currentlyFav) favoritesDao.deleteFavoriteAirline(item.code.uppercase())
        else favoritesDao.insertFavoriteAirline(item.copy(code = item.code.uppercase()))
    }

    suspend fun removeFavoriteAirline(code: String) = favoritesDao.deleteFavoriteAirline(code.uppercase())

    suspend fun toggleFavoriteAircraft(item: FavoriteAircraftEntity, currentlyFav: Boolean) {
        if (currentlyFav) favoritesDao.deleteFavoriteAircraft(item.regNumber.uppercase())
        else favoritesDao.insertFavoriteAircraft(item.copy(regNumber = item.regNumber.uppercase()))
    }

    suspend fun removeFavoriteAircraft(reg: String) = favoritesDao.deleteFavoriteAircraft(reg.uppercase())

    suspend fun recordHistory(query: String, title: String, subtitle: String, category: String) {
        val key = "$category:${query.trim().uppercase()}"
        historyDao.upsertHistory(
            SearchHistoryEntity(
                id = key,
                query = query.trim().uppercase(),
                title = title,
                subtitle = subtitle,
                category = category,
                timestampEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeHistoryItem(id: String) = historyDao.deleteHistoryItem(id)
    suspend fun clearHistory() = historyDao.clearAllHistory()

    suspend fun saveFlightAlert(alert: FlightAlertEntity) =
        alertsDao.upsertAlert(alert.copy(flightCode = alert.flightCode.uppercase()))

    suspend fun deleteFlightAlert(flightCode: String) = alertsDao.deleteAlert(flightCode.uppercase())
}
