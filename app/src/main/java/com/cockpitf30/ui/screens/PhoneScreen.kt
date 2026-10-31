package com.cockpitf30.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.cockpitf30.ui.components.FocusTile
import com.cockpitf30.ui.components.InitialFocus
import com.cockpitf30.ui.components.TopBar
import com.cockpitf30.ui.theme.Cockpit
import com.cockpitf30.ui.theme.numeric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val KEYS = listOf(
    "1" to "", "2" to "ABC", "3" to "DEF",
    "4" to "GHI", "5" to "JKL", "6" to "MNO",
    "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
    "*" to "", "0" to "+", "#" to "",
)
private val CallGreen = Color(0xFF1DB954)

private class Contact(val name: String, val number: String)

/**
 * Marcador propio con letra grande para el coche: número en Saira, teclado de teclas grandes,
 * botón de llamar y contactos del teléfono. Todo manejable con la ruleta.
 */
@Composable
fun PhoneScreen() {
    val context = LocalContext.current
    var number by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var hasContacts by remember { mutableStateOf(granted(context, Manifest.permission.READ_CONTACTS)) }

    val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasContacts = it }
    val callPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { call(context, number) }
    LaunchedEffect(hasContacts) { if (hasContacts) contacts = loadContacts(context) }

    fun dial() {
        if (number.isBlank()) return
        if (granted(context, Manifest.permission.CALL_PHONE)) call(context, number) else callPermission.launch(Manifest.permission.CALL_PHONE)
    }

    val first = remember { FocusRequester() }
    InitialFocus(first)

    Column(Modifier.fillMaxSize()) {
        TopBar("Teléfono")
        Row(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            // Número y teclado
            Column(Modifier.width(560.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth().height(84.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        number.ifEmpty { "Introduce un número" },
                        style = if (number.isEmpty()) MaterialTheme.typography.headlineSmall.copy(fontSize = 30.sp) else numeric(52, FontWeight.Medium),
                        color = if (number.isEmpty()) Cockpit.TextDim else Cockpit.Text,
                        maxLines = 1, overflow = TextOverflow.Clip,
                        modifier = Modifier.weight(1f),
                    )
                    FocusTile(onClick = { number = number.dropLast(1) }, modifier = Modifier.size(width = 104.dp, height = 80.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Borrar", Modifier.align(Alignment.Center).size(40.dp), tint = Cockpit.Text)
                    }
                }
                KEYS.chunked(3).forEachIndexed { r, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEachIndexed { c, (digit, letters) ->
                            val mod = Modifier.weight(1f).height(100.dp)
                            FocusTile(onClick = { number += digit }, modifier = if (r == 0 && c == 0) mod.focusRequester(first) else mod) {
                                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(digit, style = numeric(50, FontWeight.Medium))
                                    // Siempre se reserva la línea de letras para que todos los dígitos queden alineados.
                                    Text(letters.ifEmpty { " " }, style = MaterialTheme.typography.labelLarge, color = Cockpit.TextDim)
                                }
                            }
                        }
                    }
                }
            }
            // Llamar y contactos
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusTile(onClick = ::dial, modifier = Modifier.fillMaxWidth().height(100.dp)) {
                    Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(64.dp)) {
                            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) { drawCircle(CallGreen) }
                            Icon(Icons.Filled.Call, null, Modifier.align(Alignment.Center).size(36.dp), tint = Color.White)
                        }
                        Spacer(Modifier.width(20.dp))
                        Text("Llamar", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 36.sp))
                    }
                }
                Text("CONTACTOS", style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp), color = Cockpit.BlueLight)
                if (!hasContacts) {
                    FocusTile(onClick = { contactsPermission.launch(Manifest.permission.READ_CONTACTS) }, modifier = Modifier.fillMaxWidth().height(96.dp)) {
                        Text("Permitir acceso a los contactos", Modifier.align(Alignment.Center), style = MaterialTheme.typography.headlineSmall.copy(fontSize = 30.sp))
                    }
                } else if (contacts.isEmpty()) {
                    Text("No hay contactos en este dispositivo", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 28.sp), color = Cockpit.TextDim)
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(contacts, key = { it.name + it.number }) { contact ->
                        FocusTile(onClick = { number = contact.number }, modifier = Modifier.fillMaxWidth().height(96.dp)) {
                            Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.Center) {
                                Text(contact.name, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 34.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(contact.number, style = numeric(24), color = Cockpit.TextDim, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun granted(context: Context, permission: String) =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

/** Llama directamente si hay permiso; si no, abre el marcador del sistema con el número puesto. */
private fun call(context: Context, number: String) {
    if (number.isBlank()) return
    val uri = Uri.parse("tel:" + Uri.encode(number))
    val action = if (granted(context, Manifest.permission.CALL_PHONE)) Intent.ACTION_CALL else Intent.ACTION_DIAL
    launch(context, Intent(action, uri))
}

private suspend fun loadContacts(context: Context): List<Contact> = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC",
        )?.use { c ->
            buildList { while (c.moveToNext()) add(Contact(c.getString(0) ?: "", c.getString(1) ?: "")) }
                .distinctBy { it.name + it.number.filter(Char::isDigit) }
        } ?: emptyList()
    }.getOrDefault(emptyList())
}
