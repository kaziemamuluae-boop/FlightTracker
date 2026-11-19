package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AltitudeUnit
import com.example.model.ApiConfiguration
import com.example.model.AppThemeMode
import com.example.model.DistanceUnit
import com.example.model.MapLayerStyle
import com.example.model.SpeedUnit
import com.example.model.TimeFormatMode
import com.example.model.TimezoneMode
import com.example.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.flightTrackerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "flight_tracker_prefs"
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        val SPEED_UNIT = stringPreferencesKey("speed_unit")
        val ALTITUDE_UNIT = stringPreferencesKey("altitude_unit")
        val TIME_FORMAT = stringPreferencesKey("time_format")
        val TIMEZONE_MODE = stringPreferencesKey("timezone_mode")
        val MAP_LAYER = stringPreferencesKey("map_layer")
        val AUTO_REFRESH_SEC = intPreferencesKey("auto_refresh_sec")
        val CLUSTERING = booleanPreferencesKey("clustering")
        val SHOW_LABELS = booleanPreferencesKey("show_labels")
        val LANGUAGE = stringPreferencesKey("language")

        val NOTIFY_DEP = booleanPreferencesKey("notify_dep")
        val NOTIFY_TAKEOFF = booleanPreferencesKey("notify_takeoff")
        val NOTIFY_ARR = booleanPreferencesKey("notify_arr")
        val NOTIFY_LANDED = booleanPreferencesKey("notify_landed")
        val NOTIFY_DELAYED = booleanPreferencesKey("notify_delayed")
        val NOTIFY_CANCELLED = booleanPreferencesKey("notify_cancelled")
        val NOTIFY_GATE = booleanPreferencesKey("notify_gate")
        val NOTIFY_TERMINAL = booleanPreferencesKey("notify_terminal")
        val NOTIFY_STATUS = booleanPreferencesKey("notify_status")

        val USE_GATEWAY = booleanPreferencesKey("use_gateway")
        val API_KEY_ENC = stringPreferencesKey("api_key_enc")
        val GATEWAY_URL = stringPreferencesKey("gateway_url")
        val GATEWAY_TOKEN_ENC = stringPreferencesKey("gateway_token_enc")
        val WEBHOOK_URL = stringPreferencesKey("webhook_url")
        val LAST_TESTED_MS = longPreferencesKey("last_tested_ms")
        val LAST_CONN_OK = booleanPreferencesKey("last_conn_ok")
        val LAST_STATUS_MSG = stringPreferencesKey("last_status_msg")
        val PLAN_TYPE = stringPreferencesKey("plan_type")
        val LIMITS_HOUR = intPreferencesKey("limits_hour")
        val LIMITS_MONTH = intPreferencesKey("limits_month")
        val LIMITS_TOTAL = intPreferencesKey("limits_total")
        val KEY_EXPIRES = stringPreferencesKey("key_expires")
    }

    val userSettings: Flow<UserSettings> = context.flightTrackerDataStore.data.map { prefs ->
        UserSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
                ?: AppThemeMode.DARK,
            distanceUnit = prefs[Keys.DISTANCE_UNIT]?.let { runCatching { DistanceUnit.valueOf(it) }.getOrNull() }
                ?: DistanceUnit.KM,
            speedUnit = prefs[Keys.SPEED_UNIT]?.let { runCatching { SpeedUnit.valueOf(it) }.getOrNull() }
                ?: SpeedUnit.KTS,
            altitudeUnit = prefs[Keys.ALTITUDE_UNIT]?.let { runCatching { AltitudeUnit.valueOf(it) }.getOrNull() }
                ?: AltitudeUnit.FT,
            timeFormat = prefs[Keys.TIME_FORMAT]?.let { runCatching { TimeFormatMode.valueOf(it) }.getOrNull() }
                ?: TimeFormatMode.H24,
            timezoneMode = prefs[Keys.TIMEZONE_MODE]?.let { runCatching { TimezoneMode.valueOf(it) }.getOrNull() }
                ?: TimezoneMode.AIRPORT_LOCAL,
            mapLayerStyle = prefs[Keys.MAP_LAYER]?.let { runCatching { MapLayerStyle.valueOf(it) }.getOrNull() }
                ?: MapLayerStyle.DARK_RADAR,
            autoRefreshSeconds = prefs[Keys.AUTO_REFRESH_SEC] ?: 30,
            clusteringEnabled = prefs[Keys.CLUSTERING] ?: true,
            showFlightLabels = prefs[Keys.SHOW_LABELS] ?: true,
            languageCode = prefs[Keys.LANGUAGE] ?: "en",
            notifyDeparture = prefs[Keys.NOTIFY_DEP] ?: true,
            notifyTakeoff = prefs[Keys.NOTIFY_TAKEOFF] ?: true,
            notifyArrival = prefs[Keys.NOTIFY_ARR] ?: true,
            notifyLanded = prefs[Keys.NOTIFY_LANDED] ?: true,
            notifyDelayed = prefs[Keys.NOTIFY_DELAYED] ?: true,
            notifyCancelled = prefs[Keys.NOTIFY_CANCELLED] ?: true,
            notifyGateChange = prefs[Keys.NOTIFY_GATE] ?: true,
            notifyTerminalChange = prefs[Keys.NOTIFY_TERMINAL] ?: true,
            notifyStatusChange = prefs[Keys.NOTIFY_STATUS] ?: true
        )
    }

    val apiConfiguration: Flow<ApiConfiguration> = context.flightTrackerDataStore.data.map { prefs ->
        ApiConfiguration(
            useGateway = prefs[Keys.USE_GATEWAY] ?: false,
            apiKey = KeystoreCrypto.decrypt(prefs[Keys.API_KEY_ENC].orEmpty()),
            gatewayBaseUrl = prefs[Keys.GATEWAY_URL].orEmpty(),
            gatewayBearerToken = KeystoreCrypto.decrypt(prefs[Keys.GATEWAY_TOKEN_ENC].orEmpty()),
            webhookUrl = prefs[Keys.WEBHOOK_URL].orEmpty(),
            lastTestedAtEpochMs = prefs[Keys.LAST_TESTED_MS],
            lastConnectionSuccess = prefs[Keys.LAST_CONN_OK],
            lastStatusMessage = prefs[Keys.LAST_STATUS_MSG] ?: "Not tested",
            planType = prefs[Keys.PLAN_TYPE],
            limitsByHour = prefs[Keys.LIMITS_HOUR],
            limitsByMonth = prefs[Keys.LIMITS_MONTH],
            limitsTotal = prefs[Keys.LIMITS_TOTAL],
            keyExpiresAt = prefs[Keys.KEY_EXPIRES]
        )
    }

    suspend fun updateSettings(transform: (UserSettings) -> UserSettings) {
        context.flightTrackerDataStore.edit { prefs ->
            val current = UserSettings(
                themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
                    ?: AppThemeMode.DARK,
                distanceUnit = prefs[Keys.DISTANCE_UNIT]?.let { runCatching { DistanceUnit.valueOf(it) }.getOrNull() }
                    ?: DistanceUnit.KM,
                speedUnit = prefs[Keys.SPEED_UNIT]?.let { runCatching { SpeedUnit.valueOf(it) }.getOrNull() }
                    ?: SpeedUnit.KTS,
                altitudeUnit = prefs[Keys.ALTITUDE_UNIT]?.let { runCatching { AltitudeUnit.valueOf(it) }.getOrNull() }
                    ?: AltitudeUnit.FT,
                timeFormat = prefs[Keys.TIME_FORMAT]?.let { runCatching { TimeFormatMode.valueOf(it) }.getOrNull() }
                    ?: TimeFormatMode.H24,
                timezoneMode = prefs[Keys.TIMEZONE_MODE]?.let { runCatching { TimezoneMode.valueOf(it) }.getOrNull() }
                    ?: TimezoneMode.AIRPORT_LOCAL,
                mapLayerStyle = prefs[Keys.MAP_LAYER]?.let { runCatching { MapLayerStyle.valueOf(it) }.getOrNull() }
                    ?: MapLayerStyle.DARK_RADAR,
                autoRefreshSeconds = prefs[Keys.AUTO_REFRESH_SEC] ?: 30,
                clusteringEnabled = prefs[Keys.CLUSTERING] ?: true,
                showFlightLabels = prefs[Keys.SHOW_LABELS] ?: true,
                languageCode = prefs[Keys.LANGUAGE] ?: "en",
                notifyDeparture = prefs[Keys.NOTIFY_DEP] ?: true,
                notifyTakeoff = prefs[Keys.NOTIFY_TAKEOFF] ?: true,
                notifyArrival = prefs[Keys.NOTIFY_ARR] ?: true,
                notifyLanded = prefs[Keys.NOTIFY_LANDED] ?: true,
                notifyDelayed = prefs[Keys.NOTIFY_DELAYED] ?: true,
                notifyCancelled = prefs[Keys.NOTIFY_CANCELLED] ?: true,
                notifyGateChange = prefs[Keys.NOTIFY_GATE] ?: true,
                notifyTerminalChange = prefs[Keys.NOTIFY_TERMINAL] ?: true,
                notifyStatusChange = prefs[Keys.NOTIFY_STATUS] ?: true
            )
            val updated = transform(current)
            prefs[Keys.THEME_MODE] = updated.themeMode.name
            prefs[Keys.DISTANCE_UNIT] = updated.distanceUnit.name
            prefs[Keys.SPEED_UNIT] = updated.speedUnit.name
            prefs[Keys.ALTITUDE_UNIT] = updated.altitudeUnit.name
            prefs[Keys.TIME_FORMAT] = updated.timeFormat.name
            prefs[Keys.TIMEZONE_MODE] = updated.timezoneMode.name
            prefs[Keys.MAP_LAYER] = updated.mapLayerStyle.name
            prefs[Keys.AUTO_REFRESH_SEC] = updated.autoRefreshSeconds
            prefs[Keys.CLUSTERING] = updated.clusteringEnabled
            prefs[Keys.SHOW_LABELS] = updated.showFlightLabels
            prefs[Keys.LANGUAGE] = updated.languageCode
            prefs[Keys.NOTIFY_DEP] = updated.notifyDeparture
            prefs[Keys.NOTIFY_TAKEOFF] = updated.notifyTakeoff
            prefs[Keys.NOTIFY_ARR] = updated.notifyArrival
            prefs[Keys.NOTIFY_LANDED] = updated.notifyLanded
            prefs[Keys.NOTIFY_DELAYED] = updated.notifyDelayed
            prefs[Keys.NOTIFY_CANCELLED] = updated.notifyCancelled
            prefs[Keys.NOTIFY_GATE] = updated.notifyGateChange
            prefs[Keys.NOTIFY_TERMINAL] = updated.notifyTerminalChange
            prefs[Keys.NOTIFY_STATUS] = updated.notifyStatusChange
        }
    }

    suspend fun saveApiConfiguration(
        useGateway: Boolean,
        apiKey: String,
        gatewayUrl: String,
        gatewayBearerToken: String,
        webhookUrl: String
    ) {
        context.flightTrackerDataStore.edit { prefs ->
            prefs[Keys.USE_GATEWAY] = useGateway
            prefs[Keys.API_KEY_ENC] = KeystoreCrypto.encrypt(apiKey.trim())
            prefs[Keys.GATEWAY_URL] = gatewayUrl.trim()
            prefs[Keys.GATEWAY_TOKEN_ENC] = KeystoreCrypto.encrypt(gatewayBearerToken.trim())
            prefs[Keys.WEBHOOK_URL] = webhookUrl.trim()
        }
    }

    suspend fun saveConnectionTestResult(
        success: Boolean,
        message: String,
        planType: String?,
        limitsByHour: Int?,
        limitsByMonth: Int?,
        limitsTotal: Int?,
        expiresAt: String?
    ) {
        context.flightTrackerDataStore.edit { prefs ->
            prefs[Keys.LAST_TESTED_MS] = System.currentTimeMillis()
            prefs[Keys.LAST_CONN_OK] = success
            prefs[Keys.LAST_STATUS_MSG] = message
            if (planType != null) prefs[Keys.PLAN_TYPE] = planType else prefs.remove(Keys.PLAN_TYPE)
            if (limitsByHour != null) prefs[Keys.LIMITS_HOUR] = limitsByHour else prefs.remove(Keys.LIMITS_HOUR)
            if (limitsByMonth != null) prefs[Keys.LIMITS_MONTH] = limitsByMonth else prefs.remove(Keys.LIMITS_MONTH)
            if (limitsTotal != null) prefs[Keys.LIMITS_TOTAL] = limitsTotal else prefs.remove(Keys.LIMITS_TOTAL)
            if (expiresAt != null) prefs[Keys.KEY_EXPIRES] = expiresAt else prefs.remove(Keys.KEY_EXPIRES)
        }
    }

    suspend fun clearApiConfiguration() {
        context.flightTrackerDataStore.edit { prefs ->
            prefs.remove(Keys.USE_GATEWAY)
            prefs.remove(Keys.API_KEY_ENC)
            prefs.remove(Keys.GATEWAY_URL)
            prefs.remove(Keys.GATEWAY_TOKEN_ENC)
            prefs.remove(Keys.WEBHOOK_URL)
            prefs.remove(Keys.LAST_TESTED_MS)
            prefs.remove(Keys.LAST_CONN_OK)
            prefs.remove(Keys.LAST_STATUS_MSG)
            prefs.remove(Keys.PLAN_TYPE)
            prefs.remove(Keys.LIMITS_HOUR)
            prefs.remove(Keys.LIMITS_MONTH)
            prefs.remove(Keys.LIMITS_TOTAL)
            prefs.remove(Keys.KEY_EXPIRES)
        }
    }
}
