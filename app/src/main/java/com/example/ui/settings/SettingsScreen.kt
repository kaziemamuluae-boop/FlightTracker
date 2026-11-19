package com.example.ui.settings

import android.content.pm.PackageManager
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.AltitudeUnit
import com.example.model.AppThemeMode
import com.example.model.DistanceUnit
import com.example.model.SpeedUnit
import com.example.model.TimeFormatMode
import com.example.model.TimezoneMode
import com.example.ui.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onOpenApiConfig: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val apiConfig by viewModel.apiConfig.collectAsStateWithLifecycle()
    val cacheCounts by viewModel.cacheCounts.collectAsStateWithLifecycle()

    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val appVersion = remember(context) {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            info.versionName ?: "1.0.0"
        }.getOrDefault("1.0.0")
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }

        // API Configuration Link
        item {
            SettingsActionCard(
                icon = Icons.Filled.Key,
                title = "API Configuration",
                subtitle = if (apiConfig.isConfigured) {
                    if (apiConfig.useGateway) "Gateway Mode • ${apiConfig.lastStatusMessage}"
                    else "AirLabs Key Configured • ${apiConfig.lastStatusMessage}"
                } else {
                    "Not configured — tap to add AirLabs API Key"
                },
                onClick = onOpenApiConfig,
                modifier = Modifier.testTag("settings_api_config_row")
            )
        }

        // Appearance & Language
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Theme & Language", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Appearance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.updateSettings { it.copy(themeMode = mode) } },
                                label = {
                                    Text(
                                        when (mode) {
                                            AppThemeMode.DARK -> "Dark"
                                            AppThemeMode.LIGHT -> "Light"
                                            AppThemeMode.SYSTEM -> "System"
                                        }
                                    )
                                },
                                modifier = Modifier.testTag("theme_${mode.name.lowercase()}")
                            )
                        }
                    }

                    Text("Language", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("en" to "English", "es" to "Español", "fr" to "Français", "de" to "Deutsch").forEach { (code, name) ->
                            FilterChip(
                                selected = settings.languageCode == code,
                                onClick = { viewModel.updateSettings { it.copy(languageCode = code) } },
                                label = { Text(name) }
                            )
                        }
                    }
                }
            }
        }

        // Aviation Units & Time
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Units & Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    UnitToggleRow(
                        label = "Distance",
                        options = listOf(DistanceUnit.KM to "Kilometers", DistanceUnit.MI to "Miles"),
                        selected = settings.distanceUnit,
                        onSelect = { unit -> viewModel.updateSettings { it.copy(distanceUnit = unit) } }
                    )
                    UnitToggleRow(
                        label = "Speed",
                        options = listOf(SpeedUnit.KTS to "Knots", SpeedUnit.KMH to "km/h"),
                        selected = settings.speedUnit,
                        onSelect = { unit -> viewModel.updateSettings { it.copy(speedUnit = unit) } }
                    )
                    UnitToggleRow(
                        label = "Altitude",
                        options = listOf(AltitudeUnit.FT to "Feet", AltitudeUnit.M to "Meters"),
                        selected = settings.altitudeUnit,
                        onSelect = { unit -> viewModel.updateSettings { it.copy(altitudeUnit = unit) } }
                    )
                    UnitToggleRow(
                        label = "Time Format",
                        options = listOf(TimeFormatMode.H24 to "24-Hour", TimeFormatMode.H12 to "12-Hour"),
                        selected = settings.timeFormat,
                        onSelect = { fmt -> viewModel.updateSettings { it.copy(timeFormat = fmt) } }
                    )
                    UnitToggleRow(
                        label = "Timezone",
                        options = listOf(TimezoneMode.AIRPORT_LOCAL to "Airport Local", TimezoneMode.UTC to "UTC"),
                        selected = settings.timezoneMode,
                        onSelect = { tz -> viewModel.updateSettings { it.copy(timezoneMode = tz) } }
                    )
                }
            }
        }

        // Map Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Map Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Text("Live Refresh Frequency", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15 to "15s", 30 to "30s", 60 to "60s", 0 to "Manual").forEach { (sec, lbl) ->
                            FilterChip(
                                selected = settings.autoRefreshSeconds == sec,
                                onClick = { viewModel.updateSettings { it.copy(autoRefreshSeconds = sec) } },
                                label = { Text(lbl) }
                            )
                        }
                    }

                    SettingsSwitchRow("Aircraft Marker Clustering", settings.clusteringEnabled) { v ->
                        viewModel.updateSettings { it.copy(clusteringEnabled = v) }
                    }
                    SettingsSwitchRow("Show Flight Labels on Globe", settings.showFlightLabels) { v ->
                        viewModel.updateSettings { it.copy(showFlightLabels = v) }
                    }
                }
            }
        }

        // Notification Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text("Default Alert Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    SettingsSwitchRow("Departure Alerts", settings.notifyDeparture) { v ->
                        viewModel.updateSettings { it.copy(notifyDeparture = v) }
                    }
                    SettingsSwitchRow("Takeoff Alerts", settings.notifyTakeoff) { v ->
                        viewModel.updateSettings { it.copy(notifyTakeoff = v) }
                    }
                    SettingsSwitchRow("Arrival & Landed Alerts", settings.notifyArrival && settings.notifyLanded) { v ->
                        viewModel.updateSettings { it.copy(notifyArrival = v, notifyLanded = v) }
                    }
                    SettingsSwitchRow("Delay & Cancellation Alerts", settings.notifyDelayed && settings.notifyCancelled) { v ->
                        viewModel.updateSettings { it.copy(notifyDelayed = v, notifyCancelled = v) }
                    }
                    SettingsSwitchRow("Gate & Terminal Changes", settings.notifyGateChange && settings.notifyTerminalChange) { v ->
                        viewModel.updateSettings { it.copy(notifyGateChange = v, notifyTerminalChange = v) }
                    }
                }
            }
        }

        // Cache & History Management
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Storage & Cache", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Cached: ${cacheCounts.first} airports, ${cacheCounts.second} airlines",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.clearApiCaches() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Cached, contentDescription = null)
                            Spacer(modifier = Modifier.padding(3.dp))
                            Text("Clear Cache")
                        }
                        OutlinedButton(
                            onClick = { viewModel.clearSearchHistory() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.History, contentDescription = null)
                            Spacer(modifier = Modifier.padding(3.dp))
                            Text("Clear History")
                        }
                    }
                }
            }
        }

        // Privacy Policy & About
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPrivacyDialog = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Policy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.padding(6.dp))
                            Text("Privacy Policy", style = MaterialTheme.typography.titleSmall)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAboutDialog = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.padding(6.dp))
                            Column {
                                Text("About Flight Tracker", style = MaterialTheme.typography.titleSmall)
                                Text("Version $appVersion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy") },
            text = { Text(stringResource(id = R.string.privacy_policy_summary)) },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Close") }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Flight Tracker") },
            text = {
                Text(
                    "Version $appVersion\n\n" +
                        "Real-time global aviation tracking powered by AirLabs Aviation API v9, Open-Meteo airport weather, " +
                        "OpenStreetMap / Esri / CartoDB map tiles, and Room local persistence."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.padding(6.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
private fun <T> UnitToggleRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, title) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(title) }
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
