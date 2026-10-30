package com.cockpitf30.ui.screens

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cockpitf30.R
import com.cockpitf30.ui.components.TopBar
import com.cockpitf30.ui.theme.Cockpit
import com.cockpitf30.ui.theme.numeric
import com.cockpitf30.vehicle.Doors
import com.cockpitf30.vehicle.VehicleRepository
import com.cockpitf30.vehicle.VehicleState

/**
 * Coche visto desde arriba con halo azul, la presión de cada rueda alrededor, autonomía y
 * puertas abiertas marcadas en rojo sobre la imagen. Sin datos se muestra "--".
 */
@Composable
fun VehicleScreen() {
    val state by VehicleRepository.state.collectAsStateWithLifecycle()
    val status by VehicleRepository.status.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopBar("Vehículo") {
            Text(
                if (status.connected) status.sourceName else "Sin datos · ${status.sourceName}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (status.connected) Cockpit.TextDim else Cockpit.Alert,
            )
        }
        Row(
            Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.width(300.dp).fillMaxHeight(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.SpaceEvenly) {
                Metric("Autonomía", state.rangeKm?.let { "$it" } ?: "--", "km")
                Metric("Delantera izq.", bar(state.tyres.frontLeft), "bar")
                Metric("Trasera izq.", bar(state.tyres.rearLeft), "bar")
            }
            CarWithHalo(state.doors, Modifier.padding(horizontal = 64.dp).fillMaxHeight(0.92f))
            Column(Modifier.width(300.dp).fillMaxHeight(), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.SpaceEvenly) {
                Metric("Puertas", doorsText(state), null, doorsColor(state))
                Metric("Delantera der.", bar(state.tyres.frontRight), "bar")
                Metric("Trasera der.", bar(state.tyres.rearRight), "bar")
            }
        }
    }
}

private fun bar(v: Float?) = v?.let { "%.1f".format(it) } ?: "--"

private fun doorsText(s: VehicleState) = when {
    s.doors.anyOpen -> "Abierta"
    s.doors.driver == null -> "--"
    else -> "Cerradas"
}

private fun doorsColor(s: VehicleState) = when {
    s.doors.anyOpen -> Cockpit.Alert
    s.doors.driver == null -> Cockpit.TextDim
    else -> Cockpit.Ok
}

@Composable
private fun Metric(label: String, value: String, unit: String?, valueColor: Color = Cockpit.Text) {
    Column {
        Text(label.uppercase(), style = MaterialTheme.typography.labelLarge, color = Cockpit.TextDim)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = numeric(44, FontWeight.Medium), color = valueColor)
            if (unit != null) Text(" $unit", style = numeric(22), color = Cockpit.TextDim, modifier = Modifier.padding(bottom = 6.dp))
        }
    }
}

/** Imagen cenital con halo azul (difuminado en Android 12+, degradado en versiones anteriores). */
@Composable
private fun CarWithHalo(doors: Doors, modifier: Modifier = Modifier) {
    val car = painterResource(R.drawable.car_top)
    Box(
        modifier.drawBehind {
            drawCircle(
                Brush.radialGradient(listOf(Cockpit.Blue.copy(alpha = 0.35f), Color.Transparent), center, size.maxDimension * 0.6f),
                radius = size.maxDimension * 0.6f,
            )
        },
        contentAlignment = Alignment.Center,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Sin recortar el difuminado a los bordes de la imagen, para que el halo no quede cuadrado.
            Image(
                car, null, Modifier.fillMaxHeight().blur(18.dp, BlurredEdgeTreatment.Unbounded),
                colorFilter = ColorFilter.tint(Cockpit.BlueLight),
            )
        }
        Image(car, null, Modifier.fillMaxHeight().drawWithContent { drawContent(); drawDoors(doors) })
    }
}

/** Marca en rojo las puertas, maletero y capó abiertos sobre la imagen (morro hacia arriba). */
private fun DrawScope.drawDoors(d: Doors) {
    val w = size.width
    val h = size.height
    val stroke = 6.dp.toPx()
    fun side(open: Boolean?, left: Boolean, from: Float, to: Float) {
        if (open != true) return
        val x = if (left) w * 0.05f else w * 0.95f
        drawLine(Cockpit.Alert, Offset(x, h * from), Offset(x, h * to), stroke)
    }
    side(d.driver, left = true, 0.38f, 0.55f)
    side(d.passenger, left = false, 0.38f, 0.55f)
    side(d.rearLeft, left = true, 0.56f, 0.72f)
    side(d.rearRight, left = false, 0.56f, 0.72f)
    if (d.hood == true) drawLine(Cockpit.Alert, Offset(w * 0.25f, h * 0.02f), Offset(w * 0.75f, h * 0.02f), stroke)
    if (d.trunk == true) drawLine(Cockpit.Alert, Offset(w * 0.25f, h * 0.98f), Offset(w * 0.75f, h * 0.98f), stroke)
}
