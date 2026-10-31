package com.cockpitf30.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate

class WeatherTest {

    @Test
    fun `codigos WMO se agrupan en tipo y texto en español`() {
        assertEquals(WeatherCondition(WeatherKind.CLEAR, "Despejado"), wmoCondition(0))
        assertEquals(WeatherKind.PARTLY_CLOUDY, wmoCondition(2).kind)
        assertEquals(WeatherKind.CLOUDY, wmoCondition(3).kind)
        assertEquals(WeatherKind.FOG, wmoCondition(45).kind)
        assertEquals(WeatherKind.DRIZZLE, wmoCondition(53).kind)
        assertEquals("Lluvia fuerte", wmoCondition(65).text)
        assertEquals(WeatherKind.RAIN, wmoCondition(81).kind)
        assertEquals(WeatherKind.SNOW, wmoCondition(75).kind)
        assertEquals(WeatherKind.STORM, wmoCondition(99).kind)
        assertEquals("Desconocido", wmoCondition(42).text)
    }

    @Test
    fun `analiza la respuesta de prevision de Open-Meteo`() {
        val json = """
            {"current":{"temperature_2m":18.6,"weather_code":61,"is_day":0},
             "daily":{"time":["2026-10-31","2026-11-01"],"weather_code":[61,0],
                      "temperature_2m_max":[19.4,21.5],"temperature_2m_min":[11.2,9.8]}}
        """.trimIndent()
        val f = parseForecast(json)
        assertEquals(19, f.tempC)
        assertEquals(61, f.code)
        assertFalse(f.isDay)
        assertEquals(DayForecast(LocalDate.of(2026, 11, 1), 0, 10, 22), f.days[1])
    }

    @Test
    fun `analiza los resultados de geocodificacion`() {
        val json = """{"results":[{"name":"Madrid","admin1":"Madrid","country":"España","latitude":40.4,"longitude":-3.7},
            {"name":"Madrid","country":"Colombia","latitude":4.7,"longitude":-74.2}]}"""
        val places = parsePlaces(json)
        assertEquals(2, places.size)
        assertEquals("Madrid, España", places[0].subtitle)
        assertEquals("Colombia", places[1].subtitle)
        assertEquals(emptyList<Place>(), parsePlaces("""{"generationtime_ms":0.1}"""))
    }
}
