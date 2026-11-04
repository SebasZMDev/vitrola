package com.sebas.musicplayer.playback.sleep

import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.StateFlow

/**
 * Temporizador para dormir. Singleton de la app (AppContainer).
 * La UI lo controla; PlaybackService le entrega su ExoPlayer con [attach]/[detach]
 * para que pueda pausar (y bajar el volumen gradualmente) aunque la app esté en segundo plano.
 * CONTRATO: no cambiar sin avisar al orquestador.
 */
interface SleepTimer {
    val state: StateFlow<SleepTimerState>

    /** Pausa cuando pasen [durationMs]. Reemplaza cualquier temporizador activo. */
    fun startCountdown(durationMs: Long)

    /** Pausa al terminar la canción actual. Reemplaza cualquier temporizador activo. */
    fun startEndOfSong()

    fun cancel()

    /** Lo llama PlaybackService.onCreate. */
    fun attach(player: ExoPlayer)

    /** Lo llama PlaybackService.onDestroy, antes de liberar el player. */
    fun detach()
}

sealed interface SleepTimerState {
    data object Off : SleepTimerState

    /** [endsAtElapsedRealtime] es en el reloj de `SystemClock.elapsedRealtime()`. */
    data class Countdown(val endsAtElapsedRealtime: Long, val totalMs: Long) : SleepTimerState

    data object EndOfSong : SleepTimerState
}
