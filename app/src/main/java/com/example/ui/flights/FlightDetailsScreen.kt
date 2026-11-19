package com.example.ui.flights

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.FavoriteFlightEntity
import com.example.data.local.FlightAlertEntity
import com.example.ui.MainViewModel
import com.example.ui.components.AviationErrorCard
import com.example.ui.components.ShimmerLoadingCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.WeatherCard
import com.example.ui.theme.AviationAmberWarn
import com.example.ui.theme.AviationCyanHighlight
import com.example.ui.theme.AviationMintGreen
import com.example.ui.theme.AviationSkyBlue
import com.example.utils.AviationFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailsScreen(
    flightCode: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onTrackLive: (String) -> Unit,
    onOpenAirport: (String) -> Unit,
    onOpenAirline: (String) -> Unit,
    onOpenAircraft: (String) -> Unit,
    onOpenComparison: () -> Unit,
    onOpenApiConfig: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.flightDetailState.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val apiConfig by viewModel.apiConfig.collectAsStateWithLifecycle()
    val isFav by viewModel.isFlightFavorite(flightCode).collectAsStateWithLifecycle(initialValue = false)
    val existingAlert by viewModel.observeAlertForFlight(flightCode).collectAsStateWithLifecycle(initialValue = null)

    var showAlertSheet by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        showAlertSheet = true
    }

    LaunchedEffect(flightCode) {
        viewModel.loadFlightDetails(flightCode)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.detail?.flightIata ?: flightCode) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("flight_details_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadFlightDetails(flightCode) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh flight")
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (state.isLoading && state.detail == null) {
                items(4) { ShimmerLoadingCard() }
            } else if (state.error != null && state.detail == null) {
                item {
                    AviationErrorCard(
                        error = state.error!!,
                        onRetry = { viewModel.loadFlightDetails(flightCode) },
                        onOpenApiConfig = onOpenApiConfig
                    )
                }
            } else if (state.detail != null) {
                val d = state.detail!!
                val code = d.flightIata ?: d.flightIcao ?: flightCode

                // Header Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(enabled = !d.airlineIata.isNullOrBlank()) {
                                            d.airlineIata?.let(onOpenAirline)
                                        }
                                ) {
                                    Text(
                                        text = d.airlineName ?: d.airlineIata ?: "Airline",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = listOfNotNull(d.flightIata, d.flightIcao).distinct().joinToString(" / "),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                StatusBadge(status = d.status)
                            }
                            if (!d.csFlightIata.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Codeshare: ${d.csAirlineIata ?: ""} ${d.csFlightIata}".trim(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val delayMin = d.delayed ?: d.depDelayed ?: d.arrDelayed
                            if (delayMin != null && delayMin > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Reported delay: $delayMin min",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AviationAmberWarn
                                )
                            }
                        }
                    }
                }

                // Primary Actions Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onTrackLive(code) },
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("track_live_button")
                        ) {
                            Icon(Icons.Filled.Radar, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Track Live")
                        }
                        FilledTonalButton(
                            onClick = {
                                viewModel.toggleFavoriteFlight(
                                    FavoriteFlightEntity(
                                        flightCode = code,
                                        flightIata = d.flightIata,
                                        flightIcao = d.flightIcao,
                                        airlineName = d.airlineName,
                                        airlineIata = d.airlineIata,
                                        depIata = d.depIata,
                                        arrIata = d.arrIata,
                                        lastStatus = d.status,
                                        regNumber = d.regNumber
                                    ),
                                    currentlyFav = isFav
                                )
                            },
                            modifier = Modifier.testTag("favorite_flight_button")
                        ) {
                            Icon(
                                if (isFav) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "Favorite flight",
                                tint = if (isFav) AviationAmberWarn else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    showAlertSheet = true
                                }
                            },
                            modifier = Modifier.testTag("set_alert_button")
                        ) {
                            Icon(
                                if (existingAlert?.enabled == true) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
                                contentDescription = "Flight alerts",
                                tint = if (existingAlert?.enabled == true) AviationMintGreen else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                val shareText = buildString {
                                    append("Flight $code (${d.airlineName ?: d.airlineIata ?: ""})\n")
                                    append("Route: ${d.depIata ?: "?"} → ${d.arrIata ?: "?"}\n")
                                    append("Status: ${AviationFormatters.statusDisplay(d.status)}\n")
                                    if (d.lat != null && d.lng != null) {
                                        append("Live Position: ${AviationFormatters.formatCoordinates(d.lat, d.lng)}\n")
                                        append("Altitude: ${AviationFormatters.formatAltitude(d.altMeters, settings.altitudeUnit)}\n")
                                        append("https://www.openstreetmap.org/?mlat=${d.lat}&mlon=${d.lng}#map=7/${d.lat}/${d.lng}")
                                    }
                                }
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Flight $code"))
                            },
                            modifier = Modifier.testTag("share_flight_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share flight")
                        }
                    }
                }

                // Flight Progress & Route Map Visualization
                item {
                    val depAp = state.depAirport
                    val arrAp = state.arrAirport
                    val totalKm = if (depAp?.lat != null && depAp.lng != null && arrAp?.lat != null && arrAp.lng != null) {
                        AviationFormatters.haversineKm(depAp.lat, depAp.lng, arrAp.lat, arrAp.lng)
                    } else null
                    val coveredKm = if (depAp?.lat != null && depAp.lng != null && d.lat != null && d.lng != null) {
                        AviationFormatters.haversineKm(depAp.lat, depAp.lng, d.lat, d.lng)
                    } else null
                    val remainingKm = if (arrAp?.lat != null && arrAp.lng != null && d.lat != null && d.lng != null) {
                        AviationFormatters.haversineKm(d.lat, d.lng, arrAp.lat, arrAp.lng)
                    } else null
                    val progress = when {
                        d.status.equals("landed", true) -> 1f
                        d.status.equals("scheduled", true) -> 0f
                        totalKm != null && totalKm > 1.0 && remainingKm != null ->
                            (1f - (remainingKm / totalKm).toFloat()).coerceIn(0.03f, 0.98f)
                        else -> null
                    }
                    val estRemainingMin = if (remainingKm != null && (d.speedKmh ?: 0.0) > 80.0) {
                        ((remainingKm / d.speedKmh!!) * 60.0).toInt()
                    } else null

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Flight Progress & Route", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                OutlinedButton(
                                    onClick = {
                                        viewModel.addFlightToComparison(d)
                                        onOpenComparison()
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.CompareArrows, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Compare", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Route arc mini-canvas
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(78.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val start = Offset(28f, size.height * 0.72f)
                                    val end = Offset(size.width - 28f, size.height * 0.72f)
                                    val peakY = 16f
                                    val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                                    val steps = 40
                                    for (i in 0 until steps) {
                                        val t1 = i.toFloat() / steps
                                        val t2 = (i + 1).toFloat() / steps
                                        val p1 = quadPoint(start, Offset(size.width / 2f, peakY), end, t1)
                                        val p2 = quadPoint(start, Offset(size.width / 2f, peakY), end, t2)
                                        val completed = progress != null && t1 <= progress
                                        drawLine(
                                            color = if (completed) AviationCyanHighlight else Color(0xFF475569),
                                            start = p1,
                                            end = p2,
                                            strokeWidth = if (completed) 5f else 3f,
                                            pathEffect = if (completed) null else dash
                                        )
                                    }
                                    drawCircle(AviationMintGreen, 9f, start)
                                    drawCircle(AviationSkyBlue, 9f, end)
                                    if (progress != null) {
                                        val planePt = quadPoint(start, Offset(size.width / 2f, peakY), end, progress)
                                        drawCircle(AviationCyanHighlight, 13f, planePt)
                                        drawCircle(Color.White, 6f, planePt)
                                    }
                                }
                            }

                            if (progress != null) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                DetailStat("Covered", AviationFormatters.formatDistance(coveredKm, settings.distanceUnit))
                                DetailStat("Remaining", AviationFormatters.formatDistance(remainingKm, settings.distanceUnit))
                                DetailStat("Duration", AviationFormatters.formatDuration(d.duration))
                                DetailStat("Est. Left", AviationFormatters.formatDuration(estRemainingMin))
                            }

                            if (d.lat != null && d.lng != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Position: ${AviationFormatters.formatCoordinates(d.lat, d.lng)} • Alt ${AviationFormatters.formatAltitude(d.altMeters, settings.altitudeUnit)} • Spd ${AviationFormatters.formatSpeed(d.speedKmh, settings.speedUnit)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Departure & Arrival Information Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SegmentCard(
                            title = "Departure",
                            code = d.depIata ?: d.depIcao ?: "---",
                            airportName = d.depName ?: state.depAirport?.name,
                            cityCountry = listOfNotNull(d.depCity ?: state.depAirport?.city, d.depCountry ?: state.depAirport?.countryCode).joinToString(", "),
                            scheduled = AviationFormatters.formatScheduleTime(d.depTime, d.depTimeUtc, settings),
                            estimatedOrActualLabel = if (!d.depActual.isNullOrBlank()) "Actual" else "Estimated",
                            estimatedOrActual = AviationFormatters.formatScheduleTime(d.depActual ?: d.depEstimated, d.depActualUtc ?: d.depEstimatedUtc, settings),
                            terminal = d.depTerminal,
                            gate = d.depGate,
                            onClick = { d.depIata?.let(onOpenAirport) },
                            modifier = Modifier.weight(1f)
                        )
                        SegmentCard(
                            title = "Arrival",
                            code = d.arrIata ?: d.arrIcao ?: "---",
                            airportName = d.arrName ?: state.arrAirport?.name,
                            cityCountry = listOfNotNull(d.arrCity ?: state.arrAirport?.city, d.arrCountry ?: state.arrAirport?.countryCode).joinToString(", "),
                            scheduled = AviationFormatters.formatScheduleTime(d.arrTime, d.arrTimeUtc, settings),
                            estimatedOrActualLabel = if (!d.arrActual.isNullOrBlank()) "Actual" else "Estimated",
                            estimatedOrActual = AviationFormatters.formatScheduleTime(d.arrActual ?: d.arrEstimated, d.arrActualUtc ?: d.arrEstimatedUtc, settings),
                            terminal = d.arrTerminal,
                            gate = d.arrGate,
                            onClick = { d.arrIata?.let(onOpenAirport) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Weather Cards
                state.depWeather?.let { w ->
                    item { WeatherCard(weather = w, settings = settings) }
                }
                state.arrWeather?.let { w ->
                    item { WeatherCard(weather = w, settings = settings) }
                }

                // Aircraft Information Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !d.regNumber.isNullOrBlank()) {
                                d.regNumber?.let(onOpenAircraft)
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Aircraft Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))
                            Image(
                                painter = painterResource(id = R.drawable.img_aircraft_placeholder_1795119215265),
                                contentDescription = "Aircraft profile illustration",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                DetailStat("Model", d.model ?: d.aircraftIcao ?: "Unavailable")
                                DetailStat("Manufacturer", d.manufacturer ?: "Unavailable")
                                DetailStat("Registration", d.regNumber ?: "Unavailable")
                            }
                            if (d.built != null || d.age != null || !d.engine.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = listOfNotNull(
                                        d.built?.let { "Built $it" },
                                        d.age?.let { "Age $it yrs" },
                                        d.engine?.let { "Engine: $it" }
                                    ).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAlertSheet) {
        val d = state.detail
        val code = d?.flightIata ?: d?.flightIcao ?: flightCode
        var notifyDep by remember(existingAlert) { mutableStateOf(existingAlert?.notifyDeparture ?: settings.notifyDeparture) }
        var notifyTakeoff by remember(existingAlert) { mutableStateOf(existingAlert?.notifyTakeoff ?: settings.notifyTakeoff) }
        var notifyArr by remember(existingAlert) { mutableStateOf(existingAlert?.notifyArrival ?: settings.notifyArrival) }
        var notifyLanded by remember(existingAlert) { mutableStateOf(existingAlert?.notifyLanded ?: settings.notifyLanded) }
        var notifyDelayed by remember(existingAlert) { mutableStateOf(existingAlert?.notifyDelayed ?: settings.notifyDelayed) }
        var notifyCancelled by remember(existingAlert) { mutableStateOf(existingAlert?.notifyCancelled ?: settings.notifyCancelled) }
        var notifyGate by remember(existingAlert) { mutableStateOf(existingAlert?.notifyGateChange ?: settings.notifyGateChange) }
        var notifyTerm by remember(existingAlert) { mutableStateOf(existingAlert?.notifyTerminalChange ?: settings.notifyTerminalChange) }
        var notifyStatus by remember(existingAlert) { mutableStateOf(existingAlert?.notifyStatusChange ?: settings.notifyStatusChange) }

        ModalBottomSheet(onDismissRequest = { showAlertSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Flight Alerts for $code", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Alerts monitor confirmed AirLabs updates in the background" +
                        if (apiConfig.webhookUrl.isNotBlank()) " and register your webhook listener." else ".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider()
                AlertToggleRow("Flight Departure", notifyDep) { notifyDep = it }
                AlertToggleRow("Flight Takeoff", notifyTakeoff) { notifyTakeoff = it }
                AlertToggleRow("Flight Arrival", notifyArr) { notifyArr = it }
                AlertToggleRow("Flight Landed", notifyLanded) { notifyLanded = it }
                AlertToggleRow("Flight Delayed", notifyDelayed) { notifyDelayed = it }
                AlertToggleRow("Flight Cancelled", notifyCancelled) { notifyCancelled = it }
                AlertToggleRow("Gate Changed", notifyGate) { notifyGate = it }
                AlertToggleRow("Terminal Changed", notifyTerm) { notifyTerm = it }
                AlertToggleRow("Status Changed", notifyStatus) { notifyStatus = it }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (existingAlert != null) {
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteFlightAlert(code)
                                showAlertSheet = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Remove Alert")
                        }
                    }
                    Button(
                        onClick = {
                            viewModel.saveFlightAlert(
                                FlightAlertEntity(
                                    flightCode = code,
                                    airlineName = d?.airlineName,
                                    depIata = d?.depIata,
                                    arrIata = d?.arrIata,
                                    enabled = true,
                                    notifyDeparture = notifyDep,
                                    notifyTakeoff = notifyTakeoff,
                                    notifyArrival = notifyArr,
                                    notifyLanded = notifyLanded,
                                    notifyDelayed = notifyDelayed,
                                    notifyCancelled = notifyCancelled,
                                    notifyGateChange = notifyGate,
                                    notifyTerminalChange = notifyTerm,
                                    notifyStatusChange = notifyStatus,
                                    lastStatus = d?.status,
                                    lastDepGate = d?.depGate,
                                    lastDepTerminal = d?.depTerminal,
                                    lastArrGate = d?.arrGate,
                                    lastArrTerminal = d?.arrTerminal,
                                    lastDelayedMin = d?.delayed
                                ),
                                registerWebhook = apiConfig.webhookUrl.isNotBlank()
                            )
                            showAlertSheet = false
                        },
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text("Save Alert")
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun AlertToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SegmentCard(
    title: String,
    code: String,
    airportName: String?,
    cityCountry: String,
    scheduled: String,
    estimatedOrActualLabel: String,
    estimatedOrActual: String,
    terminal: String?,
    gate: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(code, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (!airportName.isNullOrBlank()) {
                Text(airportName, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            if (cityCountry.isNotBlank()) {
                Text(cityCountry, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Text("Sched: $scheduled", style = MaterialTheme.typography.labelMedium)
            Text("$estimatedOrActualLabel: $estimatedOrActual", style = MaterialTheme.typography.labelMedium)
            Text(
                text = "Term ${terminal ?: "—"} • Gate ${gate ?: "—"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

private fun quadPoint(p0: Offset, p1: Offset, p2: Offset, t: Float): Offset {
    val u = 1f - t
    val x = u * u * p0.x + 2 * u * t * p1.x + t * t * p2.x
    val y = u * u * p0.y + 2 * u * t * p1.y + t * t * p2.y
    return Offset(x, y)
}
