package com.cockpitf30.system

import android.content.Context

enum class SourceMode(val label: String) {
    AUTO("Auto"),
    SERIAL("Serie"),
    CAR_PROPERTY("CarProperty"),
    DEMO("Demo"),
}

/** Ajustes persistentes de la app. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("cockpit", Context.MODE_PRIVATE)

    var sourceMode: SourceMode
        get() = runCatching { SourceMode.valueOf(prefs.getString(KEY_SOURCE, null)!!) }.getOrDefault(SourceMode.AUTO)
        set(value) = prefs.edit().putString(KEY_SOURCE, value.name).apply()

    var serialPath: String
        get() = prefs.getString(KEY_PATH, "/dev/ttyMT1")!!
        set(value) = prefs.edit().putString(KEY_PATH, value).apply()

    /** 38400 es el valor habitual de Raise, pero no está confirmado para esta placa. */
    var baudRate: Int
        get() = prefs.getInt(KEY_BAUD, 38400)
        set(value) = prefs.edit().putInt(KEY_BAUD, value).apply()

    var askedBatteryExemption: Boolean
        get() = prefs.getBoolean(KEY_ASKED_BATTERY, false)
        set(value) = prefs.edit().putBoolean(KEY_ASKED_BATTERY, value).apply()

    private companion object {
        const val KEY_SOURCE = "source"
        const val KEY_PATH = "serial_path"
        const val KEY_BAUD = "baud"
        const val KEY_ASKED_BATTERY = "asked_battery"
    }
}
