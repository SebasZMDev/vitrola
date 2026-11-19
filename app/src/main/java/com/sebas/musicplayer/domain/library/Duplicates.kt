package com.sebas.musicplayer.domain.library

import com.sebas.musicplayer.domain.model.Song
import kotlin.math.abs

/**
 * Agrupa canciones que parecen la misma: mismo título y artista (sin importar mayúsculas ni espacios
 * repetidos) y duración que difiera como mucho [toleranceSec]. Solo devuelve grupos de 2 o más.
 * Dentro de cada grupo van ordenadas por id (la más antigua primero) y los grupos por título.
 */
fun findDuplicateGroups(songs: List<Song>, toleranceSec: Int = 2): List<List<Song>> {
    fun norm(s: String) = s.trim().lowercase().replace(Regex("\\s+"), " ")

    return songs
        .groupBy { norm(it.title) to norm(it.artist) }
        .values
        .flatMap { sameName -> clusterByDuration(sameName.sortedBy { it.durationMs }, toleranceSec) }
        .filter { it.size >= 2 }
        .map { group -> group.sortedBy { it.id } }
        .sortedBy { it.first().title.lowercase() }
}

/** Encadena canciones ya ordenadas por duración: una entra al grupo si está cerca de la anterior. */
private fun clusterByDuration(sorted: List<Song>, toleranceSec: Int): List<List<Song>> {
    val groups = mutableListOf<MutableList<Song>>()
    for (song in sorted) {
        val last = groups.lastOrNull()
        if (last != null && abs(song.durationMs - last.last().durationMs) <= toleranceSec * 1000L) last += song
        else groups += mutableListOf(song)
    }
    return groups
}

/** Marca para eliminar todas menos una por grupo (la primera, la más antigua). */
fun suggestedRemovals(groups: List<List<Song>>): Set<Long> =
    groups.flatMap { it.drop(1) }.mapTo(HashSet()) { it.id }
