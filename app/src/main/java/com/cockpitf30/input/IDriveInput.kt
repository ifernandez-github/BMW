package com.cockpitf30.input

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent

/** Acción de navegación independiente de cómo la mande la placa. */
enum class NavAction { NEXT, PREVIOUS, LONG_PRESS }

/**
 * Traduce lo que llegue del iDrive a acciones de foco.
 *
 * No sabemos aún cómo entrega la ZQ9109 la ruleta. Cubrimos los casos típicos:
 * - Encoder rotatorio de Android: SOURCE_ROTARY_ENCODER con AXIS_SCROLL (no AXIS_VSCROLL).
 * - Rueda de ratón emulada: ACTION_SCROLL con AXIS_VSCROLL.
 * - Teclas: DPAD, TAB, NAVIGATE_NEXT/PREVIOUS. DPAD y ENTER los gestiona Compose directamente.
 * La pantalla de Diagnóstico muestra cada evento para ajustar este mapeo.
 */
object IDriveInput {

    fun fromMotion(event: MotionEvent): NavAction? {
        val value = when {
            event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER) -> event.getAxisValue(MotionEvent.AXIS_SCROLL)
            event.action == MotionEvent.ACTION_SCROLL -> event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            else -> return null
        }
        return when {
            // En rueda de ratón, positivo = "hacia arriba"; lo tratamos como giro a la izquierda.
            value > 0 -> if (event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) NavAction.NEXT else NavAction.PREVIOUS
            value < 0 -> if (event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) NavAction.PREVIOUS else NavAction.NEXT
            else -> null
        }
    }

    /** OK/centro del iDrive mantenido: la primera repetición de la tecla marca la pulsación larga. */
    fun isCenter(keyCode: Int) =
        keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER

    fun fromKey(event: KeyEvent): NavAction? {
        if (event.action != KeyEvent.ACTION_DOWN) return null
        if (isCenter(event.keyCode)) {
            return if (event.repeatCount == 1 || (event.repeatCount > 0 && event.isLongPress)) NavAction.LONG_PRESS else null
        }
        return when (event.keyCode) {
            KeyEvent.KEYCODE_TAB -> if (event.isShiftPressed) NavAction.PREVIOUS else NavAction.NEXT
            KeyEvent.KEYCODE_NAVIGATE_NEXT -> NavAction.NEXT
            KeyEvent.KEYCODE_NAVIGATE_PREVIOUS -> NavAction.PREVIOUS
            else -> null
        }
    }

    fun describe(event: MotionEvent): String =
        "Motion src=0x%08X act=%d scroll=%.2f vscroll=%.2f dev=%s".format(
            event.source, event.actionMasked,
            event.getAxisValue(MotionEvent.AXIS_SCROLL), event.getAxisValue(MotionEvent.AXIS_VSCROLL),
            event.device?.name ?: "?",
        )

    fun describe(event: KeyEvent): String =
        "Key ${KeyEvent.keyCodeToString(event.keyCode)} (${event.keyCode}) " +
            (if (event.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP") +
            " scan=${event.scanCode} dev=${event.device?.name ?: "?"}"
}
