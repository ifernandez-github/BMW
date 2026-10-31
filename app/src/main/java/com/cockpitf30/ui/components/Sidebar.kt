package com.cockpitf30.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cockpitf30.ui.theme.Cockpit

class SideItem(
    val label: String,
    val icon: ImageVector,
    val active: Boolean,
    val onLongClick: (() -> Unit)? = null,
    val onClick: () -> Unit,
)

/**
 * Columna fija de accesos rápidos a la izquierda, la zona más cercana al conductor.
 * Cada objetivo mide al menos 80 dp para poder pulsarlo sin mirar.
 */
@Composable
fun Sidebar(items: List<SideItem>, modifier: Modifier = Modifier) {
    Column(
        modifier
            .width(96.dp)
            .fillMaxHeight()
            .background(Brush.horizontalGradient(listOf(Cockpit.Surface, Cockpit.Surface.copy(alpha = 0.6f))))
            // Desplazable: si la pantalla es más baja de lo previsto, nada queda fuera de alcance.
            .verticalScroll(rememberScrollState())
            .padding(vertical = 20.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MStripe(Modifier.padding(bottom = 12.dp), segment = 18.dp)
        items.forEach { item ->
            FocusTile(onClick = item.onClick, onLongClick = item.onLongClick, selected = item.active, modifier = Modifier.fillMaxWidth().height(88.dp)) { focused ->
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        item.icon, null, Modifier.size(32.dp),
                        tint = if (item.active || focused) Cockpit.Text else Cockpit.TextDim,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (item.active || focused) Cockpit.Text else Cockpit.TextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Marca del ítem activo.
                if (item.active) {
                    Box(
                        Modifier.align(Alignment.CenterStart).width(4.dp).height(40.dp)
                            .background(Cockpit.Blue, RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp)),
                    )
                }
            }
        }
    }
}
