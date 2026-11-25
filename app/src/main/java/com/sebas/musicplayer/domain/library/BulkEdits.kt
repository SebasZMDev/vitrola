package com.sebas.musicplayer.domain.library

import com.sebas.musicplayer.data.library.SongEdit
import com.sebas.musicplayer.domain.model.Song

/** Artista, álbum y género comunes a todas las canciones, para rellenar el diálogo ("" si difieren). */
fun commonMetadata(songs: List<Song>): Triple<String, String, String> {
    val unique = songs.distinctBy { it.id }
    val artist = unique.map { it.artist }.distinct().singleOrNull().orEmpty()
    val album = unique.map { it.album }.distinct().singleOrNull().orEmpty()
    val genre = unique.map { it.genre }.distinct().singleOrNull().orEmpty()
    return Triple(artist, album, genre)
}

/** El mismo artista, álbum y/o género para todas las canciones; un campo en blanco no se toca. */
fun bulkEdits(songs: List<Song>, artist: String, album: String, genre: String): List<SongEdit> {
    val a = artist.trim().ifEmpty { null }
    val b = album.trim().ifEmpty { null }
    val g = genre.trim().ifEmpty { null }
    if (a == null && b == null && g == null) return emptyList()
    return songs.distinctBy { it.id }.map { SongEdit(it.id, artist = a, album = b, genre = g) }
}

/** Descargas de YouTube: separa "Artista - Título" del nombre de cada canción. Las que no tienen " - " se dejan igual. */
fun splitTitleEdits(songs: List<Song>): List<SongEdit> = songs.distinctBy { it.id }.mapNotNull { song ->
    val parts = song.title.split(" - ", limit = 2)
    val artist = parts.getOrNull(0)?.trim().orEmpty()
    val title = parts.getOrNull(1)?.trim().orEmpty()
    if (artist.isEmpty() || title.isEmpty()) null else SongEdit(song.id, title = title, artist = artist)
}
