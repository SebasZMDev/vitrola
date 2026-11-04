package com.sebas.musicplayer.playback

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.ui.MainActivity

class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        val activityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        player = exo
        mediaSession = MediaSession.Builder(this, exo)
            .setSessionActivity(activityIntent)
            .build()

        // ExoPlayer no asigna su sesión de audio al construirse (queda en 0 = mezcla global, que los
        // efectos no pueden usar). Generamos una propia para que el ecualizador tenga dónde engancharse.
        val audioSessionId = getSystemService(AudioManager::class.java).generateAudioSessionId()
        exo.audioSessionId = audioSessionId

        // Temporizador y ecualizador necesitan el ExoPlayer real (vive aquí, no en la UI).
        container.sleepTimer.attach(exo)
        container.audioEffects.attach(audioSessionId)
    }

    private val container get() = (application as MusicApp).container

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        container.sleepTimer.detach()
        container.audioEffects.detach()
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
