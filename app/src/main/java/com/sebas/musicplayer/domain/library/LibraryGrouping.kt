package com.sebas.musicplayer.domain.library

import android.net.Uri
import com.sebas.musicplayer.domain.model.Song
import java.text.Normalizer

/**
 * Álbumes y artistas derivados de la lista de canciones (sin consultas extra a MediaStore).
 * CONTRATO compartido: no cambiar sin avisar al orquestador.
 */
data class Album(
    val id: Long,
    val title: String,
    /** Artista principal, o "Varios artistas" si el álbum mezcla varios. */
    val artist: String,
    val artworkUri: Uri?,
    val songs: List<Song>,
) {
    val durationMs: Long get() = songs.sumOf { it.durationMs }
}

data class Artist(
    val name: String,
    val songs: List<Song>,
    val albums: List<Album>,
)

const val VARIOUS_ARTISTS = "Varios artistas"

/** Clave de agrupación: sin espacios sobrantes ni mayúsculas ("Fallout " y "fallout" son el mismo). */
private fun groupKey(name: String) = name.trim().lowercase()

/** Id estable a partir del nombre (positivo para álbumes, negativo para géneros, así no chocan en la ruta). */
private fun stableId(key: String): Long = key.hashCode().toLong() and 0x7fffffffL

/**
 * Álbumes ordenados por título; canciones en el orden de entrada.
 * Se agrupa por NOMBRE y no por el id de MediaStore: las descargas traen un id distinto por archivo,
 * así que con el id cada canción quedaba como un álbum de una sola canción.
 */
fun groupAlbums(songs: List<Song>): List<Album> =
    songs.groupBy { groupKey(it.album) }
        .map { (key, albumSongs) ->
            val artists = albumSongs.map { it.artist }.distinct()
            Album(
                id = stableId(key),
                title = albumSongs.first().album.trim(),
                artist = artists.singleOrNull() ?: VARIOUS_ARTISTS,
                artworkUri = albumSongs.firstNotNullOfOrNull { it.artworkUri },
                songs = albumSongs,
            )
        }
        .sortedBy { it.title.lowercase() }

/** Géneros (solo canciones con género puesto), del más grande al más chico. Se muestran como un [Album]. */
fun groupGenres(songs: List<Song>): List<Album> =
    songs.filter { !it.genre.isNullOrBlank() }
        .groupBy { groupKey(it.genre!!) }
        .map { (key, genreSongs) ->
            val artists = genreSongs.map { it.artist }.distinct()
            Album(
                id = -(stableId(key) + 1),
                title = genreSongs.first().genre!!.trim(),
                artist = artists.singleOrNull() ?: "${artists.size} artistas",
                artworkUri = genreSongs.firstNotNullOfOrNull { it.artworkUri },
                songs = genreSongs,
            )
        }
        .sortedByDescending { it.songs.size }

/** Artistas ordenados por nombre, con sus canciones y los álbumes donde aparecen. */
fun groupArtists(songs: List<Song>): List<Artist> =
    songs.groupBy { it.artist }
        .map { (name, artistSongs) -> Artist(name = name, songs = artistSongs, albums = groupAlbums(artistSongs)) }
        .sortedBy { it.name.lowercase() }

/** Para búsquedas: minúsculas y sin tildes ("Canción" -> "cancion"). */
fun String.normalizedForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
