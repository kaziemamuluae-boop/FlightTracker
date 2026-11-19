package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.repository.AviationRepository
import com.example.data.repository.FavoritesAndAlertsRepository
import com.example.data.repository.WeatherRepository
import com.example.network.NetworkModule
import com.example.notification.FlightAlertWorker
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import java.io.File

class FlightTrackerApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: AppDatabase
        private set
    lateinit var settingsDataStore: SettingsDataStore
        private set
    lateinit var aviationRepository: AviationRepository
        private set
    lateinit var weatherRepository: WeatherRepository
        private set
    lateinit var favoritesRepository: FavoritesAndAlertsRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // Configure OSMDroid tile cache inside app-private storage (no external storage permission needed)
        val osmBase = File(filesDir, "osmdroid").apply { mkdirs() }
        val osmTiles = File(cacheDir, "osmdroid_tiles").apply { mkdirs() }
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = osmBase
            osmdroidTileCache = osmTiles
        }

        database = AppDatabase.getInstance(this)
        settingsDataStore = SettingsDataStore(this)
        aviationRepository = AviationRepository(
            context = this,
            api = NetworkModule.airLabsService,
            cacheDao = database.cacheDao(),
            playbackDao = database.alertsAndPlaybackDao()
        )
        weatherRepository = WeatherRepository(NetworkModule.openMeteoService)
        favoritesRepository = FavoritesAndAlertsRepository(
            favoritesDao = database.favoritesDao(),
            historyDao = database.historyDao(),
            alertsDao = database.alertsAndPlaybackDao()
        )

        appScope.launch {
            settingsDataStore.apiConfiguration.collect { cfg ->
                NetworkModule.apiConfigRef.set(cfg)
            }
        }

        NotificationHelper.ensureChannels(this)
        FlightAlertWorker.schedulePeriodic(this)
    }
}
