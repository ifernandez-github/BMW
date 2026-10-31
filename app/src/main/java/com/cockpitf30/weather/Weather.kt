package com.cockpitf30.weather

import org.json.JSONObject
import java.time.LocalDate

/** Tipo de tiempo, agrupando los códigos WMO que usan Open-Meteo y la mayoría de servicios. */
enum class WeatherKind { CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, STORM }

data class WeatherCondition(val kind: WeatherKind, val text: String)

/** Traduce un código WMO (0–99) a tipo de icono y descripción en español. */
fun wmoCondition(code: Int): WeatherCondition = when (code) {
    0 -> WeatherCondition(WeatherKind.CLEAR, "Despejado")
    1 -> WeatherCondition(WeatherKind.PARTLY_CLOUDY, "Poco nuboso")
    2 -> WeatherCondition(WeatherKind.PARTLY_CLOUDY, "Parcialmente nuboso")
    3 -> WeatherCondition(WeatherKind.CLOUDY, "Cubierto")
    45, 48 -> WeatherCondition(WeatherKind.FOG, "Niebla")
    51, 53, 55 -> WeatherCondition(WeatherKind.DRIZZLE, "Llovizna")
    56, 57 -> WeatherCondition(WeatherKind.DRIZZLE, "Llovizna helada")
    61 -> WeatherCondition(WeatherKind.RAIN, "Lluvia débil")
    63 -> WeatherCondition(WeatherKind.RAIN, "Lluvia")
    65 -> WeatherCondition(WeatherKind.RAIN, "Lluvia fuerte")
    66, 67 -> WeatherCondition(WeatherKind.RAIN, "Lluvia helada")
    71, 73, 75, 77 -> WeatherCondition(WeatherKind.SNOW, "Nieve")
    80, 81 -> WeatherCondition(WeatherKind.RAIN, "Chubascos")
    82 -> WeatherCondition(WeatherKind.RAIN, "Chubascos fuertes")
    85, 86 -> WeatherCondition(WeatherKind.SNOW, "Chubascos de nieve")
    95 -> WeatherCondition(WeatherKind.STORM, "Tormenta")
    96, 99 -> WeatherCondition(WeatherKind.STORM, "Tormenta con granizo")
    else -> WeatherCondition(WeatherKind.CLOUDY, "Desconocido")
}

data class DayForecast(val date: LocalDate, val code: Int, val minC: Int, val maxC: Int)

data class Forecast(
    val tempC: Int,
    val code: Int,
    val isDay: Boolean,
    /** El primer día es hoy. */
    val days: List<DayForecast>,
)

/** Lugar para el que se pide la previsión. */
data class Place(val name: String, val region: String?, val country: String?, val lat: Double, val lon: Double) {
    val subtitle: String get() = listOfNotNull(region, country).filter { it.isNotBlank() }.joinToString(", ")
}

/** Lo que muestra la interfaz: previsión (posiblemente en caché), lugar y hora de actualización. */
data class WeatherState(
    val forecast: Forecast? = null,
    val placeName: String? = null,
    val updatedAtMillis: Long? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

/** Analiza la respuesta de https://api.open-meteo.com/v1/forecast (current + daily). */
fun parseForecast(json: String): Forecast {
    val root = JSONObject(json)
    val current = root.getJSONObject("current")
    val daily = root.getJSONObject("daily")
    val dates = daily.getJSONArray("time")
    val codes = daily.getJSONArray("weather_code")
    val max = daily.getJSONArray("temperature_2m_max")
    val min = daily.getJSONArray("temperature_2m_min")
    val days = (0 until dates.length()).map { i ->
        DayForecast(
            date = LocalDate.parse(dates.getString(i)),
            code = codes.getInt(i),
            minC = Math.round(min.getDouble(i)).toInt(),
            maxC = Math.round(max.getDouble(i)).toInt(),
        )
    }
    return Forecast(
        tempC = Math.round(current.getDouble("temperature_2m")).toInt(),
        code = current.getInt("weather_code"),
        isDay = current.optInt("is_day", 1) == 1,
        days = days,
    )
}

/** Analiza la respuesta de https://geocoding-api.open-meteo.com/v1/search. */
fun parsePlaces(json: String): List<Place> {
    val results = JSONObject(json).optJSONArray("results") ?: return emptyList()
    return (0 until results.length()).map { i ->
        val r = results.getJSONObject(i)
        Place(
            name = r.getString("name"),
            region = r.optString("admin1").takeIf { it.isNotBlank() },
            country = r.optString("country").takeIf { it.isNotBlank() },
            lat = r.getDouble("latitude"),
            lon = r.getDouble("longitude"),
        )
    }
}
