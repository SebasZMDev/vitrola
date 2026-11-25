package com.sebas.musicplayer.playback.gain

import com.sebas.musicplayer.domain.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/** Estado de "nivelar la biblioteca". En [Running], total 0 = todavía se está leyendo la lista. */
sealed interface LevelingState {
    data object Idle : LevelingState
    data class Running(val done: Int, val total: Int) : LevelingState
    data class Done(val leveled: Int, val failed: Int) : LevelingState
    data object Failed : LevelingState
}

/**
 * Mide las canciones de a una y aplica el nivelado solo al terminar.
 * Vive en [scope] (el de la app), así sigue aunque se cierre la pantalla.
 */
class LibraryLeveler(
    private val scope: CoroutineScope,
    private val songs: suspend () -> List<Song>,
    private val measure: suspend (Song) -> Loudness?,
    private val manualIds: suspend () -> Set<Long>,
    private val apply: suspend (LevelPlan) -> Unit,
) {
    private val _state = MutableStateFlow<LevelingState>(LevelingState.Idle)
    val state: StateFlow<LevelingState> = _state.asStateFlow()

    private val lock = Any()

    /** Última tarea lanzada; la siguiente espera a que termine para no cruzarse. */
    private var job: Job? = null
    private var removing = false

    /** La tarea actual ya está aplicando: no se puede cancelar. */
    private var applying = false

    /** Sube con cada tarea o cancelación: una tarea vieja ya no puede cambiar el estado. */
    private var generation = 0

    /** No hace nada si ya está nivelando o quitando el nivelado. */
    fun start() = synchronized(lock) {
        if (_state.value is LevelingState.Running || (removing && job?.isCompleted == false)) return
        val mine = ++generation
        removing = false
        applying = false
        _state.value = LevelingState.Running(0, 0)
        launchAfterPrevious { run(mine) }
    }

    /** Detiene la medición; no se aplica nada. Si ya se está aplicando no hace nada: termina y publica el resultado. */
    fun cancel() = synchronized(lock) {
        if (removing || applying) return
        generation++
        job?.cancel()
        _state.value = LevelingState.Idle
    }

    /** Cancela lo que esté corriendo, espera a que pare y recién entonces ejecuta [block] (quitar el nivelado). */
    fun remove(block: suspend () -> Unit) = synchronized(lock) {
        val mine = ++generation
        // Un quitado anterior aún en curso no se corta.
        if (!removing) job?.cancel()
        removing = true
        applying = false
        _state.value = LevelingState.Idle
        launchAfterPrevious {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                publish(mine, LevelingState.Failed)
            }
        }
    }

    /**
     * Lanza [block] cuando termine la tarea anterior. Aunque la cancelen mientras espera, no termina antes
     * que aquella (por eso UNDISPATCHED y NonCancellable).
     */
    private fun launchAfterPrevious(block: suspend () -> Unit) {
        val previous = job
        job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) { previous?.join() }
            yield() // sale del hilo que llamó y, si la cancelaron, se detiene aquí
            block()
        }
    }

    private suspend fun run(mine: Int) {
        try {
            val list = songs()
            publish(mine, LevelingState.Running(0, list.size))
            val measures = HashMap<Long, Loudness>()
            var failed = 0
            list.forEachIndexed { index, song ->
                currentCoroutineContext().ensureActive()
                val loudness = try {
                    measure(song)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
                if (loudness == null) failed++ else measures[song.id] = loudness
                publish(mine, LevelingState.Running(index + 1, list.size))
            }
            val plan = planLevel(measures, manualIds())
            val context = currentCoroutineContext()
            // Bajo el candado: o [cancel] llegó antes y se detiene aquí, o ya no puede cancelar.
            synchronized(lock) {
                context.ensureActive()
                if (plan != null && mine == generation) applying = true
            }
            // Ganancias y objetivo van juntos: una vez empezado no se corta.
            if (plan != null) withContext(NonCancellable) { apply(plan) }
            publish(mine, LevelingState.Done(plan?.gains?.size ?: 0, failed))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            publish(mine, LevelingState.Failed)
        }
    }

    private fun publish(mine: Int, value: LevelingState) = synchronized(lock) {
        if (mine != generation) return@synchronized
        if (value !is LevelingState.Running) applying = false
        _state.value = value
    }
}
