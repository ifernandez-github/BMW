package com.cockpitf30.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.cockpitf30.ui.theme.Cockpit
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val START_ANGLE = 135f
private const val SWEEP = 270f

/**
 * Esfera circular estilo cuadro digital. [value] null muestra "--" (sin datos).
 * [redFrom] colorea la zona roja (p.ej. RPM).
 */
@Composable
fun Gauge(
    value: Int?,
    max: Int,
    unit: String,
    modifier: Modifier = Modifier,
    majorStep: Int = max / 10,
    labelDivisor: Int = 1,
    redFrom: Int? = null,
) {
    val fraction by animateFloatAsState(
        ((value ?: 0).coerceIn(0, max).toFloat() / max),
        animationSpec = tween(180),
        label = "gauge",
    )
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeW = size.minDimension * 0.035f
            val radius = min(size.width, size.height) / 2 - strokeW * 2
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)

            drawArc(Cockpit.Track, START_ANGLE, SWEEP, false, topLeft, arcSize, style = Stroke(strokeW, cap = StrokeCap.Round))
            if (redFrom != null) {
                val redStart = START_ANGLE + SWEEP * redFrom / max
                drawArc(Cockpit.Alert.copy(alpha = 0.55f), redStart, START_ANGLE + SWEEP - redStart, false, topLeft, arcSize, style = Stroke(strokeW))
            }
            drawArc(
                Brush.sweepGradient(listOf(Cockpit.Blue, Cockpit.BlueLight, Cockpit.Blue), center),
                START_ANGLE, SWEEP * fraction, false, topLeft, arcSize,
                style = Stroke(strokeW, cap = StrokeCap.Round),
            )

            // Marcas
            val steps = max / majorStep
            for (i in 0..steps) {
                val angle = Math.toRadians((START_ANGLE + SWEEP * i / steps).toDouble())
                val outer = radius - strokeW * 1.6f
                val inner = outer - strokeW * 1.6f
                val color = if (redFrom != null && i * majorStep >= redFrom) Cockpit.Alert else Cockpit.TextDim
                drawLine(
                    color,
                    Offset(center.x + outer * cos(angle).toFloat(), center.y + outer * sin(angle).toFloat()),
                    Offset(center.x + inner * cos(angle).toFloat(), center.y + inner * sin(angle).toFloat()),
                    strokeWidth = strokeW / 3,
                )
            }

            // Punto luminoso en la posición actual
            val a = Math.toRadians((START_ANGLE + SWEEP * fraction).toDouble())
            drawCircle(Cockpit.BlueLight, strokeW * 0.9f, Offset(center.x + radius * cos(a).toFloat(), center.y + radius * sin(a).toFloat()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value?.let { (it / labelDivisor).toString() } ?: "--",
                style = MaterialTheme.typography.displayLarge,
                color = Cockpit.Text,
            )
            Text(unit, style = MaterialTheme.typography.labelLarge, color = Cockpit.TextDim)
        }
    }
}
