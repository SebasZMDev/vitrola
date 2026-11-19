package com.sebas.musicplayer.data.playlist.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class PlaylistWithCount(
    val id: Long,
    val name: String,
    val songCount: Int,
)

data class PlaylistSongRef(val playlistId: Long, val songId: Long)

@Dao
abstract class PlaylistDao {
    @Query(
        """
        SELECT p.id AS id, p.name AS name, COUNT(s.id) AS songCount
        FROM playlists p LEFT JOIN playlist_songs s ON s.playlistId = p.id
        GROUP BY p.id
        ORDER BY p.name COLLATE NOCASE
        """,
    )
    abstract fun observePlaylists(): Flow<List<PlaylistWithCount>>

    @Query(
        """
        SELECT p.id AS id, p.name AS name, COUNT(s.id) AS songCount
        FROM playlists p LEFT JOIN playlist_songs s ON s.playlistId = p.id
        WHERE p.id = :playlistId
        GROUP BY p.id
        """,
    )
    abstract fun observePlaylist(playlistId: Long): Flow<PlaylistWithCount?>

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position")
    abstract fun observeSongIds(playlistId: Long): Flow<List<Long>>

    /** Las primeras canciones de cada playlist (para armar su portada). */
    @Query("SELECT playlistId, songId FROM playlist_songs WHERE position < 12 ORDER BY playlistId, position")
    abstract fun observeCoverRefs(): Flow<List<PlaylistSongRef>>

    @Insert
    abstract suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert
    abstract suspend fun insertSongs(songs: List<PlaylistSongEntity>)

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    abstract suspend fun rename(playlistId: Long, name: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    abstract suspend fun delete(playlistId: Long)

    @Query("SELECT MAX(position) FROM playlist_songs WHERE playlistId = :playlistId")
    protected abstract suspend fun maxPosition(playlistId: Long): Int?

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND position = :position")
    protected abstract suspend fun deleteAt(playlistId: Long, position: Int)

    @Query("UPDATE playlist_songs SET position = position - 1 WHERE playlistId = :playlistId AND position > :position")
    protected abstract suspend fun shiftDown(playlistId: Long, position: Int)

    @Transaction
    open suspend fun addSongs(playlistId: Long, songIds: List<Long>) {
        val start = (maxPosition(playlistId) ?: -1) + 1
        insertSongs(songIds.mapIndexed { i, songId -> PlaylistSongEntity(playlistId = playlistId, songId = songId, position = start + i) })
    }

    @Transaction
    open suspend fun removeAt(playlistId: Long, position: Int) {
        deleteAt(playlistId, position)
        shiftDown(playlistId, position)
    }
}
