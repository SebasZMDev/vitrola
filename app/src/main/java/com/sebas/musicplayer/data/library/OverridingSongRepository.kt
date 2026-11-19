package com.sebas.musicplayer.data.library

import com.sebas.musicplayer.domain.model.Song

/** Cambio de metadatos para una canción; null = no tocar ese campo. */
data class SongEdit(val songId: Long, val title: String? = null, val artist: String? = null, val album: String? = null)

/**
 * Aplica por encima de [delegate] los metadatos corregidos por el usuario (guardados en Room).
 * No escribe en los archivos de audio: es seguro y reversible.
 */
class OverridingSongRepository(
    private val delegate: SongRepository,
    private val dao: SongOverrideDao,
) : SongRepository {

    /** Canciones con los metadatos originales del archivo, sin las correcciones del usuario. */
    suspend fun getOriginalSongs(forceRefresh: Boolean = false): List<Song> = delegate.getSongs(forceRefresh)

    override suspend fun getSongs(forceRefresh: Boolean): List<Song> {
        val songs = delegate.getSongs(forceRefresh)
        val overrides = dao.getAll().associateBy { it.songId }
        if (overrides.isEmpty()) return songs
        return songs.map { song ->
            val o = overrides[song.id] ?: return@map song
            song.copy(
                title = o.title ?: song.title,
                artist = o.artist ?: song.artist,
                album = o.album ?: song.album,
            )
        }
    }

    /** Guarda los cambios fusionándolos con lo que ya hubiera corregido para cada canción. */
    suspend fun applyEdits(edits: List<SongEdit>) {
        if (edits.isEmpty()) return
        val existing = dao.getAll().associateBy { it.songId }
        dao.upsertAll(
            edits.map { e ->
                val old = existing[e.songId]
                SongOverrideEntity(
                    songId = e.songId,
                    title = e.title ?: old?.title,
                    artist = e.artist ?: old?.artist,
                    album = e.album ?: old?.album,
                )
            },
        )
    }

    /** Vuelve a los metadatos originales del archivo. */
    suspend fun clearEdits(songIds: List<Long>) {
        if (songIds.isNotEmpty()) dao.deleteByIds(songIds)
    }
}
