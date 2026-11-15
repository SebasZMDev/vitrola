package com.sebas.musicplayer.data.library

/**
 * Arma el WHERE de la consulta a MediaStore según los ajustes. Es lógica pura (sin Android)
 * para poder probarla en la JVM.
 */
object LibraryFilter {

    data class Clause(val selection: String, val args: List<String>)

    /**
     * @param pathColumn columna con la ruta (RELATIVE_PATH desde Android 10, DATA antes).
     * @param excludedFolders rutas que terminan en "/" (ver [folderOf]); se excluyen también sus subcarpetas.
     */
    fun clause(
        pathColumn: String,
        isMusicColumn: String,
        durationColumn: String,
        minDurationSec: Int,
        excludeWhatsApp: Boolean,
        excludedFolders: Set<String>,
    ): Clause {
        val selection = StringBuilder("$isMusicColumn != 0 AND $durationColumn >= ?")
        val args = mutableListOf((minDurationSec * 1000L).toString())
        if (excludeWhatsApp) {
            // Cualquier ruta que contenga "WhatsApp" (notas de voz, audios recibidos, WhatsApp Business).
            selection.append(" AND COALESCE($pathColumn, '') NOT LIKE ? COLLATE NOCASE")
            args += "%WhatsApp%"
        }
        for (folder in excludedFolders.sorted()) {
            selection.append(" AND COALESCE($pathColumn, '') NOT LIKE ? ESCAPE '\\'")
            args += escapeLike(folder) + "%"
        }
        return Clause(selection.toString(), args)
    }

    /**
     * Carpeta de una canción, siempre terminada en "/". En Android 10+ la ruta ya es relativa y
     * termina en "/"; antes se recibe la ruta completa del archivo y se le quita el nombre.
     */
    fun folderOf(rawPath: String?, isFullFilePath: Boolean): String {
        val path = rawPath.orEmpty()
        if (path.isBlank()) return ""
        return if (isFullFilePath) path.substringBeforeLast('/', "").let { if (it.isEmpty()) "" else "$it/" } else path
    }

    fun escapeLike(text: String): String =
        text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
