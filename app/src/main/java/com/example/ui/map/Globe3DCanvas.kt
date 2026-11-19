package com.example.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.example.model.AirportDto
import com.example.model.FlightDto
import com.example.ui.theme.AviationCyanHighlight
import com.example.ui.theme.AviationDeepNavy
import com.example.ui.theme.AviationMintGreen
import com.example.ui.theme.AviationSkyBlue
import com.example.utils.AviationFormatters
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Interactive 3D Orthographic Globe View rendered with Jetpack Compose Canvas.
 * Supports touch drag rotation, zoom scaling, geodesic flight arcs, and aircraft tap selection.
 */
@Composable
fun Globe3DCanvas(
    flights: List<FlightDto>,
    selectedFlight: FlightDto?,
    depAirport: AirportDto?,
    arrAirport: AirportDto?,
    zoomScale: Float,
    showLabels: Boolean,
    onSelectFlight: (FlightDto) -> Unit,
    modifier: Modifier = Modifier
) {
    var centerLonDeg by remember { mutableFloatStateOf(-20f) }
    var centerLatDeg by remember { mutableFloatStateOf(28f) }
    var projectedPositions by remember { mutableStateOf<List<Pair<Offset, FlightDto>>>(emptyList()) }
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0E1F42), AviationDeepNavy, Color(0xFF040812))
                )
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    centerLonDeg = (centerLonDeg - dragAmount.x * 0.25f)
                    centerLatDeg = (centerLatDeg + dragAmount.y * 0.22f).coerceIn(-80f, 80f)
                }
            }
            .pointerInput(flights) {
                detectTapGestures { tapOffset ->
                    val nearest = projectedPositions.minByOrNull { (pos, _) ->
                        val dx = pos.x - tapOffset.x
                        val dy = pos.y - tapOffset.y
                        dx * dx + dy * dy
                    }
                    if (nearest != null) {
                        val dx = nearest.first.x - tapOffset.x
                        val dy = nearest.first.y - tapOffset.y
                        if (sqrt(dx * dx + dy * dy) < 60f) {
                            onSelectFlight(nearest.second)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = (minOf(size.width, size.height) * 0.42f) * zoomScale.coerceIn(0.75f, 2.2f)
            val cx = size.width / 2f
            val cy = size.height / 2f
            val center = Offset(cx, cy)

            // Outer atmospheric glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AviationSkyBlue.copy(alpha = 0.22f),
                        AviationCyanHighlight.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.18f
                ),
                radius = radius * 1.18f,
                center = center
            )

            // Globe sphere body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF172C57), Color(0xFF0B1833), Color(0xFF060E1F)),
                    center = Offset(cx - radius * 0.25f, cy - radius * 0.25f),
                    radius = radius * 1.25f
                ),
                radius = radius,
                center = center
            )

            // Graticule meridians and parallels
            val gridColor = Color(0xFF28487D).copy(alpha = 0.45f)
            for (lon in -180 until 180 step 30) {
                drawMeridian(lon.toDouble(), centerLatDeg.toDouble(), centerLonDeg.toDouble(), cx, cy, radius, gridColor)
            }
            for (lat in -60..60 step 30) {
                drawParallel(lat.toDouble(), centerLatDeg.toDouble(), centerLonDeg.toDouble(), cx, cy, radius, gridColor)
            }

            // Simplified continental silhouettes for geographic reference
            drawContinents(centerLatDeg.toDouble(), centerLonDeg.toDouble(), cx, cy, radius)

            // Selected flight route arc
            if (depAirport?.lat != null && depAirport.lng != null && arrAirport?.lat != null && arrAirport.lng != null) {
                val arc = AviationFormatters.greatCircleArc(
                    depAirport.lat, depAirport.lng,
                    arrAirport.lat, arrAirport.lng,
                    steps = 40
                )
                val arcPath = Path()
                var started = false
                for ((lat, lon) in arc) {
                    val p = projectOrthographic(lat, lon, centerLatDeg.toDouble(), centerLonDeg.toDouble(), cx, cy, radius)
                    if (p != null) {
                        if (!started) {
                            arcPath.moveTo(p.x, p.y)
                            started = true
                        } else {
                            arcPath.lineTo(p.x, p.y)
                        }
                    } else {
                        started = false
                    }
                }
                drawPath(arcPath, color = AviationCyanHighlight, style = Stroke(width = 4f))
            }

            // Globe rim
            drawCircle(
                color = AviationSkyBlue.copy(alpha = 0.45f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // Live aircraft markers projected onto visible hemisphere
            val visibleTargets = mutableListOf<Pair<Offset, FlightDto>>()
            for (flight in flights.take(220)) {
                val lat = flight.lat ?: continue
                val lng = flight.lng ?: continue
                val p = projectOrthographic(lat, lng, centerLatDeg.toDouble(), centerLonDeg.toDouble(), cx, cy, radius)
                    ?: continue
                visibleTargets.add(p to flight)

                val isSelected = selectedFlight != null &&
                    ((selectedFlight.hex != null && selectedFlight.hex == flight.hex) ||
                        (selectedFlight.flightIata != null && selectedFlight.flightIata == flight.flightIata))
                val planeColor = if (isSelected) AviationCyanHighlight else AviationMintGreen

                rotate(degrees = (flight.dir ?: 0.0).toFloat(), pivot = p) {
                    val s = if (isSelected) 16f else 11f
                    val planePath = Path().apply {
                        moveTo(p.x, p.y - s)
                        lineTo(p.x + s * 0.72f, p.y + s * 0.65f)
                        lineTo(p.x, p.y + s * 0.25f)
                        lineTo(p.x - s * 0.72f, p.y + s * 0.65f)
                        close()
                    }
                    drawPath(planePath, color = planeColor)
                }

                if (showLabels && (isSelected || visibleTargets.size <= 30)) {
                    val code = flight.flightIata ?: flight.flightIcao
                    if (!code.isNullOrBlank()) {
                        drawFlightLabel(textMeasurer, code, Offset(p.x + 12f, p.y - 10f), isSelected)
                    }
                }
            }
            projectedPositions = visibleTargets
        }
    }
}

private fun projectOrthographic(
    latDeg: Double,
    lonDeg: Double,
    centerLatDeg: Double,
    centerLonDeg: Double,
    cx: Float,
    cy: Float,
    radius: Float
): Offset? {
    val phi = latDeg * PI / 180.0
    val lam = lonDeg * PI / 180.0
    val phi0 = centerLatDeg * PI / 180.0
    val lam0 = centerLonDeg * PI / 180.0

    val cosC = sin(phi0) * sin(phi) + cos(phi0) * cos(phi) * cos(lam - lam0)
    if (cosC < 0.0) return null

    val x = radius * cos(phi) * sin(lam - lam0)
    val y = radius * (cos(phi0) * sin(phi) - sin(phi0) * cos(phi) * cos(lam - lam0))
    return Offset(cx + x.toFloat(), cy - y.toFloat())
}

private fun DrawScope.drawMeridian(
    lonDeg: Double,
    centerLatDeg: Double,
    centerLonDeg: Double,
    cx: Float,
    cy: Float,
    radius: Float,
    color: Color
) {
    val path = Path()
    var started = false
    for (lat in -85..85 step 5) {
        val p = projectOrthographic(lat.toDouble(), lonDeg, centerLatDeg, centerLonDeg, cx, cy, radius)
        if (p != null) {
            if (!started) {
                path.moveTo(p.x, p.y)
                started = true
            } else {
                path.lineTo(p.x, p.y)
            }
        } else {
            started = false
        }
    }
    drawPath(path, color = color, style = Stroke(width = 1.2f))
}

private fun DrawScope.drawParallel(
    latDeg: Double,
    centerLatDeg: Double,
    centerLonDeg: Double,
    cx: Float,
    cy: Float,
    radius: Float,
    color: Color
) {
    val path = Path()
    var started = false
    for (lon in -180..180 step 5) {
        val p = projectOrthographic(latDeg, lon.toDouble(), centerLatDeg, centerLonDeg, cx, cy, radius)
        if (p != null) {
            if (!started) {
                path.moveTo(p.x, p.y)
                started = true
            } else {
                path.lineTo(p.x, p.y)
            }
        } else {
            started = false
        }
    }
    drawPath(path, color = color, style = Stroke(width = 1.2f))
}

private fun DrawScope.drawContinents(
    centerLatDeg: Double,
    centerLonDeg: Double,
    cx: Float,
    cy: Float,
    radius: Float
) {
    val landStroke = Color(0xFF3E6BA8).copy(alpha = 0.65f)
    for (poly in CONTINENT_OUTLINES) {
        val path = Path()
        var started = false
        for ((lat, lon) in poly) {
            val p = projectOrthographic(lat, lon, centerLatDeg, centerLonDeg, cx, cy, radius)
            if (p != null) {
                if (!started) {
                    path.moveTo(p.x, p.y)
                    started = true
                } else {
                    path.lineTo(p.x, p.y)
                }
            } else {
                started = false
            }
        }
        drawPath(path, color = landStroke, style = Stroke(width = 2.2f))
    }
}

private fun DrawScope.drawFlightLabel(
    measurer: TextMeasurer,
    text: String,
    topLeft: Offset,
    highlighted: Boolean
) {
    val result = measurer.measure(
        text = text,
        style = TextStyle(
            color = if (highlighted) AviationCyanHighlight else Color(0xFFE2E8F0),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    )
    drawText(result, topLeft = topLeft)
}

private val CONTINENT_OUTLINES: List<List<Pair<Double, Double>>> = listOf(
    // North America
    listOf(
        70.0 to -160.0, 60.0 to -165.0, 55.0 to -135.0, 48.0 to -125.0, 32.0 to -117.0,
        20.0 to -105.0, 10.0 to -84.0, 20.0 to -87.0, 25.0 to -80.0, 35.0 to -75.0,
        45.0 to -66.0, 52.0 to -56.0, 62.0 to -75.0, 70.0 to -95.0, 70.0 to -160.0
    ),
    // South America
    listOf(
        11.0 to -74.0, 5.0 to -52.0, -5.0 to -35.0, -23.0 to -42.0, -35.0 to -56.0,
        -54.0 to -68.0, -42.0 to -74.0, -18.0 to -71.0, -5.0 to -81.0, 11.0 to -74.0
    ),
    // Europe
    listOf(
        36.0 to -9.0, 43.0 to -9.0, 49.0 to -4.0, 54.0 to 8.0, 60.0 to 5.0,
        70.0 to 25.0, 60.0 to 30.0, 50.0 to 40.0, 42.0 to 29.0, 38.0 to 24.0,
        41.0 to 12.0, 36.0 to -5.0
    ),
    // Africa
    listOf(
        35.0 to -6.0, 32.0 to 32.0, 12.0 to 51.0, -10.0 to 40.0, -34.0 to 20.0,
        -22.0 to 14.0, 5.0 to 9.0, 5.0 to -10.0, 15.0 to -17.0, 35.0 to -6.0
    ),
    // Asia
    listOf(
        42.0 to 29.0, 55.0 to 60.0, 72.0 to 105.0, 66.0 to 170.0, 52.0 to 140.0,
        35.0 to 139.0, 22.0 to 114.0, 10.0 to 105.0, 1.0 to 104.0, 16.0 to 96.0,
        8.0 to 77.0, 24.0 to 67.0, 26.0 to 56.0, 13.0 to 44.0, 32.0 to 34.0, 42.0 to 29.0
    ),
    // Australia
    listOf(
        -12.0 to 130.0, -12.0 to 142.0, -27.0 to 153.0, -38.0 to 146.0,
        -35.0 to 116.0, -22.0 to 114.0, -12.0 to 130.0
    )
)
