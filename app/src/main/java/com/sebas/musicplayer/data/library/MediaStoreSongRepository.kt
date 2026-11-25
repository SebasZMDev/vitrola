package com.sebas.musicplayer.data.library

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.sebas.musicplayer.data.settings.AppSettings
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
    private val settings: AppSettings,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SongRepository {

    override suspend fun getSongs(forceRefresh: Boolean): List<Song> = withContext(ioDispatcher) {
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
            MediaStore.Audio.Media.DATE_ADDED,
        )
        // Solo música, con la duración mínima, sin WhatsApp y sin las carpetas que el usuario excluyó
        // (ver LibraryFilter). RELATIVE_PATH existe desde Android 10; antes se filtra por la ruta
        // completa (DATA).
        val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.RELATIVE_PATH
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Audio.Media.DATA
        }
        val prefs = settings.current
        val clause = LibraryFilter.clause(
            pathColumn = pathColumn,
            isMusicColumn = MediaStore.Audio.Media.IS_MUSIC,
            durationColumn = MediaStore.Audio.Media.DURATION,
            minDurationSec = prefs.minDurationSec,
            excludeWhatsApp = prefs.excludeWhatsApp,
            excludedFolders = prefs.excludedFolders,
        )
        val selection = clause.selection
        val selectionArgs = clause.args.toTypedArray()
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
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

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
                    dateAdded = cursor.getLong(dateAddedCol),
                )
            }
        }
        songs
    }

    private companion object {
        const val UNKNOWN_ARTIST = "<unknown>"
        val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}
