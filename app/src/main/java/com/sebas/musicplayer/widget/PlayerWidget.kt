package com.sebas.musicplayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.SystemClock
import android.text.format.DateUtils
import android.view.KeyEvent
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.R
import com.sebas.musicplayer.playback.PlaybackService
import com.sebas.musicplayer.ui.MainActivity
import com.sebas.musicplayer.ui.components.dominantColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/** Lo que el widget necesita saber de la reproducción actual. */
data class WidgetState(
    val songId: Long,
    val title: String,
    val artist: String,
    val artworkUri: Uri?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    /** Posición en la cola (desde 1) y largo de la cola. */
    val track: Int,
    val trackCount: Int,
    val nextTitle: String?,
    val isFavorite: Boolean,
)

/** Mientras la canción suena solo cambia la posición: basta con mover la barra. */
internal fun onlyPositionChanged(old: WidgetState?, new: WidgetState?): Boolean =
    old != null && new != null && old.copy(positionMs = new.positionMs) == new

/**
 * Widget de pantalla de inicio: cabecera "LADO A · PISTA 03 DE 12", portada, título, artista, lo que sigue,
 * tiempo (corre solo con un Chronometer, sin redibujar), barra de avance, favorita y controles.
 * Los botones mandan teclas multimedia al PlaybackService (que arranca solo si estaba apagado).
 * El servicio llama a [update] cuando cambia algo visible.
 */
class PlayerWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        // Al poner el widget (o reiniciar el teléfono) mostramos lo último que sabemos.
        update(context, last, force = true)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_FAVORITE) {
            val id = intent.getLongExtra(EXTRA_SONG_ID, -1L)
            val make = intent.getBooleanExtra(EXTRA_MAKE_FAVORITE, true)
            if (id >= 0) {
                // Se ve al instante; la base se escribe aparte (y el servicio confirma al recibir el cambio).
                last?.takeIf { it.songId == id }?.let { update(context, it.copy(isFavorite = make)) }
                val pending = goAsync()
                val stats = (context.applicationContext as MusicApp).container.statsRepository
                bg.launch {
                    try { stats.setFavorite(id, make) } finally { pending.finish() }
                }
            }
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        private const val ACTION_FAVORITE = "com.sebas.musicplayer.widget.FAVORITE"
        private const val EXTRA_SONG_ID = "songId"
        private const val EXTRA_MAKE_FAVORITE = "make"

        @Volatile private var last: WidgetState? = null
        private val io = Executors.newSingleThreadExecutor()
        private val bg = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Funda ya dibujada de la última canción (solo la usa el hilo [io]): pausar/reanudar no la repite. */
        private var cachedCoverUri: Uri? = null
        private var cachedArt: Bitmap? = null
        private var cachedTint: Int? = null

        /** [force]: redibujar aunque el estado sea igual (el sistema acaba de poner o restaurar el widget). */
        fun update(context: Context, state: WidgetState?, force: Boolean = false) {
            // El servicio avisa en cada evento del reproductor; si nada visible cambió, no hacemos nada.
            if (!force && state == last) return
            val app = context.applicationContext
            val progressOnly = !force && onlyPositionChanged(last, state)
            last = state
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, PlayerWidget::class.java))
            if (ids.isEmpty()) return
            io.execute {
                if (progressOnly && state != null) {
                    // Sin reenviar la carátula ni el resto del widget.
                    val views = RemoteViews(app.packageName, R.layout.widget_player)
                    bindProgress(views, state)
                    manager.partiallyUpdateAppWidget(ids, views)
                    return@execute
                }
                val uri = state?.artworkUri
                val art = cachedArt?.takeIf { uri == cachedCoverUri } ?: run {
                    val cover = uri?.let { loadCover(app, it) }
                    cachedTint = cover?.let(::tintFrom)
                    cachedCoverUri = uri
                    sleeveArt(cover).also { cachedArt = it }
                }
                manager.updateAppWidget(ids, build(app, state, art, cachedTint))
            }
        }

        private fun bindProgress(views: RemoteViews, state: WidgetState?) {
            // Tiempo: el Chronometer cuenta solo mientras suena, el widget no se redibuja cada segundo.
            val pos = state?.positionMs ?: 0L
            val dur = state?.durationMs ?: 0L
            val playing = state?.isPlaying == true
            views.setChronometer(R.id.widget_elapsed, SystemClock.elapsedRealtime() - pos, null, playing)
            // En pausa va un texto fijo: un Chronometer detenido se recalcula cada vez que el launcher redibuja.
            views.setTextViewText(R.id.widget_paused, DateUtils.formatElapsedTime(pos / 1000))
            views.setViewVisibility(R.id.widget_elapsed, if (playing) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_paused, if (playing) View.GONE else View.VISIBLE)
            views.setProgressBar(R.id.widget_progress, 1000, if (dur > 0) (pos * 1000 / dur).toInt() else 0, false)
        }

        private fun build(context: Context, state: WidgetState?, art: Bitmap, tint: Int?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_player)
            views.setTextViewText(R.id.widget_title, state?.title?.ifBlank { null } ?: "Vitrola")
            views.setTextViewText(R.id.widget_artist, state?.artist?.ifBlank { null } ?: "Toca para abrir")
            views.setTextViewText(R.id.widget_header, header(state))

            val next = state?.nextTitle
            views.setViewVisibility(R.id.widget_next_up, if (next.isNullOrBlank()) View.GONE else View.VISIBLE)
            if (!next.isNullOrBlank()) views.setTextViewText(R.id.widget_next_up, "Sigue · $next")

            views.setImageViewBitmap(R.id.widget_cover, art)
            // Fondo teñido con el color de la carátula (oscurecido para que el texto crema se lea).
            views.setInt(R.id.widget_tint, "setColorFilter", tint ?: 0xFF0B0B0C.toInt())

            val dur = state?.durationMs ?: 0L
            views.setTextViewText(R.id.widget_total, if (dur > 0) formatTime(dur) else "")
            bindProgress(views, state)

            views.setImageViewResource(
                R.id.widget_play,
                if (state?.isPlaying == true) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            )
            views.setOnClickPendingIntent(R.id.widget_prev, mediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
            views.setOnClickPendingIntent(R.id.widget_play, mediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            views.setOnClickPendingIntent(R.id.widget_next, mediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT))

            if (state == null) {
                views.setViewVisibility(R.id.widget_fav, View.INVISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_fav, View.VISIBLE)
                views.setImageViewResource(
                    R.id.widget_fav,
                    if (state.isFavorite) R.drawable.ic_widget_fav else R.drawable.ic_widget_fav_off,
                )
                val fav = Intent(context, PlayerWidget::class.java)
                    .setAction(ACTION_FAVORITE)
                    .putExtra(EXTRA_SONG_ID, state.songId)
                    .putExtra(EXTRA_MAKE_FAVORITE, !state.isFavorite)
                views.setOnClickPendingIntent(
                    R.id.widget_fav,
                    PendingIntent.getBroadcast(context, 1, fav, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
                )
            }

            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_cover, open)
            views.setOnClickPendingIntent(R.id.widget_title, open)
            views.setOnClickPendingIntent(R.id.widget_artist, open)
            return views
        }

        /** "VITROLA · LADO A · PISTA 03 DE 12" (lado A impares, B pares, como en el reproductor). */
        private fun header(state: WidgetState?): String {
            if (state == null || state.track <= 0) return "VITROLA"
            val side = if (state.track % 2 == 1) "A" else "B"
            return "VITROLA · LADO $side · PISTA %02d DE %d".format(state.track, state.trackCount)
        }

        private fun formatTime(ms: Long): String {
            val s = ms / 1000
            return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
        }

        private fun tintFrom(cover: Bitmap): Int? {
            val small = Bitmap.createScaledBitmap(cover, 24, 24, true)
            val pixels = IntArray(24 * 24)
            small.getPixels(pixels, 0, 24, 0, 0, 24, 24)
            val dominant = dominantColor(pixels) ?: return null
            // 55 % hacia el carbón; el degradado del fondo termina de oscurecer el lado del texto.
            return ColorUtils.blendARGB(dominant, 0xFF0B0B0C.toInt(), 0.55f)
        }

        private fun mediaKey(context: Context, keyCode: Int): PendingIntent {
            val intent = Intent(context, PlaybackService::class.java)
                .setAction(Intent.ACTION_MEDIA_BUTTON)
                .putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            return PendingIntent.getForegroundService(
                context,
                keyCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        /** Carátula reducida (el widget viaja por binder: un bitmap grande lo rompe). */
        private fun loadCover(context: Context, uri: Uri): Bitmap? = runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 300) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        }.getOrNull()
    }
}
