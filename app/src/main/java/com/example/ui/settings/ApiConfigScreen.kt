package com.example.ui.settings

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.theme.AviationCoralRed
import com.example.ui.theme.AviationMintGreen
import com.example.utils.AviationFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiConfigScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val apiConfig by viewModel.apiConfig.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()

    var useGateway by remember { mutableStateOf(apiConfig.useGateway) }
    var apiKeyInput by remember { mutableStateOf(apiConfig.apiKey) }
    var gatewayUrlInput by remember { mutableStateOf(apiConfig.gatewayBaseUrl) }
    var gatewayTokenInput by remember { mutableStateOf(apiConfig.gatewayBearerToken) }
    var webhookUrlInput by remember { mutableStateOf(apiConfig.webhookUrl) }
    var showKey by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(apiConfig) {
        if (apiKeyInput.isBlank() && apiConfig.apiKey.isNotBlank()) apiKeyInput = apiConfig.apiKey
        if (gatewayUrlInput.isBlank() && apiConfig.gatewayBaseUrl.isNotBlank()) gatewayUrlInput = apiConfig.gatewayBaseUrl
        if (gatewayTokenInput.isBlank() && apiConfig.gatewayBearerToken.isNotBlank()) gatewayTokenInput = apiConfig.gatewayBearerToken
        if (webhookUrlInput.isBlank() && apiConfig.webhookUrl.isNotBlank()) webhookUrlInput = apiConfig.webhookUrl
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Configuration") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("api_config_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Connection Status Card
            item {
                val ok = apiConfig.lastConnectionSuccess
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        when (ok) {
                            true -> AviationMintGreen.copy(alpha = 0.55f)
                            false -> AviationCoralRed.copy(alpha = 0.55f)
                            null -> MaterialTheme.colorScheme.outlineVariant
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (ok == true) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = when (ok) {
                                    true -> AviationMintGreen
                                    false -> AviationCoralRed
                                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (ok) {
                                    true -> "Connection Verified"
                                    false -> "Connection Error"
                                    null -> "Connection Not Tested"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = feedbackMessage ?: apiConfig.lastStatusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (apiConfig.lastTestedAtEpochMs != null) {
                            Text(
                                text = "Last checked: ${AviationFormatters.formatTimestampShortMs(apiConfig.lastTestedAtEpochMs, settings)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (apiConfig.planType != null || apiConfig.limitsByMonth != null || apiConfig.keyExpiresAt != null) {
                            Text(
                                text = listOfNotNull(
                                    apiConfig.planType?.let { "Plan: $it" },
                                    apiConfig.limitsByHour?.let { "Hour limit: $it" },
                                    apiConfig.limitsByMonth?.let { "Month limit: $it" },
                                    apiConfig.keyExpiresAt?.let { "Expires: $it" }
                                ).joinToString(" • "),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Mode Toggle (Direct Key vs Secure Backend Gateway)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Use Secure Backend Gateway", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "Route AirLabs requests through your proxy server so your API key stays off the device.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = useGateway, onCheckedChange = { useGateway = it })
                        }

                        if (!useGateway) {
                            OutlinedTextField(
                                value = apiKeyInput,
                                onValueChange = { apiKeyInput = it },
                                label = { Text("AirLabs API Key") },
                                placeholder = { Text("Paste your AirLabs v9 key") },
                                singleLine = true,
                                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    Row {
                                        IconButton(onClick = { showKey = !showKey }) {
                                            Icon(
                                                if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = if (showKey) "Hide API Key" else "Show API Key"
                                            )
                                        }
                                        IconButton(onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            val pasted = cm?.primaryClip?.getItemAt(0)?.text?.toString()
                                            if (!pasted.isNullOrBlank()) {
                                                apiKeyInput = pasted.trim()
                                            }
                                        }) {
                                            Icon(Icons.Filled.ContentPaste, contentDescription = "Paste API Key")
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("api_key_input")
                            )
                            Text(
                                "Stored locally encrypted via Android Keystore AES-256-GCM. Never hardcoded in APK.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            OutlinedTextField(
                                value = gatewayUrlInput,
                                onValueChange = { gatewayUrlInput = it },
                                label = { Text("Backend Gateway Base URL") },
                                placeholder = { Text("https://api.yourserver.com/airlabs/v9/") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = gatewayTokenInput,
                                onValueChange = { gatewayTokenInput = it },
                                label = { Text("Gateway Bearer Token (Optional)") },
                                singleLine = true,
                                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showKey = !showKey }) {
                                        Icon(
                                            if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = "Show or hide token"
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = webhookUrlInput,
                            onValueChange = { webhookUrlInput = it },
                            label = { Text("Alert Webhook URL for /listen (Optional)") },
                            placeholder = { Text("https://yourserver.com/webhooks/airlabs-fcm") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    viewModel.saveApiConfig(
                                        useGateway = useGateway,
                                        apiKey = apiKeyInput,
                                        gatewayUrl = gatewayUrlInput,
                                        gatewayBearerToken = gatewayTokenInput,
                                        webhookUrl = webhookUrlInput
                                    )
                                    feedbackMessage = "Configuration saved locally."
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_api_config_button")
                            ) {
                                Text("Save")
                            }
                            Button(
                                onClick = {
                                    isTesting = true
                                    feedbackMessage = "Testing AirLabs v9 connection..."
                                    viewModel.testApiConnection(
                                        useGateway = useGateway,
                                        apiKey = apiKeyInput,
                                        gatewayUrl = gatewayUrlInput,
                                        gatewayBearerToken = gatewayTokenInput,
                                        webhookUrl = webhookUrlInput
                                    ) { _, msg ->
                                        isTesting = false
                                        feedbackMessage = msg
                                    }
                                },
                                enabled = !isTesting,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("test_api_connection_button")
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text("Test Connection")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                apiKeyInput = ""
                                gatewayUrlInput = ""
                                gatewayTokenInput = ""
                                webhookUrlInput = ""
                                viewModel.clearApiConfig()
                                feedbackMessage = "API configuration cleared."
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clear_api_config_button")
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear API Configuration")
                        }
                    }
                }
            }

            // FCM & Backend Webhook Notice
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Push Notifications (FCM & Webhooks)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "On-device WorkManager alert polling runs automatically for saved flight alerts. " +
                                "To enable instant server-side push via Firebase Cloud Messaging, connect your Firebase project in Firebase Console, " +
                                "add FCM messaging credentials, and configure your AirLabs /listen webhook endpoint above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
