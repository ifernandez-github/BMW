package com.cockpitf30.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceMatchTest {
    private val contacts = listOf("Ana García", "Ángel Pérez", "Taller BMW", "Mamá", "Ana Belén Ruiz")

    @Test
    fun `quita la orden y los acentos`() {
        assertEquals("angel perez", VoiceMatch.stripCommand("Llama a Ángel Pérez"))
        assertEquals("mama", VoiceMatch.stripCommand("llamar a mamá"))
        assertEquals("taller bmw", VoiceMatch.stripCommand("Taller BMW"))
    }

    @Test
    fun `encuentra el contacto por nombre completo o parcial`() {
        assertEquals(listOf("Ángel Pérez"), VoiceMatch.match("llama a angel perez", contacts) { it })
        assertEquals(listOf("Mamá"), VoiceMatch.match("Llama a mamá", contacts) { it })
        assertEquals(listOf("Taller BMW"), VoiceMatch.match("llama al taller", contacts) { it })
    }

    @Test
    fun `si hay varios igual de buenos los devuelve todos`() {
        assertEquals(listOf("Ana García", "Ana Belén Ruiz"), VoiceMatch.match("llama a Ana", contacts) { it })
        assertEquals(emptyList<String>(), VoiceMatch.match("llama a Pedro", contacts) { it })
    }

    @Test
    fun `reconoce numeros dictados`() {
        assertEquals("612345678", VoiceMatch.spokenNumber("llama al 612 345 678"))
        assertNull(VoiceMatch.spokenNumber("llama a Ana"))
    }
}
