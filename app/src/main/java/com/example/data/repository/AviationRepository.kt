package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.local.AlertsAndPlaybackDao
import com.example.data.local.CacheDao
import com.example.data.local.CachedAirlineEntity
import com.example.data.local.CachedAirportEntity
import com.example.data.local.PositionSnapshotEntity
import com.example.model.AirLabsError
import com.example.model.AirLabsResponse
import com.example.model.AirlineDto
import com.example.model.AirportDto
import com.example.model.ApiResult
import com.example.model.FleetAircraftDto
import com.example.model.FlightDetailDto
import com.example.model.FlightDto
import com.example.model.ListenResponseDto
import com.example.model.NearbyResponseDto
import com.example.model.PlaybackPoint
import com.example.model.RouteDto
import com.example.model.ScheduleDto
import com.example.model.SuggestEntry
import com.example.model.SuggestKind
import com.example.network.AirLabsApiService
import com.example.network.NetworkModule
import com.squareup.moshi.JsonDataException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class AviationRepository(
    private val context: Context,
    private val api: AirLabsApiService,
    private val cacheDao: CacheDao,
    private val playbackDao: AlertsAndPlaybackDao
) {
    private data class TimedEntry<T>(val value: T, val timestampMs: Long)

    private val flightCache = ConcurrentHashMap<String, TimedEntry<List<FlightDto>>>()
    private val scheduleCache = ConcurrentHashMap<String, TimedEntry<List<ScheduleDto>>>()

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun ensureConfigured(): AirLabsError? {
        return if (!NetworkModule.apiConfigRef.get().isConfigured) {
            AirLabsError.MissingApiKey
        } else null
    }

    private fun <T> mapResponse(res: Response<AirLabsResponse<T>>): ApiResult<T> {
        if (!res.isSuccessful) {
            val rawErr = runCatching { res.errorBody()?.string() }.getOrNull().orEmpty()
            val parsedErr = runCatching {
                val adapter = NetworkModule.moshi.adapter(AirLabsResponse::class.java)
                adapter.fromJson(rawErr)?.error
            }.getOrNull()
            val code = parsedErr?.code?.lowercase().orEmpty()
            val msg = parsedErr?.message ?: "HTTP ${res.code()} ${res.message()}"
            return ApiResult.Failure(
                when {
                    res.code() == 401 || code == "unknown_api_key" || code == "invalid_api_key" ->
                        AirLabsError.InvalidApiKey(msg)
                    code == "expired_api_key" || msg.contains("expired", ignoreCase = true) ->
                        AirLabsError.ExpiredApiKey(msg)
                    res.code() == 429 || code.contains("limit") || code.contains("quota") ->
                        AirLabsError.RateLimitExceeded(msg)
                    res.code() == 400 || code.contains("wrong_params") || code.contains("invalid") ->
                        AirLabsError.InvalidParameters(msg)
                    res.code() == 404 ->
                        AirLabsError.NotFound(msg)
                    else -> AirLabsError.ServerError(msg)
                }
            )
        }
        val body = res.body() ?: return ApiResult.Failure(AirLabsError.ParseError("Empty response body"))
        val err = body.error
        if (err != null) {
            val code = err.code?.lowercase().orEmpty()
            val msg = err.message ?: "AirLabs API error"
            return ApiResult.Failure(
                when {
                    code.contains("unknown_api_key") || code.contains("invalid_api_key") || code.contains("missing_api_key") ->
                        AirLabsError.InvalidApiKey(msg)
                    code.contains("expired") ->
                        AirLabsError.ExpiredApiKey(msg)
                    code.contains("limit") || code.contains("hour_limit") || code.contains("month_limit") ->
                        AirLabsError.RateLimitExceeded(msg)
                    code.contains("wrong_params") ->
                        AirLabsError.InvalidParameters(msg)
                    code.contains("not_found") ->
                        AirLabsError.NotFound(msg)
                    else -> AirLabsError.ServerError(msg)
                }
            )
        }
        val data = body.response ?: return ApiResult.Failure(AirLabsError.NotFound("No data returned"))
        return ApiResult.Success(data = data, meta = body.request)
    }

    private suspend fun <T> executeCall(block: suspend () -> Response<AirLabsResponse<T>>): ApiResult<T> {
        ensureConfigured()?.let { return ApiResult.Failure(it) }
        if (!isNetworkAvailable()) {
            return ApiResult.Failure(AirLabsError.NetworkError("No active internet connection."))
        }
        return withContext(Dispatchers.IO) {
            try {
                mapResponse(block())
            } catch (e: JsonDataException) {
                ApiResult.Failure(AirLabsError.ParseError(e.localizedMessage ?: "Invalid JSON format"))
            } catch (e: IOException) {
                ApiResult.Failure(AirLabsError.NetworkError(e.localizedMessage ?: "Network timeout"))
            } catch (e: Exception) {
                ApiResult.Failure(AirLabsError.ServerError(e.localizedMessage ?: "Unexpected error"))
            }
        }
    }

    suspend fun testConnection(): ApiResult<List<AirlineDto>> {
        return executeCall {
            api.getAirlines(iataCode = "AA")
        }
    }

    suspend fun getLiveFlights(
        bbox: String? = null,
        zoom: Int? = null,
        airlineIata: String? = null,
        airlineIcao: String? = null,
        flightIata: String? = null,
        flightIcao: String? = null,
        depIata: String? = null,
        arrIata: String? = null,
        regNumber: String? = null,
        forceRefresh: Boolean = false
    ): ApiResult<List<FlightDto>> {
        val cacheKey = listOf(bbox, zoom, airlineIata, airlineIcao, flightIata, flightIcao, depIata, arrIata, regNumber)
            .joinToString("|")
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            flightCache[cacheKey]?.let { entry ->
                if (now - entry.timestampMs < 12_000L) {
                    return ApiResult.Success(entry.value, fromCache = true, fetchedAtEpochMs = entry.timestampMs)
                }
            }
        }
        val result = executeCall {
            api.getLiveFlights(
                bbox = bbox,
                zoom = zoom,
                airlineIata = airlineIata,
                airlineIcao = airlineIcao,
                flightIata = flightIata,
                flightIcao = flightIcao,
                depIata = depIata,
                arrIata = arrIata,
                regNumber = regNumber
            )
        }
        if (result is ApiResult.Success) {
            flightCache[cacheKey] = TimedEntry(result.data, now)
            recordSnapshots(result.data)
        }
        return result
    }

    private suspend fun recordSnapshots(flights: List<FlightDto>) = withContext(Dispatchers.IO) {
        val sample = flights.take(60)
        val nowSec = System.currentTimeMillis() / 1000L
        for (f in sample) {
            val key = f.flightIata ?: f.flightIcao ?: f.hex ?: continue
            val lat = f.lat ?: continue
            val lng = f.lng ?: continue
            playbackDao.insertSnapshot(
                PositionSnapshotEntity(
                    flightKey = key.uppercase(),
                    lat = lat,
                    lng = lng,
                    altMeters = f.altMeters,
                    speedKmh = f.speedKmh,
                    headingDeg = f.dir,
                    timestampEpochSec = f.updated ?: nowSec
                )
            )
        }
        playbackDao.pruneOldSnapshots(nowSec - 7 * 24 * 3600L)
    }

    suspend fun getFlightPlayback(flightKey: String): List<PlaybackPoint> = withContext(Dispatchers.IO) {
        playbackDao.getSnapshotsForFlight(flightKey.uppercase()).map {
            PlaybackPoint(
                lat = it.lat,
                lng = it.lng,
                altMeters = it.altMeters,
                speedKmh = it.speedKmh,
                headingDeg = it.headingDeg,
                timestampEpochSec = it.timestampEpochSec
            )
        }
    }

    suspend fun getFlightDetails(code: String): ApiResult<FlightDetailDto> {
        val clean = code.trim().uppercase()
        if (clean.isBlank()) {
            return ApiResult.Failure(AirLabsError.InvalidParameters("Please enter a flight IATA or ICAO code."))
        }
        val isIcao = clean.length >= 4 && clean.take(3).all { it.isLetter() }
        val result = executeCall {
            if (isIcao) api.getFlightDetails(flightIcao = clean)
            else api.getFlightDetails(flightIata = clean)
        }
        // Fallback to opposite param if first returned NotFound
        val finalResult = if (result is ApiResult.Failure && result.error is AirLabsError.NotFound) {
            executeCall {
                if (isIcao) api.getFlightDetails(flightIata = clean)
                else api.getFlightDetails(flightIcao = clean)
            }
        } else {
            result
        }
        if (finalResult is ApiResult.Success) {
            val d = finalResult.data
            if (d.lat != null && d.lng != null) {
                withContext(Dispatchers.IO) {
                    playbackDao.insertSnapshot(
                        PositionSnapshotEntity(
                            flightKey = (d.flightIata ?: d.flightIcao ?: clean).uppercase(),
                            lat = d.lat,
                            lng = d.lng,
                            altMeters = d.altMeters,
                            speedKmh = d.speedKmh,
                            headingDeg = d.dir,
                            timestampEpochSec = d.updated ?: (System.currentTimeMillis() / 1000L)
                        )
                    )
                }
            }
        }
        return finalResult
    }

    suspend fun getAirportSchedules(
        airportIata: String,
        departures: Boolean,
        forceRefresh: Boolean = false
    ): ApiResult<List<ScheduleDto>> {
        val code = airportIata.trim().uppercase()
        val cacheKey = "${code}_${if (departures) "DEP" else "ARR"}"
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            scheduleCache[cacheKey]?.let { entry ->
                if (now - entry.timestampMs < 60_000L) {
                    return ApiResult.Success(entry.value, fromCache = true, fetchedAtEpochMs = entry.timestampMs)
                }
            }
        }
        val res = executeCall {
            if (departures) api.getSchedules(depIata = code) else api.getSchedules(arrIata = code)
        }
        if (res is ApiResult.Success) {
            scheduleCache[cacheKey] = TimedEntry(res.data, now)
        }
        return res
    }

    suspend fun getAirport(code: String): ApiResult<AirportDto> {
        val clean = code.trim().uppercase()
        val cached = withContext(Dispatchers.IO) { cacheDao.getAirportByCode(clean) }
        if (cached != null && System.currentTimeMillis() - cached.cachedAtEpochMs < 24 * 3600_000L) {
            return ApiResult.Success(
                AirportDto(
                    name = cached.name,
                    iataCode = cached.iataCode,
                    icaoCode = cached.icaoCode,
                    lat = cached.lat,
                    lng = cached.lng,
                    city = cached.city,
                    countryCode = cached.countryCode,
                    timezone = cached.timezone,
                    elevation = cached.elevation
                ),
                fromCache = true,
                fetchedAtEpochMs = cached.cachedAtEpochMs
            )
        }
        val res = executeCall {
            if (clean.length == 4) api.getAirports(icaoCode = clean)
            else api.getAirports(iataCode = clean)
        }
        return when (res) {
            is ApiResult.Success -> {
                val list = res.data
                withContext(Dispatchers.IO) {
                    cacheDao.insertAirports(
                        list.mapNotNull { dto ->
                            val key = dto.iataCode ?: dto.icaoCode ?: return@mapNotNull null
                            CachedAirportEntity(
                                code = key.uppercase(),
                                name = dto.name ?: key,
                                iataCode = dto.iataCode,
                                icaoCode = dto.icaoCode,
                                city = dto.city,
                                countryCode = dto.countryCode,
                                lat = dto.lat,
                                lng = dto.lng,
                                timezone = dto.timezone,
                                elevation = dto.elevation
                            )
                        }
                    )
                }
                val first = list.firstOrNull()
                if (first != null) ApiResult.Success(first, meta = res.meta)
                else ApiResult.Failure(AirLabsError.NotFound("Airport $clean not found"))
            }
            is ApiResult.Failure -> {
                if (cached != null) {
                    ApiResult.Success(
                        AirportDto(
                            name = cached.name,
                            iataCode = cached.iataCode,
                            icaoCode = cached.icaoCode,
                            lat = cached.lat,
                            lng = cached.lng,
                            city = cached.city,
                            countryCode = cached.countryCode,
                            timezone = cached.timezone,
                            elevation = cached.elevation
                        ),
                        fromCache = true,
                        fetchedAtEpochMs = cached.cachedAtEpochMs
                    )
                } else res
            }
        }
    }

    suspend fun getAirline(code: String): ApiResult<AirlineDto> {
        val clean = code.trim().uppercase()
        val cached = withContext(Dispatchers.IO) { cacheDao.getAirlineByCode(clean) }
        if (cached != null && System.currentTimeMillis() - cached.cachedAtEpochMs < 24 * 3600_000L) {
            return ApiResult.Success(
                AirlineDto(
                    name = cached.name,
                    iataCode = cached.iataCode,
                    icaoCode = cached.icaoCode,
                    callsign = cached.callsign,
                    countryCode = cached.countryCode,
                    isScheduled = cached.isScheduled,
                    isPassenger = cached.isPassenger,
                    isCargo = cached.isCargo,
                    totalAircrafts = cached.totalAircrafts,
                    averageFleetAge = cached.averageFleetAge
                ),
                fromCache = true,
                fetchedAtEpochMs = cached.cachedAtEpochMs
            )
        }
        val res = executeCall {
            if (clean.length == 3) api.getAirlines(icaoCode = clean)
            else api.getAirlines(iataCode = clean)
        }
        return when (res) {
            is ApiResult.Success -> {
                withContext(Dispatchers.IO) {
                    cacheDao.insertAirlines(
                        res.data.mapNotNull { dto ->
                            val key = dto.iataCode ?: dto.icaoCode ?: return@mapNotNull null
                            CachedAirlineEntity(
                                code = key.uppercase(),
                                name = dto.name ?: key,
                                iataCode = dto.iataCode,
                                icaoCode = dto.icaoCode,
                                callsign = dto.callsign,
                                countryCode = dto.countryCode,
                                isScheduled = dto.isScheduled,
                                isPassenger = dto.isPassenger,
                                isCargo = dto.isCargo,
                                totalAircrafts = dto.totalAircrafts,
                                averageFleetAge = dto.averageFleetAge
                            )
                        }
                    )
                }
                val first = res.data.firstOrNull()
                if (first != null) ApiResult.Success(first, meta = res.meta)
                else ApiResult.Failure(AirLabsError.NotFound("Airline $clean not found"))
            }
            is ApiResult.Failure -> res
        }
    }

    suspend fun getAirlineFleet(airlineCode: String): ApiResult<List<FleetAircraftDto>> {
        val clean = airlineCode.trim().uppercase()
        return executeCall {
            if (clean.length == 3) api.getFleets(airlineIcao = clean)
            else api.getFleets(airlineIata = clean)
        }
    }

    suspend fun lookupAircraft(regOrHex: String): ApiResult<List<FleetAircraftDto>> {
        val clean = regOrHex.trim().uppercase()
        val res = executeCall { api.getFleets(regNumber = clean) }
        return if (res is ApiResult.Success && res.data.isNotEmpty()) res
        else if (res is ApiResult.Failure && res.error !is AirLabsError.NotFound) res
        else executeCall { api.getFleets(hex = clean) }
    }

    suspend fun getAirlineRoutes(airlineCode: String): ApiResult<List<RouteDto>> {
        val clean = airlineCode.trim().uppercase()
        return executeCall {
            if (clean.length == 3) api.getRoutes(airlineIcao = clean)
            else api.getRoutes(airlineIata = clean)
        }
    }

    suspend fun getNearbyAirports(lat: Double, lng: Double, distanceKm: Int = 180): ApiResult<NearbyResponseDto> {
        return executeCall { api.getNearby(lat = lat, lng = lng, distanceKm = distanceKm) }
    }

    suspend fun searchSuggestions(query: String): ApiResult<List<SuggestEntry>> {
        val q = query.trim()
        if (q.length < 2) return ApiResult.Success(emptyList())
        val res = executeCall { api.getSuggestions(q) }
        return when (res) {
            is ApiResult.Success -> {
                val dto = res.data
                val items = buildList {
                    dto.airports?.forEach { a ->
                        val code = a.iataCode ?: a.icaoCode ?: ""
                        if (code.isNotBlank()) {
                            add(
                                SuggestEntry(
                                    title = "${a.name ?: code} ($code)",
                                    subtitle = listOfNotNull(a.city, a.countryCode).joinToString(", "),
                                    code = code,
                                    kind = SuggestKind.AIRPORT,
                                    lat = a.lat,
                                    lng = a.lng
                                )
                            )
                        }
                    }
                    dto.airlines?.forEach { al ->
                        val code = al.iataCode ?: al.icaoCode ?: ""
                        if (code.isNotBlank()) {
                            add(
                                SuggestEntry(
                                    title = "${al.name ?: code} ($code)",
                                    subtitle = "Airline • ${al.countryCode ?: ""}",
                                    code = code,
                                    kind = SuggestKind.AIRLINE
                                )
                            )
                        }
                    }
                    dto.cities?.forEach { c ->
                        val code = c.cityCode ?: c.name ?: ""
                        if (code.isNotBlank()) {
                            add(
                                SuggestEntry(
                                    title = c.name ?: code,
                                    subtitle = "City • ${c.countryCode ?: ""}",
                                    code = code,
                                    kind = SuggestKind.CITY,
                                    lat = c.lat,
                                    lng = c.lng
                                )
                            )
                        }
                    }
                }
                ApiResult.Success(items, meta = res.meta)
            }
            is ApiResult.Failure -> res
        }
    }

    suspend fun registerWebhookAlert(webhookUrl: String, flightCode: String): ApiResult<ListenResponseDto> {
        val clean = flightCode.trim().uppercase()
        val isIcao = clean.length >= 4 && clean.take(3).all { it.isLetter() }
        return executeCall {
            if (isIcao) api.registerFlightAlertWebhook(webhookUrl = webhookUrl, flightIcao = clean)
            else api.registerFlightAlertWebhook(webhookUrl = webhookUrl, flightIata = clean)
        }
    }

    suspend fun getCacheStats(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        cacheDao.countCachedAirports() to cacheDao.countCachedAirlines()
    }

    suspend fun clearCaches() = withContext(Dispatchers.IO) {
        flightCache.clear()
        scheduleCache.clear()
        cacheDao.clearAirportCache()
        cacheDao.clearAirlineCache()
    }
}
