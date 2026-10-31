package com.cockpitf30.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.random.Random

/*
 * Dibujos "realistas" del tiempo, sin imágenes ni RenderEffect (no existe antes de Android 12):
 * el volumen y el desenfoque se simulan con degradados radiales superpuestos.
 */

/**
 * Cúmulo esponjoso: muchas bolas con degradado radial blanco que se desvanece en el borde,
 * sombra azulada en la base y brillo arriba. [width] es el ancho total de la nube.
 */
fun DrawScope.fluffyCloud(center: Offset, width: Float, seed: Int, dark: Boolean = false) {
    val r = Random(seed)
    val count = 16
    val top = if (dark) Color(0xFFB7C0CC) else Color.White
    val mid = if (dark) Color(0xFF8D98A8) else Color(0xFFF2F5FA)
    val shade = if (dark) Color(0xFF4E5868) else Color(0xFFB4C2D6)
    // Posiciones de las bolas: forma de cúpula, más altas en el centro.
    val puffs = List(count) {
        val fx = (r.nextFloat() - 0.5f) * 0.78f
        val dome = 1f - (abs(fx) / 0.39f)
        val px = center.x + fx * width
        val py = center.y - dome * width * (0.16f + r.nextFloat() * 0.1f) + r.nextFloat() * width * 0.05f
        val pr = width * (0.1f + dome * 0.1f + r.nextFloat() * 0.06f)
        Triple(px, py, pr)
    }
    // Sombra difusa bajo la nube.
    drawOval(
        Brush.radialGradient(listOf(shade.copy(alpha = 0.35f), Color.Transparent), Offset(center.x, center.y + width * 0.08f), width * 0.5f),
        Offset(center.x - width * 0.5f, center.y - width * 0.06f), Size(width, width * 0.28f),
    )
    // Cuerpo: primero la parte sombreada (desplazada abajo) y encima la iluminada.
    puffs.forEach { (x, y, pr) ->
        drawCircle(Brush.radialGradient(0f to shade.copy(alpha = 0.9f), 0.7f to shade.copy(alpha = 0.6f), 1f to Color.Transparent, center = Offset(x, y + pr * 0.25f), radius = pr * 1.05f), pr * 1.05f, Offset(x, y + pr * 0.25f))
    }
    puffs.forEach { (x, y, pr) ->
        val c = Offset(x - pr * 0.18f, y - pr * 0.22f)
        drawCircle(Brush.radialGradient(0f to top, 0.55f to mid.copy(alpha = 0.95f), 1f to mid.copy(alpha = 0f), center = c, radius = pr), pr, Offset(x, y))
    }
    // Base plana y algo más gris, típica del cúmulo.
    drawOval(
        Brush.verticalGradient(listOf(mid.copy(alpha = 0.0f), shade.copy(alpha = 0.55f)), startY = center.y - width * 0.02f, endY = center.y + width * 0.1f),
        Offset(center.x - width * 0.38f, center.y - width * 0.05f), Size(width * 0.76f, width * 0.16f),
    )
}

/** Pre-renderiza un cúmulo en un bitmap para desplazarlo luego sin coste. */
fun renderCloud(widthPx: Int, seed: Int, dark: Boolean, density: Density): ImageBitmap {
    val w = widthPx.coerceAtLeast(16)
    val h = (w * 0.62f).toInt()
    val bitmap = ImageBitmap(w, h)
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(bitmap), Size(w.toFloat(), h.toFloat())) {
        fluffyCloud(Offset(w / 2f, h * 0.62f), w * 0.86f, seed, dark)
    }
    return bitmap
}

/** Copo de nieve desenfocado (degradado radial), pre-renderizado. */
fun renderFlake(sizePx: Int, density: Density): ImageBitmap {
    val s = sizePx.coerceAtLeast(4)
    val bitmap = ImageBitmap(s, s)
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(bitmap), Size(s.toFloat(), s.toFloat())) {
        drawCircle(Brush.radialGradient(0f to Color.White, 0.45f to Color.White.copy(alpha = 0.8f), 1f to Color.Transparent), s / 2f)
    }
    return bitmap
}

/**
 * Sol volumétrico: halo grande y suave, rayos muy tenues que giran despacio, esfera con degradado
 * de amarillo claro a naranja y un pequeño destello de lente. [t] en segundos (0 para estático).
 */
fun DrawScope.glowSun(c: Offset, r: Float, t: Float = 0f, flare: Boolean = true) {
    val pulse = 1f + 0.04f * kotlin.math.sin(t * 1.3f)
    val halo = r * 4.2f * pulse
    drawCircle(
        Brush.radialGradient(0f to Color(0xFFFFF3B0).copy(alpha = 0.6f), 0.25f to Color(0xFFFFD54F).copy(alpha = 0.32f), 1f to Color.Transparent, center = c, radius = halo),
        halo, c,
    )
    rotate(t * 3f, c) {
        for (i in 0 until 14) {
            rotate(i * (360f / 14), c) {
                val ray = Path().apply {
                    moveTo(c.x - r * 0.08f, c.y - r * 1.05f)
                    lineTo(c.x, c.y - r * (2.6f + (i % 3) * 0.5f))
                    lineTo(c.x + r * 0.08f, c.y - r * 1.05f)
                    close()
                }
                drawPath(ray, Brush.verticalGradient(listOf(Color(0xFFFFF2C0).copy(alpha = 0f), Color(0xFFFFF2C0).copy(alpha = 0.22f)), startY = c.y - r * 3f, endY = c.y - r))
            }
        }
    }
    drawCircle(
        Brush.radialGradient(0f to Color(0xFFFFF6B0), 0.45f to Color(0xFFFFE066), 0.8f to Color(0xFFFFC21A), 1f to Color(0xFFFF8A00), center = Offset(c.x - r * 0.3f, c.y - r * 0.3f), radius = r * 1.35f),
        r, c,
    )
    drawCircle(Color(0xFFFFF8D6).copy(alpha = 0.35f), r * 1.04f, c, style = Stroke(r * 0.06f))
    if (flare) {
        // Destellos de lente hacia abajo a la izquierda.
        listOf(1.8f to 0.18f, 2.6f to 0.1f, 3.3f to 0.26f).forEach { (d, s) ->
            val p = Offset(c.x - r * d, c.y + r * d * 0.55f)
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE9A0).copy(alpha = 0.22f), Color.Transparent), p, r * s * 2f), r * s * 2f, p)
        }
    }
}

/** Luna con cráteres suaves y halo frío. */
fun DrawScope.softMoon(c: Offset, r: Float) {
    drawCircle(Brush.radialGradient(listOf(Color(0xFFBFD3FF).copy(alpha = 0.35f), Color.Transparent), c, r * 3f), r * 3f, c)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFF7F9FF), Color(0xFFD5DCEA), Color(0xFFA9B4C8)), Offset(c.x - r * 0.3f, c.y - r * 0.3f), r * 1.4f), r, c)
    listOf(Triple(0.3f, -0.2f, 0.22f), Triple(-0.25f, 0.3f, 0.16f), Triple(-0.1f, -0.4f, 0.1f), Triple(0.35f, 0.4f, 0.12f)).forEach { (dx, dy, s) ->
        val p = Offset(c.x + dx * r, c.y + dy * r)
        drawCircle(Brush.radialGradient(listOf(Color(0xFF8E99AE).copy(alpha = 0.45f), Color.Transparent), p, r * s), r * s, p)
    }
}

/** Rayo ramificado a partir de una semilla (cambia en cada descarga). */
fun lightningPath(start: Offset, length: Float, seed: Int, into: Path) {
    into.reset()
    val r = Random(seed)
    fun branch(from: Offset, len: Float, depth: Int) {
        var p = from
        into.moveTo(p.x, p.y)
        val steps = 7
        for (i in 0 until steps) {
            val next = Offset(p.x + (r.nextFloat() - 0.5f) * len * 0.22f, p.y + len / steps)
            into.lineTo(next.x, next.y)
            if (depth < 2 && r.nextFloat() < 0.28f) {
                branch(next, len * 0.45f, depth + 1)
                into.moveTo(next.x, next.y)
            }
            p = next
        }
    }
    branch(start, length, 0)
}

/** Velo azulado sobre lo ya dibujado (para iluminar las nubes con un relámpago, por ejemplo). */
fun DrawScope.tint(color: Color) = drawRect(color, blendMode = BlendMode.SrcAtop)
