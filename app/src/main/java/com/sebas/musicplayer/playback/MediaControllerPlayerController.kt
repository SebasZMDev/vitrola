package com.sebas.musicplayer.playback

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.sebas.musicplayer.domain.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

/**
 * Puente entre la UI y el MediaController. Conecta al crearse; los comandos
 * se ejecutan dentro de una coroutine que espera al future de conexión,
 * así nunca fallan por "no conectado" y mantienen su orden.
 */
class MediaControllerPlayerController(context: Context) : PlayerController {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var positionJob: Job? = null

    /** Conexión en curso (si la hay); evita crear dos futures a la vez. */
    private var connecting: Deferred<MediaController>? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
            updatePositionLoop(player)
        }
    }

    private val controllerListener = object : MediaController.Listener {
        override fun onDisconnected(controller: MediaController) {
            // Ignora desconexiones de controllers ya descartados.
            if (controller === this@MediaControllerPlayerController.controller) {
                dropController()
            }
        }
    }

    init {
        // Conecta al crearse para reflejar un servicio que ya estaba sonando.
        scope.launch { connectedController() }
    }

    /** Libera el controller actual y publica el estado desconectado. */
    private fun dropController() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        positionJob?.cancel()
        positionJob = null
        _state.value = PlayerState()
    }

    /** Devuelve un controller conectado, reconectando si hace falta; null si falla. */
    private suspend fun connectedController(): MediaController? {
        controller?.let { if (it.isConnected) return it }
        if (controller != null) dropController()
        // takeIf(isActive): nunca reutilizar un intento ya terminado (apuntaría a un controller liberado).
        val pending = connecting?.takeIf { it.isActive } ?: scope.async { connect() }.also { connecting = it }
        return try {
            pending.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun connect(): MediaController {
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token)
            .setListener(controllerListener)
            .buildAsync()
        try {
            val c = future.await()
            controller = c
            c.addListener(listener)
            publish(c)
            updatePositionLoop(c)
            return c
        } catch (e: CancellationException) {
            future.cancel(false)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo conectar con PlaybackService", e)
            _state.value = PlayerState()
            throw e
        } finally {
            connecting = null
        }
    }

    /** Ejecuta [action] cuando el controller esté listo (en orden de llamada). */
    private fun withController(action: (MediaController) -> Unit) {
        scope.launch {
            connectedController()?.let(action)
        }
    }

    private fun publish(player: Player) {
        val duration = player.duration
        val items = List(player.mediaItemCount) { player.getMediaItemAt(it) }
        _state.value = PlayerState(
            isConnected = true,
            currentSong = player.currentMediaItem?.toSong(),
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = if (duration == C.TIME_UNSET || duration < 0) 0L else duration,
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode.toRepeatMode(),
            queue = items.mapNotNull { it.toSong() },
            currentIndex = if (items.isEmpty()) -1 else player.currentMediaItemIndex,
        )
    }

    private fun updatePositionLoop(player: Player) {
        if (player.isPlaying) {
            if (positionJob?.isActive == true) return
            positionJob = scope.launch {
                while (true) {
                    _state.value = _state.value.copy(
                        positionMs = player.currentPosition.coerceAtLeast(0L),
                    )
                    delay(POSITION_INTERVAL_MS)
                }
            }
        } else {
            positionJob?.cancel()
            positionJob = null
        }
    }

    override fun playQueue(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        withController { c ->
            c.setMediaItems(songs.map { it.toMediaItem() }, startIndex.coerceIn(0, songs.lastIndex), 0L)
            c.prepare()
            c.play()
        }
    }

    override fun togglePlayPause() = withController { c ->
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition(0)
            c.play()
        }
    }

    override fun next() = withController { it.seekToNext() }
    override fun previous() = withController { it.seekToPrevious() }
    override fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs) }

    override fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    override fun cycleRepeatMode() = withController {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    override fun skipToQueueItem(index: Int) = withController { c ->
        if (index !in 0 until c.mediaItemCount) return@withController
        c.seekToDefaultPosition(index)
        c.play()
    }

    override fun playNext(song: Song) = withController { c ->
        if (c.mediaItemCount == 0) {
            startWith(c, song)
        } else {
            c.addMediaItem(c.currentMediaItemIndex + 1, song.toMediaItem())
        }
    }

    override fun addToQueue(song: Song) = withController { c ->
        if (c.mediaItemCount == 0) startWith(c, song) else c.addMediaItem(song.toMediaItem())
    }

    override fun removeFromQueue(index: Int) = withController { c ->
        if (index in 0 until c.mediaItemCount) c.removeMediaItem(index)
    }

    override fun moveQueueItem(from: Int, to: Int) = withController { c ->
        val count = c.mediaItemCount
        if (from in 0 until count && to in 0 until count && from != to) c.moveMediaItem(from, to)
    }

    /** Cola vacía: la canción pasa a ser la cola entera y empieza a sonar. */
    private fun startWith(c: MediaController, song: Song) {
        c.setMediaItem(song.toMediaItem())
        c.prepare()
        c.play()
    }

    private fun Int.toRepeatMode() = when (this) {
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        Player.REPEAT_MODE_ONE -> RepeatMode.ONE
        else -> RepeatMode.OFF
    }

    private companion object {
        const val TAG = "PlayerController"
        const val POSITION_INTERVAL_MS = 500L
    }
}
