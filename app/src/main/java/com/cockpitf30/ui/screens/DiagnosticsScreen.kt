package com.cockpitf30.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cockpitf30.service.VehicleService
import com.cockpitf30.system.AppSettings
import com.cockpitf30.system.BatteryOptimization
import com.cockpitf30.system.SourceMode
import com.cockpitf30.ui.components.FocusTile
import com.cockpitf30.ui.components.InitialFocus
import com.cockpitf30.ui.components.TopBar
import com.cockpitf30.ui.theme.Cockpit
import com.cockpitf30.vehicle.VehicleRepository
import com.cockpitf30.vehicle.source.CarPropertySource
import com.cockpitf30.vehicle.source.SerialSource

/**
 * Herramientas para descubrir cómo habla esta placa: elegir fuente y puerto, ver tramas
 * en crudo y ver qué eventos manda el iDrive.
 */
@Composable
fun DiagnosticsScreen() {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }
    val status by VehicleRepository.status.collectAsStateWithLifecycle()
    val raw by VehicleRepository.rawLog.collectAsStateWithLifecycle()
    val input by VehicleRepository.inputLog.collectAsStateWithLifecycle()

    var mode by remember { mutableStateOf(settings.sourceMode) }
    var path by remember { mutableStateOf(settings.serialPath) }
    var baud by remember { mutableIntStateOf(settings.baudRate) }
    var batteryOk by remember { mutableStateOf(BatteryOptimization.isIgnoring(context)) }
    val ports = remember { SerialSource.candidatePorts() }
    val automotive = remember { CarPropertySource.isAvailable(context) }

    fun apply() {
        settings.sourceMode = mode
        settings.serialPath = path
        settings.baudRate = baud
        VehicleService.restart(context)
    }

    // Al volver del diálogo de batería refrescamos el estado.
    LifecycleResumeEffect(Unit) {
        batteryOk = BatteryOptimization.isIgnoring(context)
        onPauseOrDispose {}
    }

    val first = remember { FocusRequester() }
    InitialFocus(first)

    Column(Modifier.fillMaxSize()) {
        TopBar("Diagnóstico")
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Section("Estado")
                Info("Fuente: ${status.sourceName} · ${if (status.connected) "conectada" else "desconectada"}")
                Info("Tramas: ${status.framesReceived} · checksum erróneo: ${status.badChecksums}")
                status.lastError?.let { Info("Último error: $it", Cockpit.Alert) }
                Info("Android Automotive (CarPropertyManager): ${if (automotive) "sí" else "no"}")
            }
            item {
                Section("Fuente de datos")
                ChipRow(SourceMode.entries.map { it.label }, SourceMode.entries.indexOf(mode), Modifier.focusRequester(first)) {
                    mode = SourceMode.entries[it]
                    apply()
                }
            }
            item {
                Section("Puerto serie")
                if (ports.isEmpty()) Info("No se encontró ningún /dev/ttyMT* ni /dev/ttyS*")
                ChipRow(
                    ports.map { "${it.name} ${if (it.canRead()) "r" else "-"}${if (it.canWrite()) "w" else "-"}" },
                    ports.indexOfFirst { it.absolutePath == path },
                ) {
                    path = ports[it].absolutePath
                    apply()
                }
                ChipRow(BAUDS.map { "$it" }, BAUDS.indexOf(baud)) {
                    baud = BAUDS[it]
                    apply()
                }
            }
            item {
                Section("Batería")
                ChipRow(
                    listOf(if (batteryOk) "Excluida de la optimización ✓" else "Pedir exclusión de optimización"),
                    if (batteryOk) 0 else -1,
                ) {
                    BatteryOptimization.request(context)
                }
            }
            item { Section("Tramas recibidas (más reciente arriba)") }
            if (raw.isEmpty()) item { Info("Ninguna todavía") }
            items(raw) { Mono(it) }
            item { Section("Eventos de entrada (gira el iDrive y pulsa botones)") }
            if (input.isEmpty()) item { Info("Ninguno todavía") }
            items(input) { Mono(it) }
        }
    }
}

private val BAUDS = listOf(19200, 38400, 57600, 115200)

@Composable
private fun Section(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = Cockpit.BlueLight,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun Info(text: String, color: androidx.compose.ui.graphics.Color = Cockpit.Text) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = color)
}

@Composable
private fun Mono(text: String) {
    Text(text, fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = Cockpit.TextDim)
}

@Composable
private fun ChipRow(labels: List<String>, selected: Int, firstModifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        labels.forEachIndexed { i, label ->
            FocusTile(onClick = { onSelect(i) }, selected = i == selected, modifier = if (i == 0) firstModifier else Modifier) {
                Text(label, Modifier.padding(horizontal = 20.dp, vertical = 12.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
