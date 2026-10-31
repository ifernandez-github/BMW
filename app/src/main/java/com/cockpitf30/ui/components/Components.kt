package com.cockpitf30.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.cockpitf30.ui.theme.Cockpit
import com.cockpitf30.ui.theme.numeric
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay



/** Tres franjas M como acento. */
@Composable
fun MStripe(modifier: Modifier = Modifier, segment: Dp = 28.dp) {
    Row(modifier.height(3.dp)) {
        listOf(Cockpit.MLight, Cockpit.MDark, Cockpit.MRed).forEach {
            Box(Modifier.width(segment).height(3.dp).background(it))
        }
    }
}

/**
 * Elemento enfocable con el resaltado azul del iDrive. Es la base de toda la navegación
 * con ruleta: cada cosa que se pueda pulsar debe ir dentro de uno.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FocusTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.03f else 1f, label = "scale")
    val border by animateColorAsState(
        when {
            focused -> Cockpit.BlueLight
            selected -> Cockpit.Blue
            else -> Cockpit.GlassBorder
        },
        label = "border",
    )
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .scale(scale)
            // Brillo azul alrededor del elemento enfocado.
            .shadow(if (focused) 24.dp else 0.dp, shape, ambientColor = Cockpit.Blue, spotColor = Cockpit.BlueLight)
            .clip(shape)
            // Base opaca al enfocar para que el brillo no se transparente dentro de la tarjeta.
            .background(if (focused) Cockpit.Background else Cockpit.Background.copy(alpha = 0.55f))
            .background(
                if (focused) Brush.verticalGradient(listOf(Cockpit.Blue.copy(alpha = 0.38f), Cockpit.Blue.copy(alpha = 0.12f)))
                else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.03f))),
            )
            .border(if (focused || selected) 2.dp else 1.dp, border, shape)
            .onFocusChanged { focused = it.isFocused }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        content(focused)
    }
}

/**
 * Da el foco inicial a [requester]. Dentro de AnimatedContent o de listas perezosas el nodo
 * puede no existir aún en el primer frame, así que reintentamos un momento.
 */
@Composable
fun InitialFocus(requester: FocusRequester) {
    LaunchedEffect(requester) {
        repeat(10) {
            if (runCatching { requester.requestFocus() }.isSuccess) return@LaunchedEffect
            delay(50)
        }
    }
}

@Composable
fun ClockText(modifier: Modifier = Modifier, style: TextStyle = numeric(26, FontWeight.Medium)) {
    val now by produceState(LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(1_000)
        }
    }
    Text(
        now.format(DateTimeFormatter.ofPattern("HH:mm")),
        modifier = modifier,
        style = style,
        color = Cockpit.Text,
    )
}

/** Barra superior común: título, franja M y reloj. */
@Composable
fun TopBar(title: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = Cockpit.TextDim)
            Spacer(Modifier.height(6.dp))
            MStripe()
        }
        Spacer(Modifier.weight(1f))
        trailing()
        ClockText()
    }
}
