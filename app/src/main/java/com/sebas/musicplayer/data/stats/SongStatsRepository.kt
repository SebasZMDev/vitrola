package com.sebas.musicplayer.data.stats

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Favoritas e historial de reproducción, guardados en Room. */
class SongStatsRepository(private val dao: SongStatsDao) {

    /** Ids favoritos, el más reciente primero. */
    val favoriteIds: Flow<List<Long>> = dao.observeFavoriteIds()

    /** Ids por última reproducción (la más reciente primero). */
    val recentIds: Flow<List<Long>> = dao.observeStats().map { stats ->
        stats.sortedByDescending { it.lastPlayedAt }.map { it.songId }
    }

    /** Ids por veces escuchadas (más primero); desempata por la más reciente. */
    val topIds: Flow<List<Long>> = dao.observeStats().map { stats ->
        stats.sortedWith(compareByDescending<PlayStatEntity> { it.playCount }.thenByDescending { it.lastPlayedAt })
            .map { it.songId }
    }

    suspend fun setFavorite(songId: Long, favorite: Boolean) {
        if (favorite) dao.addFavorites(listOf(FavoriteEntity(songId, System.currentTimeMillis())))
        else dao.removeFavorite(songId)
    }

    suspend fun recordPlay(songId: Long, now: Long = System.currentTimeMillis()) {
        if (dao.insertStatIfMissing(PlayStatEntity(songId, 1, now)) == -1L) dao.bumpStat(songId, now)
    }
}
