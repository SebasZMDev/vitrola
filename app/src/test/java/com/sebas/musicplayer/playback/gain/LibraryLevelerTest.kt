package com.sebas.musicplayer.playback.gain

import com.sebas.musicplayer.domain.model.Song
import io.mockk.mockk
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryLevelerTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val applied = CopyOnWriteArrayList<LevelPlan>()
    private val measured = CopyOnWriteArrayList<Long>()

    @After
    fun tearDown() = scope.cancel()

    private fun song(id: Long) = Song(id, mockk(), "t$id", "a", "al", 0, 1000, null)

    private fun leveler(
        ids: List<Long> = listOf(1, 2, 3),
        manual: Set<Long> = emptySet(),
        apply: suspend (LevelPlan) -> Unit = { applied += it },
        measure: suspend (Long) -> Loudness? = { Loudness(-14f, 0.5f) },
    ) = LibraryLeveler(
        scope = scope,
        songs = { ids.map(::song) },
        measure = { measured += it.id; measure(it.id) },
        manualIds = { manual },
        apply = apply,
    )

    private suspend fun LibraryLeveler.await(predicate: (LevelingState) -> Boolean) =
        withTimeout(5000) { state.first(predicate) }

    private suspend fun LibraryLeveler.awaitEnd() =
        await { it is LevelingState.Done || it == LevelingState.Failed }

    @Test
    fun appliesOnlyAfterMeasuringAll() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val leveler = leveler(manual = setOf(2L)) { if (it == 3L) gate.await(); Loudness(-14f, 0.5f) }
        leveler.start()
        leveler.await { it == LevelingState.Running(2, 3) }
        assertTrue(applied.isEmpty())
        gate.complete(Unit)
        assertEquals(LevelingState.Done(leveled = 2, failed = 0), leveler.awaitEnd())
        assertEquals(setOf(1L, 3L), applied.single().gains.keys)
    }

    @Test
    fun cancelAppliesNothing() = runBlocking {
        val reached = CompletableDeferred<Unit>()
        val stopped = CompletableDeferred<Unit>()
        val leveler = leveler { id ->
            if (id == 2L) {
                reached.complete(Unit)
                try {
                    CompletableDeferred<Unit>().await()
                } finally {
                    stopped.complete(Unit)
                }
            }
            Loudness(-14f, 0.5f)
        }
        leveler.start()
        withTimeout(5000) { reached.await() }
        leveler.cancel()
        withTimeout(5000) { stopped.await() }
        assertEquals(LevelingState.Idle, leveler.state.value)
        assertTrue(applied.isEmpty())
        assertEquals(listOf(1L, 2L), measured.toList())
    }

    @Test
    fun unreadableSongCountsAsFailed() = runBlocking {
        val leveler = leveler {
            when (it) {
                1L -> null
                2L -> error("roto")
                else -> Loudness(-14f, 0.5f)
            }
        }
        leveler.start()
        assertEquals(LevelingState.Done(leveled = 1, failed = 2), leveler.awaitEnd())
        assertEquals(setOf(3L), applied.single().gains.keys)
    }

    @Test
    fun emptyLibraryDoesNotApply() = runBlocking {
        val leveler = leveler(ids = emptyList())
        leveler.start()
        assertEquals(LevelingState.Done(0, 0), leveler.awaitEnd())
        assertTrue(applied.isEmpty())
    }

    @Test
    fun secondStartIgnoredWhileRunning() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val leveler = leveler(ids = listOf(1L)) { gate.await(); Loudness(-14f, 0.5f) }
        leveler.start()
        leveler.start()
        gate.complete(Unit)
        leveler.awaitEnd()
        assertEquals(listOf(1L), measured.toList())
        assertEquals(1, applied.size)
    }

    @Test
    fun applyErrorEndsAsFailed() = runBlocking {
        val leveler = leveler(apply = { error("sin espacio") })
        leveler.start()
        assertEquals(LevelingState.Failed, leveler.awaitEnd())
    }

    @Test
    fun cancelWhileApplyingStillPublishesDone() = runBlocking {
        val applying = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val leveler = leveler(apply = {
            applying.complete(Unit)
            gate.await()
            applied += it
        })
        leveler.start()
        withTimeout(5000) { applying.await() }
        leveler.cancel()
        assertEquals(LevelingState.Running(3, 3), leveler.state.value)
        gate.complete(Unit)
        assertEquals(LevelingState.Done(leveled = 3, failed = 0), leveler.awaitEnd())
        assertEquals(1, applied.size)
    }

    @Test
    fun removeWhileApplyingRunsAfterApplyEnds() = runBlocking {
        val applying = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val events = CopyOnWriteArrayList<String>()
        val removed = CompletableDeferred<Unit>()
        val leveler = leveler(apply = {
            applying.complete(Unit)
            gate.await()
            events += "apply"
        })
        leveler.start()
        withTimeout(5000) { applying.await() }
        leveler.remove { events += "remove"; removed.complete(Unit) }
        leveler.start() // ignorado mientras se quita
        gate.complete(Unit)
        withTimeout(5000) { removed.await() }
        assertEquals(listOf("apply", "remove"), events.toList())
        assertEquals(LevelingState.Idle, leveler.state.value)
        assertEquals(3, measured.size)
    }
}
