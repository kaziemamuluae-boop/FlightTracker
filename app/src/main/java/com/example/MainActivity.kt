package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.notification.NotificationHelper
import com.example.ui.MainViewModel
import com.example.ui.aircraft.AircraftScreen
import com.example.ui.airlines.AirlineScreen
import com.example.ui.airports.AirportScreen
import com.example.ui.favorites.FavoritesScreen
import com.example.ui.flights.FlightComparisonScreen
import com.example.ui.flights.FlightDetailsScreen
import com.example.ui.flights.FlightsSearchScreen
import com.example.ui.home.HomeScreen
import com.example.ui.map.LiveMapScreen
import com.example.ui.settings.ApiConfigScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.FlightTrackerTheme
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object LiveMapRoute
@Serializable object FlightsRoute
@Serializable object FavoritesRoute
@Serializable object SettingsRoute
@Serializable data class FlightDetailsRoute(val flightCode: String)
@Serializable data class AirportRoute(val airportCode: String)
@Serializable data class AirlineRoute(val airlineCode: String)
@Serializable data class AircraftRoute(val regOrHex: String = "")
@Serializable object ComparisonRoute
@Serializable object ApiConfigRoute

private data class TopTab<T : Any>(
    val route: T,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialFlightFromNotification = intent?.getStringExtra(NotificationHelper.EXTRA_FLIGHT_CODE)

        setContent {
            val viewModel: MainViewModel = viewModel()
            val settings by viewModel.userSettings.collectAsStateWithLifecycle()

            FlightTrackerTheme(themeMode = settings.themeMode) {
                FlightTrackerRoot(
                    viewModel = viewModel,
                    initialFlightCode = initialFlightFromNotification
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun FlightTrackerRoot(
    viewModel: MainViewModel,
    initialFlightCode: String?
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    LaunchedEffect(initialFlightCode) {
        if (!initialFlightCode.isNullOrBlank()) {
            navController.navigate(FlightDetailsRoute(initialFlightCode))
        }
    }

    val topTabs = listOf(
        TopTab(HomeRoute, "Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
        TopTab(LiveMapRoute, "Live Map", Icons.Filled.Radar, Icons.Outlined.Radar, "nav_live_map"),
        TopTab(FlightsRoute, "Flights", Icons.Filled.Flight, Icons.Outlined.Flight, "nav_flights"),
        TopTab(FavoritesRoute, "Favorites", Icons.Filled.Star, Icons.Outlined.StarOutline, "nav_favorites"),
        TopTab(SettingsRoute, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
    )

    val showTopNav = currentDest?.let { dest ->
        dest.hasRoute<HomeRoute>() ||
            dest.hasRoute<LiveMapRoute>() ||
            dest.hasRoute<FlightsRoute>() ||
            dest.hasRoute<FavoritesRoute>() ||
            dest.hasRoute<SettingsRoute>()
    } ?: true

    val isLiveMap = currentDest?.hasRoute<LiveMapRoute>() == true

    fun navigateTopTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = if (isLiveMap) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing,
            bottomBar = {
                if (showTopNav && !useRail) {
                    NavigationBar {
                        topTabs.forEach { tab ->
                            val selected = when (tab.route) {
                                is HomeRoute -> currentDest?.hasRoute<HomeRoute>() == true
                                is LiveMapRoute -> currentDest?.hasRoute<LiveMapRoute>() == true
                                is FlightsRoute -> currentDest?.hasRoute<FlightsRoute>() == true
                                is FavoritesRoute -> currentDest?.hasRoute<FavoritesRoute>() == true
                                is SettingsRoute -> currentDest?.hasRoute<SettingsRoute>() == true
                                else -> false
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopNav && useRail) {
                    NavigationRail {
                        topTabs.forEach { tab ->
                            val selected = when (tab.route) {
                                is HomeRoute -> currentDest?.hasRoute<HomeRoute>() == true
                                is LiveMapRoute -> currentDest?.hasRoute<LiveMapRoute>() == true
                                is FlightsRoute -> currentDest?.hasRoute<FlightsRoute>() == true
                                is FavoritesRoute -> currentDest?.hasRoute<FavoritesRoute>() == true
                                is SettingsRoute -> currentDest?.hasRoute<SettingsRoute>() == true
                                else -> false
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopTab(tab.route) },
                                icon = {
                                    Icon(
                                        if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = HomeRoute
                    ) {
                        composable<HomeRoute> {
                            HomeScreen(
                                viewModel = viewModel,
                                onOpenLiveMap = { navigateTopTab(LiveMapRoute) },
                                onOpenSearch = { navigateTopTab(FlightsRoute) },
                                onOpenFlightDetails = { code -> navController.navigate(FlightDetailsRoute(code)) },
                                onOpenAirport = { code -> navController.navigate(AirportRoute(code)) },
                                onOpenAirline = { code -> navController.navigate(AirlineRoute(code)) },
                                onOpenAircraftLookup = { navController.navigate(AircraftRoute()) },
                                onOpenComparison = { navController.navigate(ComparisonRoute) },
                                onOpenApiConfig = { navController.navigate(ApiConfigRoute) }
                            )
                        }

                        composable<LiveMapRoute> {
                            LiveMapScreen(
                                viewModel = viewModel,
                                onOpenFlightDetails = { code -> navController.navigate(FlightDetailsRoute(code)) },
                                onOpenAirport = { code -> navController.navigate(AirportRoute(code)) },
                                onOpenApiConfig = { navController.navigate(ApiConfigRoute) }
                            )
                        }

                        composable<FlightsRoute> {
                            FlightsSearchScreen(
                                viewModel = viewModel,
                                onOpenFlightDetails = { code -> navController.navigate(FlightDetailsRoute(code)) },
                                onOpenAirport = { code -> navController.navigate(AirportRoute(code)) },
                                onOpenAirline = { code -> navController.navigate(AirlineRoute(code)) },
                                onOpenAircraftLookup = { navController.navigate(AircraftRoute()) },
                                onOpenComparison = { navController.navigate(ComparisonRoute) },
                                onOpenApiConfig = { navController.navigate(ApiConfigRoute) }
                            )
                        }

                        composable<FavoritesRoute> {
                            FavoritesScreen(
                                viewModel = viewModel,
                                onOpenFlightDetails = { code -> navController.navigate(FlightDetailsRoute(code)) },
                                onOpenAirport = { code -> navController.navigate(AirportRoute(code)) },
                                onOpenAirline = { code -> navController.navigate(AirlineRoute(code)) },
                                onOpenAircraft = { reg -> navController.navigate(AircraftRoute(reg)) }
                            )
                        }

                        composable<SettingsRoute> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onOpenApiConfig = { navController.navigate(ApiConfigRoute) }
                            )
                        }

                        composable<FlightDetailsRoute> { entry ->
                            val args = entry.toRoute<FlightDetailsRoute>()
                            FlightDetailsScreen(
                                flightCode = args.flightCode,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onTrackLive = { code ->
                                    viewModel.focusFlightOnMap(code)
                                    navigateTopTab(LiveMapRoute)
                                },
                                onOpenAirport = { code -> navController.navigate(AirportRoute(code)) },
                                onOpenAirline = { code -> navController.navigate(AirlineRoute(code)) },
                                onOpenAircraft = { reg -> navController.navigate(AircraftRoute(reg)) },
                                onOpenComparison = { navController.navigate(ComparisonRoute) },
                                onOpenApiConfig = { navController.navigate(ApiConfigRoute) }
                            )
                        }

                        composable<AirportRoute> { entry ->
                            val args = entry.toRoute<AirportRoute>()
                            AirportScreen(
                                initialAirportCode = args.airportCode,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onOpenFlightDetails = { code -> navController.navigate(FlightDetailsRoute(code)) },
                                onOpenApiConfig = { navController.navigate(ApiConfigRoute) }
                            )
                        }

                        composable<AirlineRoute> { entry ->
                            val args = entry.toRoute<AirlineRoute>()
                            AirlineScreen(
                                initialAirlineCode = args.airlineCode,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onOpenFlightDetails = { code -> navController.navigate(FlightDetailsRoute(code)) },
                                onOpenAircraft = { reg -> navController.navigate(AircraftRoute(reg)) },
                                onOpenApiConfig = { navController.navigate(ApiConfigRoute) }
                            )
                        }

                        composable<AircraftRoute> { entry ->
                            val args = entry.toRoute<AircraftRoute>()
                            AircraftScreen(
                                initialRegOrHex = args.regOrHex.ifBlank { null },
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onOpenFlightDetails = { code -> navController.navigate(FlightDetailsRoute(code)) },
                                onOpenApiConfig = { navController.navigate(ApiConfigRoute) }
                            )
                        }

                        composable<ComparisonRoute> {
                            FlightComparisonScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                                onOpenFlightDetails = { code -> navController.navigate(FlightDetailsRoute(code)) }
                            )
                        }

                        composable<ApiConfigRoute> {
                            ApiConfigScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
