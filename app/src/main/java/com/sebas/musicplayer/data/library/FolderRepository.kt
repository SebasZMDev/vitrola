package com.sebas.musicplayer.data.library

import android.content.ContentResolver
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Carpeta con música. [path] termina en "/" y es la clave que se guarda al excluirla. */
data class MusicFolder(val path: String, val songCount: Int)

interface FolderRepository {
    /** Carpetas con música de al menos [minDurationSec] segundos (sin aplicar exclusiones). */
    suspend fun getFolders(minDurationSec: Int): List<MusicFolder>
}

class MediaStoreFolderRepository(
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FolderRepository {

    override suspend fun getFolders(minDurationSec: Int): List<MusicFolder> = withContext(ioDispatcher) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val relative = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        @Suppress("DEPRECATION")
        val pathColumn = if (relative) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA

        val counts = sortedMapOf<String, Int>(String.CASE_INSENSITIVE_ORDER)
        contentResolver.query(
            collection,
            arrayOf(pathColumn),
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?",
            arrayOf((minDurationSec * 1000L).toString()),
            null,
        )?.use { cursor ->
            val pathCol = cursor.getColumnIndexOrThrow(pathColumn)
            while (cursor.moveToNext()) {
                val folder = LibraryFilter.folderOf(cursor.getString(pathCol), isFullFilePath = !relative)
                // Las carpetas de WhatsApp se controlan con su propio interruptor.
                if (folder.isBlank() || folder.contains("whatsapp", ignoreCase = true)) continue
                counts[folder] = (counts[folder] ?: 0) + 1
            }
        }
        counts.map { (path, count) -> MusicFolder(path, count) }
    }
}
