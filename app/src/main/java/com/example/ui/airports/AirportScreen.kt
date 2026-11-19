package com.example.ui.airports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalAirport
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.FavoriteAirportEntity
import com.example.model.ScheduleDto
import com.example.ui.MainViewModel
import com.example.ui.components.AviationErrorCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ShimmerLoadingCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.WeatherCard
import com.example.ui.theme.AviationAmberWarn
import com.example.utils.AviationFormatters
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirportScreen(
    initialAirportCode: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenFlightDetails: (String) -> Unit,
    onOpenApiConfig: () -> Unit
) {
    val state by viewModel.airportState.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    var searchCode by remember(initialAirportCode) { mutableStateOf(initialAirportCode) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Departures, 1 = Arrivals
    val activeCode = state.airport?.iataCode ?: state.airportCode.ifBlank { initialAirportCode }
    val isFav by viewModel.isAirportFavorite(activeCode).collectAsStateWithLifecycle(initialValue = false)

    LaunchedEffect(initialAirportCode) {
        viewModel.loadAirport(initialAirportCode)
    }

    val rawBoard = if (selectedTab == 0) state.departures else state.arrivals
    val filteredBoard = remember(rawBoard, state.boardFilter, state.showDelayedOnly, state.showCancelledOnly) {
        rawBoard.filter { row ->
            val q = state.boardFilter.trim()
            val matchesQuery = q.isBlank() ||
                row.flightIata?.contains(q, true) == true ||
                row.flightIcao?.contains(q, true) == true ||
                row.airlineIata?.contains(q, true) == true ||
                row.depIata?.contains(q, true) == true ||
                row.arrIata?.contains(q, true) == true
            val matchesDelayed = !state.showDelayedOnly ||
                row.status.equals("delayed", true) || (row.delayed ?: 0) > 0
            val matchesCancelled = !state.showCancelledOnly ||
                row.status.equals("cancelled", true) || row.status.equals("canceled", true)
            matchesQuery && matchesDelayed && matchesCancelled
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Airport & Flight Boards") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    state.airport?.let { ap ->
                        IconButton(onClick = {
                            viewModel.toggleFavoriteAirport(
                                FavoriteAirportEntity(
                                    iataOrIcao = activeCode,
                                    name = ap.name ?: activeCode,
                                    iataCode = ap.iataCode,
                                    icaoCode = ap.icaoCode,
                                    city = ap.city,
                                    countryCode = ap.countryCode,
                                    lat = ap.lat,
                                    lng = ap.lng,
                                    timezone = ap.timezone
                                ),
                                currentlyFav = isFav
                            )
                        }) {
                            Icon(
                                if (isFav) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "Favorite airport",
                                tint = if (isFav) AviationAmberWarn else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.loadAirport(activeCode, forceRefresh = true) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh flight board")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Airport Lookup Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchCode,
                        onValueChange = { searchCode = it },
                        placeholder = { Text("IATA or ICAO (e.g. JFK, EGLL)") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.loadAirport(searchCode, true) }),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("airport_search_input")
                    )
                    Button(
                        onClick = { viewModel.loadAirport(searchCode, true) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(54.dp)
                    ) {
                        Text("Load")
                    }
                }
            }

            if (state.error != null) {
                item {
                    AviationErrorCard(
                        error = state.error!!,
                        onRetry = { viewModel.loadAirport(activeCode, true) },
                        onOpenApiConfig = onOpenApiConfig
                    )
                }
            }

            if (state.isLoading && state.airport == null) {
                items(3) { ShimmerLoadingCard() }
            }

            state.airport?.let { ap ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${ap.iataCode ?: ""} / ${ap.icaoCode ?: ""}".trim(' ', '/'),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = ap.name ?: activeCode,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = listOfNotNull(ap.city, ap.countryCode).joinToString(", "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "Coords: ${AviationFormatters.formatCoordinates(ap.lat, ap.lng)} • TZ: ${ap.timezone ?: "—"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (ap.lat != null && ap.lng != null) {
                                AndroidView(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(145.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                    factory = { ctx ->
                                        MapView(ctx).apply {
                                            setTileSource(TileSourceFactory.MAPNIK)
                                            setMultiTouchControls(true)
                                            controller.setZoom(11.5)
                                            val pt = GeoPoint(ap.lat, ap.lng)
                                            controller.setCenter(pt)
                                            overlays.add(Marker(this).apply {
                                                position = pt
                                                title = ap.name ?: activeCode
                                            })
                                        }
                                    },
                                    update = { mv ->
                                        val pt = GeoPoint(ap.lat, ap.lng)
                                        mv.controller.setCenter(pt)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            state.weather?.let { w ->
                item { WeatherCard(weather = w, settings = settings) }
            }

            // Flight Boards Tabs + Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Departures (${state.departures.size})") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Arrivals (${state.arrivals.size})") }
                        )
                    }
                    OutlinedTextField(
                        value = state.boardFilter,
                        onValueChange = { viewModel.updateAirportBoardFilter(it, state.showDelayedOnly, state.showCancelledOnly) },
                        placeholder = { Text("Filter flight #, airline, or airport...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.showDelayedOnly,
                            onClick = {
                                viewModel.updateAirportBoardFilter(
                                    state.boardFilter, !state.showDelayedOnly, state.showCancelledOnly
                                )
                            },
                            label = { Text("Delayed") }
                        )
                        FilterChip(
                            selected = state.showCancelledOnly,
                            onClick = {
                                viewModel.updateAirportBoardFilter(
                                    state.boardFilter, state.showDelayedOnly, !state.showCancelledOnly
                                )
                            },
                            label = { Text("Cancelled") }
                        )
                    }
                }
            }

            if (filteredBoard.isEmpty() && !state.isLoading) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.LocalAirport,
                        title = "No Scheduled Flights",
                        subtitle = "No matching flights returned for this airport board."
                    )
                }
            } else {
                items(filteredBoard.take(60)) { row ->
                    ScheduleRowCard(
                        row = row,
                        isDeparture = selectedTab == 0,
                        settings = settings,
                        onClick = {
                            val code = row.flightIata ?: row.flightIcao
                            if (!code.isNullOrBlank()) onOpenFlightDetails(code)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleRowCard(
    row: ScheduleDto,
    isDeparture: Boolean,
    settings: com.example.model.UserSettings,
    onClick: () -> Unit
) {
    val flightCode = row.flightIata ?: row.flightIcao ?: "Flight"
    val sched = if (isDeparture) {
        AviationFormatters.formatScheduleTime(row.depTime, row.depTimeUtc, settings)
    } else {
        AviationFormatters.formatScheduleTime(row.arrTime, row.arrTimeUtc, settings)
    }
    val est = if (isDeparture) {
        AviationFormatters.formatScheduleTime(row.depEstimated, row.depEstimatedUtc, settings)
    } else {
        AviationFormatters.formatScheduleTime(row.arrEstimated, row.arrEstimatedUtc, settings)
    }
    val terminal = if (isDeparture) row.depTerminal else row.arrTerminal
    val gate = if (isDeparture) row.depGate else row.arrGate
    val counterAirport = if (isDeparture) (row.arrIata ?: row.arrIcao) else (row.depIata ?: row.depIcao)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(flightCode, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDeparture) "→ ${counterAirport ?: "---"}" else "← ${counterAirport ?: "---"}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                StatusBadge(status = row.status)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sched: $sched • Est: $est", style = MaterialTheme.typography.bodySmall)
                Text(
                    "T${terminal ?: "-"} / G${gate ?: "-"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!row.csFlightIata.isNullOrBlank()) {
                Text(
                    "Codeshare: ${row.csAirlineIata ?: ""} ${row.csFlightIata}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
