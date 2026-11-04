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

/** Álbumes ordenados por título; canciones en el orden de entrada. */
fun groupAlbums(songs: List<Song>): List<Album> =
    songs.groupBy { it.albumId }
        .map { (albumId, albumSongs) ->
            val first = albumSongs.first()
            val artists = albumSongs.map { it.artist }.distinct()
            Album(
                id = albumId,
                title = first.album,
                artist = artists.singleOrNull() ?: VARIOUS_ARTISTS,
                artworkUri = first.artworkUri,
                songs = albumSongs,
            )
        }
        .sortedBy { it.title.lowercase() }

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
