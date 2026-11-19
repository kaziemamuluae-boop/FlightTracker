package com.example

import com.example.model.AltitudeUnit
import com.example.model.DistanceUnit
import com.example.model.FlightDto
import com.example.model.FlightFilter
import com.example.model.SpeedUnit
import com.example.utils.AviationFormatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AviationFormattersTest {

    @Test
    fun altitudeConversion_metersToFeetAndMeters() {
        assertEquals("32,808 ft", AviationFormatters.formatAltitude(10000.0, AltitudeUnit.FT))
        assertEquals("10,000 m", AviationFormatters.formatAltitude(10000.0, AltitudeUnit.M))
        assertEquals("N/A", AviationFormatters.formatAltitude(null, AltitudeUnit.FT))
    }

    @Test
    fun speedConversion_kmhToKnots() {
        assertEquals("521 kts", AviationFormatters.formatSpeed(966.0, SpeedUnit.KTS))
        assertEquals("966 km/h", AviationFormatters.formatSpeed(966.0, SpeedUnit.KMH))
    }

    @Test
    fun haversineAndGreatCircle_jfkToLhr() {
        val dist = AviationFormatters.haversineKm(40.6413, -73.7781, 51.4700, -0.4543)
        assertTrue(dist in 5500.0..5600.0)
        val arc = AviationFormatters.greatCircleArc(40.6413, -73.7781, 51.4700, -0.4543, 20)
        assertEquals(21, arc.size)
        assertEquals(40.64, arc.first().first, 0.05)
        assertEquals(51.47, arc.last().first, 0.05)
    }

    @Test
    fun distanceFormatting_kmAndMiles() {
        assertEquals("1,000 km", AviationFormatters.formatDistance(1000.0, DistanceUnit.KM))
        assertEquals("621 mi", AviationFormatters.formatDistance(1000.0, DistanceUnit.MI))
    }

    @Test
    fun flightFilters_matchDomesticVsInternational() {
        val intl = FlightDto(depIcao = "KJFK", arrIcao = "EGLL", status = "en-route")
        val dom = FlightDto(depIcao = "KJFK", arrIcao = "KLAX", status = "delayed")
        assertTrue(AviationFormatters.matchesFilter(intl, FlightFilter.INTERNATIONAL))
        assertFalse(AviationFormatters.matchesFilter(intl, FlightFilter.DOMESTIC))
        assertTrue(AviationFormatters.matchesFilter(dom, FlightFilter.DOMESTIC))
        assertTrue(AviationFormatters.matchesFilter(dom, FlightFilter.DELAYED))
    }
}
