package com.sebas.musicplayer.data.playlist

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.sebas.musicplayer.data.playlist.db.MusicDatabase
import com.sebas.musicplayer.data.playlist.db.PlaylistEntity
import com.sebas.musicplayer.data.playlist.db.PlaylistWithCount
import com.sebas.musicplayer.domain.model.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPlaylistRepository(context: Context) : PlaylistRepository {
    private val appContext = context.applicationContext
    private val dao = MusicDatabase.get(context).playlistDao()

    override fun observePlaylists(): Flow<List<Playlist>> =
        dao.observePlaylists().map { list -> list.map { it.toDomain() } }

    override fun observePlaylist(playlistId: Long): Flow<Playlist?> =
        dao.observePlaylist(playlistId).map { it?.toDomain() }

    override fun observeSongIds(playlistId: Long): Flow<List<Long>> = dao.observeSongIds(playlistId)

    override fun observeCoverSongIds(): Flow<Map<Long, List<Long>>> =
        dao.observeCoverRefs().map { refs -> refs.groupBy({ it.playlistId }, { it.songId }) }

    override suspend fun create(name: String): Long =
        dao.insertPlaylist(PlaylistEntity(name = name, createdAt = System.currentTimeMillis()))

    override suspend fun rename(playlistId: Long, name: String) = dao.rename(playlistId, name)

    override suspend fun delete(playlistId: Long) {
        val cover = dao.getCoverPath(playlistId)
        dao.delete(playlistId)
        cover?.let { File(it).delete() }
    }

    override suspend fun setCover(playlistId: Long, source: Uri?): Boolean = withContext(Dispatchers.IO) {
        val old = dao.getCoverPath(playlistId)
        if (source == null) {
            dao.setCoverPath(playlistId, null)
            old?.let { File(it).delete() }
            return@withContext true
        }
        val dir = File(appContext.filesDir, "playlist_covers").apply { mkdirs() }
        // Nombre nuevo en cada cambio: así la imagen en pantalla (y su caché) se actualiza al instante.
        val target = File(dir, "${playlistId}_${System.currentTimeMillis()}.jpg")
        if (!CoverImage.saveSquareJpeg(appContext.contentResolver, source, target)) {
            target.delete()
            return@withContext false
        }
        dao.setCoverPath(playlistId, target.absolutePath)
        old?.let { File(it).delete() }
        true
    }

    override suspend fun addSongs(playlistId: Long, songIds: List<Long>) {
        if (songIds.isNotEmpty()) dao.addSongs(playlistId, songIds)
    }

    override suspend fun removeAt(playlistId: Long, position: Int) = dao.removeAt(playlistId, position)

    private fun PlaylistWithCount.toDomain() = Playlist(id, name, songCount, coverPath)
}
