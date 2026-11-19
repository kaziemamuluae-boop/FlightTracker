package com.example.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Airlines
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocalAirport
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.MainViewModel
import com.example.ui.POPULAR_AIRPORTS
import com.example.ui.components.AviationErrorCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FlightSummaryCard
import com.example.ui.components.ShimmerLoadingCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AviationAmberWarn
import com.example.ui.theme.AviationCyanHighlight
import com.example.ui.theme.AviationDeepNavy
import com.example.ui.theme.AviationMintGreen
import com.example.ui.theme.AviationSkyBlue
import com.example.utils.AviationFormatters

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenLiveMap: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFlightDetails: (String) -> Unit,
    onOpenAirport: (String) -> Unit,
    onOpenAirline: (String) -> Unit,
    onOpenAircraftLookup: () -> Unit,
    onOpenComparison: () -> Unit,
    onOpenApiConfig: () -> Unit
) {
    val context = LocalContext.current
    val homeState by viewModel.homeState.collectAsStateWithLifecycle()
    val apiConfig by viewModel.apiConfig.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val favoriteFlights by viewModel.favoriteFlights.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.fetchUserLocationAndNearby()
        }
    }

    LaunchedEffect(apiConfig.isConfigured) {
        if (apiConfig.isConfigured && homeState.liveFlightsSample.isEmpty() && !homeState.isLoading) {
            viewModel.loadHomeDashboard(forceRefresh = false)
        }
        val hasLoc = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasLoc && apiConfig.isConfigured && homeState.nearbyAirports.isEmpty()) {
            viewModel.fetchUserLocationAndNearby()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_aviation_hero_1795119216071),
                    contentDescription = "Aviation radar horizon banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    AviationDeepNavy.copy(alpha = 0.25f),
                                    AviationDeepNavy.copy(alpha = 0.88f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_flight_tracker_fg_1795119215085),
                                contentDescription = "Flight Tracker Logo",
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Flight Tracker",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (apiConfig.isConfigured) {
                                        "AirLabs v9 • Updated ${AviationFormatters.formatTimestampShortMs(homeState.lastUpdatedMs, settings)}"
                                    } else {
                                        "API Connection Required"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.loadHomeDashboard(forceRefresh = true) },
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.14f), CircleShape)
                                .testTag("home_refresh_button")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh dashboard", tint = Color.White)
                        }
                    }

                    // Live Aircraft Count pill (only when data retrieved) + Search Shortcut
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (homeState.liveFlightsSample.isNotEmpty()) {
                            Surface(
                                color = AviationMintGreen.copy(alpha = 0.20f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(AviationMintGreen, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${homeState.liveFlightsSample.size}+ Live Aircraft Active",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = AviationMintGreen
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onOpenSearch)
                                .testTag("home_search_bar")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Search flight, airport, airline, or tail number...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Actions Strip
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    QuickActionTile("Live Map", Icons.Filled.Radar, AviationCyanHighlight, "qa_live_map", onOpenLiveMap)
                }
                item {
                    QuickActionTile("Airports", Icons.Filled.LocalAirport, AviationSkyBlue, "qa_airports") {
                        onOpenAirport("JFK")
                    }
                }
                item {
                    QuickActionTile("Airlines", Icons.Filled.Airlines, AviationMintGreen, "qa_airlines") {
                        onOpenAirline("BA")
                    }
                }
                item {
                    QuickActionTile("Aircraft", Icons.Filled.Public, AviationAmberWarn, "qa_aircraft", onOpenAircraftLookup)
                }
                item {
                    QuickActionTile("Compare", Icons.Filled.CompareArrows, AviationSkyBlue, "qa_compare", onOpenComparison)
                }
                item {
                    QuickActionTile("API Config", Icons.Filled.Key, AviationCyanHighlight, "qa_api", onOpenApiConfig)
                }
            }
        }

        // Error or API setup card
        if (homeState.error != null) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AviationErrorCard(
                        error = homeState.error!!,
                        onRetry = { viewModel.loadHomeDashboard(forceRefresh = true) },
                        onOpenApiConfig = onOpenApiConfig
                    )
                }
            }
        }

        // Flight Status Overview Strip (from real retrieved flights)
        if (homeState.liveFlightsSample.isNotEmpty()) {
            item {
                val flights = homeState.liveFlightsSample
                val enRoute = flights.count { it.status.equals("en-route", true) || (it.altMeters ?: 0.0) > 100 }
                val scheduled = flights.count { it.status.equals("scheduled", true) }
                val delayed = flights.count { it.status.equals("delayed", true) }
                val landed = flights.count { it.status.equals("landed", true) }
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Flight Status Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OverviewStatBox("In Air", enRoute.toString(), AviationMintGreen, Modifier.weight(1f))
                        OverviewStatBox("Scheduled", scheduled.toString(), AviationSkyBlue, Modifier.weight(1f))
                        OverviewStatBox("Delayed", delayed.toString(), AviationAmberWarn, Modifier.weight(1f))
                        OverviewStatBox("Landed", landed.toString(), AviationCyanHighlight, Modifier.weight(1f))
                    }
                }
            }
        }

        // Nearby Airports
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nearby Airports",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FilledTonalButton(
                        onClick = {
                            val hasLoc = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasLoc) viewModel.fetchUserLocationAndNearby()
                            else locationLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Locate", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (homeState.nearbyAirports.isEmpty()) {
                    Text(
                        text = "Tap Locate to discover airports near your GPS coordinates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(homeState.nearbyAirports.take(10)) { ap ->
                            val code = ap.iataCode ?: ap.icaoCode ?: ""
                            Card(
                                modifier = Modifier
                                    .width(175.dp)
                                    .clickable { if (code.isNotBlank()) onOpenAirport(code) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = code,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = ap.name ?: "Airport",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = AviationFormatters.formatDistance(ap.distanceKm, settings.distanceUnit),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AviationMintGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Popular Hubs (Departures / Arrivals boards)
        item {
            Column {
                Text(
                    text = "Popular Airport Boards",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(POPULAR_AIRPORTS) { hub ->
                        Card(
                            modifier = Modifier
                                .width(180.dp)
                                .clickable { onOpenAirport(hub.iata) }
                                .testTag("popular_airport_${hub.iata}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = hub.iata,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(
                                            Icons.Filled.FlightTakeoff,
                                            contentDescription = "Departures",
                                            modifier = Modifier.size(15.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Icon(
                                            Icons.Filled.FlightLand,
                                            contentDescription = "Arrivals",
                                            modifier = Modifier.size(15.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = hub.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${hub.city}, ${hub.country}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Favorite Flights
        if (favoriteFlights.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Favorite & Upcoming Flights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(favoriteFlights) { fav ->
                            Card(
                                modifier = Modifier
                                    .width(200.dp)
                                    .clickable { onOpenFlightDetails(fav.flightCode) },
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
                                            Icon(
                                                Icons.Filled.Star,
                                                contentDescription = null,
                                                tint = AviationAmberWarn,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                fav.flightCode,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        StatusBadge(status = fav.lastStatus)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${fav.depIata ?: "---"} → ${fav.arrIata ?: "---"}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = fav.airlineName ?: fav.airlineIata ?: "Tracked Flight",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Searches
        if (history.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Recent Searches",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(history.take(8)) { h ->
                            AssistChip(
                                onClick = {
                                    when (h.category) {
                                        "AIRPORT" -> onOpenAirport(h.query)
                                        "AIRLINE" -> onOpenAirline(h.query)
                                        "FLIGHT" -> onOpenFlightDetails(h.query)
                                        else -> onOpenSearch()
                                    }
                                },
                                label = { Text("${h.title}") }
                            )
                        }
                    }
                }
            }
        }

        // Live Airborne Flights Feed
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Airborne Flights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Open Radar →",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onOpenLiveMap)
                )
            }
        }

        if (homeState.isLoading && homeState.liveFlightsSample.isEmpty()) {
            items(3) {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ShimmerLoadingCard()
                }
            }
        } else if (homeState.liveFlightsSample.isEmpty() && homeState.error == null) {
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Filled.Radar,
                        title = "Ready for Live Radar",
                        subtitle = "Configure your AirLabs API key to stream live global aircraft telemetry.",
                        actionLabel = "Open API Setup",
                        onAction = onOpenApiConfig
                    )
                }
            }
        } else {
            items(
                items = homeState.liveFlightsSample.take(12),
                key = { it.hex ?: it.flightIata ?: it.hashCode().toString() }
            ) { flight ->
                val code = flight.flightIata ?: flight.flightIcao ?: flight.hex
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    FlightSummaryCard(
                        flight = flight,
                        settings = settings,
                        onClick = {
                            if (!code.isNullOrBlank()) onOpenFlightDetails(code)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    icon: ImageVector,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .width(88.dp)
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.16f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OverviewStatBox(
    label: String,
    count: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
