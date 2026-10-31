package com.cockpitf30.ui.screens

/** Lógica pura del orden de las tarjetas del Inicio, separada de la interfaz para poder probarla. */
object CardOrder {
    /**
     * Aplica el orden guardado a las tarjetas disponibles: respeta el orden de las que siguen
     * existiendo, descarta las que ya no están y añade al final las nuevas en su orden por defecto.
     */
    fun merge(saved: List<String>, available: List<String>): List<String> {
        val known = saved.distinct().filter { it in available }
        return known + available.filter { it !in known }
    }

    /** Mueve el elemento de [from] a [to] (índices acotados a la lista). */
    fun <T> move(list: List<T>, from: Int, to: Int): List<T> {
        if (from !in list.indices) return list
        val target = to.coerceIn(0, list.lastIndex)
        if (from == target) return list
        return list.toMutableList().apply { add(target, removeAt(from)) }
    }
}
