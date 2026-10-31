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
    fun `analiza la prevision por horas y la agrupa por dia`() {
        val json = """
            {"current":{"temperature_2m":18,"weather_code":0,"is_day":1},
             "daily":{"time":["2026-10-31"],"weather_code":[0],"temperature_2m_max":[20],"temperature_2m_min":[10],
                      "sunrise":["2026-10-31T07:25"],"sunset":["2026-10-31T18:12"],"precipitation_sum":[1.2],
                      "precipitation_probability_max":[80],"wind_speed_10m_max":[22.6]},
             "hourly":{"time":["2026-10-31T23:00","2026-11-01T00:00"],"temperature_2m":[12.4,11.6],"weather_code":[3,61],
                       "is_day":[0,0],"precipitation_probability":[null,80],"precipitation":[0.0,1.2],
                       "wind_speed_10m":[9.6,14.4],"wind_direction_10m":[270,225],"apparent_temperature":[11.0,9.6],
                       "wind_gusts_10m":[20.1,30.4],"relative_humidity_2m":[70,88]}}
        """.trimIndent()
        val f = parseForecast(json)
        assertEquals(2, f.hours.size)
        assertEquals(null, f.hours[0].precipProb)
        assertEquals(
            HourForecast(java.time.LocalDateTime.of(2026, 11, 1, 0, 0), 12, 61, false, 80, 1.2f, 14, 225, apparentC = 10, gustKmh = 30, humidity = 88),
            f.hours[1],
        )
        assertEquals(1, f.hoursOf(LocalDate.of(2026, 11, 1)).size)
        val day = f.days[0]
        assertEquals(java.time.LocalTime.of(7, 25), day.sunrise)
        assertEquals(java.time.LocalTime.of(18, 12), day.sunset)
        assertEquals(80, day.precipProbMax)
        assertEquals(23, day.windMaxKmh)
    }

    @Test
    fun `una cache antigua sin datos por horas sigue funcionando`() {
        val old = """{"current":{"temperature_2m":18,"weather_code":0},"daily":{"time":["2026-10-31"],"weather_code":[0],"temperature_2m_max":[20],"temperature_2m_min":[10]}}"""
        val f = parseForecast(old)
        assertEquals(emptyList<HourForecast>(), f.hours)
        assertEquals(null, f.days[0].sunrise)
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
