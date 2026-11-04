package com.sebas.musicplayer.ui.sleeptimer

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.playback.sleep.SleepTimer
import com.sebas.musicplayer.playback.sleep.SleepTimerState
import kotlinx.coroutines.flow.StateFlow

class SleepTimerViewModel(private val timer: SleepTimer) : ViewModel() {
    val state: StateFlow<SleepTimerState> = timer.state

    fun startCountdown(minutes: Int) = timer.startCountdown(minutes * 60_000L)
    fun startEndOfSong() = timer.startEndOfSong()
    fun cancel() = timer.cancel()

    fun addMinutes(minutes: Int) {
        val current = timer.state.value as? SleepTimerState.Countdown ?: return
        val remaining = (current.endsAtElapsedRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        timer.startCountdown(remaining + minutes * 60_000L)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MusicApp
                SleepTimerViewModel(app.container.sleepTimer)
            }
        }
    }
}
