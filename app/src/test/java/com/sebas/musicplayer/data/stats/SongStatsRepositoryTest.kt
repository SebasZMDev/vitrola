package com.sebas.musicplayer.data.stats

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SongStatsRepositoryTest {

    private class FakeDao : SongStatsDao {
        val favorites = MutableStateFlow<List<FavoriteEntity>>(emptyList())
        val stats = MutableStateFlow<Map<Long, PlayStatEntity>>(emptyMap())

        override fun observeFavoriteIds(): Flow<List<Long>> =
            favorites.map { list -> list.sortedByDescending { it.addedAt }.map { it.songId } }

        override suspend fun addFavorites(items: List<FavoriteEntity>) {
            favorites.value = favorites.value.filter { f -> items.none { it.songId == f.songId } } + items
        }

        override suspend fun removeFavorite(songId: Long) { favorites.value = favorites.value.filter { it.songId != songId } }
        override fun observeStats(): Flow<List<PlayStatEntity>> = stats.map { it.values.toList() }

        override suspend fun insertStatIfMissing(stat: PlayStatEntity): Long {
            if (stat.songId in stats.value) return -1L
            stats.value = stats.value + (stat.songId to stat)
            return 1L
        }

        override suspend fun bumpStat(songId: Long, now: Long) {
            val old = stats.value.getValue(songId)
            stats.value = stats.value + (songId to old.copy(playCount = old.playCount + 1, lastPlayedAt = now))
        }

        override suspend fun getAllFavorites() = favorites.value
        override suspend fun getAllStats() = stats.value.values.toList()
        override suspend fun upsertStats(items: List<PlayStatEntity>) { stats.value = stats.value + items.associateBy { it.songId } }
        override suspend fun removeFavorites(ids: List<Long>) { favorites.value = favorites.value.filter { it.songId !in ids } }
        override suspend fun removeStats(ids: List<Long>) { stats.value = stats.value - ids.toSet() }
    }

    @Test
    fun `la primera reproduccion crea el registro y las siguientes suman`() = runBlocking {
        val repo = SongStatsRepository(FakeDao())
        repo.recordPlay(7, now = 100)
        repo.recordPlay(7, now = 200)
        repo.recordPlay(7, now = 300)
        assertEquals(listOf(7L), repo.topIds.first())
    }

    @Test
    fun `recientes ordena por la ultima reproduccion`() = runBlocking {
        val repo = SongStatsRepository(FakeDao())
        repo.recordPlay(1, now = 100)
        repo.recordPlay(2, now = 200)
        repo.recordPlay(1, now = 300)
        assertEquals(listOf(1L, 2L), repo.recentIds.first())
    }

    @Test
    fun `mas escuchadas ordena por veces y desempata por la mas reciente`() = runBlocking {
        val dao = FakeDao()
        val repo = SongStatsRepository(dao)
        repo.recordPlay(1, now = 100)
        repo.recordPlay(2, now = 200)
        repo.recordPlay(2, now = 300)
        repo.recordPlay(3, now = 400)
        // 2 tiene 2 veces; 1 y 3 tienen 1 y gana la más reciente (3).
        assertEquals(listOf(2L, 3L, 1L), repo.topIds.first())
    }

    @Test
    fun `favoritas con la mas reciente primero y se pueden quitar`() = runBlocking {
        val repo = SongStatsRepository(FakeDao())
        repo.setFavorite(1, true)
        Thread.sleep(2)
        repo.setFavorite(2, true)
        assertEquals(listOf(2L, 1L), repo.favoriteIds.first())
        repo.setFavorite(2, false)
        assertEquals(listOf(1L), repo.favoriteIds.first())
    }
}
