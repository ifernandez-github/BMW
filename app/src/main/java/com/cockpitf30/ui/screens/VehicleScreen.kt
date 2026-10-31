package com.cockpitf30.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
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

/** Proporciones de la escena car_top_scene (576x906, morro arriba). */
private const val SCENE_ASPECT = 576f / 906f
private const val BODY_LEFT = 0.255f
private const val BODY_RIGHT = 0.715f
private const val FRONT_AXLE = 0.28f
private const val REAR_AXLE = 0.71f

/**
 * Escena del coche visto desde arriba (morro arriba) con fondo de hexágonos, centrada y fundida
 * con el fondo. Alrededor, la presión de cada rueda a la altura de su eje, autonomía y puertas;
 * lo abierto se marca en rojo sobre la imagen. Sin datos se muestra "--".
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
        BoxWithConstraints(Modifier.fillMaxSize().padding(bottom = 12.dp)) {
            val h = maxHeight
            val w = h * SCENE_ASPECT
            val left = (maxWidth - w) / 2
            fun x(f: Float) = left + w * f
            fun y(f: Float) = h * f

            Image(
                painterResource(R.drawable.car_top_scene), null,
                Modifier.offset(x = left).size(w, h)
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                    drawContent()
                    drawDoors(state.doors)
                    vignette()
                },
            )

            val t = state.tyres
            val chip = 220.dp
            Info("Del. izq.", bar(t.frontLeft), "bar", Modifier.offset(x(BODY_LEFT) - chip - 100.dp, y(FRONT_AXLE) - 40.dp).width(chip), Alignment.End)
            Info("Tras. izq.", bar(t.rearLeft), "bar", Modifier.offset(x(BODY_LEFT) - chip - 100.dp, y(REAR_AXLE) - 40.dp).width(chip), Alignment.End)
            Info("Del. der.", bar(t.frontRight), "bar", Modifier.offset(x(BODY_RIGHT) + 100.dp, y(FRONT_AXLE) - 40.dp).width(chip), Alignment.Start)
            Info("Tras. der.", bar(t.rearRight), "bar", Modifier.offset(x(BODY_RIGHT) + 100.dp, y(REAR_AXLE) - 40.dp).width(chip), Alignment.Start)

            // Autonomía y puertas, más hacia fuera y a media altura.
            val side = 300.dp
            Info("Autonomía", state.rangeKm?.let { "$it" } ?: "--", "km", Modifier.offset(left - side - 170.dp, y(0.45f) - 40.dp).width(side), Alignment.End)
            Column(Modifier.offset(left + w + 170.dp, y(0.45f) - 40.dp).width(side)) {
                Info("Puertas", doorsText(state), null, Modifier, Alignment.Start, doorsColor(state))
                openItems(state.doors).forEach {
                    Text(it, style = MaterialTheme.typography.labelLarge, color = Cockpit.Alert, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

private fun bar(v: Float?) = v?.let { "%.1f".format(it) } ?: "--"

private fun doorsText(s: VehicleState) = when {
    s.doors.anyOpen -> "Abierto"
    s.doors.driver == null -> "--"
    else -> "Cerradas"
}

private fun doorsColor(s: VehicleState) = when {
    s.doors.anyOpen -> Cockpit.Alert
    s.doors.driver == null -> Cockpit.TextDim
    else -> Cockpit.Ok
}

private fun openItems(d: Doors) = buildList {
    if (d.driver == true) add("PUERTA DEL. IZQ.")
    if (d.passenger == true) add("PUERTA DEL. DER.")
    if (d.rearLeft == true) add("PUERTA TRAS. IZQ.")
    if (d.rearRight == true) add("PUERTA TRAS. DER.")
    if (d.hood == true) add("CAPÓ ABIERTO")
    if (d.trunk == true) add("MALETERO ABIERTO")
}

/** Rótulo pequeño y valor grande en Saira, en un recuadro translúcido. */
@Composable
private fun Info(
    label: String, value: String, unit: String?, modifier: Modifier, align: Alignment.Horizontal,
    valueColor: Color = Cockpit.Text,
) {
    Column(modifier, horizontalAlignment = align) {
        Column(
            Modifier.background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(10.dp)).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = align,
        ) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelLarge, color = Cockpit.TextDim)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = numeric(40, FontWeight.Medium), color = valueColor)
                if (unit != null) Text(" $unit", style = numeric(20), color = Cockpit.TextDim, modifier = Modifier.padding(bottom = 6.dp))
            }
        }
    }
}

/** Funde los bordes de la escena con el fondo de la app, haciéndolos transparentes. */
private fun DrawScope.vignette() {
    val fx = 0.22f
    val fy = 0.12f
    drawRect(Brush.horizontalGradient(0f to Color.Transparent, fx to Color.Black, 1f - fx to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
    drawRect(Brush.verticalGradient(0f to Color.Transparent, fy to Color.Black, 1f - fy to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
}

/** Puertas, capó y maletero abiertos en rojo sobre la imagen (coordenadas relativas a la escena). */
private fun DrawScope.drawDoors(d: Doors) {
    val w = size.width
    val h = size.height
    val stroke = 7.dp.toPx()
    val red = Cockpit.Alert
    fun side(open: Boolean?, x: Float, from: Float, to: Float) {
        if (open != true) return
        drawLine(red.copy(alpha = 0.35f), Offset(w * x, h * from), Offset(w * x, h * to), stroke * 3)
        drawLine(red, Offset(w * x, h * from), Offset(w * x, h * to), stroke)
    }
    side(d.driver, BODY_LEFT, 0.36f, 0.55f)
    side(d.rearLeft, BODY_LEFT, 0.56f, 0.69f)
    side(d.passenger, BODY_RIGHT, 0.36f, 0.55f)
    side(d.rearRight, BODY_RIGHT, 0.56f, 0.69f)
    if (d.hood == true) drawRect(red.copy(alpha = 0.3f), Offset(w * 0.30f, h * 0.13f), Size(w * 0.38f, h * 0.18f))
    if (d.trunk == true) drawRect(red.copy(alpha = 0.3f), Offset(w * 0.32f, h * 0.76f), Size(w * 0.36f, h * 0.115f))
}

