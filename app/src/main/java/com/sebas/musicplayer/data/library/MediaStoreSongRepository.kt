package com.sebas.musicplayer.data.library

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.sebas.musicplayer.domain.model.Song
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lee las canciones del dispositivo desde MediaStore.
 * Lanza SecurityException si falta el permiso; la UI lo pide antes de llamar.
 */
class MediaStoreSongRepository(
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SongRepository {

    override suspend fun getSongs(): List<Song> = withContext(ioDispatcher) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )
        // Solo música y descarta sonidos cortos (< 30 s)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
            "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(MIN_DURATION_MS.toString())
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val songs = mutableListOf<Song>()
        contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)
                val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                    ?: cursor.getString(nameCol)?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                    ?: "Sin título"
                val artist = cursor.getString(artistCol)
                    ?.takeIf { it.isNotBlank() && it != UNKNOWN_ARTIST }
                    ?: "Artista desconocido"
                val album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() }
                    ?: "Álbum desconocido"

                songs += Song(
                    id = id,
                    uri = ContentUris.withAppendedId(collection, id),
                    title = title,
                    artist = artist,
                    album = album,
                    albumId = albumId,
                    durationMs = cursor.getLong(durationCol),
                    artworkUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId),
                )
            }
        }
        songs
    }

    private companion object {
        const val MIN_DURATION_MS = 30_000L
        const val UNKNOWN_ARTIST = "<unknown>"
        val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}
