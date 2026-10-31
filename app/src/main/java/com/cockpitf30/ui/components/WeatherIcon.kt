package com.cockpitf30.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.cockpitf30.weather.WeatherKind

/** Icono del tiempo con el mismo estilo realista que la escena: sol volumétrico y nubes esponjosas. */
@Composable
fun WeatherIcon(kind: WeatherKind, isDay: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val w = size.width
        val h = size.height
        fun cloud(x: Float, y: Float, k: Float, dark: Boolean = false) = fluffyCloud(Offset(w * x, h * y), s * k, seed = (x * 100).toInt(), dark = dark)
        when (kind) {
            WeatherKind.CLEAR -> if (isDay) glowSun(Offset(w / 2, h / 2), s * 0.24f, flare = false) else softMoon(Offset(w / 2, h / 2), s * 0.26f)
            WeatherKind.PARTLY_CLOUDY -> {
                if (isDay) glowSun(Offset(w * 0.62f, h * 0.38f), s * 0.2f, flare = false) else softMoon(Offset(w * 0.62f, h * 0.36f), s * 0.2f)
                cloud(0.44f, 0.72f, 0.78f)
            }
            WeatherKind.CLOUDY -> {
                cloud(0.62f, 0.5f, 0.6f, dark = true)
                cloud(0.44f, 0.72f, 0.8f)
            }
            WeatherKind.FOG -> {
                cloud(0.5f, 0.52f, 0.78f)
                for (i in 0..2) {
                    val y = h * (0.7f + i * 0.09f)
                    drawLine(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.8f - i * 0.2f), Color.Transparent)), Offset(w * 0.1f, y), Offset(w * 0.9f, y), s * 0.05f, StrokeCap.Round)
                }
            }
            WeatherKind.DRIZZLE, WeatherKind.RAIN -> {
                val heavy = kind == WeatherKind.RAIN
                val n = if (heavy) 5 else 3
                for (i in 0 until n) {
                    val x = w * (0.26f + i * 0.48f / (n - 1))
                    drop(Offset(x, h * 0.84f), s * (if (heavy) 0.16f else 0.11f), s * (if (heavy) 0.035f else 0.025f))
                }
                cloud(0.5f, 0.52f, 0.86f, dark = true)
            }
            WeatherKind.SNOW -> {
                listOf(0.3f to 0.82f, 0.5f to 0.9f, 0.7f to 0.82f).forEach { (x, y) ->
                    val p = Offset(w * x, h * y)
                    drawCircle(Brush.radialGradient(listOf(Color.White, Color.Transparent), p, s * 0.07f), s * 0.07f, p)
                }
                cloud(0.5f, 0.52f, 0.86f)
            }
            WeatherKind.STORM -> {
                bolt(Offset(w * 0.5f, h * 0.74f), s * 0.36f)
                cloud(0.5f, 0.5f, 0.86f, dark = true)
            }
        }
    }
}

/** Gota inclinada: trazo con cabeza más opaca. */
private fun DrawScope.drop(c: Offset, len: Float, width: Float) {
    val dx = len * 0.25f
    drawLine(Color(0xFFBFE0FF).copy(alpha = 0.35f), Offset(c.x + dx, c.y - len), c, width, StrokeCap.Round)
    drawLine(Color(0xFFE8F4FF), Offset(c.x + dx * 0.4f, c.y - len * 0.4f), c, width, StrokeCap.Round)
}

private fun DrawScope.bolt(c: Offset, h: Float) {
    val w = h * 0.55f
    val p = Path().apply {
        moveTo(c.x + w * 0.1f, c.y - h * 0.5f); lineTo(c.x - w * 0.45f, c.y + h * 0.05f); lineTo(c.x - w * 0.02f, c.y + h * 0.05f)
        lineTo(c.x - w * 0.2f, c.y + h * 0.5f); lineTo(c.x + w * 0.45f, c.y - h * 0.1f); lineTo(c.x + w * 0.02f, c.y - h * 0.1f); close()
    }
    drawPath(p, Color(0xFFFFE082).copy(alpha = 0.45f), style = Stroke(h * 0.14f))
    drawPath(p, Brush.verticalGradient(listOf(Color(0xFFFFFDE7), Color(0xFFFFC107)), startY = c.y - h / 2, endY = c.y + h / 2))
}
