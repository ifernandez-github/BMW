package com.cockpitf30.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Fondo de malla hexagonal como el de las imágenes del coche: azul marino profundo, hexágonos de
 * línea fina más visibles en el centro y algunos destellos. Se calcula una vez por tamaño.
 */
@Composable
fun HexBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    Box(modifier.hexBackground(), content = content)
}

/** El mismo fondo como modificador, para ponerlo detrás de cualquier pantalla. */
fun Modifier.hexBackground(): Modifier = drawWithCache {
    val r = 30.dp.toPx()
    val hexW = sqrt(3f) * r
    val grid = Path()
    var row = 0
    var y = -r
    while (y < size.height + r) {
        var x = if (row % 2 == 0) 0f else hexW / 2
        while (x < size.width + hexW) {
            for (i in 0..6) {
                val a = Math.toRadians((60.0 * i) - 30.0)
                val px = x + r * kotlin.math.cos(a).toFloat()
                val py = y + r * kotlin.math.sin(a).toFloat()
                if (i == 0) grid.moveTo(px, py) else grid.lineTo(px, py)
            }
            x += hexW
        }
        y += r * 1.5f
        row++
    }
    val rnd = Random(7)
    val sparks = List(26) { Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height) to (1.2f + rnd.nextFloat() * 2.2f) }
    val dashes = List(14) { Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height) to (8f + rnd.nextFloat() * 30f) }
    val center = Offset(size.width / 2, size.height / 2)
    val bg = Brush.radialGradient(listOf(Color(0xFF0E2340), Color(0xFF0A1A2E), Color(0xFF06101E)), center, size.maxDimension * 0.7f)
    // Más visible en el centro, se desvanece hacia los bordes.
    val lines = Brush.radialGradient(
        listOf(Color(0xFF4FA3FF).copy(alpha = 0.16f), Color(0xFF4FA3FF).copy(alpha = 0.07f), Color.Transparent),
        center, size.maxDimension * 0.62f,
    )
    onDrawBehind {
        drawRect(bg)
        drawPath(grid, lines, style = Stroke(1.dp.toPx()))
        sparks.forEach { (p, s) ->
            drawCircle(Color(0xFF8FD3FF).copy(alpha = 0.18f), s * 3.dp.toPx(), p)
            drawCircle(Color(0xFFCDEBFF).copy(alpha = 0.7f), s.dp.toPx(), p)
        }
        dashes.forEach { (p, len) ->
            drawLine(Color(0xFF8FD3FF).copy(alpha = 0.35f), p, Offset(p.x + len.dp.toPx(), p.y), 1.5.dp.toPx())
        }
    }
}
