package com.example.ui.aircraft

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Flight
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.FavoriteAircraftEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AviationErrorCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ShimmerLoadingCard
import com.example.ui.theme.AviationAmberWarn
import com.example.utils.AviationFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AircraftScreen(
    initialRegOrHex: String?,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenFlightDetails: (String) -> Unit,
    onOpenApiConfig: () -> Unit
) {
    val state by viewModel.aircraftState.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    var query by remember(initialRegOrHex) { mutableStateOf(initialRegOrHex.orEmpty()) }
    val primary = state.results.firstOrNull()
    val favKey = primary?.regNumber ?: query.uppercase()
    val isFav by viewModel.isAircraftFavorite(favKey).collectAsStateWithLifecycle(initialValue = false)

    LaunchedEffect(initialRegOrHex) {
        if (!initialRegOrHex.isNullOrBlank()) {
            viewModel.lookupAircraft(initialRegOrHex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aircraft Specifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (primary != null && !primary.regNumber.isNullOrBlank()) {
                        IconButton(onClick = {
                            viewModel.toggleFavoriteAircraft(
                                FavoriteAircraftEntity(
                                    regNumber = primary.regNumber,
                                    hex = primary.hex,
                                    model = primary.model,
                                    manufacturer = primary.manufacturer,
                                    aircraftIcao = primary.icao,
                                    airlineIata = primary.airlineIata,
                                    builtYear = primary.built
                                ),
                                currentlyFav = isFav
                            )
                        }) {
                            Icon(
                                if (isFav) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "Favorite aircraft",
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
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Registration (9V-SFM) or ICAO Hex") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.lookupAircraft(query) }),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { viewModel.lookupAircraft(query) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(54.dp)
                    ) {
                        Text("Lookup")
                    }
                }
            }

            if (state.error != null) {
                item {
                    AviationErrorCard(
                        error = state.error!!,
                        onRetry = { viewModel.lookupAircraft(query) },
                        onOpenApiConfig = onOpenApiConfig
                    )
                }
            }

            if (state.isLoading) {
                items(2) { ShimmerLoadingCard() }
            }

            if (state.results.isEmpty() && !state.isLoading && state.error == null) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.Flight,
                        title = "Aircraft Registry Lookup",
                        subtitle = "Search by tail registration number or Mode-S 24-bit ICAO hex code to view verified airframe metadata."
                    )
                }
            }

            items(state.results) { ac ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_aircraft_placeholder_1795119215265),
                            contentDescription = "Aircraft profile illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Text(
                            text = ac.model ?: ac.icao ?: "Unknown Model",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        SpecRow("Manufacturer", ac.manufacturer ?: "Unavailable")
                        SpecRow("Registration", ac.regNumber ?: "Unavailable")
                        SpecRow("ICAO Aircraft Type", ac.icao ?: "Unavailable")
                        SpecRow("Mode-S Hex", ac.hex ?: "Unavailable")
                        SpecRow("Airline Operator", ac.airlineIata ?: ac.airlineIcao ?: "Unavailable")
                        SpecRow("Manufacturing Year", ac.built?.toString() ?: "Unavailable")
                        SpecRow("Aircraft Age", ac.age?.let { "$it years" } ?: "Unavailable")
                        SpecRow(
                            "Engine Details",
                            listOfNotNull(ac.engineCount?.let { "${it}x" }, ac.engine).joinToString(" ").ifBlank { "Unavailable" }
                        )
                        if (!ac.msn.isNullOrBlank()) {
                            SpecRow("Serial Number (MSN)", ac.msn)
                        }
                    }
                }
            }

            state.livePosition?.let { live ->
                val code = live.flightIata ?: live.flightIcao
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Currently Airborne", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Flight ${code ?: "—"} (${live.depIata ?: "?"} → ${live.arrIata ?: "?"})")
                            Text(
                                "Alt ${AviationFormatters.formatAltitude(live.altMeters, settings.altitudeUnit)} • Spd ${AviationFormatters.formatSpeed(live.speedKmh, settings.speedUnit)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (!code.isNullOrBlank()) {
                                Button(onClick = { onOpenFlightDetails(code) }) {
                                    Text("Open Live Flight")
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
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}
