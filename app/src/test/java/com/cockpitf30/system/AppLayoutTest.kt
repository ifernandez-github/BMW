package com.cockpitf30.system

import com.cockpitf30.system.LayoutEntry.App
import com.cockpitf30.system.LayoutEntry.Group
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLayoutTest {
    private val installed = listOf("a", "b", "c", "d")

    @Test
    fun `sin disposicion guardada salen todas en orden`() {
        assertEquals(installed.map(::App), AppLayout.merge(emptyList(), installed))
    }

    @Test
    fun `quita desinstaladas, vacia grupos y añade nuevas al final`() {
        val saved = listOf(Group("g1", "Música", listOf("x", "c")), App("y"), App("a"), Group("g2", "Vacío", listOf("z")))
        assertEquals(
            listOf(Group("g1", "Música", listOf("c")), App("a"), App("b"), App("d")),
            AppLayout.merge(saved, installed),
        )
    }

    @Test
    fun `crear grupo en la posicion de la app`() {
        val list = installed.map(::App)
        assertEquals(listOf(App("a"), Group("g", "Grupo 1", listOf("b")), App("c"), App("d")), AppLayout.newGroup(list, "b", "g", "Grupo 1"))
    }

    @Test
    fun `añadir y sacar de un grupo`() {
        val list = listOf(Group("g", "G", listOf("a")), App("b"), App("c"))
        val added = AppLayout.addToGroup(list, "c", "g")
        assertEquals(listOf(Group("g", "G", listOf("a", "c")), App("b")), added)
        assertEquals(listOf(Group("g", "G", listOf("a")), App("c"), App("b")), AppLayout.removeFromGroup(added, "c"))
        // Al sacar la última, el grupo desaparece y queda la app en su sitio.
        assertEquals(listOf(App("a"), App("b"), App("c")), AppLayout.removeFromGroup(list, "a"))
    }

    @Test
    fun `mover dentro de un grupo y renombrar`() {
        val list = listOf(Group("g", "G", listOf("a", "b", "c")))
        assertEquals(listOf(Group("g", "Juegos", listOf("b", "a", "c"))), AppLayout.rename(AppLayout.moveInGroup(list, "g", 0, 1), "g", "Juegos"))
    }

    @Test
    fun `eliminar grupo deja sus apps sueltas en su sitio`() {
        val list = listOf(App("a"), Group("g", "G", listOf("b", "c")), App("d"))
        assertEquals(listOf(App("a"), App("b"), App("c"), App("d")), AppLayout.ungroup(list, "g"))
    }

    @Test
    fun `ida y vuelta en JSON`() {
        val list = listOf(App("a/A"), Group("g", "Música y radio", listOf("b/B", "c/C")))
        assertEquals(list, AppLayout.fromJson(AppLayout.toJson(list)))
        assertEquals(emptyList<LayoutEntry>(), AppLayout.fromJson("no es json"))
    }
}
