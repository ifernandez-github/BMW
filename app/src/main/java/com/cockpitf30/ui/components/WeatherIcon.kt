package com.cockpitf30.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.cockpitf30.weather.WeatherKind
import kotlin.math.cos
import kotlin.math.sin

/** Icono del tiempo con degradados y brillo, dibujado con formas simples. */
@Composable
fun WeatherIcon(kind: WeatherKind, isDay: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        when (kind) {
            WeatherKind.CLEAR -> if (isDay) sun(Offset(size.width / 2, size.height / 2), s * 0.36f) else moon(s)
            WeatherKind.PARTLY_CLOUDY -> {
                if (isDay) sun(Offset(size.width * 0.62f, size.height * 0.36f), s * 0.26f) else moon(s * 0.8f)
                cloud(Offset(size.width * 0.42f, size.height * 0.62f), s * 0.62f)
            }
            WeatherKind.CLOUDY -> {
                cloud(Offset(size.width * 0.62f, size.height * 0.42f), s * 0.48f, dim = true)
                cloud(Offset(size.width * 0.46f, size.height * 0.60f), s * 0.66f)
            }
            WeatherKind.FOG -> {
                cloud(Offset(size.width * 0.5f, size.height * 0.44f), s * 0.6f)
                for (i in 0..2) {
                    val y = size.height * (0.70f + i * 0.09f)
                    drawLine(Color.White.copy(alpha = 0.75f - i * 0.15f), Offset(size.width * 0.18f, y), Offset(size.width * 0.82f, y), s * 0.045f, StrokeCap.Round)
                }
            }
            WeatherKind.DRIZZLE, WeatherKind.RAIN -> {
                cloud(Offset(size.width * 0.5f, size.height * 0.40f), s * 0.66f)
                val n = if (kind == WeatherKind.RAIN) 4 else 3
                for (i in 0 until n) {
                    val x = size.width * (0.30f + i * 0.4f / (n - 1))
                    drop(Offset(x, size.height * 0.80f), s * (if (kind == WeatherKind.RAIN) 0.07f else 0.05f))
                }
            }
            WeatherKind.SNOW -> {
                cloud(Offset(size.width * 0.5f, size.height * 0.40f), s * 0.66f)
                listOf(0.32f to 0.78f, 0.5f to 0.86f, 0.68f to 0.78f).forEach { (x, y) -> flake(Offset(size.width * x, size.height * y), s * 0.06f) }
            }
            WeatherKind.STORM -> {
                cloud(Offset(size.width * 0.5f, size.height * 0.40f), s * 0.66f, dim = true)
                bolt(Offset(size.width * 0.5f, size.height * 0.62f), s * 0.34f)
            }
        }
    }
}

private fun DrawScope.sun(c: Offset, r: Float) {
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE082).copy(alpha = 0.55f), Color.Transparent), c, r * 1.7f), r * 1.7f, c)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFC107), Color(0xFFFF8F00)), Offset(c.x - r * 0.35f, c.y - r * 0.35f), r * 1.5f), r, c)
    drawOval(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), startY = c.y - r * 0.85f, endY = c.y - r * 0.1f),
        Offset(c.x - r * 0.55f, c.y - r * 0.85f), Size(r * 0.9f, r * 0.6f))
}

private fun DrawScope.moon(s: Float) {
    val c = Offset(size.width / 2, size.height / 2)
    val r = s * 0.3f
    drawCircle(Brush.radialGradient(listOf(Color(0xFFB3C7FF).copy(alpha = 0.35f), Color.Transparent), c, r * 1.7f), r * 1.7f, c)
    val moon = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(c, r))
    }
    val cut = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(c.x + r * 0.45f, c.y - r * 0.3f), r * 0.9f)) }
    val crescent = Path().apply { op(moon, cut, androidx.compose.ui.graphics.PathOperation.Difference) }
    drawPath(crescent, Brush.linearGradient(listOf(Color.White, Color(0xFFC5D3F0)), Offset(c.x - r, c.y - r), Offset(c.x + r, c.y + r)))
}

private fun DrawScope.cloud(c: Offset, w: Float, dim: Boolean = false) {
    val h = w * 0.5f
    val top = if (dim) Color(0xFFB8C4D4) else Color.White
    val bottom = if (dim) Color(0xFF7D8DA3) else Color(0xFFC9D6E8)
    val brush = Brush.verticalGradient(listOf(top, bottom), startY = c.y - h * 0.8f, endY = c.y + h * 0.5f)
    val path = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(Offset(c.x - w * 0.22f, c.y - h * 0.05f), w * 0.24f))
        addOval(androidx.compose.ui.geometry.Rect(Offset(c.x + w * 0.04f, c.y - h * 0.28f), w * 0.3f))
        addOval(androidx.compose.ui.geometry.Rect(Offset(c.x + w * 0.28f, c.y + h * 0.05f), w * 0.2f))
        addRoundRect(androidx.compose.ui.geometry.RoundRect(c.x - w * 0.42f, c.y, c.x + w * 0.46f, c.y + h * 0.5f, h * 0.25f, h * 0.25f))
    }
    drawPath(path, brush)
}

private fun DrawScope.drop(c: Offset, r: Float) {
    val p = Path().apply {
        moveTo(c.x, c.y - r * 2.2f)
        cubicTo(c.x + r * 0.6f, c.y - r, c.x + r, c.y - r * 0.2f, c.x + r, c.y + r * 0.2f)
        cubicTo(c.x + r, c.y + r * 1.3f, c.x - r, c.y + r * 1.3f, c.x - r, c.y + r * 0.2f)
        cubicTo(c.x - r, c.y - r * 0.2f, c.x - r * 0.6f, c.y - r, c.x, c.y - r * 2.2f)
        close()
    }
    drawPath(p, Brush.verticalGradient(listOf(Color(0xFF8FD3FF), Color(0xFF1C69D8)), startY = c.y - r * 2.2f, endY = c.y + r * 1.2f))
}

private fun DrawScope.flake(c: Offset, r: Float) {
    for (i in 0 until 3) {
        val a = Math.PI * i / 3
        val dx = (cos(a) * r).toFloat()
        val dy = (sin(a) * r).toFloat()
        drawLine(Color.White, Offset(c.x - dx, c.y - dy), Offset(c.x + dx, c.y + dy), r * 0.35f, StrokeCap.Round)
    }
}

private fun DrawScope.bolt(c: Offset, h: Float) {
    val w = h * 0.55f
    val p = Path().apply {
        moveTo(c.x + w * 0.1f, c.y - h * 0.5f); lineTo(c.x - w * 0.45f, c.y + h * 0.05f); lineTo(c.x - w * 0.02f, c.y + h * 0.05f)
        lineTo(c.x - w * 0.2f, c.y + h * 0.5f); lineTo(c.x + w * 0.45f, c.y - h * 0.1f); lineTo(c.x + w * 0.02f, c.y - h * 0.1f); close()
    }
    drawPath(p, Color(0xFFFFE082).copy(alpha = 0.4f), style = Stroke(h * 0.12f))
    drawPath(p, Brush.verticalGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFA000)), startY = c.y - h / 2, endY = c.y + h / 2))
}
