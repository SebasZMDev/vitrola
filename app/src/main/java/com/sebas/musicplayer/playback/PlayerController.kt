package com.sebas.musicplayer.playback

import com.sebas.musicplayer.domain.model.Song
import kotlinx.coroutines.flow.StateFlow

/**
 * Lo que la UI puede hacer con el reproductor y lo que puede observar.
 * La UI depende de esta interfaz, nunca de Media3 directamente.
 * CONTRATO: no cambiar sin avisar al orquestador.
 */
interface PlayerController {
    val state: StateFlow<PlayerState>

    /** Reemplaza la cola por [songs] y empieza a reproducir desde [startIndex]. */
    fun playQueue(songs: List<Song>, startIndex: Int)
    fun togglePlayPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun toggleShuffle()
    /** OFF -> ALL -> ONE -> OFF */
    fun cycleRepeatMode()
}

data class PlayerState(
    val isConnected: Boolean = false,
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = -1,
)

enum class RepeatMode { OFF, ALL, ONE }
