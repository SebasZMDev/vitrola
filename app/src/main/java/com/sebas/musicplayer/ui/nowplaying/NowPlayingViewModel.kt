package com.sebas.musicplayer.ui.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.data.settings.AppSettings
import com.sebas.musicplayer.data.settings.PlayerAppearance
import com.sebas.musicplayer.playback.PlayerController
import com.sebas.musicplayer.playback.PlayerState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class NowPlayingViewModel(
    private val playerController: PlayerController,
    settings: AppSettings,
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = playerController.state

    /** Estilo, fondo y opciones del vinilo elegidos en Configuración. */
    val appearance: StateFlow<PlayerAppearance> = settings.state
        .map { it.appearance }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, settings.current.appearance)

    fun togglePlayPause() = playerController.togglePlayPause()
    fun next() = playerController.next()
    fun previous() = playerController.previous()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun toggleShuffle() = playerController.toggleShuffle()
    fun cycleRepeatMode() = playerController.cycleRepeatMode()
    fun skipToQueueItem(index: Int) = playerController.skipToQueueItem(index)
    fun removeFromQueue(index: Int) = playerController.removeFromQueue(index)
    fun moveQueueItem(from: Int, to: Int) = playerController.moveQueueItem(from, to)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MusicApp
                NowPlayingViewModel(app.container.playerController, app.container.settings)
            }
        }
    }
}
