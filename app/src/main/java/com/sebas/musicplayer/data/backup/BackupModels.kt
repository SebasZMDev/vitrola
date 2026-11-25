package com.sebas.musicplayer.data.backup

import com.sebas.musicplayer.domain.model.Song
import kotlin.math.abs

/**
 * Cómo se reconoce una canción en un respaldo. Los ids de MediaStore cambian al reinstalar o cambiar de
 * teléfono, así que se usa título + artista + duración (con los metadatos originales del archivo).
 */
data class SongKey(val title: String, val artist: String, val durationSec: Int)

fun Song.toKey() = SongKey(title, artist, (durationMs / 1000).toInt())

data class BackupPlaylist(val name: String, val songs: List<SongKey>)
data class BackupFavorite(val key: SongKey, val addedAt: Long)
data class BackupStat(val key: SongKey, val playCount: Int, val lastPlayedAt: Long)
data class BackupGain(val key: SongKey, val gainDb: Float)
data class BackupOverride(val key: SongKey, val title: String?, val artist: String?, val album: String?, val genre: String? = null)

/** Una canción de la biblioteca tal como se ve hoy (solo informativo: al importar se ignora; los cambios van en overrides). */
data class BackupSong(val key: SongKey, val title: String, val artist: String, val album: String, val genre: String?)

data class BackupData(
    val exportedAt: Long,
    val playlists: List<BackupPlaylist>,
    val favorites: List<BackupFavorite>,
    val stats: List<BackupStat>,
    val overrides: List<BackupOverride>,
    val gains: List<BackupGain> = emptyList(),
    val library: List<BackupSong> = emptyList(),
) {
    companion object {
        const val VERSION = 3
    }
}

/** Resultado de importar un respaldo. */
data class ImportReport(
    val playlistsAdded: Int,
    val playlistsSkipped: Int,
    val favorites: Int,
    val stats: Int,
    val overrides: Int,
    /** Canciones del respaldo que no se encontraron en este teléfono. */
    val missingSongs: Int,
    val gains: Int = 0,
) {
    fun summary(): String = buildString {
        append("Importado: ")
        append("$playlistsAdded playlists, $favorites favoritas, $stats con historial, $overrides editadas")
        if (gains > 0) append(", $gains con volumen propio")
        if (playlistsSkipped > 0) append(". $playlistsSkipped playlists ya existían y se dejaron igual")
        if (missingSongs > 0) append(". $missingSongs canciones no están en este teléfono")
    }
}

/** Busca en la biblioteca actual la canción que corresponde a una [SongKey]. */
class SongMatcher(songs: List<Song>) {
    private val byName: Map<String, List<Song>> = songs.groupBy { nameKey(it.title, it.artist) }

    /** Id de la canción con el mismo título y artista y una duración que difiera como mucho [toleranceSec]; null si no hay. */
    fun find(key: SongKey, toleranceSec: Int = 2): Long? =
        byName[nameKey(key.title, key.artist)]
            ?.map { it to abs((it.durationMs / 1000).toInt() - key.durationSec) }
            ?.filter { it.second <= toleranceSec }
            ?.minByOrNull { it.second }
            ?.first?.id

    private fun nameKey(title: String, artist: String) = "${title.trim().lowercase()}|${artist.trim().lowercase()}"
}
