package com.cockpitf30.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.cockpitf30.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cockpitf30.ui.components.CardArt
import com.cockpitf30.ui.components.CardTheme
import com.cockpitf30.ui.components.ClusterStrip
import com.cockpitf30.ui.components.glassBackground
import androidx.compose.ui.draw.drawBehind
import com.cockpitf30.ui.components.FocusTile
import com.cockpitf30.ui.components.InitialFocus
import androidx.compose.ui.text.font.FontWeight
import com.cockpitf30.ui.theme.Cockpit
import com.cockpitf30.ui.theme.numeric
import com.cockpitf30.vehicle.VehicleRepository
import com.cockpitf30.vehicle.VehicleState

/** Tarjeta de la parrilla panorámica: un resumen rápido de una función. */
private class Card(
    val title: String,
    val subtitle: String,
    val theme: CardTheme,
    val action: (Context) -> Unit,
    val bodyAlignment: Alignment = Alignment.Center,
    val body: (@Composable (VehicleState) -> Unit)? = null,
)

@Composable
fun HomeScreen(onOpen: (Screen) -> Unit) {
    val state by VehicleRepository.state.collectAsStateWithLifecycle()
    val status by VehicleRepository.status.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val cards = remember {
        listOf(
            Card("Música", "Abrir reproductor", CardTheme.MUSIC, ::openMusic),
            Card("Navegación", "Mapas", CardTheme.NAV, ::openMaps),
            Card("Datos de viaje", "Velocidad y motor", CardTheme.TRIP, { onOpen(Screen.VEHICLE) }, Alignment.BottomStart) { TripBody(it) },
            Card("Neumáticos", "Presión (bar)", CardTheme.TYRES, { onOpen(Screen.VEHICLE) }) { TyreBody(it) },
            Card("Vehículo", "Puertas y cuadro", CardTheme.VEHICLE, { onOpen(Screen.VEHICLE) }),
            Card("Teléfono", "Marcador", CardTheme.PHONE, { ctx -> launch(ctx, Intent(Intent.ACTION_DIAL)) }),
            Card("Diagnóstico", "Fuente de datos e iDrive", CardTheme.DIAG, { onOpen(Screen.DIAGNOSTICS) }),
        )
    }
    val firstCard = remember { FocusRequester() }
    InitialFocus(firstCard)

    Column(Modifier.fillMaxSize().padding(top = 10.dp, bottom = 16.dp)) {
        // El cuadro ocupa algo más de la mitad de la altura; las tarjetas, el resto.
        ClusterStrip(state, status, Modifier.fillMaxWidth().height(410.dp).padding(horizontal = 12.dp))
        Spacer(Modifier.height(6.dp))
        LazyRow(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            itemsIndexed(cards) { i, card ->
                val mod = Modifier.width(380.dp).fillMaxHeight()
                FocusTile(
                    onClick = { card.action(context) },
                    modifier = if (i == 0) mod.focusRequester(firstCard) else mod,
                ) {
                    // Ilustración tipo cristal con el color de la función; título abajo a la izquierda.
                    Box(Modifier.fillMaxSize().drawBehind { glassBackground(card.theme) }) {
                        CardArt(card.theme)
                        Column(Modifier.fillMaxSize().padding(24.dp)) {
                            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = card.bodyAlignment) {
                                card.body?.invoke(state)
                            }
                            Text(card.title, style = MaterialTheme.typography.headlineSmall)
                            Text(card.subtitle, style = MaterialTheme.typography.bodyMedium, color = Cockpit.TextDim)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripBody(state: VehicleState) {
    Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.Bottom) {
        Metric(state.speedKmh?.toString() ?: "--", "km/h")
        Metric(state.rpm?.toString() ?: "--", "rpm")
    }
}

@Composable
private fun Metric(value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.displaySmall)
        Text(unit, style = MaterialTheme.typography.labelMedium, color = Cockpit.TextDim)
    }
}

/** Coche visto desde arriba con la presión de cada rueda ("--" si no hay datos). */
@Composable
private fun TyreBody(state: VehicleState) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(32.dp), horizontalAlignment = Alignment.End) {
            Pressure(state.tyres.frontLeft); Pressure(state.tyres.rearLeft)
        }
        Image(painterResource(R.drawable.car_top), null, Modifier.height(104.dp))
        Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
            Pressure(state.tyres.frontRight); Pressure(state.tyres.rearRight)
        }
    }
}

@Composable
private fun Pressure(value: Float? = null) {
    Text(value?.let { "%.1f".format(it) } ?: "--", style = numeric(24, FontWeight.SemiBold))
}

internal fun openMaps(context: Context) = launch(
    context,
    context.packageManager.getLaunchIntentForPackage("com.google.android.apps.maps")
        ?: Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0")),
)

internal fun openMusic(context: Context) = launch(
    context,
    context.packageManager.getLaunchIntentForPackage("com.spotify.music")
        ?: Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC),
)

internal fun launch(context: Context, intent: Intent) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
    }
}

enum class Screen { HOME, VEHICLE, APPS, DIAGNOSTICS }
