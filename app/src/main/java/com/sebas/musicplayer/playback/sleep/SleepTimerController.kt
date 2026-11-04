package com.sebas.musicplayer.playback.sleep

import android.os.SystemClock
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SleepTimerController : SleepTimer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<SleepTimerState>(SleepTimerState.Off)
    override val state: StateFlow<SleepTimerState> = _state

    private var player: ExoPlayer? = null
    private var countdownJob: Job? = null

    private val endOfSongListener = object : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) finishEndOfSong()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) finishEndOfSong()
        }
    }

    override fun startCountdown(durationMs: Long) {
        clear()
        val duration = durationMs.coerceAtLeast(0L)
        val endsAt = SystemClock.elapsedRealtime() + duration
        _state.value = SleepTimerState.Countdown(endsAt, duration)
        countdownJob = scope.launch {
            val fadeMs = minOf(FADE_MS, duration)
            delay((duration - fadeMs).coerceAtLeast(0L))
            if (player?.isPlaying == true) fadeOut(endsAt)
            val remaining = endsAt - SystemClock.elapsedRealtime()
            if (remaining > 0) delay(remaining)
            player?.let {
                it.pause()
                it.volume = 1f
            }
            _state.value = SleepTimerState.Off
        }
    }

    private suspend fun fadeOut(endsAt: Long) {
        val fadeMs = (endsAt - SystemClock.elapsedRealtime()).coerceAtLeast(1L)
        while (true) {
            val p = player ?: return
            val left = endsAt - SystemClock.elapsedRealtime()
            if (left <= 0) break
            p.volume = (left.toFloat() / fadeMs).coerceIn(0f, 1f)
            delay(FADE_STEP_MS)
        }
        player?.volume = 0f
    }

    override fun startEndOfSong() {
        clear()
        _state.value = SleepTimerState.EndOfSong
        player?.let(::armEndOfSong)
    }

    override fun cancel() {
        clear()
        _state.value = SleepTimerState.Off
    }

    override fun attach(player: ExoPlayer) {
        this.player?.let { releasePlayer(it) }
        this.player = player
        if (_state.value == SleepTimerState.EndOfSong) armEndOfSong(player)
    }

    override fun detach() {
        player?.let { releasePlayer(it) }
        player = null
    }

    private fun armEndOfSong(p: ExoPlayer) {
        p.removeListener(endOfSongListener)
        p.addListener(endOfSongListener)
        p.pauseAtEndOfMediaItems = true
    }

    private fun finishEndOfSong() {
        if (_state.value != SleepTimerState.EndOfSong) return
        player?.let { releasePlayer(it) }
        _state.value = SleepTimerState.Off
    }

    private fun releasePlayer(p: ExoPlayer) {
        p.removeListener(endOfSongListener)
        p.pauseAtEndOfMediaItems = false
        p.volume = 1f
    }

    private fun clear() {
        countdownJob?.cancel()
        countdownJob = null
        player?.let { releasePlayer(it) }
    }

    private companion object {
        const val FADE_MS = 10_000L
        const val FADE_STEP_MS = 100L
    }
}
