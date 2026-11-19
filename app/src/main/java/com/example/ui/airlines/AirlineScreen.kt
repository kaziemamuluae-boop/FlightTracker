package com.example.ui.airlines

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Airlines
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.FavoriteAirlineEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AviationErrorCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FlightSummaryCard
import com.example.ui.components.ShimmerLoadingCard
import com.example.ui.theme.AviationAmberWarn
import com.example.utils.AviationFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirlineScreen(
    initialAirlineCode: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenFlightDetails: (String) -> Unit,
    onOpenAircraft: (String) -> Unit,
    onOpenApiConfig: () -> Unit
) {
    val state by viewModel.airlineState.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    var searchCode by remember(initialAirlineCode) { mutableStateOf(initialAirlineCode) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Fleet, 1=Routes, 2=Live Flights
    val activeCode = state.airline?.iataCode ?: state.airline?.icaoCode ?: state.airlineCode.ifBlank { initialAirlineCode }
    val isFav by viewModel.isAirlineFavorite(activeCode).collectAsStateWithLifecycle(initialValue = false)

    LaunchedEffect(initialAirlineCode) {
        viewModel.loadAirline(initialAirlineCode)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Airline Directory") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    state.airline?.let { al ->
                        IconButton(onClick = {
                            viewModel.toggleFavoriteAirline(
                                FavoriteAirlineEntity(
                                    code = activeCode,
                                    name = al.name ?: activeCode,
                                    iataCode = al.iataCode,
                                    icaoCode = al.icaoCode,
                                    countryCode = al.countryCode,
                                    totalAircrafts = al.totalAircrafts
                                ),
                                currentlyFav = isFav
                            )
                        }) {
                            Icon(
                                if (isFav) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "Favorite airline",
                                tint = if (isFav) AviationAmberWarn else MaterialTheme.colorScheme.onSurface
                            )
                        }
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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchCode,
                        onValueChange = { searchCode = it },
                        placeholder = { Text("Airline IATA/ICAO (e.g. BA, AA, UAE)") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.loadAirline(searchCode) }),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { viewModel.loadAirline(searchCode) },
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
                        onRetry = { viewModel.loadAirline(searchCode) },
                        onOpenApiConfig = onOpenApiConfig
                    )
                }
            }

            if (state.isLoading && state.airline == null) {
                items(3) { ShimmerLoadingCard() }
            }

            state.airline?.let { al ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "${al.iataCode ?: ""} / ${al.icaoCode ?: ""}".trim(' ', '/'),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(al.name ?: activeCode, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = listOfNotNull(
                                    al.countryCode?.let { "Country: $it" },
                                    al.callsign?.let { "Callsign: $it" }
                                ).joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Fleet: ${al.totalAircrafts ?: "N/A"}", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    "Avg Age: ${al.averageFleetAge?.let { String.format("%.1f yrs", it) } ?: "N/A"}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            item {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Fleet (${state.fleet.size})") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Routes (${state.routes.size})") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Live (${state.liveFlights.size})") })
                }
            }

            when (selectedTab) {
                0 -> {
                    if (state.fleet.isEmpty() && !state.isLoading) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.Airlines,
                                title = "No Fleet Data",
                                subtitle = "Fleet information was not returned for this airline."
                            )
                        }
                    } else {
                        items(state.fleet.take(50)) { ac ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !ac.regNumber.isNullOrBlank()) {
                                        ac.regNumber?.let(onOpenAircraft)
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(ac.regNumber ?: ac.hex ?: "Unknown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            listOfNotNull(ac.manufacturer, ac.model ?: ac.icao).joinToString(" "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(ac.icao ?: "—", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                        Text(
                                            ac.age?.let { "$it yrs" } ?: "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    if (state.routes.isEmpty() && !state.isLoading) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.Airlines,
                                title = "No Routes Found",
                                subtitle = "No scheduled routes were returned for this carrier."
                            )
                        }
                    } else {
                        items(state.routes.take(50)) { r ->
                            val code = r.flightIata ?: r.flightIcao
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !code.isNullOrBlank()) { code?.let(onOpenFlightDetails) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "${r.depIata ?: r.depIcao ?: "?"} → ${r.arrIata ?: r.arrIcao ?: "?"}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Flight ${code ?: "—"} • Dep ${AviationFormatters.formatScheduleTime(r.depTime, r.depTimeUtc, settings)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        AviationFormatters.formatDuration(r.duration),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {
                    if (state.liveFlights.isEmpty() && !state.isLoading) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.Airlines,
                                title = "No Tracked Flights",
                                subtitle = "No airborne flights currently tracked for this airline."
                            )
                        }
                    } else {
                        items(state.liveFlights.take(40)) { fl ->
                            val code = fl.flightIata ?: fl.flightIcao ?: fl.hex
                            FlightSummaryCard(
                                flight = fl,
                                settings = settings,
                                onClick = { if (!code.isNullOrBlank()) onOpenFlightDetails(code) }
                            )
                        }
                    }
                }
            }
        }
    }
}
