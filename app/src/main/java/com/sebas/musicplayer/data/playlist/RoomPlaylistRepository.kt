package com.sebas.musicplayer.data.playlist

import android.content.Context
import com.sebas.musicplayer.data.playlist.db.MusicDatabase
import com.sebas.musicplayer.data.playlist.db.PlaylistEntity
import com.sebas.musicplayer.data.playlist.db.PlaylistWithCount
import com.sebas.musicplayer.domain.model.Playlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPlaylistRepository(context: Context) : PlaylistRepository {
    private val dao = MusicDatabase.get(context).playlistDao()

    override fun observePlaylists(): Flow<List<Playlist>> =
        dao.observePlaylists().map { list -> list.map { it.toDomain() } }

    override fun observePlaylist(playlistId: Long): Flow<Playlist?> =
        dao.observePlaylist(playlistId).map { it?.toDomain() }

    override fun observeSongIds(playlistId: Long): Flow<List<Long>> = dao.observeSongIds(playlistId)

    override suspend fun create(name: String): Long =
        dao.insertPlaylist(PlaylistEntity(name = name, createdAt = System.currentTimeMillis()))

    override suspend fun rename(playlistId: Long, name: String) = dao.rename(playlistId, name)

    override suspend fun delete(playlistId: Long) = dao.delete(playlistId)

    override suspend fun addSongs(playlistId: Long, songIds: List<Long>) {
        if (songIds.isNotEmpty()) dao.addSongs(playlistId, songIds)
    }

    override suspend fun removeAt(playlistId: Long, position: Int) = dao.removeAt(playlistId, position)

    private fun PlaylistWithCount.toDomain() = Playlist(id, name, songCount)
}
