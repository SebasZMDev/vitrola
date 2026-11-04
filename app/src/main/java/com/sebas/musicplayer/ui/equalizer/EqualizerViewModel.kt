package com.sebas.musicplayer.ui.equalizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.playback.effects.AudioEffects
import com.sebas.musicplayer.playback.effects.EqualizerInfo
import com.sebas.musicplayer.playback.effects.EqualizerSettings
import kotlinx.coroutines.flow.StateFlow

class EqualizerViewModel(private val audioEffects: AudioEffects) : ViewModel() {
    val info: StateFlow<EqualizerInfo?> = audioEffects.info
    val settings: StateFlow<EqualizerSettings> = audioEffects.settings

    fun setEnabled(enabled: Boolean) = audioEffects.setEnabled(enabled)
    fun usePreset(index: Int) = audioEffects.usePreset(index)
    fun setBandLevel(band: Int, levelMb: Int) = audioEffects.setBandLevel(band, levelMb)
    fun setBassBoost(strength: Int) = audioEffects.setBassBoost(strength)
    fun reset() = audioEffects.reset()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MusicApp
                EqualizerViewModel(app.container.audioEffects)
            }
        }
    }
}
