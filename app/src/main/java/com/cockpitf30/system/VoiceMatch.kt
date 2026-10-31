package com.cockpitf30.system

import java.text.Normalizer

/** Busca a quién llamar a partir de lo que se ha dicho por voz ("llama a Ana", "Ana móvil", "612 345 678"). */
object VoiceMatch {
    private val PREFIXES = listOf(
        "llama a", "llamar a", "llama al", "llamar al", "llama", "llamar", "telefonea a", "marca", "marcar", "call",
    )

    fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ").trim()

    /** Quita "llama a" y similares del principio. */
    fun stripCommand(spoken: String): String {
        val n = normalize(spoken)
        val p = PREFIXES.firstOrNull { n == it || n.startsWith("$it ") } ?: return n
        return n.removePrefix(p).trim()
    }

    /** Si lo dicho es un número de teléfono, lo devuelve solo con cifras (y "+" inicial). */
    fun spokenNumber(spoken: String): String? {
        val rest = stripCommand(spoken)
        val digits = rest.filter { it.isDigit() }
        return digits.takeIf { it.length >= 3 && rest.all { c -> c.isDigit() || c == ' ' } }
            ?.let { if (spoken.trim().startsWith("+")) "+$it" else it }
    }

    /**
     * Ordena los nombres por parecido con lo dicho: coincidencia exacta, luego los que contienen
     * todas las palabras y después los que empiezan por alguna. Devuelve los índices que encajan.
     */
    fun <T> match(spoken: String, candidates: List<T>, name: (T) -> String): List<T> {
        val q = stripCommand(spoken)
        if (q.isBlank()) return emptyList()
        val words = q.split(' ')
        return candidates.mapNotNull { c ->
            val n = normalize(name(c))
            val nw = n.split(' ')
            val score = when {
                n == q -> 3
                words.all { w -> nw.any { it == w } } -> 2
                words.all { w -> nw.any { it.startsWith(w) } } -> 1
                else -> 0
            }
            if (score > 0) c to score else null
        }.sortedByDescending { it.second }.let { scored ->
            val best = scored.firstOrNull()?.second ?: return emptyList()
            scored.filter { it.second == best }.map { it.first }
        }
    }
}
