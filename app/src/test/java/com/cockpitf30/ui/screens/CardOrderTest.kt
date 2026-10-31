package com.cockpitf30.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class CardOrderTest {
    private val defaults = listOf("music", "nav", "trip", "weather", "vehicle")

    @Test
    fun `sin orden guardado se usa el orden por defecto`() {
        assertEquals(defaults, CardOrder.merge(emptyList(), defaults))
    }

    @Test
    fun `respeta el orden guardado`() {
        val saved = listOf("weather", "music", "nav", "trip", "vehicle")
        assertEquals(saved, CardOrder.merge(saved, defaults))
    }

    @Test
    fun `descarta tarjetas que ya no existen y añade las nuevas al final`() {
        val saved = listOf("tyres", "trip", "music")
        assertEquals(listOf("trip", "music", "nav", "weather", "vehicle"), CardOrder.merge(saved, defaults))
    }

    @Test
    fun `ignora duplicados en el orden guardado`() {
        assertEquals(listOf("nav", "music", "trip", "weather", "vehicle"), CardOrder.merge(listOf("nav", "nav", "music"), defaults))
    }

    @Test
    fun `mover a izquierda y derecha`() {
        assertEquals(listOf("nav", "music", "trip", "weather", "vehicle"), CardOrder.move(defaults, 0, 1))
        assertEquals(listOf("music", "nav", "weather", "trip", "vehicle"), CardOrder.move(defaults, 3, 2))
        assertEquals(listOf("music", "nav", "trip", "vehicle", "weather"), CardOrder.move(defaults, 3, 9))
    }

    @Test
    fun `mover fuera de rango no cambia nada`() {
        assertEquals(defaults, CardOrder.move(defaults, 0, -1))
        assertEquals(defaults, CardOrder.move(defaults, 7, 2))
    }
}
