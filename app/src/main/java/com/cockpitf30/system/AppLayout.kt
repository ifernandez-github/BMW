package com.cockpitf30.system

import org.json.JSONArray
import org.json.JSONObject

/** Elemento de la pantalla de apps: una app suelta o un grupo con varias. Las apps se identifican por "paquete/actividad". */
sealed interface LayoutEntry {
    val id: String

    data class App(val key: String) : LayoutEntry {
        override val id get() = key
    }

    data class Group(override val id: String, val name: String, val apps: List<String>) : LayoutEntry
}

/** Lógica pura del orden y los grupos de apps, separada de la interfaz para poder probarla. */
object AppLayout {

    /**
     * Aplica la disposición guardada a las apps instaladas: quita las desinstaladas (y los grupos
     * que se queden vacíos) y añade al final las nuevas, en el orden recibido.
     */
    fun merge(saved: List<LayoutEntry>, installed: List<String>): List<LayoutEntry> {
        val present = installed.toSet()
        val seen = mutableSetOf<String>()
        val kept = saved.mapNotNull { e ->
            when (e) {
                is LayoutEntry.App -> e.takeIf { it.key in present && seen.add(it.key) }
                is LayoutEntry.Group -> e.copy(apps = e.apps.filter { it in present && seen.add(it) }).takeIf { it.apps.isNotEmpty() }
            }
        }
        return kept + installed.filter { it !in seen }.map { LayoutEntry.App(it) }
    }

    fun <T> move(list: List<T>, from: Int, to: Int): List<T> {
        if (from !in list.indices) return list
        val target = to.coerceIn(0, list.lastIndex)
        if (from == target) return list
        return list.toMutableList().apply { add(target, removeAt(from)) }
    }

    /** Convierte la app en un grupo nuevo en su misma posición. */
    fun newGroup(list: List<LayoutEntry>, appKey: String, groupId: String, name: String): List<LayoutEntry> {
        val stripped = removeEverywhere(list, appKey)
        val index = list.indexOfFirst { it is LayoutEntry.App && it.key == appKey }.takeIf { it >= 0 } ?: stripped.size
        return stripped.toMutableList().apply { add(index.coerceAtMost(size), LayoutEntry.Group(groupId, name, listOf(appKey))) }
    }

    /** Mete la app al final del grupo, sacándola de donde estuviera. */
    fun addToGroup(list: List<LayoutEntry>, appKey: String, groupId: String): List<LayoutEntry> =
        removeEverywhere(list, appKey).map { if (it is LayoutEntry.Group && it.id == groupId) it.copy(apps = it.apps + appKey) else it }

    /** Saca la app del grupo y la deja justo detrás de él; si el grupo se queda vacío, desaparece. */
    fun removeFromGroup(list: List<LayoutEntry>, appKey: String): List<LayoutEntry> {
        val gi = list.indexOfFirst { it is LayoutEntry.Group && appKey in it.apps }
        if (gi < 0) return list
        val group = list[gi] as LayoutEntry.Group
        val rest = group.apps - appKey
        return list.toMutableList().apply {
            if (rest.isEmpty()) {
                set(gi, LayoutEntry.App(appKey))
            } else {
                set(gi, group.copy(apps = rest))
                add(gi + 1, LayoutEntry.App(appKey))
            }
        }
    }

    /** Elimina el grupo dejando sus apps sueltas en su lugar. */
    fun ungroup(list: List<LayoutEntry>, groupId: String): List<LayoutEntry> = list.flatMap {
        if (it is LayoutEntry.Group && it.id == groupId) it.apps.map(LayoutEntry::App) else listOf(it)
    }

    fun rename(list: List<LayoutEntry>, groupId: String, name: String) =
        list.map { if (it is LayoutEntry.Group && it.id == groupId) it.copy(name = name) else it }

    /** Reordena una app dentro de su grupo. */
    fun moveInGroup(list: List<LayoutEntry>, groupId: String, from: Int, to: Int) =
        list.map { if (it is LayoutEntry.Group && it.id == groupId) it.copy(apps = move(it.apps, from, to)) else it }

    private fun removeEverywhere(list: List<LayoutEntry>, appKey: String): List<LayoutEntry> = list.mapNotNull {
        when (it) {
            is LayoutEntry.App -> it.takeIf { e -> e.key != appKey }
            is LayoutEntry.Group -> it.copy(apps = it.apps - appKey).takeIf { g -> g.apps.isNotEmpty() }
        }
    }

    fun toJson(list: List<LayoutEntry>): String = JSONArray().apply {
        list.forEach { e ->
            put(
                when (e) {
                    is LayoutEntry.App -> JSONObject().put("app", e.key)
                    is LayoutEntry.Group -> JSONObject().put("group", e.id).put("name", e.name).put("apps", JSONArray(e.apps))
                },
            )
        }
    }.toString()

    fun fromJson(json: String?): List<LayoutEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                when {
                    o.has("app") -> LayoutEntry.App(o.getString("app"))
                    o.has("group") -> {
                        val apps = o.getJSONArray("apps")
                        LayoutEntry.Group(o.getString("group"), o.optString("name", "Grupo"), (0 until apps.length()).map(apps::getString))
                    }
                    else -> null
                }
            }
        }.getOrDefault(emptyList())
    }
}
