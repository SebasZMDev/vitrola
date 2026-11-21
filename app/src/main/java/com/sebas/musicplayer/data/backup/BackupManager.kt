package com.sebas.musicplayer.data.backup

import com.sebas.musicplayer.data.library.OverridingSongRepository
import com.sebas.musicplayer.data.library.SongOverrideDao
import com.sebas.musicplayer.data.library.SongOverrideEntity
import com.sebas.musicplayer.data.playlist.PlaylistRepository
import com.sebas.musicplayer.data.stats.FavoriteEntity
import com.sebas.musicplayer.data.stats.PlayStatEntity
import com.sebas.musicplayer.data.stats.SongStatsDao
import com.sebas.musicplayer.playback.gain.SongGainEntity
import com.sebas.musicplayer.playback.gain.SongGainRepository
import kotlinx.coroutines.flow.first

/**
 * Exporta e importa playlists, favoritas, historial y metadatos editados.
 * Importar solo suma: nunca borra nada del teléfono, y las playlists con un nombre que ya existe se dejan igual.
 */
class BackupManager(
    private val songs: OverridingSongRepository,
    private val overrideDao: SongOverrideDao,
    private val statsDao: SongStatsDao,
    private val playlists: PlaylistRepository,
    private val gains: SongGainRepository? = null,
) {

    suspend fun export(now: Long = System.currentTimeMillis()): BackupData {
        val idToKey = songs.getOriginalSongs().associate { it.id to it.toKey() }
        return BackupData(
            exportedAt = now,
            playlists = playlists.observePlaylists().first().map { p ->
                BackupPlaylist(p.name, playlists.observeSongIds(p.id).first().mapNotNull { idToKey[it] })
            },
            favorites = statsDao.getAllFavorites().mapNotNull { f -> idToKey[f.songId]?.let { BackupFavorite(it, f.addedAt) } },
            stats = statsDao.getAllStats().mapNotNull { s -> idToKey[s.songId]?.let { BackupStat(it, s.playCount, s.lastPlayedAt) } },
            overrides = overrideDao.getAll().mapNotNull { o ->
                idToKey[o.songId]?.let { BackupOverride(it, o.title, o.artist, o.album) }
            },
            gains = gains?.getAll().orEmpty().mapNotNull { g -> idToKey[g.songId]?.let { BackupGain(it, g.gainDb) } },
        )
    }

    suspend fun import(data: BackupData): ImportReport {
        val matcher = SongMatcher(songs.getOriginalSongs())
        // Canciones distintas del respaldo que no están en este teléfono.
        val missingKeys = mutableSetOf<SongKey>()
        fun find(key: SongKey): Long? = matcher.find(key).also { if (it == null) missingKeys += key }

        // Playlists: se crean solo las que no existen por nombre.
        val existingNames = playlists.observePlaylists().first().map { it.name.trim().lowercase() }.toMutableSet()
        var added = 0
        var skipped = 0
        for (p in data.playlists) {
            if (!existingNames.add(p.name.trim().lowercase())) { skipped++; continue }
            val ids = p.songs.mapNotNull { find(it) }
            playlists.addSongs(playlists.create(p.name), ids)
            added++
        }

        val favorites = data.favorites.mapNotNull { f -> find(f.key)?.let { FavoriteEntity(it, f.addedAt) } }
        statsDao.addFavorites(favorites)

        // Historial: si ya hay datos de esa canción se queda el mayor de cada campo.
        val currentStats = statsDao.getAllStats().associateBy { it.songId }
        val stats = data.stats.mapNotNull { s ->
            val id = find(s.key) ?: return@mapNotNull null
            val old = currentStats[id]
            PlayStatEntity(id, maxOf(s.playCount, old?.playCount ?: 0), maxOf(s.lastPlayedAt, old?.lastPlayedAt ?: 0))
        }
        statsDao.upsertStats(stats)

        // Metadatos editados: lo del respaldo manda campo a campo; lo que no trae se conserva.
        val currentOverrides = overrideDao.getAll().associateBy { it.songId }
        val overrides = data.overrides.mapNotNull { o ->
            val id = find(o.key) ?: return@mapNotNull null
            val old = currentOverrides[id]
            SongOverrideEntity(id, o.title ?: old?.title, o.artist ?: old?.artist, o.album ?: old?.album)
        }
        overrideDao.upsertAll(overrides)

        // Volumen por canción: lo del respaldo manda.
        val gainRows = data.gains.mapNotNull { g -> find(g.key)?.let { SongGainEntity(it, g.gainDb) } }
        gains?.importAll(gainRows)

        return ImportReport(added, skipped, favorites.size, stats.size, overrides.size, missingKeys.size, gainRows.size)
    }
}
