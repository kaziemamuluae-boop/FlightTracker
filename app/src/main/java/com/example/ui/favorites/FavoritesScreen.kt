package com.example.ui.favorites

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge

@Composable
fun FavoritesScreen(
    viewModel: MainViewModel,
    onOpenFlightDetails: (String) -> Unit,
    onOpenAirport: (String) -> Unit,
    onOpenAirline: (String) -> Unit,
    onOpenAircraft: (String) -> Unit
) {
    val favFlights by viewModel.favoriteFlights.collectAsStateWithLifecycle()
    val favAirports by viewModel.favoriteAirports.collectAsStateWithLifecycle()
    val favAirlines by viewModel.favoriteAirlines.collectAsStateWithLifecycle()
    val favAircraft by viewModel.favoriteAircraft.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Flights (${favFlights.size})",
        "Airports (${favAirports.size})",
        "Airlines (${favAirlines.size})",
        "Aircraft (${favAircraft.size})",
        "Alerts (${alerts.size})",
        "History (${history.size})"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Favorites & History", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (selectedTab == 5 && history.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearSearchHistory() },
                    modifier = Modifier.testTag("clear_all_history_button")
                ) {
                    Text("Clear All")
                }
            }
        }

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
            tabs.forEachIndexed { idx, title ->
                Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(title) })
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    if (favFlights.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.Star,
                                title = "No Favorite Flights",
                                subtitle = "Tap the star icon on any Flight Details screen to save it for quick tracking."
                            )
                        }
                    } else {
                        items(favFlights, key = { it.flightCode }) { f ->
                            FavoriteItemRow(
                                title = f.flightCode,
                                subtitle = "${f.depIata ?: "---"} → ${f.arrIata ?: "---"} • ${f.airlineName ?: f.airlineIata ?: ""}".trim(' ', '•'),
                                badge = f.lastStatus,
                                onClick = { onOpenFlightDetails(f.flightCode) },
                                onDelete = { viewModel.removeFavoriteFlight(f.flightCode) }
                            )
                        }
                    }
                }
                1 -> {
                    if (favAirports.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.Star,
                                title = "No Favorite Airports",
                                subtitle = "Star any airport to access its Departures and Arrivals boards in one tap."
                            )
                        }
                    } else {
                        items(favAirports, key = { it.iataOrIcao }) { a ->
                            FavoriteItemRow(
                                title = "${a.name} (${a.iataOrIcao})",
                                subtitle = listOfNotNull(a.city, a.countryCode).joinToString(", "),
                                onClick = { onOpenAirport(a.iataOrIcao) },
                                onDelete = { viewModel.removeFavoriteAirport(a.iataOrIcao) }
                            )
                        }
                    }
                }
                2 -> {
                    if (favAirlines.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.Star,
                                title = "No Favorite Airlines",
                                subtitle = "Star airlines to quickly inspect their fleets, routes, and live flights."
                            )
                        }
                    } else {
                        items(favAirlines, key = { it.code }) { al ->
                            FavoriteItemRow(
                                title = "${al.name} (${al.code})",
                                subtitle = listOfNotNull(al.countryCode, al.totalAircrafts?.let { "$it aircraft" }).joinToString(" • "),
                                onClick = { onOpenAirline(al.code) },
                                onDelete = { viewModel.removeFavoriteAirline(al.code) }
                            )
                        }
                    }
                }
                3 -> {
                    if (favAircraft.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.Star,
                                title = "No Favorite Aircraft",
                                subtitle = "Save specific tail registrations from Aircraft Details."
                            )
                        }
                    } else {
                        items(favAircraft, key = { it.regNumber }) { ac ->
                            FavoriteItemRow(
                                title = ac.regNumber,
                                subtitle = listOfNotNull(ac.manufacturer, ac.model ?: ac.aircraftIcao).joinToString(" "),
                                onClick = { onOpenAircraft(ac.regNumber) },
                                onDelete = { viewModel.removeFavoriteAircraft(ac.regNumber) }
                            )
                        }
                    }
                }
                4 -> {
                    if (alerts.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.NotificationsActive,
                                title = "No Active Flight Alerts",
                                subtitle = "Open any Flight Details screen and tap the alert bell to track status, delay, or gate changes."
                            )
                        }
                    } else {
                        items(alerts, key = { it.flightCode }) { alert ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenFlightDetails(alert.flightCode) },
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(alert.flightCode, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            "${alert.depIata ?: "?"} → ${alert.arrIata ?: "?"} • Last: ${alert.lastStatus ?: "Monitoring"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = alert.enabled,
                                        onCheckedChange = { enabled ->
                                            viewModel.saveFlightAlert(alert.copy(enabled = enabled), false)
                                        }
                                    )
                                    IconButton(onClick = { viewModel.deleteFlightAlert(alert.flightCode) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete alert")
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    if (history.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.History,
                                title = "Search History Empty",
                                subtitle = "Flights, airports, airlines, and aircraft you view will appear here."
                            )
                        }
                    } else {
                        items(history, key = { it.id }) { h ->
                            FavoriteItemRow(
                                title = h.title,
                                subtitle = "${h.category} • ${h.subtitle}",
                                onClick = {
                                    when (h.category) {
                                        "AIRPORT" -> onOpenAirport(h.query)
                                        "AIRLINE" -> onOpenAirline(h.query)
                                        "AIRCRAFT" -> onOpenAircraft(h.query)
                                        else -> onOpenFlightDetails(h.query)
                                    }
                                },
                                onDelete = { viewModel.removeHistoryItem(h.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteItemRow(
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!badge.isNullOrBlank()) {
                StatusBadge(status = badge)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove item")
            }
        }
    }
}
