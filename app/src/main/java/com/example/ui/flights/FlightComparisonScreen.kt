package com.example.ui.flights

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusBadge
import com.example.utils.AviationFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightComparisonScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenFlightDetails: (String) -> Unit
) {
    val flights by viewModel.compareFlights.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flight Status Comparison") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (flights.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                EmptyStateCard(
                    icon = Icons.Filled.CompareArrows,
                    title = "No Flights Selected to Compare",
                    subtitle = "Open any Flight Details screen and tap Compare to pin up to 4 flights side by side."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        flights.forEach { d ->
                            val code = d.flightIata ?: d.flightIcao ?: "Flight"
                            Card(
                                modifier = Modifier.width(230.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            code,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(onClick = { viewModel.removeFlightFromComparison(code) }) {
                                            Icon(Icons.Filled.Close, contentDescription = "Remove")
                                        }
                                    }
                                    StatusBadge(status = d.status)
                                    HorizontalDivider()
                                    CompareRow("Route", "${d.depIata ?: "?"} → ${d.arrIata ?: "?"}")
                                    CompareRow("Airline", d.airlineName ?: d.airlineIata ?: "—")
                                    CompareRow("Dep Time", AviationFormatters.formatScheduleTime(d.depTime, d.depTimeUtc, settings))
                                    CompareRow("Arr Time", AviationFormatters.formatScheduleTime(d.arrTime, d.arrTimeUtc, settings))
                                    CompareRow("Delay", d.delayed?.let { "$it min" } ?: "None")
                                    CompareRow("Altitude", AviationFormatters.formatAltitude(d.altMeters, settings.altitudeUnit))
                                    CompareRow("Speed", AviationFormatters.formatSpeed(d.speedKmh, settings.speedUnit))
                                    CompareRow("Dep Gate", "T${d.depTerminal ?: "-"} / G${d.depGate ?: "-"}")
                                    CompareRow("Arr Gate", "T${d.arrTerminal ?: "-"} / G${d.arrGate ?: "-"}")
                                    CompareRow("Aircraft", d.model ?: d.aircraftIcao ?: "—")
                                    Spacer(modifier = Modifier.height(4.dp))
                                    androidx.compose.material3.FilledTonalButton(
                                        onClick = { onOpenFlightDetails(code) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Open Flight")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}
