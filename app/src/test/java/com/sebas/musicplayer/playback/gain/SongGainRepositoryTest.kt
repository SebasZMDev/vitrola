package com.sebas.musicplayer.playback.gain

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap

class SongGainRepositoryTest {

    private class FakeDao(vararg initial: SongGainEntity) : SongGainDao {
        val rows = ConcurrentHashMap(initial.associateBy { it.songId })

        /** Si se pone, la siguiente escritura en lote espera aquí (para colar otra en medio). */
        @Volatile
        var gate: CompletableDeferred<Unit>? = null

        @Volatile
        var failNext = false

        override suspend fun getAll() = rows.values.sortedBy { it.songId }
        override suspend fun upsertAll(items: List<SongGainEntity>) {
            gate?.also { gate = null }?.await()
            if (failNext) {
                failNext = false
                error("sin espacio")
            }
            items.forEach { rows[it.songId] = it }
        }
        override suspend fun deleteAuto() {
            rows.values.removeIf { it.isAuto }
        }
        override suspend fun getLoudness(songId: Long): SongLoudnessEntity? = null
        override suspend fun upsertLoudness(item: SongLoudnessEntity) = Unit
    }

    private fun repo(dao: FakeDao) = SongGainRepository(dao, measure = { null })

    /** La memoria queda igual que la base. */
    private suspend fun assertSame(expected: Map<Long, Pair<Float, Boolean>>, repo: SongGainRepository, dao: FakeDao) {
        val stored = repo.getAll() // espera las escrituras pendientes
        assertEquals(expected, stored.associate { it.songId to (it.gainDb to it.isAuto) })
        assertEquals(expected.mapValues { it.value.first }, repo.gains.value)
        assertEquals(expected.size, dao.rows.size)
    }

    @Test
    fun setZeroIsStoredAsManualAndAutoNeverOverwritesIt() = runBlocking {
        val dao = FakeDao()
        val repo = repo(dao)
        repo.set(1L, 0f)
        repo.applyAuto(LevelPlan(-12f, mapOf(1L to 4f, 2L to -2f)))
        assertSame(mapOf(1L to (0f to false), 2L to (-2f to true)), repo, dao)
    }

    @Test
    fun applyAutoReplacesPreviousAutoAndRemoveAutoKeepsManual() = runBlocking {
        val dao = FakeDao(SongGainEntity(1L, 3f), SongGainEntity(2L, 1f, isAuto = true))
        val repo = repo(dao)
        repo.applyAuto(LevelPlan(-12f, mapOf(1L to -4f, 2L to -2f)))
        assertSame(mapOf(1L to (3f to false), 2L to (-2f to true)), repo, dao)
        repo.removeAuto()
        assertSame(mapOf(1L to (3f to false)), repo, dao)
    }

    @Test
    fun setDuringApplyAutoWins() = runBlocking {
        withTimeout(5_000) {
            val dao = FakeDao()
            val repo = repo(dao)
            repo.getAll()
            val gate = CompletableDeferred<Unit>()
            dao.gate = gate
            val applying = async(start = CoroutineStart.UNDISPATCHED) {
                repo.applyAuto(LevelPlan(-12f, mapOf(1L to 4f)))
            }
            repo.set(1L, -3f)
            gate.complete(Unit)
            applying.await()
            assertEquals(-3f, repo.gainOf(1L))
            assertSame(mapOf(1L to (-3f to false)), repo, dao)
        }
    }

    @Test
    fun importedGainsAreManual() = runBlocking {
        val dao = FakeDao(SongGainEntity(1L, 1f, isAuto = true))
        val repo = repo(dao)
        repo.importAll(listOf(SongGainEntity(1L, 2f, isAuto = true)))
        assertSame(mapOf(1L to (2f to false)), repo, dao)
    }

    @Test
    fun failedSetDoesNotBlockLaterWrites() = runBlocking {
        val dao = FakeDao(SongGainEntity(1L, 1f, isAuto = true))
        val repo = repo(dao)
        repo.getAll()
        dao.failNext = true
        repo.set(1L, 3f)
        repo.applyAuto(LevelPlan(-12f, mapOf(1L to -2f)))
        assertSame(mapOf(1L to (-2f to true)), repo, dao)
    }

    @Test
    fun quickSetsKeepTheLastValue() = runBlocking {
        val dao = FakeDao()
        val repo = repo(dao)
        for (i in -12..12) repo.set(1L, i / 2f)
        assertSame(mapOf(1L to (6f to false)), repo, dao)
    }
}
