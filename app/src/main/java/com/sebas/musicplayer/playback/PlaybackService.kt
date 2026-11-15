package com.sebas.musicplayer.playback

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.playback.session.QueueStore
import com.sebas.musicplayer.ui.MainActivity

class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    private val queueStore by lazy { QueueStore(this) }

    /** Fallos seguidos al reproducir; si falla toda la cola paramos en vez de saltar en bucle. */
    private var consecutiveErrors = 0

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

        restoreQueue(exo)
        exo.addListener(persistListener)
        exo.addListener(skipBrokenListener)
        exo.addListener(reshuffleListener)
    }

    /**
     * Modo mix (aleatorio + repetir todo): ExoPlayer repetiría el mismo orden al dar la vuelta.
     * Al volver a la primera canción del orden se genera un orden nuevo.
     */
    private val reshuffleListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val exo = player ?: return
            if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) return
            if (!exo.shuffleModeEnabled || exo.repeatMode != Player.REPEAT_MODE_ALL) return
            if (exo.mediaItemCount < 3) return
            val timeline = exo.currentTimeline
            if (exo.currentMediaItemIndex == timeline.getFirstWindowIndex(true)) {
                exo.setShuffleOrder(DefaultShuffleOrder(exo.mediaItemCount))
            }
        }
    }

    private val container get() = (application as MusicApp).container

    private fun restoreQueue(exo: ExoPlayer) {
        val saved = queueStore.load() ?: return
        exo.shuffleModeEnabled = saved.shuffle
        exo.repeatMode = saved.repeatMode
        exo.setMediaItems(queueStore.toMediaItems(saved), saved.index, saved.positionMs)
        exo.prepare() // queda en pausa: no suena nada hasta que el usuario pulse play
    }

    /** Guarda la cola cuando cambia algo relevante (no en cada tick de posición). */
    private val persistListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                    Player.EVENT_REPEAT_MODE_CHANGED,
                )
            ) {
                queueStore.save(player)
            }
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED) && player.isPlaying) consecutiveErrors = 0
        }
    }

    /** Si un archivo desapareció o no se puede leer, salta a la siguiente canción. */
    private val skipBrokenListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            val exo = player ?: return
            consecutiveErrors++
            val canSkip = exo.hasNextMediaItem() && consecutiveErrors < exo.mediaItemCount
            if (canSkip) {
                exo.seekToNextMediaItem()
                // playWhenReady no cambia con un error: si estaba pausada (p. ej. cola restaurada)
                // sigue pausada, y si sonaba sigue sonando en la siguiente.
                exo.prepare()
            } else {
                exo.stop()
                consecutiveErrors = 0
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Guarda la posición exacta al cerrar la app desde recientes; si no suena, apaga el servicio.
        player?.let { queueStore.save(it) }
        if (player?.playWhenReady != true) stopSelf()
    }

    override fun onDestroy() {
        player?.let { queueStore.save(it) }
        container.sleepTimer.detach()
        container.audioEffects.detach()
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
