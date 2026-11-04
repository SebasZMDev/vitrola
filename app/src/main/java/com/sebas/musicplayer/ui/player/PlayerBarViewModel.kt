package com.sebas.musicplayer.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.playback.PlayerController
import com.sebas.musicplayer.playback.PlayerState
import kotlinx.coroutines.flow.StateFlow

/** Estado del mini reproductor global (vive a nivel de Activity, fuera del NavHost). */
class PlayerBarViewModel(private val playerController: PlayerController) : ViewModel() {
    val playerState: StateFlow<PlayerState> = playerController.state

    fun togglePlayPause() = playerController.togglePlayPause()
    fun next() = playerController.next()
    fun previous() = playerController.previous()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MusicApp
                PlayerBarViewModel(app.container.playerController)
            }
        }
    }
}
