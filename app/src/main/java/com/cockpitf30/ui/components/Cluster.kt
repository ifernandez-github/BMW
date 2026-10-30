package com.cockpitf30.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.cockpitf30.R
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cockpitf30.ui.theme.Cockpit
import com.cockpitf30.ui.theme.numeric
import com.cockpitf30.vehicle.SourceStatus
import com.cockpitf30.vehicle.VehicleState
import kotlin.math.abs
import kotlin.math.hypot

private val Cyan = Color(0xFF29B6F6)
private val SpeedLabel = Color(0xFFE6F0FF)
private val RpmGlow = Color(0xFFFF3B3B)
private val RpmLabel = Color(0xFFFFD2D2)
/** Fondo del panel, del mismo azul marino que la escena del coche para que se funda. */
private val SceneTop = Color(0xFF091A2D)

/** Marcas de velocidad. Escala no lineal: 0–150 ocupa más recorrido que 150–250, como en el iDrive. */
private val SPEED_MARKS = listOf(0, 30, 60, 90, 120, 150, 200, 250)
private fun speedFraction(kmh: Int): Float {
    val v = kmh.coerceIn(0, 250)
    return if (v <= 150) v / 150f * 0.625f else 0.625f + (v - 150) / 100f * 0.375f
}

private const val RPM_MAX = 8000
private const val RPM_RED = 6000

/**
 * Cuadro de instrumentos estilo iDrive 8: marcos en chevron cian (velocidad) y rojo (rpm) con las
 * cifras por fuera, panel central con la imagen del coche visto desde atrás y sus indicadores,
 * velocidad y marcha en pestañas oscuras, combustible y aceite, y una línea de estado debajo.
 * Lo que no llega se muestra "--".
 */
@Composable
fun ClusterStrip(state: VehicleState, status: SourceStatus, modifier: Modifier = Modifier) {
    val speed by animateFloatAsState(state.speedKmh?.let(::speedFraction) ?: 0f, tween(180), label = "speed")
    val rpm by animateFloatAsState(((state.rpm ?: 0).coerceIn(0, RPM_MAX)).toFloat() / RPM_MAX, tween(180), label = "rpm")
    val measurer = rememberTextMeasurer()
    val scaleStyle = numeric(30, FontWeight.SemiBold)
    val smallStyle = numeric(17, FontWeight.SemiBold)
    val captionStyle = numeric(17, FontWeight.Medium)
    val scene = ImageBitmap.imageResource(R.drawable.car_rear_scene)
    val overlay = Overlay(
        value = numeric(24, FontWeight.SemiBold),
        unit = numeric(14),
        caption = MaterialTheme.typography.labelMedium,
        alert = MaterialTheme.typography.labelLarge,
    )

    BoxWithConstraints(modifier) {
        val g = Geometry(maxWidth, maxHeight)
        Canvas(Modifier.fillMaxSize()) {
            val geo = g.px(this)
            drawPanel(geo, scene, state, measurer, overlay)
            drawFrame(geo, mirrored = false, fraction = speed, glow = Cyan, deep = Cockpit.Blue)
            drawFrame(geo, mirrored = true, fraction = rpm, glow = RpmGlow, deep = Cockpit.Alert)

            SPEED_MARKS.forEach { v -> drawMark(geo, measurer, "$v", speedFraction(v), false, scaleStyle, SpeedLabel) }
            (1..8).forEach { k ->
                drawMark(geo, measurer, "$k", k * 1000f / RPM_MAX, true, scaleStyle, if (k * 1000 >= RPM_RED) RpmGlow else RpmLabel)
            }
            drawCaption(geo, measurer, "km/h", mirrored = false, captionStyle)
            drawCaption(geo, measurer, "1/min x 1000", mirrored = true, captionStyle)

            drawMiniBar(geo, measurer, state.fuelLevel, listOf("1", "1/2", "0"), mirrored = false, smallStyle)
            val oil = state.oilTempC?.let { ((it - 50) / 100f).coerceIn(0f, 1f) }
            drawMiniBar(geo, measurer, oil, listOf("150", "100", "50"), mirrored = true, smallStyle)
        }

        // Velocidad y marcha en pestañas oscuras sobre el panel.
        Row(
            Modifier.fillMaxWidth().height(g.frameBottom).padding(top = g.frameTop + 40.dp, bottom = 110.dp)
                .padding(start = g.contentInset, end = g.contentInset),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabValue(state.speedKmh?.toString() ?: "--", "km/h", Modifier.weight(1f), alignEnd = false)
            Spacer(Modifier.weight(1.5f))
            TabValue(state.gear ?: "--", state.rpm?.let { "$it rpm" } ?: "-- rpm", Modifier.weight(1f), alignEnd = true)
        }
        StatusLine(
            state, status,
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(g.statusHeight)
                .padding(start = g.scaleWidth - 10.dp, end = g.scaleWidth - 10.dp, top = 8.dp),
        )
    }
}

/** Medidas del cuadro en dp, compartidas entre el dibujo y los textos. */
private class Geometry(val width: Dp, val height: Dp) {
    val frameTop = 30.dp
    val statusHeight = 50.dp
    val frameBottom = height - statusHeight
    val scaleWidth = 74.dp
    val band = 64.dp
    val cut = (frameBottom - frameTop) * 0.24f
    val contentInset = scaleWidth + cut + band + 20.dp

    fun px(scope: DrawScope) = with(scope) {
        GeoPx(
            w = size.width, y0 = frameTop.toPx(), y1 = frameBottom.toPx(),
            xv = (scaleWidth + 6.dp).toPx(), cut = cut.toPx(), band = band.toPx(), density = density,
        )
    }
}

private class GeoPx(val w: Float, val y0: Float, val y1: Float, val xv: Float, val cut: Float, val band: Float, val density: Float) {
    val ym = (y0 + y1) / 2
    val half = (y1 - y0) / 2
    fun dp(v: Float) = v * density

    /** x del chevron izquierdo desplazado [inset] hacia dentro, a la altura [y]. */
    fun chevronX(y: Float, inset: Float) = xv + inset + cut * abs(y - ym) / half
    fun mirror(x: Float, mirrored: Boolean) = if (mirrored) w - x else x
}

/** Polígono con las esquinas redondeadas. */
private fun roundedPolygon(points: List<Offset>, radius: Float) = Path().apply {
    val n = points.size
    for (i in 0 until n) {
        val prev = points[(i - 1 + n) % n]; val cur = points[i]; val next = points[(i + 1) % n]
        fun toward(from: Offset, to: Offset): Offset {
            val d = hypot(to.x - from.x, to.y - from.y)
            val r = minOf(radius, d / 2) / d
            return Offset(from.x + (to.x - from.x) * r, from.y + (to.y - from.y) * r)
        }
        val a = toward(cur, prev); val b = toward(cur, next)
        if (i == 0) moveTo(a.x, a.y) else lineTo(a.x, a.y)
        quadraticBezierTo(cur.x, cur.y, b.x, b.y)
    }
    close()
}

/** Estilos de los indicadores que van encima de la imagen del coche. */
private class Overlay(val value: TextStyle, val unit: TextStyle, val caption: TextStyle, val alert: TextStyle)

/**
 * Panel central: la escena del coche visto desde atrás, ajustada a la altura del panel y fundida
 * con una viñeta oscura hacia los chevrons. Encima, presiones de las ruedas y testigos de
 * puertas, maletero y capó abiertos.
 */
private fun DrawScope.drawPanel(g: GeoPx, scene: ImageBitmap, state: VehicleState, tm: TextMeasurer, st: Overlay) {
    val inset = g.band + g.dp(14f)
    val top = g.y0 + g.dp(16f)
    val bottom = g.y1 - g.dp(10f)
    val pts = listOf(
        Offset(g.chevronX(top, inset), top), Offset(g.w - g.chevronX(top, inset), top),
        Offset(g.w - g.xv - inset, g.ym), Offset(g.w - g.chevronX(bottom, inset), bottom),
        Offset(g.chevronX(bottom, inset), bottom), Offset(g.xv + inset, g.ym),
    )
    val panel = roundedPolygon(pts, g.dp(22f))
    val ph = bottom - top
    val iw = ph * scene.width / scene.height
    val il = g.w / 2 - iw / 2
    val ir = il + iw

    // Fondo liso del mismo tono que los bordes de la escena, para que la viñeta no deje corte.
    drawPath(panel, SceneTop)
    clipPath(panel) {
        drawImage(
            scene,
            dstOffset = IntOffset(il.roundToInt(), top.roundToInt()),
            dstSize = IntSize(iw.roundToInt(), ph.roundToInt()),
        )
        // Viñeta: funde los bordes de la imagen con el fondo del panel.
        val fade = iw * 0.26f
        drawRect(Brush.horizontalGradient(listOf(SceneTop, Color.Transparent), startX = il, endX = il + fade), Offset(il - 1f, top), Size(fade + 1f, ph))
        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, SceneTop), startX = ir - fade, endX = ir), Offset(ir - fade, top), Size(fade + 1f, ph))
        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent), startY = top, endY = top + ph * 0.18f), Offset(0f, top), Size(size.width, ph * 0.18f))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)), startY = bottom - ph * 0.15f, endY = bottom), Offset(0f, bottom - ph * 0.15f), Size(size.width, ph * 0.15f))

        // Puntos de la imagen en proporción (el coche ocupa x 0.31–0.70 de la escena).
        fun at(fx: Float, fy: Float) = Offset(il + iw * fx, top + ph * fy)
        val d = state.doors
        val red = Cockpit.Alert
        val line = g.dp(5f)
        if (d.trunk == true) drawRoundRect(red.copy(alpha = 0.35f), at(0.37f, 0.40f), Size(iw * 0.26f, ph * 0.24f), CornerRadius(g.dp(10f)))
        if (d.driver == true || d.rearLeft == true) drawLine(red, at(0.315f, 0.36f), at(0.315f, 0.78f), line)
        if (d.passenger == true || d.rearRight == true) drawLine(red, at(0.69f, 0.36f), at(0.69f, 0.78f), line)
        if (d.hood == true) drawLine(red, at(0.40f, 0.05f), at(0.60f, 0.05f), line)

        // Rótulos de lo que está abierto, arriba en el centro.
        val open = buildList {
            if (d.driver == true) add("PUERTA CONDUCTOR")
            if (d.passenger == true) add("PUERTA ACOMPAÑANTE")
            if (d.rearLeft == true) add("PUERTA TRASERA IZQ.")
            if (d.rearRight == true) add("PUERTA TRASERA DER.")
            if (d.trunk == true) add("MALETERO ABIERTO")
            if (d.hood == true) add("CAPÓ ABIERTO")
        }
        var y = top + g.dp(14f)
        open.forEach { text ->
            val l = tm.measure(text, st.alert)
            val pad = g.dp(10f)
            drawRoundRect(Color.Black.copy(alpha = 0.6f), Offset(g.w / 2 - l.size.width / 2f - pad, y - g.dp(3f)), Size(l.size.width + pad * 2, l.size.height + g.dp(6f)), CornerRadius(g.dp(6f)))
            drawText(l, color = red, topLeft = Offset(g.w / 2 - l.size.width / 2f, y))
            y += l.size.height + g.dp(12f)
        }

        // Presión de las ruedas: delanteras arriba, traseras junto a las ruedas.
        val t = state.tyres
        tyre(tm, st, "DEL. IZQ.", t.frontLeft, at(0.27f, 0.30f), alignRight = true)
        tyre(tm, st, "DEL. DER.", t.frontRight, at(0.73f, 0.30f), alignRight = false)
        tyre(tm, st, "TRAS. IZQ.", t.rearLeft, at(0.27f, 0.70f), alignRight = true)
        tyre(tm, st, "TRAS. DER.", t.rearRight, at(0.73f, 0.70f), alignRight = false)
    }
    drawPath(panel, Color.White.copy(alpha = 0.10f), style = Stroke(g.dp(1.2f)))
}

/** Presión de una rueda: rótulo pequeño y valor en Saira; "--" si no hay dato. */
private fun DrawScope.tyre(tm: TextMeasurer, st: Overlay, caption: String, bar: Float?, at: Offset, alignRight: Boolean) {
    val c = tm.measure(caption, st.caption)
    val v = tm.measure(bar?.let { "%.1f".format(it) } ?: "--", st.value)
    val u = tm.measure(" bar", st.unit)
    val width = maxOf(c.size.width, v.size.width + u.size.width)
    val x = if (alignRight) at.x - width else at.x
    drawRoundRect(Color.Black.copy(alpha = 0.35f), Offset(x - 10.dp.toPx(), at.y - 6.dp.toPx()), Size(width + 20.dp.toPx(), c.size.height + v.size.height + 10.dp.toPx()), CornerRadius(8.dp.toPx()))
    drawText(c, color = Color.White.copy(alpha = 0.65f), topLeft = Offset(x, at.y))
    drawText(v, color = Color.White, topLeft = Offset(x, at.y + c.size.height))
    drawText(u, color = Color.White.copy(alpha = 0.65f), topLeft = Offset(x + v.size.width, at.y + c.size.height + v.size.height - u.size.height - 3.dp.toPx()))
}

/** Marco en chevron: línea exterior fina y brillante, línea interior gruesa y franja entre ambas con el relleno del valor. */
private fun DrawScope.drawFrame(g: GeoPx, mirrored: Boolean, fraction: Float, glow: Color, deep: Color) {
    fun o(x: Float, y: Float) = Offset(g.mirror(x, mirrored), y)
    fun chevron(inset: Float, top: Float, bottom: Float) = Path().apply {
        val a = o(g.chevronX(bottom, inset), bottom); val v = o(g.xv + inset, g.ym); val b = o(g.chevronX(top, inset), top)
        moveTo(a.x, a.y); lineTo(v.x, v.y); lineTo(b.x, b.y)
    }

    /** Franja entre el borde exterior y el interior, de la altura [yTop] a [yBottom]. */
    fun band(yTop: Float, yBottom: Float) = Path().apply {
        val outer = mutableListOf(o(g.chevronX(yBottom, 0f), yBottom))
        if (yTop < g.ym && yBottom > g.ym) outer += o(g.xv, g.ym)
        outer += o(g.chevronX(yTop, 0f), yTop)
        val inner = mutableListOf(o(g.chevronX(yTop, g.band), yTop))
        if (yTop < g.ym && yBottom > g.ym) inner += o(g.xv + g.band, g.ym)
        inner += o(g.chevronX(yBottom, g.band), yBottom)
        (outer + inner).forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
        close()
    }

    // Líneas horizontales arriba y abajo que se pierden hacia el centro.
    val x0 = g.chevronX(g.y0, 0f)
    val topEnd = x0 + g.w * 0.17f
    val bottomEnd = g.w * 0.5f - g.dp(40f)
    val fade = if (mirrored) listOf(Color.Transparent, glow) else listOf(glow, Color.Transparent)
    drawLine(Brush.horizontalGradient(fade, o(x0, 0f).x, o(topEnd, 0f).x), o(x0, g.y0), o(topEnd, g.y0), g.dp(2f))
    drawLine(Brush.horizontalGradient(fade, o(x0, 0f).x, o(bottomEnd, 0f).x), o(g.chevronX(g.y1, 0f), g.y1), o(bottomEnd, g.y1), g.dp(3f))

    // Franja de fondo con rayado diagonal.
    val full = band(g.y0, g.y1)
    drawPath(full, Color(0xFF151A24))
    clipPath(full) {
        var y = g.y0 - g.band
        while (y < g.y1 + g.band) {
            drawLine(Color.White.copy(alpha = 0.05f), o(g.xv - g.dp(10f), y + g.band), o(g.xv + g.cut + g.band, y), g.dp(2f))
            y += g.dp(9f)
        }
    }
    // Relleno del valor, de abajo arriba.
    if (fraction > 0f) {
        val level = g.y1 - (g.y1 - g.y0) * fraction
        // Degradado perpendicular al borde: color pleno fuera y transparente hacia el centro.
        clipPath(band(level, g.y1)) {
            val steps = 20
            val step = g.band / steps
            for (i in 0 until steps) {
                val a = 1f - i / steps.toFloat()
                val color = if (i < steps / 3) glow else deep
                drawPath(chevron(step * (i + 0.5f), g.y0 - g.band, g.y1 + g.band), color.copy(alpha = a * a), style = Stroke(step + 1f))
            }
        }
        // Borde brillante en el nivel actual.
        drawLine(glow, o(g.chevronX(level, 0f), level), o(g.chevronX(level, g.band * 0.7f), level), g.dp(3f))
    }
    // Línea exterior brillante e interior gruesa.
    val outer = chevron(0f, g.y0, g.y1)
    drawPath(outer, glow.copy(alpha = 0.3f), style = Stroke(g.dp(9f)))
    drawPath(outer, glow, style = Stroke(g.dp(2.5f)))
    drawPath(chevron(g.band, g.y0, g.y1), deep.copy(alpha = 0.5f), style = Stroke(g.dp(2f)))
}

/** Cifra de escala por fuera del borde, a la altura correspondiente a [fraction]. */
private fun DrawScope.drawMark(g: GeoPx, tm: TextMeasurer, text: String, fraction: Float, mirrored: Boolean, style: TextStyle, color: Color) {
    val y = g.y1 - (g.y1 - g.y0) * fraction
    val x = g.chevronX(y, 0f)
    val label = tm.measure(text, style)
    val right = x - g.dp(14f)
    val left = if (mirrored) g.w - right else right - label.size.width
    drawText(label, color = color, topLeft = Offset(left, y - label.size.height / 2f))
}

private fun DrawScope.drawCaption(g: GeoPx, tm: TextMeasurer, text: String, mirrored: Boolean, style: TextStyle) {
    val label = tm.measure(text, style)
    val x = g.chevronX(g.y0, 0f) + g.w * 0.17f - label.size.width
    val left = if (mirrored) g.w - x - label.size.width else x
    drawText(label, color = Color.White.copy(alpha = 0.85f), topLeft = Offset(left, g.y0 - label.size.height - g.dp(2f)))
}

/** Barra inclinada de segmentos blancos (combustible o aceite) junto al tramo inferior del chevron. */
private fun DrawScope.drawMiniBar(g: GeoPx, tm: TextMeasurer, fraction: Float?, labels: List<String>, mirrored: Boolean, style: TextStyle) {
    val inset = g.band + g.dp(34f)
    val yTop = g.ym + g.dp(26f)
    val yBottom = g.y1 - g.dp(22f)
    val blocks = 6
    val bh = (yBottom - yTop) / blocks
    val bw = g.dp(10f)
    for (i in 0 until blocks) {
        val yb = yBottom - i * bh
        val lit = fraction != null && fraction * blocks >= i + 0.5f
        val x1 = g.chevronX(yb, inset); val x2 = g.chevronX(yb - bh + g.dp(3f), inset)
        val seg = Path().apply {
            moveTo(g.mirror(x1, mirrored), yb); lineTo(g.mirror(x1 + bw, mirrored), yb)
            lineTo(g.mirror(x2 + bw, mirrored), yb - bh + g.dp(3f)); lineTo(g.mirror(x2, mirrored), yb - bh + g.dp(3f)); close()
        }
        drawPath(seg, if (lit) Color.White else Color.White.copy(alpha = 0.18f))
    }
    labels.forEachIndexed { i, text ->
        val y = yTop + (yBottom - yTop) * i / (labels.size - 1)
        val label = tm.measure(text, style)
        val x = g.chevronX(y, inset) + bw + g.dp(10f)
        val left = if (mirrored) g.w - x - label.size.width else x
        drawText(label, color = Color.White, topLeft = Offset(left, y - label.size.height / 2f))
    }
}

@Composable
private fun TabValue(value: String, caption: String, modifier: Modifier = Modifier, alignEnd: Boolean) {
    Column(modifier, horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        // Cifra directamente sobre la imagen, con sombra suave para que se lea sobre el azul.
        Text(
            value,
            style = numeric(80, FontWeight.Bold).copy(shadow = Shadow(Color.Black.copy(alpha = 0.7f), Offset(0f, 3f), 18f)),
            color = Cockpit.Text,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Text(
            caption,
            style = numeric(20, FontWeight.Medium),
            color = Cockpit.BlueLight,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

/**
 * Línea inferior: franjas M y hora, surtidor y autonomía a la izquierda; segmentos azules en el
 * centro (coche en rojo si hay una puerta abierta); estado y temperatura exterior a la derecha.
 */
@Composable
private fun StatusLine(state: VehicleState, status: SourceStatus, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        MStripe(segment = 14.dp)
        Spacer(Modifier.width(18.dp))
        ClockText(style = numeric(28, FontWeight.SemiBold))
        Spacer(Modifier.width(36.dp))
        FuelPump(Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(state.rangeKm?.let { "$it" } ?: "--", style = numeric(28, FontWeight.SemiBold), color = Cockpit.Text)
        Text(" km", style = numeric(18), color = Cockpit.TextDim)
        Spacer(Modifier.weight(1f))
        if (state.doors.anyOpen) {
            CarIcon(state, Modifier.size(width = 60.dp, height = 26.dp))
            Spacer(Modifier.width(16.dp))
        }
        Segments(Modifier.size(width = 240.dp, height = 22.dp))
        Spacer(Modifier.weight(1f))
        Text(
            if (status.connected) "READY" else "SIN DATOS",
            style = MaterialTheme.typography.labelLarge,
            color = if (status.connected) Cockpit.Text else Cockpit.Alert,
        )
        Spacer(Modifier.width(32.dp))
        Text(
            state.outsideTempC?.let { "%+.1f".format(it) } ?: "--",
            style = numeric(28, FontWeight.SemiBold),
            color = Cockpit.Text,
        )
        Text(" °C", style = numeric(18), color = Cockpit.TextDim)
    }
}

/** Segmentos azules decorativos del centro de la línea de estado. */
@Composable
private fun Segments(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val n = 4
        val gap = 6.dp.toPx()
        val sw = (size.width - gap * (n - 1)) / n
        val s = size.height * 0.5f
        for (i in 0 until n) {
            val x = i * (sw + gap)
            val p = Path().apply {
                moveTo(x + s, 0f); lineTo(x + sw, 0f); lineTo(x + sw - s, size.height); lineTo(x, size.height); close()
            }
            drawPath(p, Brush.verticalGradient(listOf(Cockpit.Blue.copy(alpha = 0.9f), Cockpit.MDark)))
        }
    }
}

/** Icono de surtidor dibujado con formas simples. */
@Composable
private fun FuelPump(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawRoundRect(Cockpit.TextDim, Offset(w * 0.1f, h * 0.1f), Size(w * 0.5f, h * 0.85f), CornerRadius(w * 0.08f))
        drawRect(Cockpit.Background, Offset(w * 0.2f, h * 0.2f), Size(w * 0.3f, h * 0.22f))
        drawLine(Cockpit.TextDim, Offset(w * 0.6f, h * 0.3f), Offset(w * 0.85f, h * 0.45f), w * 0.08f)
        drawLine(Cockpit.TextDim, Offset(w * 0.85f, h * 0.45f), Offset(w * 0.85f, h * 0.8f), w * 0.08f)
    }
}

/** Coche visto desde arriba (morro a la derecha); puertas abiertas en rojo. */
@Composable
private fun CarIcon(state: VehicleState, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val body = Size(size.width, size.height * 0.8f)
        val origin = Offset(0f, (size.height - body.height) / 2)
        drawRoundRect(Cockpit.SurfaceHigh, origin, body, CornerRadius(body.height / 2.5f))
        drawRoundRect(Cockpit.TextDim, origin, body, CornerRadius(body.height / 2.5f), style = Stroke(1.5.dp.toPx()))
        fun door(open: Boolean?, x: Float, top: Boolean) {
            if (open != true) return
            val y = if (top) origin.y else origin.y + body.height
            drawLine(Cockpit.Alert, Offset(x, y), Offset(x + size.width * 0.18f, y), 3.dp.toPx())
        }
        door(state.doors.driver, size.width * 0.5f, top = true)
        door(state.doors.passenger, size.width * 0.5f, top = false)
        door(state.doors.rearLeft, size.width * 0.28f, top = true)
        door(state.doors.rearRight, size.width * 0.28f, top = false)
        if (state.doors.trunk == true) drawLine(Cockpit.Alert, Offset(0f, origin.y + 4.dp.toPx()), Offset(0f, origin.y + body.height - 4.dp.toPx()), 3.dp.toPx())
        if (state.doors.hood == true) drawLine(Cockpit.Alert, Offset(size.width, origin.y + 4.dp.toPx()), Offset(size.width, origin.y + body.height - 4.dp.toPx()), 3.dp.toPx())
    }
}
