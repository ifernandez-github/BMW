package com.cockpitf30.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.cockpitf30.weather.WeatherKind
import kotlin.math.sin
import kotlin.random.Random

/** Intensidad de la precipitación según el código WMO (0 = nada, 1 = fuerte). */
fun precipitation(code: Int): Float = when (code) {
    51, 56 -> 0.25f
    53, 61, 80 -> 0.45f
    55, 57, 63, 66, 71, 81, 85 -> 0.7f
    65, 67, 73, 75, 77, 82, 86, 95, 96, 99 -> 1f
    else -> 0.5f
}

/** Segundos de animación; solo avanza mientras la pantalla está en primer plano. */
@Composable
private fun rememberSceneTime(): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val offset = (time.floatValue * 1e9).toLong()
            val start = withFrameNanos { it } - offset
            while (true) withFrameNanos { time.floatValue = (it - start) / 1e9f }
        }
    }
    return time
}

/** Semillas fijas de las partículas: x, velocidad, tamaño y fase en [0,1). Sin objetos nuevos por frame. */
private class Particles(count: Int, seed: Int) {
    val n = count
    val x = FloatArray(count)
    val speed = FloatArray(count)
    val size = FloatArray(count)
    val phase = FloatArray(count)

    init {
        val r = Random(seed)
        for (i in 0 until count) {
            x[i] = r.nextFloat(); speed[i] = r.nextFloat(); size[i] = r.nextFloat(); phase[i] = r.nextFloat()
        }
    }
}

/**
 * Escena animada del tiempo: cielo, sol con rayos o luna con estrellas, capas de nubes con
 * parallax, lluvia inclinada, relámpagos, nieve con vaivén o bancos de niebla. [maxParticles]
 * limita el coste (unas 100 a pantalla completa, menos en la tarjeta).
 */
@Composable
fun WeatherScene(
    kind: WeatherKind, isDay: Boolean, code: Int, modifier: Modifier = Modifier, maxParticles: Int = 100,
    /** Posición del sol o la luna, en fracción del ancho y alto. */
    sunAt: Offset = Offset(0.78f, 0.3f),
) {
    val time = rememberSceneTime()
    val drops = remember(maxParticles) { Particles(maxParticles, 1) }
    val stars = remember { Particles(40, 2) }
    val intensity = precipitation(code)
    Canvas(modifier.clipToBounds()) {
        val t = time.value
        sky(kind, isDay)
        when (kind) {
            WeatherKind.CLEAR -> if (isDay) sun(t, Offset(size.width * sunAt.x, size.height * sunAt.y), size.minDimension * 0.16f) else night(t, stars, sunAt)
            WeatherKind.PARTLY_CLOUDY -> {
                if (isDay) sun(t, Offset(size.width * sunAt.x, size.height * sunAt.y), size.minDimension * 0.13f) else night(t, stars, sunAt)
                clouds(t, layers = 2, dim = false)
            }
            WeatherKind.CLOUDY -> clouds(t, layers = 3, dim = false)
            WeatherKind.FOG -> fog(t)
            WeatherKind.DRIZZLE, WeatherKind.RAIN -> {
                clouds(t, layers = 3, dim = true)
                rain(t, drops, (drops.n * intensity).toInt().coerceAtLeast(12))
            }
            WeatherKind.STORM -> {
                clouds(t, layers = 3, dim = true)
                rain(t, drops, drops.n)
                lightning(t)
            }
            WeatherKind.SNOW -> {
                clouds(t, layers = 2, dim = false)
                snow(t, drops, (drops.n * intensity).toInt().coerceAtLeast(16))
            }
        }
    }
}

private fun DrawScope.sky(kind: WeatherKind, isDay: Boolean) {
    val colors = when {
        !isDay -> listOf(Color(0xFF050B1A), Color(0xFF0B1A33))
        kind == WeatherKind.CLEAR || kind == WeatherKind.PARTLY_CLOUDY -> listOf(Color(0xFF0D4F8B), Color(0xFF1B78B8))
        kind == WeatherKind.STORM -> listOf(Color(0xFF151A26), Color(0xFF283245))
        else -> listOf(Color(0xFF263449), Color(0xFF3A4C63))
    }
    drawRect(Brush.verticalGradient(colors))
}

/** Sol con rayos que giran despacio y un brillo que late. */
private fun DrawScope.sun(t: Float, c: Offset, r: Float) {
    val pulse = 1f + 0.06f * sin(t * 1.6f)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE082).copy(alpha = 0.45f), Color.Transparent), c, r * 3.2f * pulse), r * 3.2f * pulse, c)
    rotate(t * 8f, c) {
        for (i in 0 until 12) {
            rotate(i * 30f, c) {
                drawLine(Color(0xFFFFD54F).copy(alpha = 0.8f), Offset(c.x, c.y - r * 1.35f), Offset(c.x, c.y - r * (1.75f + 0.12f * sin(t * 2f + i))), r * 0.09f, StrokeCap.Round)
            }
        }
    }
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFC107), Color(0xFFFF8F00)), Offset(c.x - r * 0.35f, c.y - r * 0.35f), r * 1.5f), r, c)
}

/** Luna y estrellas que titilan. */
private fun DrawScope.night(t: Float, stars: Particles, at: Offset) {
    for (i in 0 until stars.n) {
        val a = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * (1f + stars.speed[i] * 2f) + stars.phase[i] * 6.28f))
        drawCircle(Color.White.copy(alpha = a), 1f + stars.size[i] * 2.2f, Offset(stars.x[i] * size.width, stars.phase[i] * size.height * 0.7f))
    }
    val c = Offset(size.width * at.x, size.height * at.y)
    val r = size.minDimension * 0.12f
    drawCircle(Brush.radialGradient(listOf(Color(0xFFB3C7FF).copy(alpha = 0.3f), Color.Transparent), c, r * 2.6f), r * 2.6f, c)
    drawCircle(Color(0xFFE6ECF8), r, c)
    drawCircle(Color(0xFF0A1730), r * 0.9f, Offset(c.x + r * 0.45f, c.y - r * 0.25f))
}

/** Capas de nubes que se desplazan a distinta velocidad (parallax). */
private fun DrawScope.clouds(t: Float, layers: Int, dim: Boolean) {
    for (layer in 0 until layers) {
        val depth = 1f - layer * 0.28f
        val speed = size.width * (0.012f + 0.02f * layer)
        val w = size.width * 0.28f * depth
        val y = size.height * (0.02f + 0.09f * layer)
        val alpha = (if (dim) 0.42f else 0.28f) + 0.1f * layer
        val base = if (dim) Color(0xFF8A97AA) else Color.White
        for (k in 0..2) {
            val span = size.width + w
            val x = ((k * span / 2.4f + t * speed + layer * 137f) % span) - w
            cloud(Offset(x, y + k * size.height * 0.03f), w, base.copy(alpha = alpha))
        }
    }
}

private fun DrawScope.cloud(o: Offset, w: Float, color: Color) {
    val h = w * 0.42f
    drawCircle(color, w * 0.18f, Offset(o.x + w * 0.28f, o.y + h * 0.55f))
    drawCircle(color, w * 0.24f, Offset(o.x + w * 0.52f, o.y + h * 0.38f))
    drawCircle(color, w * 0.16f, Offset(o.x + w * 0.76f, o.y + h * 0.6f))
    drawRoundRect(color, Offset(o.x + w * 0.1f, o.y + h * 0.55f), Size(w * 0.82f, h * 0.45f), CornerRadius(h * 0.22f))
}

/** Gotas inclinadas cayendo. */
private fun DrawScope.rain(t: Float, p: Particles, count: Int) {
    val len = size.height * 0.07f
    val slant = len * 0.35f
    val color = Color(0xFFAFD8FF).copy(alpha = 0.6f)
    for (i in 0 until count.coerceAtMost(p.n)) {
        val v = 0.9f + p.speed[i] * 0.8f
        val y = ((p.phase[i] + t * v) % 1.2f) * size.height - len
        val x = p.x[i] * (size.width + slant * 6) - slant * 3 - (y / size.height) * slant * 3
        drawLine(color, Offset(x, y), Offset(x - slant, y + len * (0.6f + p.size[i] * 0.6f)), 1.6f + p.size[i] * 1.4f, StrokeCap.Round)
    }
}

/** Copos cayendo con vaivén. */
private fun DrawScope.snow(t: Float, p: Particles, count: Int) {
    for (i in 0 until count.coerceAtMost(p.n)) {
        val v = 0.08f + p.speed[i] * 0.12f
        val y = ((p.phase[i] + t * v) % 1.1f) * size.height - 10f
        val x = p.x[i] * size.width + sin(t * (0.8f + p.speed[i]) + p.phase[i] * 6.28f) * size.width * 0.02f
        drawCircle(Color.White.copy(alpha = 0.85f), 2f + p.size[i] * 4f, Offset(x, y))
    }
}

/** Relámpago ocasional: destello de fondo y un rayo en zigzag. */
private fun DrawScope.lightning(t: Float) {
    val period = 5.5f
    val local = t % period
    if (local > 0.35f) return
    val flash = if (local < 0.08f || (local > 0.16f && local < 0.24f)) 0.55f else 0.15f
    drawRect(Color.White.copy(alpha = flash * 0.5f))
    val cycle = (t / period).toInt()
    val x0 = size.width * (0.25f + 0.5f * ((cycle * 37) % 10) / 10f)
    val bolt = Path().apply {
        moveTo(x0, size.height * 0.25f)
        lineTo(x0 - size.width * 0.03f, size.height * 0.45f); lineTo(x0 + size.width * 0.01f, size.height * 0.46f)
        lineTo(x0 - size.width * 0.04f, size.height * 0.72f)
    }
    drawPath(bolt, Color(0xFFFFF6C2).copy(alpha = flash + 0.3f), style = androidx.compose.ui.graphics.drawscope.Stroke(5f, cap = StrokeCap.Round))
}

/** Bancos de niebla translúcidos que se desplazan. */
private fun DrawScope.fog(t: Float) {
    for (i in 0 until 5) {
        val y = size.height * (0.25f + i * 0.14f)
        val w = size.width * 1.4f
        val x = ((t * size.width * (0.015f + i * 0.006f) + i * 211f) % w) - w * 0.4f
        drawRoundRect(
            Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.14f), Color.Transparent), x, x + w * 0.8f),
            Offset(x, y), Size(w * 0.8f, size.height * 0.07f), CornerRadius(size.height * 0.035f),
        )
    }
}

