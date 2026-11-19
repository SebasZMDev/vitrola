package com.sebas.musicplayer.data.lyrics

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.sebas.musicplayer.data.settings.AppSettings
import com.sebas.musicplayer.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Busca la letra de una canción en la carpeta que el usuario eligió (acceso por SAF, sin permisos extra).
 * Un archivo .lrc o .txt con el mismo nombre que el audio, o con el patrón "Artista - Título".
 * Android no deja leer archivos que no son audio junto a la música, por eso hay que elegir la carpeta.
 */
class LyricsRepository(
    private val context: Context,
    private val settings: AppSettings,
) {
    private var indexedTree: String? = null
    private var index: Map<String, Uri> = emptyMap()
    private var indexedAt = 0L

    suspend fun find(song: Song): Lyrics? = withContext(Dispatchers.IO) {
        val tree = settings.current.lyricsTreeUri ?: return@withContext null
        val candidates = candidateNames(song)
        var doc = candidates.firstNotNullOfOrNull { indexFor(tree, forceRescan = false)[it] }
        // Si no aparece, quizá el usuario acaba de copiar letras: un solo reescaneo cada 30 s.
        if (doc == null && System.currentTimeMillis() - indexedAt > RESCAN_AFTER_MS) {
            doc = candidates.firstNotNullOfOrNull { indexFor(tree, forceRescan = true)[it] }
        }
        doc?.let(::readText)?.let(LrcParser::parse)
    }

    private fun candidateNames(song: Song): List<String> {
        val fileName = runCatching {
            context.contentResolver.query(song.uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
        return listOfNotNull(
            fileName?.substringBeforeLast('.'),
            "${song.artist} - ${song.title}",
            song.title,
        ).map(::normalize).distinct()
    }

    @Synchronized
    private fun indexFor(tree: String, forceRescan: Boolean): Map<String, Uri> {
        if (tree != indexedTree || forceRescan) {
            index = scan(Uri.parse(tree))
            indexedTree = tree
            indexedAt = System.currentTimeMillis()
        }
        return index
    }

    private fun scan(tree: Uri): Map<String, Uri> {
        val found = HashMap<String, Uri>()
        val pending = ArrayDeque<String>()
        pending.add(DocumentsContract.getTreeDocumentId(tree))
        var folders = 0
        while (pending.isNotEmpty() && folders < MAX_FOLDERS) {
            val parent = pending.removeLast()
            folders++
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parent)
            runCatching {
                context.contentResolver.query(
                    children,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                    ),
                    null, null, null,
                )?.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getString(0)
                        val name = c.getString(1) ?: continue
                        if (c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                            pending.add(id)
                        } else if (name.endsWith(".lrc", true) || name.endsWith(".txt", true)) {
                            // Si hay .lrc y .txt con el mismo nombre gana el primero (normalmente el .lrc, que trae tiempos).
                            val key = normalize(name.substringBeforeLast('.'))
                            val uri = DocumentsContract.buildDocumentUriUsingTree(tree, id)
                            if (name.endsWith(".lrc", true) || key !in found) found[key] = uri
                        }
                    }
                }
            }
        }
        return found
    }

    private fun readText(uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            String(input.readBytes().take(MAX_BYTES).toByteArray(), Charsets.UTF_8)
        }
    }.getOrNull()

    private fun normalize(name: String) = name.lowercase().trim().replace(Regex("\\s+"), " ")

    private companion object {
        const val RESCAN_AFTER_MS = 30_000L
        const val MAX_FOLDERS = 2000
        const val MAX_BYTES = 512 * 1024
    }
}
