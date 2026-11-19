package com.example.ui

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FlightTrackerApp
import com.example.data.local.FavoriteAircraftEntity
import com.example.data.local.FavoriteAirlineEntity
import com.example.data.local.FavoriteAirportEntity
import com.example.data.local.FavoriteFlightEntity
import com.example.data.local.FlightAlertEntity
import com.example.model.AirLabsError
import com.example.model.ApiConfiguration
import com.example.model.ApiResult
import com.example.model.FlightDetailDto
import com.example.model.FlightDto
import com.example.model.FlightFilter
import com.example.model.UserSettings
import com.example.network.NetworkModule
import com.example.utils.AviationFormatters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as FlightTrackerApp
    private val aviationRepo = app.aviationRepository
    private val weatherRepo = app.weatherRepository
    private val favoritesRepo = app.favoritesRepository
    private val settingsStore = app.settingsDataStore

    val userSettings: StateFlow<UserSettings> = settingsStore.userSettings
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    val apiConfig: StateFlow<ApiConfiguration> = settingsStore.apiConfiguration
        .stateIn(viewModelScope, SharingStarted.Eagerly, ApiConfiguration())

    val favoriteFlights = favoritesRepo.favoriteFlights
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favoriteAirports = favoritesRepo.favoriteAirports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favoriteAirlines = favoritesRepo.favoriteAirlines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favoriteAircraft = favoritesRepo.favoriteAircraft
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val history = favoritesRepo.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val alerts = favoritesRepo.alerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _homeState = MutableStateFlow(HomeUiState())
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    private val _mapState = MutableStateFlow(LiveMapUiState())
    val mapState: StateFlow<LiveMapUiState> = _mapState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _flightDetailState = MutableStateFlow(FlightDetailUiState())
    val flightDetailState: StateFlow<FlightDetailUiState> = _flightDetailState.asStateFlow()

    private val _airportState = MutableStateFlow(AirportUiState())
    val airportState: StateFlow<AirportUiState> = _airportState.asStateFlow()

    private val _airlineState = MutableStateFlow(AirlineUiState())
    val airlineState: StateFlow<AirlineUiState> = _airlineState.asStateFlow()

    private val _aircraftState = MutableStateFlow(AircraftUiState())
    val aircraftState: StateFlow<AircraftUiState> = _aircraftState.asStateFlow()

    private val _compareFlights = MutableStateFlow<List<FlightDetailDto>>(emptyList())
    val compareFlights: StateFlow<List<FlightDetailDto>> = _compareFlights.asStateFlow()

    private val _cacheCounts = MutableStateFlow(0 to 0)
    val cacheCounts: StateFlow<Pair<Int, Int>> = _cacheCounts.asStateFlow()

    private var mapAutoRefreshJob: Job? = null
    private var suggestJob: Job? = null
    private var playbackJob: Job? = null

    init {
        refreshCacheStats()
    }

    fun refreshCacheStats() {
        viewModelScope.launch {
            _cacheCounts.value = aviationRepo.getCacheStats()
        }
    }

    // --- Home Dashboard ---
    fun loadHomeDashboard(forceRefresh: Boolean = false) {
        if (!apiConfig.value.isConfigured) {
            _homeState.update { it.copy(isLoading = false, error = AirLabsError.MissingApiKey) }
            return
        }
        viewModelScope.launch {
            _homeState.update { it.copy(isLoading = true, error = null) }
            when (val res = aviationRepo.getLiveFlights(forceRefresh = forceRefresh)) {
                is ApiResult.Success -> {
                    _homeState.update {
                        it.copy(
                            isLoading = false,
                            liveFlightsSample = res.data.take(120),
                            lastUpdatedMs = res.fetchedAtEpochMs,
                            error = null
                        )
                    }
                    if (_mapState.value.flights.isEmpty()) {
                        _mapState.update {
                            it.copy(flights = res.data, lastUpdatedMs = res.fetchedAtEpochMs)
                        }
                    }
                    refreshCacheStats()
                }
                is ApiResult.Failure -> {
                    _homeState.update { it.copy(isLoading = false, error = res.error) }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchUserLocationAndNearby() {
        val fused = LocationServices.getFusedLocationProviderClient(app)
        fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    _homeState.update { it.copy(userLat = loc.latitude, userLng = loc.longitude) }
                    _mapState.update { it.copy(userLat = loc.latitude, userLng = loc.longitude) }
                    loadNearbyAirports(loc.latitude, loc.longitude)
                }
            }
    }

    fun loadNearbyAirports(lat: Double, lng: Double) {
        if (!apiConfig.value.isConfigured) return
        viewModelScope.launch {
            when (val res = aviationRepo.getNearbyAirports(lat, lng, 180)) {
                is ApiResult.Success -> {
                    val list = res.data.airports.orEmpty()
                    _homeState.update { it.copy(nearbyAirports = list) }
                    _airportState.update { it.copy(nearbyAirports = list) }
                }
                is ApiResult.Failure -> Unit
            }
        }
    }

    // --- Live Map ---
    fun setMapScreenActive(active: Boolean) {
        mapAutoRefreshJob?.cancel()
        if (!active) return
        if (_mapState.value.flights.isEmpty() && apiConfig.value.isConfigured) {
            refreshMapFlights(forceRefresh = false)
        }
        mapAutoRefreshJob = viewModelScope.launch {
            while (isActive) {
                val intervalSec = userSettings.value.autoRefreshSeconds
                if (intervalSec > 0 && _mapState.value.isAutoRefreshEnabled && apiConfig.value.isConfigured) {
                    delay(intervalSec * 1000L)
                    if (_mapState.value.isAutoRefreshEnabled) {
                        refreshMapFlights(forceRefresh = true)
                    }
                } else {
                    delay(3000L)
                }
            }
        }
    }

    fun refreshMapFlights(bbox: String? = null, forceRefresh: Boolean = true) {
        if (!apiConfig.value.isConfigured) {
            _mapState.update { it.copy(isLoading = false, error = AirLabsError.MissingApiKey) }
            return
        }
        viewModelScope.launch {
            _mapState.update { it.copy(isLoading = true, error = null) }
            val currentQuery = _mapState.value.searchQuery.trim().uppercase()
            val res = if (currentQuery.isNotBlank()) {
                aviationRepo.getLiveFlights(flightIata = currentQuery, forceRefresh = forceRefresh)
            } else {
                aviationRepo.getLiveFlights(bbox = bbox, forceRefresh = forceRefresh)
            }
            when (res) {
                is ApiResult.Success -> {
                    val currentSelected = _mapState.value.selectedFlight
                    val updatedSelected = currentSelected?.let { sel ->
                        res.data.firstOrNull {
                            (sel.hex != null && it.hex == sel.hex) ||
                                (sel.flightIata != null && it.flightIata == sel.flightIata)
                        }
                    } ?: currentSelected
                    _mapState.update {
                        it.copy(
                            isLoading = false,
                            flights = res.data,
                            selectedFlight = updatedSelected,
                            lastUpdatedMs = res.fetchedAtEpochMs,
                            error = null
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _mapState.update { it.copy(isLoading = false, error = res.error) }
                }
            }
        }
    }

    fun selectMapFlight(flight: FlightDto?) {
        playbackJob?.cancel()
        _mapState.update {
            it.copy(
                selectedFlight = flight,
                selectedRouteDep = null,
                selectedRouteArr = null,
                playbackPoints = emptyList(),
                isPlayingBack = false,
                playbackIndex = 0
            )
        }
        if (flight == null) return
        viewModelScope.launch {
            val depDeferred = flight.depIata?.let { async { aviationRepo.getAirport(it) } }
            val arrDeferred = flight.arrIata?.let { async { aviationRepo.getAirport(it) } }
            val key = flight.flightIata ?: flight.flightIcao ?: flight.hex
            val playback = key?.let { aviationRepo.getFlightPlayback(it) }.orEmpty()
            val dep = (depDeferred?.await() as? ApiResult.Success)?.data
            val arr = (arrDeferred?.await() as? ApiResult.Success)?.data
            _mapState.update {
                it.copy(
                    selectedRouteDep = dep,
                    selectedRouteArr = arr,
                    playbackPoints = playback
                )
            }
        }
    }

    fun focusFlightOnMap(flightCode: String) {
        val clean = flightCode.trim().uppercase()
        _mapState.update { it.copy(searchQuery = clean) }
        viewModelScope.launch {
            when (val res = aviationRepo.getLiveFlights(flightIata = clean, forceRefresh = true)) {
                is ApiResult.Success -> {
                    val match = res.data.firstOrNull()
                    if (match != null) {
                        val merged = (_mapState.value.flights + res.data)
                            .distinctBy { it.hex ?: it.flightIata ?: it.flightIcao ?: it.hashCode().toString() }
                        _mapState.update {
                            it.copy(
                                flights = merged,
                                focusLatLng = if (match.lat != null && match.lng != null) match.lat to match.lng else null,
                                error = null
                            )
                        }
                        selectMapFlight(match)
                    } else {
                        // Try ICAO
                        val icaoRes = aviationRepo.getLiveFlights(flightIcao = clean, forceRefresh = true)
                        if (icaoRes is ApiResult.Success && icaoRes.data.isNotEmpty()) {
                            val m = icaoRes.data.first()
                            _mapState.update {
                                it.copy(
                                    flights = (_mapState.value.flights + icaoRes.data).distinctBy { f -> f.hex ?: f.flightIata },
                                    focusLatLng = if (m.lat != null && m.lng != null) m.lat to m.lng else null
                                )
                            }
                            selectMapFlight(m)
                        } else {
                            _mapState.update {
                                it.copy(error = AirLabsError.NotFound("No live airborne position found for $clean."))
                            }
                        }
                    }
                }
                is ApiResult.Failure -> _mapState.update { it.copy(error = res.error) }
            }
        }
    }

    fun clearMapSearch() {
        _mapState.update { it.copy(searchQuery = "", focusLatLng = null) }
        refreshMapFlights(forceRefresh = false)
    }

    fun toggleAutoRefresh() {
        _mapState.update { it.copy(isAutoRefreshEnabled = !it.isAutoRefreshEnabled) }
    }

    fun toggleNearbyOnly() {
        _mapState.update { it.copy(nearbyOnly = !it.nearbyOnly) }
    }

    fun toggleMultiTrackFlight(code: String) {
        val clean = code.uppercase()
        _mapState.update { state ->
            val next = if (clean in state.multiTrackCodes) state.multiTrackCodes - clean
            else state.multiTrackCodes + clean
            state.copy(multiTrackCodes = next)
        }
    }

    fun startFlightPlayback() {
        val points = _mapState.value.playbackPoints
        if (points.size < 2) return
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            _mapState.update { it.copy(isPlayingBack = true, playbackIndex = 0) }
            for (idx in points.indices) {
                _mapState.update { it.copy(playbackIndex = idx) }
                delay(650L)
            }
            _mapState.update { it.copy(isPlayingBack = false) }
        }
    }

    fun stopFlightPlayback() {
        playbackJob?.cancel()
        _mapState.update { it.copy(isPlayingBack = false) }
    }

    // --- Universal Search ---
    fun updateSearchQuery(query: String) {
        _searchState.update { it.copy(query = query) }
        suggestJob?.cancel()
        if (query.trim().length < 2) {
            _searchState.update { it.copy(suggestions = emptyList()) }
            return
        }
        suggestJob = viewModelScope.launch {
            delay(300L)
            when (val res = aviationRepo.searchSuggestions(query)) {
                is ApiResult.Success -> _searchState.update { it.copy(suggestions = res.data) }
                is ApiResult.Failure -> Unit
            }
        }
    }

    fun setSearchFilter(filter: FlightFilter) {
        _searchState.update { it.copy(activeFilter = filter) }
    }

    fun clearSearch() {
        _searchState.update { SearchUiState() }
    }

    fun executeFlightSearch(queryOverride: String? = null) {
        val q = (queryOverride ?: _searchState.value.query).trim().uppercase()
        if (q.isBlank()) return
        if (!apiConfig.value.isConfigured) {
            _searchState.update { it.copy(error = AirLabsError.MissingApiKey) }
            return
        }
        viewModelScope.launch {
            _searchState.update { it.copy(query = q, isLoading = true, error = null) }
            favoritesRepo.recordHistory(q, q, "Flight search", "QUERY")

            // Smart dispatch: registration (contains '-') -> regNumber, 2 chars -> airline, 3 chars -> airport dep/arr, else flight_iata/icao
            val res = when {
                q.contains("-") -> aviationRepo.getLiveFlights(regNumber = q, forceRefresh = true)
                q.length == 2 -> aviationRepo.getLiveFlights(airlineIata = q, forceRefresh = true)
                q.length == 3 && q.all { it.isLetter() } -> aviationRepo.getLiveFlights(depIata = q, forceRefresh = true)
                else -> {
                    val byIata = aviationRepo.getLiveFlights(flightIata = q, forceRefresh = true)
                    if (byIata is ApiResult.Success && byIata.data.isNotEmpty()) byIata
                    else aviationRepo.getLiveFlights(flightIcao = q, forceRefresh = true)
                }
            }

            when (res) {
                is ApiResult.Success -> {
                    _searchState.update {
                        it.copy(
                            isLoading = false,
                            flightResults = res.data,
                            error = if (res.data.isEmpty()) AirLabsError.NotFound("No live flights matched \"$q\".") else null
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _searchState.update { it.copy(isLoading = false, error = res.error) }
                }
            }
        }
    }

    // --- Flight Details ---
    fun loadFlightDetails(flightCode: String) {
        val clean = flightCode.trim().uppercase()
        if (!apiConfig.value.isConfigured) {
            _flightDetailState.update { FlightDetailUiState(flightCode = clean, error = AirLabsError.MissingApiKey) }
            return
        }
        viewModelScope.launch {
            _flightDetailState.update {
                FlightDetailUiState(flightCode = clean, isLoading = true)
            }
            when (val res = aviationRepo.getFlightDetails(clean)) {
                is ApiResult.Success -> {
                    val d = res.data
                    val code = d.flightIata ?: d.flightIcao ?: clean
                    favoritesRepo.recordHistory(
                        query = code,
                        title = "${d.airlineName ?: d.airlineIata ?: ""} $code".trim(),
                        subtitle = "${d.depIata ?: "?"} → ${d.arrIata ?: "?"}",
                        category = "FLIGHT"
                    )
                    val depDeferred = d.depIata?.let { async { aviationRepo.getAirport(it) } }
                    val arrDeferred = d.arrIata?.let { async { aviationRepo.getAirport(it) } }
                    val depAirport = (depDeferred?.await() as? ApiResult.Success)?.data
                    val arrAirport = (arrDeferred?.await() as? ApiResult.Success)?.data

                    val depWeatherDef = if (depAirport?.lat != null && depAirport.lng != null) {
                        async { weatherRepo.getAirportWeather(depAirport.iataCode ?: d.depIata.orEmpty(), depAirport.lat, depAirport.lng).getOrNull() }
                    } else null
                    val arrWeatherDef = if (arrAirport?.lat != null && arrAirport.lng != null) {
                        async { weatherRepo.getAirportWeather(arrAirport.iataCode ?: d.arrIata.orEmpty(), arrAirport.lat, arrAirport.lng).getOrNull() }
                    } else null

                    val playback = aviationRepo.getFlightPlayback(code)
                    _flightDetailState.update {
                        it.copy(
                            flightCode = code,
                            isLoading = false,
                            detail = d,
                            depAirport = depAirport,
                            arrAirport = arrAirport,
                            depWeather = depWeatherDef?.await(),
                            arrWeather = arrWeatherDef?.await(),
                            playbackPoints = playback,
                            error = null
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _flightDetailState.update { it.copy(isLoading = false, error = res.error) }
                }
            }
        }
    }

    // --- Airport & Flight Boards ---
    fun loadAirport(code: String, forceRefresh: Boolean = false) {
        val clean = code.trim().uppercase()
        if (!apiConfig.value.isConfigured) {
            _airportState.update { AirportUiState(airportCode = clean, error = AirLabsError.MissingApiKey) }
            return
        }
        viewModelScope.launch {
            _airportState.update { it.copy(airportCode = clean, isLoading = true, error = null) }
            val airportRes = aviationRepo.getAirport(clean)
            val airport = (airportRes as? ApiResult.Success)?.data
            if (airport != null) {
                favoritesRepo.recordHistory(
                    query = airport.iataCode ?: clean,
                    title = "${airport.name ?: clean} (${airport.iataCode ?: clean})",
                    subtitle = listOfNotNull(airport.city, airport.countryCode).joinToString(", "),
                    category = "AIRPORT"
                )
            }

            val iataForBoard = airport?.iataCode ?: clean
            val depDeferred = async { aviationRepo.getAirportSchedules(iataForBoard, departures = true, forceRefresh = forceRefresh) }
            val arrDeferred = async { aviationRepo.getAirportSchedules(iataForBoard, departures = false, forceRefresh = forceRefresh) }
            val weatherDeferred = if (airport?.lat != null && airport.lng != null) {
                async { weatherRepo.getAirportWeather(iataForBoard, airport.lat, airport.lng).getOrNull() }
            } else null

            val depRes = depDeferred.await()
            val arrRes = arrDeferred.await()
            val error = (airportRes as? ApiResult.Failure)?.error
                ?: (depRes as? ApiResult.Failure)?.error
                ?: (arrRes as? ApiResult.Failure)?.error

            _airportState.update {
                it.copy(
                    airportCode = clean,
                    isLoading = false,
                    airport = airport,
                    weather = weatherDeferred?.await(),
                    departures = (depRes as? ApiResult.Success)?.data.orEmpty(),
                    arrivals = (arrRes as? ApiResult.Success)?.data.orEmpty(),
                    lastUpdatedMs = System.currentTimeMillis(),
                    error = if (airport == null && depRes is ApiResult.Failure) error else null
                )
            }
        }
    }

    fun updateAirportBoardFilter(query: String, delayedOnly: Boolean, cancelledOnly: Boolean) {
        _airportState.update {
            it.copy(
                boardFilter = query,
                showDelayedOnly = delayedOnly,
                showCancelledOnly = cancelledOnly
            )
        }
    }

    // --- Airlines ---
    fun loadAirline(code: String) {
        val clean = code.trim().uppercase()
        if (!apiConfig.value.isConfigured) {
            _airlineState.update { AirlineUiState(airlineCode = clean, error = AirLabsError.MissingApiKey) }
            return
        }
        viewModelScope.launch {
            _airlineState.update { AirlineUiState(airlineCode = clean, isLoading = true) }
            val airlineRes = aviationRepo.getAirline(clean)
            val fleetDef = async { aviationRepo.getAirlineFleet(clean) }
            val routesDef = async { aviationRepo.getAirlineRoutes(clean) }
            val liveDef = async {
                if (clean.length == 2) aviationRepo.getLiveFlights(airlineIata = clean)
                else aviationRepo.getLiveFlights(airlineIcao = clean)
            }

            val airline = (airlineRes as? ApiResult.Success)?.data
            if (airline != null) {
                favoritesRepo.recordHistory(
                    query = airline.iataCode ?: airline.icaoCode ?: clean,
                    title = airline.name ?: clean,
                    subtitle = "Airline • ${airline.countryCode ?: ""}",
                    category = "AIRLINE"
                )
            }
            _airlineState.update {
                it.copy(
                    airlineCode = clean,
                    isLoading = false,
                    airline = airline,
                    fleet = (fleetDef.await() as? ApiResult.Success)?.data.orEmpty(),
                    routes = (routesDef.await() as? ApiResult.Success)?.data.orEmpty(),
                    liveFlights = (liveDef.await() as? ApiResult.Success)?.data.orEmpty(),
                    error = (airlineRes as? ApiResult.Failure)?.error
                )
            }
        }
    }

    // --- Aircraft Info ---
    fun lookupAircraft(regOrHex: String) {
        val clean = regOrHex.trim().uppercase()
        if (clean.isBlank()) return
        if (!apiConfig.value.isConfigured) {
            _aircraftState.update { AircraftUiState(query = clean, error = AirLabsError.MissingApiKey) }
            return
        }
        viewModelScope.launch {
            _aircraftState.update { AircraftUiState(query = clean, isLoading = true) }
            val fleetDef = async { aviationRepo.lookupAircraft(clean) }
            val liveDef = async { aviationRepo.getLiveFlights(regNumber = clean, forceRefresh = true) }
            val snapshotsDef = async { aviationRepo.getFlightPlayback(clean) }

            val fleetRes = fleetDef.await()
            val liveRes = liveDef.await()
            val list = (fleetRes as? ApiResult.Success)?.data.orEmpty()
            val live = (liveRes as? ApiResult.Success)?.data?.firstOrNull()

            if (list.isNotEmpty()) {
                val first = list.first()
                favoritesRepo.recordHistory(
                    query = first.regNumber ?: clean,
                    title = "${first.manufacturer ?: ""} ${first.model ?: clean}".trim(),
                    subtitle = "Reg ${first.regNumber ?: clean}",
                    category = "AIRCRAFT"
                )
            }
            _aircraftState.update {
                it.copy(
                    query = clean,
                    isLoading = false,
                    results = list,
                    livePosition = live,
                    historySnapshots = snapshotsDef.await(),
                    error = if (list.isEmpty() && fleetRes is ApiResult.Failure) fleetRes.error
                    else if (list.isEmpty()) AirLabsError.NotFound("No aircraft matched registration or hex \"$clean\".")
                    else null
                )
            }
        }
    }

    // --- Favorites, Alerts, Comparison, History ---
    fun isFlightFavorite(code: String): Flow<Boolean> = favoritesRepo.isFlightFavorite(code)
    fun isAirportFavorite(code: String): Flow<Boolean> = favoritesRepo.isAirportFavorite(code)
    fun isAirlineFavorite(code: String): Flow<Boolean> = favoritesRepo.isAirlineFavorite(code)
    fun isAircraftFavorite(reg: String): Flow<Boolean> = favoritesRepo.isAircraftFavorite(reg)
    fun observeAlertForFlight(code: String): Flow<FlightAlertEntity?> = favoritesRepo.observeAlertForFlight(code)

    fun toggleFavoriteFlight(item: FavoriteFlightEntity, currentlyFav: Boolean) {
        viewModelScope.launch { favoritesRepo.toggleFavoriteFlight(item, currentlyFav) }
    }

    fun removeFavoriteFlight(code: String) {
        viewModelScope.launch { favoritesRepo.removeFavoriteFlight(code) }
    }

    fun toggleFavoriteAirport(item: FavoriteAirportEntity, currentlyFav: Boolean) {
        viewModelScope.launch { favoritesRepo.toggleFavoriteAirport(item, currentlyFav) }
    }

    fun removeFavoriteAirport(code: String) {
        viewModelScope.launch { favoritesRepo.removeFavoriteAirport(code) }
    }

    fun toggleFavoriteAirline(item: FavoriteAirlineEntity, currentlyFav: Boolean) {
        viewModelScope.launch { favoritesRepo.toggleFavoriteAirline(item, currentlyFav) }
    }

    fun removeFavoriteAirline(code: String) {
        viewModelScope.launch { favoritesRepo.removeFavoriteAirline(code) }
    }

    fun toggleFavoriteAircraft(item: FavoriteAircraftEntity, currentlyFav: Boolean) {
        viewModelScope.launch { favoritesRepo.toggleFavoriteAircraft(item, currentlyFav) }
    }

    fun removeFavoriteAircraft(reg: String) {
        viewModelScope.launch { favoritesRepo.removeFavoriteAircraft(reg) }
    }

    fun saveFlightAlert(alert: FlightAlertEntity, registerWebhook: Boolean) {
        viewModelScope.launch {
            var finalAlert = alert
            val webhookUrl = apiConfig.value.webhookUrl
            if (registerWebhook && webhookUrl.isNotBlank()) {
                val res = aviationRepo.registerWebhookAlert(webhookUrl, alert.flightCode)
                if (res is ApiResult.Success) {
                    finalAlert = alert.copy(webhookListenerId = res.data.listenerId)
                }
            }
            favoritesRepo.saveFlightAlert(finalAlert)
        }
    }

    fun deleteFlightAlert(flightCode: String) {
        viewModelScope.launch { favoritesRepo.deleteFlightAlert(flightCode) }
    }

    fun addFlightToComparison(detail: FlightDetailDto) {
        val code = detail.flightIata ?: detail.flightIcao ?: return
        _compareFlights.update { current ->
            if (current.any { (it.flightIata ?: it.flightIcao) == code }) current
            else (current + detail).takeLast(4)
        }
    }

    fun removeFlightFromComparison(code: String) {
        _compareFlights.update { current ->
            current.filterNot { (it.flightIata ?: it.flightIcao) == code }
        }
    }

    fun removeHistoryItem(id: String) {
        viewModelScope.launch { favoritesRepo.removeHistoryItem(id) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { favoritesRepo.clearHistory() }
    }

    fun clearApiCaches() {
        viewModelScope.launch {
            aviationRepo.clearCaches()
            refreshCacheStats()
        }
    }

    // --- Settings & API Configuration ---
    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { settingsStore.updateSettings(transform) }
    }

    fun saveApiConfig(
        useGateway: Boolean,
        apiKey: String,
        gatewayUrl: String,
        gatewayBearerToken: String,
        webhookUrl: String
    ) {
        viewModelScope.launch {
            settingsStore.saveApiConfiguration(
                useGateway = useGateway,
                apiKey = apiKey,
                gatewayUrl = gatewayUrl,
                gatewayBearerToken = gatewayBearerToken,
                webhookUrl = webhookUrl
            )
            // Update NetworkModule synchronously before running test/queries
            NetworkModule.apiConfigRef.set(
                apiConfig.value.copy(
                    useGateway = useGateway,
                    apiKey = apiKey.trim(),
                    gatewayBaseUrl = gatewayUrl.trim(),
                    gatewayBearerToken = gatewayBearerToken.trim(),
                    webhookUrl = webhookUrl.trim()
                )
            )
        }
    }

    fun testApiConnection(
        useGateway: Boolean,
        apiKey: String,
        gatewayUrl: String,
        gatewayBearerToken: String,
        webhookUrl: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            settingsStore.saveApiConfiguration(useGateway, apiKey, gatewayUrl, gatewayBearerToken, webhookUrl)
            NetworkModule.apiConfigRef.set(
                ApiConfiguration(
                    useGateway = useGateway,
                    apiKey = apiKey.trim(),
                    gatewayBaseUrl = gatewayUrl.trim(),
                    gatewayBearerToken = gatewayBearerToken.trim(),
                    webhookUrl = webhookUrl.trim()
                )
            )
            when (val res = aviationRepo.testConnection()) {
                is ApiResult.Success -> {
                    val keyMeta = res.meta?.key
                    val msg = buildString {
                        append("Connected successfully")
                        keyMeta?.type?.let { append(" • Plan: $it") }
                        keyMeta?.limitsByMonth?.let { append(" • Monthly limit: $it") }
                    }
                    settingsStore.saveConnectionTestResult(
                        success = true,
                        message = msg,
                        planType = keyMeta?.type,
                        limitsByHour = keyMeta?.limitsByHour,
                        limitsByMonth = keyMeta?.limitsByMonth,
                        limitsTotal = keyMeta?.limitsTotal,
                        expiresAt = keyMeta?.expired
                    )
                    onComplete(true, msg)
                    loadHomeDashboard(forceRefresh = true)
                }
                is ApiResult.Failure -> {
                    val msg = "${res.error.title}: ${res.error.message}"
                    settingsStore.saveConnectionTestResult(
                        success = false,
                        message = msg,
                        planType = null,
                        limitsByHour = null,
                        limitsByMonth = null,
                        limitsTotal = null,
                        expiresAt = null
                    )
                    onComplete(false, msg)
                }
            }
        }
    }

    fun clearApiConfig() {
        viewModelScope.launch {
            settingsStore.clearApiConfiguration()
            NetworkModule.apiConfigRef.set(ApiConfiguration())
            _homeState.update { HomeUiState(error = AirLabsError.MissingApiKey) }
            _mapState.update { LiveMapUiState(error = AirLabsError.MissingApiKey) }
        }
    }

    fun distanceFromUserKm(lat: Double?, lng: Double?): Double? {
        val uLat = _mapState.value.userLat ?: _homeState.value.userLat ?: return null
        val uLng = _mapState.value.userLng ?: _homeState.value.userLng ?: return null
        if (lat == null || lng == null) return null
        return AviationFormatters.haversineKm(uLat, uLng, lat, lng)
    }
}
