package com.cockpitf30.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cockpitf30.system.AppSettings
import com.cockpitf30.ui.components.FocusTile
import com.cockpitf30.ui.components.InitialFocus
import com.cockpitf30.ui.components.TopBar
import com.cockpitf30.ui.components.WeatherIcon
import com.cockpitf30.ui.components.WeatherScene
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.cockpitf30.ui.components.dayLabel
import com.cockpitf30.ui.components.updatedText
import com.cockpitf30.ui.theme.Cockpit
import com.cockpitf30.ui.theme.numeric
import com.cockpitf30.weather.Place
import com.cockpitf30.weather.WeatherKind
import com.cockpitf30.weather.WeatherRepository
import com.cockpitf30.weather.wmoCondition
import kotlinx.coroutines.launch

/** Previsión de 4 días y selección de la localidad: GPS o búsqueda por nombre. */
@Composable
fun WeatherScreen() {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    val scope = rememberCoroutineScope()
    val weather by WeatherRepository.state.collectAsStateWithLifecycle()
    var usingGps by remember { mutableStateOf(settings.weatherPlace == null) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var searchMessage by remember { mutableStateOf<String?>(null) }

    fun select(place: Place?) {
        settings.weatherPlace = place
        usingGps = place == null
        scope.launch { WeatherRepository.refresh(context) }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { select(null) }
    fun useGps() {
        if (WeatherRepository.hasLocationPermission(context)) select(null)
        else permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
    }
    fun search() {
        searchMessage = "Buscando…"
        scope.launch {
            runCatching { WeatherRepository.search(query) }
                .onSuccess { results = it; searchMessage = if (it.isEmpty()) "Sin resultados" else null }
                .onFailure { searchMessage = "Error: ${it.message}" }
        }
    }

    val first = remember { FocusRequester() }
    InitialFocus(first)

    Box(Modifier.fillMaxSize()) {
        weather.forecast?.let { f ->
            WeatherScene(wmoCondition(f.code).kind, f.isDay, f.code, Modifier.fillMaxSize(), maxParticles = 110, sunAt = Offset(0.5f, 0.36f))
            // Velo oscuro a la derecha (selector) y abajo para que todo se lea sobre la animación.
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0.58f to Color.Transparent, 0.7f to Color.Black.copy(alpha = 0.55f))))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.45f))))
        }
        Column(Modifier.fillMaxSize()) {
            TopBar("Tiempo") {
                Text(updatedText(weather), style = MaterialTheme.typography.bodyMedium, color = Cockpit.TextDim)
            }
            Row(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                // Previsión
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    val f = weather.forecast
                    val c = f?.let { wmoCondition(it.code) }
                    Text(weather.placeName ?: "Sin localidad", style = MaterialTheme.typography.headlineSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Con datos, el sol, las nubes o la lluvia los dibuja la escena animada del fondo.
                        if (f == null) {
                            WeatherIcon(c?.kind ?: WeatherKind.CLOUDY, true, Modifier.size(150.dp))
                            Spacer(Modifier.width(24.dp))
                        }
                        Column {
                            Text(f?.let { "${it.tempC}°" } ?: "--°", style = numeric(96, FontWeight.Medium))
                            Text(c?.text ?: "Sin datos", style = MaterialTheme.typography.titleMedium, color = Cockpit.TextDim)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        (f?.days ?: emptyList()).forEach { d ->
                            Column(
                                Modifier.width(150.dp).padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(dayLabel(d.date), style = MaterialTheme.typography.labelLarge, color = Cockpit.TextDim)
                                WeatherIcon(wmoCondition(d.code).kind, true, Modifier.size(64.dp))
                                Text("${d.minC}° / ${d.maxC}°", style = numeric(24, FontWeight.Medium))
                                Text(wmoCondition(d.code).text, style = MaterialTheme.typography.labelMedium, color = Cockpit.TextDim, maxLines = 1)
                            }
                        }
                    }
                }
                // Selección de localidad
                Column(Modifier.width(620.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("LOCALIDAD", style = MaterialTheme.typography.labelLarge, color = Cockpit.BlueLight)
                    FocusTile(onClick = ::useGps, selected = usingGps, modifier = Modifier.fillMaxWidth().height(72.dp).focusRequester(first)) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, null, tint = Cockpit.BlueLight)
                            Spacer(Modifier.width(14.dp))
                            Text("Usar ubicación GPS", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = query, onValueChange = { query = it },
                            placeholder = { Text("Buscar localidad…") },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { search() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Cockpit.BlueLight, unfocusedBorderColor = Cockpit.GlassBorder,
                                focusedTextColor = Cockpit.Text, unfocusedTextColor = Cockpit.Text, cursorColor = Cockpit.BlueLight,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        FocusTile(onClick = ::search, modifier = Modifier.size(72.dp)) {
                            Icon(Icons.Filled.Search, null, Modifier.align(Alignment.Center).size(32.dp), tint = Cockpit.Text)
                        }
                    }
                    searchMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Cockpit.TextDim) }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                        items(results) { place ->
                            val selected = !usingGps && settings.weatherPlace?.let { it.lat == place.lat && it.lon == place.lon } == true
                            FocusTile(onClick = { select(place) }, selected = selected, modifier = Modifier.fillMaxWidth().height(72.dp)) {
                                Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.Center) {
                                    Text(place.name, style = MaterialTheme.typography.titleMedium)
                                    Text(place.subtitle, style = MaterialTheme.typography.bodyMedium, color = Cockpit.TextDim)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
