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
import android.os.Bundle
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import androidx.media3.session.MediaSessionService
import com.sebas.musicplayer.MusicApp
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.sebas.musicplayer.playback.gain.GainAudioProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.sebas.musicplayer.playback.session.QueueStore
import com.sebas.musicplayer.ui.MainActivity
import com.sebas.musicplayer.widget.PlayerWidget
import com.sebas.musicplayer.widget.WidgetState

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    /** Volumen propio de cada canción: va dentro de la cadena de audio (puede subir, no solo bajar). */
    private val gainProcessor = GainAudioProcessor()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    private val queueStore by lazy { QueueStore(this) }

    /** Fallos seguidos al reproducir; si falla toda la cola paramos en vez de saltar en bucle. */
    private var consecutiveErrors = 0

    override fun onCreate() {
        super.onCreate()
        val renderers = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(arrayOf(gainProcessor))
                .build()
        }
        val exo = ExoPlayer.Builder(this, renderers)
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
            .setCallback(sessionCallback)
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
        exo.addListener(modeButtonListener)
        exo.addListener(widgetListener)
        exo.addListener(gainListener)
        pushWidget(exo)
        // Si se cambia el volumen de la canción que suena (o terminan de cargar), se oye al momento.
        scope.launch { container.songGains.gains.collect { applyGain(exo) } }
        scope.launch {
            container.statsRepository.favoriteIds.collect { favoriteIds = it.toSet(); pushWidget(exo) }
        }
        // La barra del widget avanza cada 15 s mientras suena (el tiempo en números corre solo).
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(15_000)
                if (exo.isPlaying) pushWidget(exo)
            }
        }
        mediaSession?.setCustomLayout(ImmutableList.of(modeButton(exo)))
    }

    private val gainListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            player?.let(::applyGain)
        }
    }

    private fun applyGain(p: Player) {
        val id = p.currentMediaItem?.mediaId?.toLongOrNull()
        gainProcessor.gainDb = if (id == null) 0f else container.songGains.gainOf(id)
    }

    /** Mantiene el widget de inicio al día con la canción y el estado play/pausa. */
    private val widgetListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_MEDIA_METADATA_CHANGED,
                    Player.EVENT_IS_PLAYING_CHANGED,
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_POSITION_DISCONTINUITY,
                    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                )
            ) pushWidget(player)
        }
    }

    /** Favoritas (para el corazón del widget); se mantiene al día desde la base. */
    @Volatile private var favoriteIds: Set<Long> = emptySet()

    private fun pushWidget(p: Player) {
        val item = p.currentMediaItem
        val md = item?.mediaMetadata
        val id = item?.mediaId?.toLongOrNull()
        val state = if (md == null || id == null) null else {
            val nextIndex = p.nextMediaItemIndex
            WidgetState(
                songId = id,
                title = md.title?.toString().orEmpty(),
                artist = md.artist?.toString().orEmpty(),
                artworkUri = md.artworkUri,
                isPlaying = p.isPlaying,
                positionMs = p.currentPosition,
                durationMs = p.duration.takeIf { it > 0 } ?: md.durationMs ?: 0L,
                track = p.currentMediaItemIndex + 1,
                trackCount = p.mediaItemCount,
                nextTitle = if (nextIndex == C.INDEX_UNSET) null
                else p.getMediaItemAt(nextIndex).mediaMetadata.title?.toString(),
                isFavorite = id in favoriteIds,
            )
        }
        PlayerWidget.update(this, state)
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


    // --- Botón de modo en la notificación: normal -> repetir una -> mix (igual que en la app). ---
    private val cycleModeCommand = SessionCommand("zm.cycle_mode", Bundle.EMPTY)

    private fun modeButton(exo: Player): CommandButton {
        val (icon, name) = when {
            exo.shuffleModeEnabled -> CommandButton.ICON_SHUFFLE_ON to "Mix: aleatorio sin parar"
            exo.repeatMode == Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE to "Repetir una canción"
            else -> CommandButton.ICON_REPEAT_OFF to "Reproducción normal"
        }
        return CommandButton.Builder(icon)
            .setDisplayName(name)
            .setSessionCommand(cycleModeCommand)
            .build()
    }

    private val modeButtonListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED, Player.EVENT_REPEAT_MODE_CHANGED)) {
                mediaSession?.setCustomLayout(ImmutableList.of(modeButton(player)))
            }
        }
    }

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(cycleModeCommand)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .setCustomLayout(ImmutableList.of(modeButton(session.player)))
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == cycleModeCommand.customAction) {
                val p = session.player
                val repeatOne = p.repeatMode == Player.REPEAT_MODE_ONE && !p.shuffleModeEnabled
                when {
                    repeatOne -> { p.shuffleModeEnabled = true; p.repeatMode = Player.REPEAT_MODE_ALL }
                    p.shuffleModeEnabled -> { p.shuffleModeEnabled = false; p.repeatMode = Player.REPEAT_MODE_OFF }
                    else -> p.repeatMode = Player.REPEAT_MODE_ONE
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
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
        scope.cancel()
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
