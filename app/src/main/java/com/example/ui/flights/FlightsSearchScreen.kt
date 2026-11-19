package com.example.ui.flights

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Airlines
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalAirport
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.FlightFilter
import com.example.model.SuggestKind
import com.example.ui.MainViewModel
import com.example.ui.components.AviationErrorCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.FlightSummaryCard
import com.example.ui.components.ShimmerLoadingCard
import com.example.utils.AviationFormatters

@Composable
fun FlightsSearchScreen(
    viewModel: MainViewModel,
    onOpenFlightDetails: (String) -> Unit,
    onOpenAirport: (String) -> Unit,
    onOpenAirline: (String) -> Unit,
    onOpenAircraftLookup: () -> Unit,
    onOpenComparison: () -> Unit,
    onOpenApiConfig: () -> Unit
) {
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val compareFlights by viewModel.compareFlights.collectAsStateWithLifecycle()

    val filteredResults = remember(searchState.flightResults, searchState.activeFilter) {
        searchState.flightResults.filter { AviationFormatters.matchesFilter(it, searchState.activeFilter) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Flight Search",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Flight #, airline, airport, city, or tail reg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onOpenComparison) {
                    Icon(Icons.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Compare (${compareFlights.size})")
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchState.query,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("e.g. AA6, JFK, BA, N12345, London") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchState.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearSearch() }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.executeFlightSearch() }),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("flights_search_input")
                )
                Button(
                    onClick = { viewModel.executeFlightSearch() },
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("flights_search_submit"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Search")
                }
            }
        }

        // Autocomplete suggestions from /suggest
        if (searchState.suggestions.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column {
                        searchState.suggestions.take(6).forEachIndexed { idx, entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        when (entry.kind) {
                                            SuggestKind.AIRPORT -> onOpenAirport(entry.code)
                                            SuggestKind.AIRLINE -> onOpenAirline(entry.code)
                                            SuggestKind.CITY -> viewModel.executeFlightSearch(entry.code)
                                            SuggestKind.FLIGHT -> onOpenFlightDetails(entry.code)
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (entry.kind) {
                                        SuggestKind.AIRPORT -> Icons.Filled.LocalAirport
                                        SuggestKind.AIRLINE -> Icons.Filled.Airlines
                                        SuggestKind.CITY -> Icons.Filled.LocationCity
                                        SuggestKind.FLIGHT -> Icons.Filled.Flight
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    if (entry.subtitle.isNotBlank()) {
                                        Text(
                                            entry.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    entry.kind.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (idx < searchState.suggestions.take(6).lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }

        // Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FlightFilter.entries) { filter ->
                    val label = when (filter) {
                        FlightFilter.ALL -> "All"
                        FlightFilter.IN_AIR -> "In Air"
                        FlightFilter.SCHEDULED -> "Scheduled"
                        FlightFilter.LANDED -> "Landed"
                        FlightFilter.DELAYED -> "Delayed"
                        FlightFilter.CANCELLED -> "Cancelled"
                        FlightFilter.DOMESTIC -> "Domestic"
                        FlightFilter.INTERNATIONAL -> "International"
                    }
                    FilterChip(
                        selected = searchState.activeFilter == filter,
                        onClick = { viewModel.setSearchFilter(filter) },
                        label = { Text(label) }
                    )
                }
            }
        }

        // Quick lookup shortcuts (Direct Flight Lookup, Aircraft Registration)
        if (searchState.flightResults.isEmpty() && !searchState.isLoading) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (searchState.query.isNotBlank()) {
                                onOpenFlightDetails(searchState.query.trim().uppercase())
                            }
                        },
                        enabled = searchState.query.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Open Flight Details")
                    }
                    OutlinedButton(
                        onClick = onOpenAircraftLookup,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Aircraft Reg Lookup")
                    }
                }
            }
        }

        // Recent Search History
        if (searchState.flightResults.isEmpty() && history.isNotEmpty()) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Recent History",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(history.take(10)) { h ->
                            AssistChip(
                                onClick = {
                                    when (h.category) {
                                        "AIRPORT" -> onOpenAirport(h.query)
                                        "AIRLINE" -> onOpenAirline(h.query)
                                        "FLIGHT" -> onOpenFlightDetails(h.query)
                                        else -> viewModel.executeFlightSearch(h.query)
                                    }
                                },
                                label = { Text(h.title) }
                            )
                        }
                    }
                }
            }
        }

        if (searchState.error != null) {
            item {
                AviationErrorCard(
                    error = searchState.error!!,
                    onRetry = { viewModel.executeFlightSearch() },
                    onOpenApiConfig = onOpenApiConfig
                )
            }
        }

        if (searchState.isLoading) {
            items(3) { ShimmerLoadingCard() }
        } else if (filteredResults.isNotEmpty()) {
            items(
                items = filteredResults,
                key = { it.hex ?: it.flightIata ?: it.hashCode().toString() }
            ) { flight ->
                val code = flight.flightIata ?: flight.flightIcao ?: flight.hex
                FlightSummaryCard(
                    flight = flight,
                    settings = settings,
                    onClick = { if (!code.isNullOrBlank()) onOpenFlightDetails(code) }
                )
            }
        } else if (searchState.flightResults.isNotEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Flight,
                    title = "No Flights Match Filter",
                    subtitle = "No retrieved flights match the selected status filter. Note that not all statuses are returned by every endpoint."
                )
            }
        } else if (searchState.error == null) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Search,
                    title = "Search Global Aviation",
                    subtitle = "Enter an IATA/ICAO flight number (e.g. AA6, BA117), airport code (JFK), airline (SQ), or aircraft registration."
                )
            }
        }
    }
}
