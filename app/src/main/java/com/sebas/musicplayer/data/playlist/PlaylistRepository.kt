package com.sebas.musicplayer.data.playlist

import com.sebas.musicplayer.domain.model.Playlist
import kotlinx.coroutines.flow.Flow

/**
 * Playlists del usuario, persistidas localmente.
 * Las canciones se guardan como ids de MediaStore en orden; si un archivo se borra del
 * teléfono, su id simplemente no se resuelve y la UI lo omite.
 * CONTRATO: no cambiar sin avisar al orquestador.
 */
interface PlaylistRepository {
    /** Todas las playlists ordenadas por nombre, con su conteo de canciones. */
    fun observePlaylists(): Flow<List<Playlist>>

    /** La playlist o null si no existe (por ejemplo, se borró). */
    fun observePlaylist(playlistId: Long): Flow<Playlist?>

    /** Ids de canciones de la playlist, en orden. Puede tener duplicados si el usuario agrega dos veces. */
    fun observeSongIds(playlistId: Long): Flow<List<Long>>

    /** Ids de las primeras canciones de cada playlist, para su portada. */
    fun observeCoverSongIds(): Flow<Map<Long, List<Long>>>

    /** Crea una playlist y devuelve su id. */
    suspend fun create(name: String): Long
    suspend fun rename(playlistId: Long, name: String)
    suspend fun delete(playlistId: Long)

    /** Agrega al final, en el orden dado. */
    suspend fun addSongs(playlistId: Long, songIds: List<Long>)

    /** Quita la canción en [position] (índice en observeSongIds). */
    suspend fun removeAt(playlistId: Long, position: Int)
}
