package com.example.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.FlightDto
import com.example.model.MapLayerStyle
import com.example.ui.MainViewModel
import com.example.ui.components.AviationErrorCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.AviationAmberWarn
import com.example.ui.theme.AviationCyanHighlight
import com.example.ui.theme.AviationMintGreen
import com.example.utils.AviationFormatters
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.floor

private val DARK_RADAR_TILE_SOURCE = object : OnlineTileSourceBase(
    "CartoDarkMatter",
    0, 18, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/dark_all/",
        "https://b.basemaps.cartocdn.com/dark_all/",
        "https://c.basemaps.cartocdn.com/dark_all/"
    )
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return "$baseUrl$z/$x/$y.png"
    }
}

private val SATELLITE_TILE_SOURCE = object : OnlineTileSourceBase(
    "EsriWorldImagery",
    0, 17, 256, ".jpg",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/")
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        return "$baseUrl$z/$y/$x"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMapScreen(
    viewModel: MainViewModel,
    onOpenFlightDetails: (String) -> Unit,
    onOpenAirport: (String) -> Unit,
    onOpenApiConfig: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapState by viewModel.mapState.collectAsStateWithLifecycle()
    val settings by viewModel.userSettings.collectAsStateWithLifecycle()

    var searchInput by remember { mutableStateOf(mapState.searchQuery) }
    var showLayerMenu by remember { mutableStateOf(false) }
    var globeZoomScale by remember { mutableFloatStateOf(1f) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    val markerPool = remember { mutableMapOf<String, Marker>() }
    val clusterPool = remember { mutableListOf<Marker>() }
    var routePolyline by remember { mutableStateOf<Polyline?>(null) }
    var playbackPolyline by remember { mutableStateOf<Polyline?>(null) }
    var depMarker by remember { mutableStateOf<Marker?>(null) }
    var arrMarker by remember { mutableStateOf<Marker?>(null) }
    var userMarker by remember { mutableStateOf<Marker?>(null) }
    var playbackMarker by remember { mutableStateOf<Marker?>(null) }

    val planeDrawableNormal = remember(context) { createAircraftDrawable(context, 0xFF38BDF8.toInt(), 58) }
    val planeDrawableSelected = remember(context) { createAircraftDrawable(context, 0xFF22D3EE.toInt(), 74) }
    val airportDrawable = remember(context) { createAirportDotDrawable(context, 0xFF34D399.toInt()) }
    val userDrawable = remember(context) { createAirportDotDrawable(context, 0xFFFBBF24.toInt()) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.fetchUserLocationAndNearby()
        }
    }

    // Lifecycle-aware live updates: pause polling when screen leaves or app backgrounds
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.setMapScreenActive(true)
                    mapViewRef?.onResume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.setMapScreenActive(false)
                    mapViewRef?.onPause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.setMapScreenActive(false)
        }
    }

    val displayedFlights = remember(mapState.flights, mapState.nearbyOnly, mapState.userLat, mapState.userLng) {
        if (!mapState.nearbyOnly || mapState.userLat == null || mapState.userLng == null) {
            mapState.flights
        } else {
            mapState.flights.filter { f ->
                val lat = f.lat ?: return@filter false
                val lng = f.lng ?: return@filter false
                AviationFormatters.haversineKm(mapState.userLat!!, mapState.userLng!!, lat, lng) <= 350.0
            }
        }
    }

    // Focus camera when requested
    LaunchedEffect(mapState.focusLatLng, mapViewRef) {
        val focus = mapState.focusLatLng ?: return@LaunchedEffect
        mapViewRef?.controller?.apply {
            animateTo(GeoPoint(focus.first, focus.second))
            setZoom(7.5)
        }
    }

    // Efficient in-place marker diffing & route overlay update for OSMDroid
    LaunchedEffect(
        displayedFlights,
        mapState.selectedFlight,
        mapState.selectedRouteDep,
        mapState.selectedRouteArr,
        mapState.playbackPoints,
        mapState.playbackIndex,
        mapState.isPlayingBack,
        mapState.userLat,
        mapState.userLng,
        settings.mapLayerStyle,
        settings.clusteringEnabled,
        mapViewRef
    ) {
        val mv = mapViewRef ?: return@LaunchedEffect
        if (settings.mapLayerStyle == MapLayerStyle.GLOBE_3D) return@LaunchedEffect

        val tileSource = when (settings.mapLayerStyle) {
            MapLayerStyle.DARK_RADAR -> DARK_RADAR_TILE_SOURCE
            MapLayerStyle.NORMAL -> TileSourceFactory.MAPNIK
            MapLayerStyle.SATELLITE -> SATELLITE_TILE_SOURCE
            MapLayerStyle.TERRAIN -> TileSourceFactory.OpenTopo
            MapLayerStyle.GLOBE_3D -> DARK_RADAR_TILE_SOURCE
        }
        if (mv.tileProvider.tileSource.name() != tileSource.name()) {
            mv.setTileSource(tileSource)
        }

        // Remove previous cluster markers
        clusterPool.forEach { mv.overlays.remove(it) }
        clusterPool.clear()

        val zoom = mv.zoomLevelDouble
        val shouldCluster = settings.clusteringEnabled && zoom < 5.5 && displayedFlights.size > 40

        if (shouldCluster) {
            // Hide individual markers
            markerPool.values.forEach { mv.overlays.remove(it) }
            val cellSize = if (zoom < 3.5) 14.0 else 7.0
            val buckets = displayedFlights.filter { it.lat != null && it.lng != null }.groupBy { f ->
                val gx = floor((f.lat!! + 90.0) / cellSize).toInt()
                val gy = floor((f.lng!! + 180.0) / cellSize).toInt()
                gx to gy
            }
            for ((_, group) in buckets) {
                if (group.size == 1) {
                    val f = group.first()
                    val m = Marker(mv).apply {
                        position = GeoPoint(f.lat!!, f.lng!!)
                        rotation = -(f.dir?.toFloat() ?: 0f)
                        icon = planeDrawableNormal
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = f.flightIata ?: f.flightIcao ?: "Aircraft"
                        setOnMarkerClickListener { _, _ ->
                            viewModel.selectMapFlight(f)
                            true
                        }
                    }
                    clusterPool.add(m)
                    mv.overlays.add(m)
                } else {
                    val avgLat = group.mapNotNull { it.lat }.average()
                    val avgLng = group.mapNotNull { it.lng }.average()
                    val clusterMarker = Marker(mv).apply {
                        position = GeoPoint(avgLat, avgLng)
                        icon = createClusterDrawable(context, group.size)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        setOnMarkerClickListener { _, _ ->
                            mv.controller.animateTo(GeoPoint(avgLat, avgLng))
                            mv.controller.setZoom((zoom + 2.2).coerceAtMost(12.0))
                            true
                        }
                    }
                    clusterPool.add(clusterMarker)
                    mv.overlays.add(clusterMarker)
                }
            }
        } else {
            // Diff existing markers without recreating all markers
            val activeKeys = HashSet<String>()
            val visibleSubset = displayedFlights.take(250)
            for (f in visibleSubset) {
                val lat = f.lat ?: continue
                val lng = f.lng ?: continue
                val key = f.hex ?: f.flightIata ?: f.flightIcao ?: continue
                activeKeys.add(key)
                val isSelected = mapState.selectedFlight != null &&
                    ((mapState.selectedFlight!!.hex != null && mapState.selectedFlight!!.hex == f.hex) ||
                        (mapState.selectedFlight!!.flightIata != null && mapState.selectedFlight!!.flightIata == f.flightIata))

                val existing = markerPool[key]
                if (existing != null) {
                    existing.position = GeoPoint(lat, lng)
                    existing.rotation = -(f.dir?.toFloat() ?: 0f)
                    existing.icon = if (isSelected) planeDrawableSelected else planeDrawableNormal
                    if (!mv.overlays.contains(existing)) mv.overlays.add(existing)
                } else {
                    val m = Marker(mv).apply {
                        position = GeoPoint(lat, lng)
                        rotation = -(f.dir?.toFloat() ?: 0f)
                        icon = if (isSelected) planeDrawableSelected else planeDrawableNormal
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = f.flightIata ?: f.flightIcao ?: f.regNumber ?: key
                        setOnMarkerClickListener { _, _ ->
                            viewModel.selectMapFlight(f)
                            true
                        }
                    }
                    markerPool[key] = m
                    mv.overlays.add(m)
                }
            }
            val staleKeys = markerPool.keys.filter { it !in activeKeys }
            for (k in staleKeys) {
                markerPool.remove(k)?.let { mv.overlays.remove(it) }
            }
        }

        // Route great-circle polyline & departure/arrival airport pins
        routePolyline?.let { mv.overlays.remove(it) }
        depMarker?.let { mv.overlays.remove(it) }
        arrMarker?.let { mv.overlays.remove(it) }

        val dep = mapState.selectedRouteDep
        val arr = mapState.selectedRouteArr
        if (dep?.lat != null && dep.lng != null && arr?.lat != null && arr.lng != null) {
            val arc = AviationFormatters.greatCircleArc(dep.lat, dep.lng, arr.lat, arr.lng)
            routePolyline = Polyline(mv).apply {
                setPoints(arc.map { GeoPoint(it.first, it.second) })
                outlinePaint.color = 0xFF22D3EE.toInt()
                outlinePaint.strokeWidth = 5f
            }
            mv.overlays.add(routePolyline)

            depMarker = Marker(mv).apply {
                position = GeoPoint(dep.lat, dep.lng)
                icon = airportDrawable
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "${dep.iataCode ?: ""} ${dep.name ?: ""}".trim()
            }
            arrMarker = Marker(mv).apply {
                position = GeoPoint(arr.lat, arr.lng)
                icon = airportDrawable
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "${arr.iataCode ?: ""} ${arr.name ?: ""}".trim()
            }
            mv.overlays.add(depMarker)
            mv.overlays.add(arrMarker)
        }

        // Historical playback trail
        playbackPolyline?.let { mv.overlays.remove(it) }
        playbackMarker?.let { mv.overlays.remove(it) }
        if (mapState.playbackPoints.size >= 2) {
            playbackPolyline = Polyline(mv).apply {
                setPoints(mapState.playbackPoints.map { GeoPoint(it.lat, it.lng) })
                outlinePaint.color = 0xFFFBBF24.toInt()
                outlinePaint.strokeWidth = 5f
            }
            mv.overlays.add(playbackPolyline)
            if (mapState.isPlayingBack) {
                val pt = mapState.playbackPoints.getOrNull(mapState.playbackIndex)
                if (pt != null) {
                    playbackMarker = Marker(mv).apply {
                        position = GeoPoint(pt.lat, pt.lng)
                        rotation = -(pt.headingDeg?.toFloat() ?: 0f)
                        icon = planeDrawableSelected
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    mv.overlays.add(playbackMarker)
                }
            }
        }

        // User location marker
        userMarker?.let { mv.overlays.remove(it) }
        if (mapState.userLat != null && mapState.userLng != null) {
            userMarker = Marker(mv).apply {
                position = GeoPoint(mapState.userLat!!, mapState.userLng!!)
                icon = userDrawable
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                title = "Your Location"
            }
            mv.overlays.add(userMarker)
        }

        mv.invalidate()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (settings.mapLayerStyle == MapLayerStyle.GLOBE_3D) {
            Globe3DCanvas(
                flights = displayedFlights,
                selectedFlight = mapState.selectedFlight,
                depAirport = mapState.selectedRouteDep,
                arrAirport = mapState.selectedRouteArr,
                zoomScale = globeZoomScale,
                showLabels = settings.showFlightLabels,
                onSelectFlight = { viewModel.selectMapFlight(it) }
            )
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        setMultiTouchControls(true)
                        setBuiltInZoomControls(false)
                        minZoomLevel = 2.0
                        maxZoomLevel = 17.0
                        controller.setZoom(3.2)
                        controller.setCenter(GeoPoint(30.0, -10.0))
                        mapViewRef = this
                    }
                }
            )
        }

        // Top search + status overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        placeholder = { Text("Focus flight (e.g. BA117, AAL6)") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchInput.isNotBlank()) {
                                IconButton(onClick = {
                                    searchInput = ""
                                    viewModel.clearMapSearch()
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.focusFlightOnMap(searchInput) }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("map_search_input")
                    )
                    FilledIconButton(
                        onClick = { viewModel.focusFlightOnMap(searchInput) },
                        modifier = Modifier.testTag("map_search_button")
                    ) {
                        Icon(Icons.Filled.Flight, contentDescription = "Focus aircraft")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick status & multi-track strip
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (mapState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(AviationMintGreen, CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${displayedFlights.size} tracked • ${AviationFormatters.formatTimestampShortMs(mapState.lastUpdatedMs, settings)}",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                item {
                    FilterChip(
                        selected = mapState.nearbyOnly,
                        onClick = {
                            val hasLoc = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!hasLoc && mapState.userLat == null) {
                                locationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                            viewModel.toggleNearbyOnly()
                        },
                        label = { Text("Nearby") },
                        leadingIcon = { Icon(Icons.Filled.NearMe, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = mapState.isAutoRefreshEnabled,
                        onClick = { viewModel.toggleAutoRefresh() },
                        label = { Text(if (mapState.isAutoRefreshEnabled) "Auto ${settings.autoRefreshSeconds}s" else "Paused") },
                        leadingIcon = {
                            Icon(
                                if (mapState.isAutoRefreshEnabled) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                items(mapState.multiTrackCodes.toList()) { code ->
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.focusFlightOnMap(code) },
                        label = { Text("Pin: $code") },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.PushPin,
                                contentDescription = "Focused pin",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }

            if (mapState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                AviationErrorCard(
                    error = mapState.error!!,
                    onRetry = { viewModel.refreshMapFlights(forceRefresh = true) },
                    onOpenApiConfig = onOpenApiConfig
                )
            }
        }

        // Right-side map controls (Zoom In, Zoom Out, Layer Picker, My Location, Refresh)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                FilledIconButton(
                    onClick = { showLayerMenu = true },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.testTag("map_layers_button")
                ) {
                    Icon(
                        if (settings.mapLayerStyle == MapLayerStyle.GLOBE_3D) Icons.Filled.Public else Icons.Filled.Layers,
                        contentDescription = "Map layers"
                    )
                }
                DropdownMenu(expanded = showLayerMenu, onDismissRequest = { showLayerMenu = false }) {
                    MapLayerStyle.entries.forEach { layer ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (layer) {
                                        MapLayerStyle.DARK_RADAR -> "Dark Aviation Radar"
                                        MapLayerStyle.NORMAL -> "Normal Map"
                                        MapLayerStyle.SATELLITE -> "Satellite Imagery"
                                        MapLayerStyle.TERRAIN -> "Terrain Map"
                                        MapLayerStyle.GLOBE_3D -> "3D Globe View"
                                    },
                                    fontWeight = if (settings.mapLayerStyle == layer) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                viewModel.updateSettings { it.copy(mapLayerStyle = layer) }
                                showLayerMenu = false
                            }
                        )
                    }
                }
            }

            FilledIconButton(
                onClick = {
                    if (settings.mapLayerStyle == MapLayerStyle.GLOBE_3D) {
                        globeZoomScale = (globeZoomScale + 0.2f).coerceAtMost(2.2f)
                    } else {
                        mapViewRef?.controller?.zoomIn()
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("map_zoom_in_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Zoom in")
            }

            FilledIconButton(
                onClick = {
                    if (settings.mapLayerStyle == MapLayerStyle.GLOBE_3D) {
                        globeZoomScale = (globeZoomScale - 0.2f).coerceAtLeast(0.75f)
                    } else {
                        mapViewRef?.controller?.zoomOut()
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("map_zoom_out_button")
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Zoom out")
            }

            FilledIconButton(
                onClick = {
                    val hasLoc = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasLoc) {
                        viewModel.fetchUserLocationAndNearby()
                        if (mapState.userLat != null && mapState.userLng != null) {
                            mapViewRef?.controller?.animateTo(GeoPoint(mapState.userLat!!, mapState.userLng!!))
                            mapViewRef?.controller?.setZoom(7.5)
                        }
                    } else {
                        locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("map_my_location_button")
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "My location")
            }

            FilledIconButton(
                onClick = { viewModel.refreshMapFlights(forceRefresh = true) },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("map_refresh_button")
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh aircraft")
            }
        }

        // Selected Aircraft Bottom Sheet Card
        val selected = mapState.selectedFlight
        if (selected != null) {
            val flightCode = selected.flightIata ?: selected.flightIcao ?: selected.hex ?: "Aircraft"
            val stale = AviationFormatters.isPositionStale(selected.updated)
            val distKm = viewModel.distanceFromUserKm(selected.lat, selected.lng)
            val isPinned = flightCode.uppercase() in mapState.multiTrackCodes

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp)
                    .testTag("aircraft_bottom_sheet"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = flightCode,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusBadge(status = selected.status)
                            }
                            Text(
                                text = listOfNotNull(
                                    selected.airlineIata?.let { "Airline $it" },
                                    selected.aircraftIcao,
                                    selected.regNumber
                                ).joinToString(" • ").ifBlank { "Live Aircraft" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.selectMapFlight(null) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close aircraft sheet")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Route banner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { selected.depIata?.let(onOpenAirport) },
                            enabled = !selected.depIata.isNullOrBlank()
                        ) {
                            Text("DEP: ${selected.depIata ?: selected.depIcao ?: "---"}")
                        }
                        Text(
                            text = if (distKm != null) "${AviationFormatters.formatDistance(distKm, settings.distanceUnit)} away" else "→",
                            style = MaterialTheme.typography.labelMedium,
                            color = AviationCyanHighlight
                        )
                        FilledTonalButton(
                            onClick = { selected.arrIata?.let(onOpenAirport) },
                            enabled = !selected.arrIata.isNullOrBlank()
                        ) {
                            Text("ARR: ${selected.arrIata ?: selected.arrIcao ?: "---"}")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Telemetry grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MiniStat("Altitude", AviationFormatters.formatAltitude(selected.altMeters, settings.altitudeUnit))
                        MiniStat("Speed", AviationFormatters.formatSpeed(selected.speedKmh, settings.speedUnit))
                        MiniStat("Heading", AviationFormatters.formatHeading(selected.dir))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Coordinates: ${AviationFormatters.formatCoordinates(selected.lat, selected.lng)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (stale) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Last position reported at ${AviationFormatters.formatTimestamp(selected.updated, settings)} (stale)",
                            style = MaterialTheme.typography.labelSmall,
                            color = AviationAmberWarn
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.toggleMultiTrackFlight(flightCode) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPinned) "Pinned" else "Pin")
                        }
                        if (mapState.playbackPoints.size >= 2) {
                            OutlinedButton(
                                onClick = {
                                    if (mapState.isPlayingBack) viewModel.stopFlightPlayback()
                                    else viewModel.startFlightPlayback()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    if (mapState.isPlayingBack) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (mapState.isPlayingBack) "Stop" else "Playback")
                            }
                        }
                        Button(
                            onClick = { onOpenFlightDetails(flightCode) },
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("open_selected_flight_details")
                        ) {
                            Text("Details")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

private fun createAircraftDrawable(context: Context, color: Int, sizePx: Int): BitmapDrawable {
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = 0xFF0A1122.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }
    val cx = sizePx / 2f
    val s = sizePx * 0.4f
    val path = AndroidPath().apply {
        moveTo(cx, cx - s)                  // nose
        lineTo(cx + s * 0.16f, cx - s * 0.2f)
        lineTo(cx + s * 0.95f, cx + s * 0.2f) // right wing
        lineTo(cx + s * 0.95f, cx + s * 0.42f)
        lineTo(cx + s * 0.16f, cx + s * 0.18f)
        lineTo(cx + s * 0.12f, cx + s * 0.72f)
        lineTo(cx + s * 0.42f, cx + s * 0.95f) // right tail
        lineTo(cx, cx + s * 0.82f)
        lineTo(cx - s * 0.42f, cx + s * 0.95f) // left tail
        lineTo(cx - s * 0.12f, cx + s * 0.72f)
        lineTo(cx - s * 0.16f, cx + s * 0.18f)
        lineTo(cx - s * 0.95f, cx + s * 0.42f) // left wing
        lineTo(cx - s * 0.95f, cx + s * 0.2f)
        lineTo(cx - s * 0.16f, cx - s * 0.2f)
        close()
    }
    canvas.drawPath(path, paint)
    canvas.drawPath(path, stroke)
    return BitmapDrawable(context.resources, bmp)
}

private fun createAirportDotDrawable(context: Context, color: Int): BitmapDrawable {
    val size = 34
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val outer = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = 0xFF0A1122.toInt() }
    val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    canvas.drawCircle(size / 2f, size / 2f, 13f, outer)
    canvas.drawCircle(size / 2f, size / 2f, 9f, inner)
    return BitmapDrawable(context.resources, bmp)
}

private fun createClusterDrawable(context: Context, count: Int): BitmapDrawable {
    val size = 68
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0284C7.toInt() }
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF38BDF8.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 24f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    canvas.drawCircle(size / 2f, size / 2f, 28f, bg)
    canvas.drawCircle(size / 2f, size / 2f, 28f, border)
    canvas.drawText(count.toString(), size / 2f, size / 2f + 8f, text)
    return BitmapDrawable(context.resources, bmp)
}
