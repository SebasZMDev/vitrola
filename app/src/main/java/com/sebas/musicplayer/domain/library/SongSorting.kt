package com.sebas.musicplayer.domain.library

import com.sebas.musicplayer.data.settings.SongSort
import com.sebas.musicplayer.data.settings.SongSortField
import com.sebas.musicplayer.domain.model.Song

/** Orden por defecto de una lista: tal como viene. */
val ListDefaultSort = SongSort(SongSortField.ORIGINAL, true)

/**
 * Devuelve una copia ordenada de [items]; [song] saca la canción de cada elemento.
 * Compara sin mayúsculas y, en empate, desempata por título.
 */
fun <T> sortSongs(items: List<T>, sort: SongSort, song: (T) -> Song): List<T> {
    val byTitle = compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
    val base: Comparator<Song> = when (sort.field) {
        SongSortField.ORIGINAL -> return if (sort.ascending) items.toList() else items.asReversed().toList()
        SongSortField.TITLE -> byTitle
        SongSortField.ARTIST -> compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.artist }.then(byTitle)
        SongSortField.ALBUM -> compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.album }.then(byTitle)
        SongSortField.DURATION -> compareBy<Song> { it.durationMs }.then(byTitle)
        SongSortField.RECENT -> compareBy<Song> { it.dateAdded }.then(byTitle)
    }
    val comparator = if (sort.ascending) base else base.reversed()
    return items.sortedWith { a, b -> comparator.compare(song(a), song(b)) }
}

/** Tocar el criterio activo invierte el sentido; uno nuevo empieza ascendente, salvo "Recientes". */
fun nextSort(current: SongSort, field: SongSortField): SongSort =
    if (field == current.field) current.copy(ascending = !current.ascending)
    else SongSort(field, ascending = field != SongSortField.RECENT)

fun encodeSort(sort: SongSort): String = "${sort.field.name}:${sort.ascending}"

/** null si [value] no tiene el formato de [encodeSort]. */
fun decodeSort(value: String?): SongSort? {
    val parts = value?.split(':') ?: return null
    if (parts.size != 2) return null
    val field = SongSortField.entries.firstOrNull { it.name == parts[0] } ?: return null
    val ascending = parts[1].toBooleanStrictOrNull() ?: return null
    return SongSort(field, ascending)
}
