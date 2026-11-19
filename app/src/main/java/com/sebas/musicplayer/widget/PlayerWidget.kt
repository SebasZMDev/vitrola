package com.sebas.musicplayer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.os.Build
import androidx.core.graphics.ColorUtils
import com.sebas.musicplayer.ui.components.dominantColor
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.KeyEvent
import android.widget.RemoteViews
import com.sebas.musicplayer.R
import com.sebas.musicplayer.playback.PlaybackService
import com.sebas.musicplayer.ui.MainActivity
import java.util.concurrent.Executors

/** Lo que el widget necesita saber de la reproducción actual. */
data class WidgetState(
    val title: String,
    val artist: String,
    val artworkUri: Uri?,
    val isPlaying: Boolean,
)

/**
 * Widget de pantalla de inicio: portada, título, artista y anterior / play-pausa / siguiente.
 * Los botones mandan teclas multimedia al PlaybackService (que arranca solo si estaba apagado).
 * El servicio llama a [update] cada vez que cambia la canción o el estado.
 */
class PlayerWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        // Al poner el widget (o reiniciar el teléfono) mostramos lo último que sabemos.
        update(context, last, force = true)
    }

    companion object {
        @Volatile private var last: WidgetState? = null
        private val io = Executors.newSingleThreadExecutor()

        /** Última portada decodificada (solo la usa el hilo [io]): pausar/reanudar no vuelve a leer el archivo. */
        private var cachedCoverUri: Uri? = null
        private var cachedCover: Bitmap? = null

        /** [force]: redibujar aunque el estado sea igual (el sistema acaba de poner o restaurar el widget). */
        fun update(context: Context, state: WidgetState?, force: Boolean = false) {
            // El servicio avisa en cada evento del reproductor; si nada visible cambió, no hacemos nada.
            if (!force && state == last) return
            val app = context.applicationContext
            last = state
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, PlayerWidget::class.java))
            if (ids.isEmpty()) return
            io.execute {
                val uri = state?.artworkUri
                val cover = when {
                    uri == null -> null
                    uri == cachedCoverUri -> cachedCover
                    else -> loadCover(app, uri).also { cachedCoverUri = uri; cachedCover = it }
                }
                val views = build(app, state, cover)
                manager.updateAppWidget(ids, views)
            }
        }

        private fun build(context: Context, state: WidgetState?, cover: Bitmap?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_player)
            views.setTextViewText(R.id.widget_title, state?.title?.ifBlank { null } ?: "Vitrola")
            views.setTextViewText(
                R.id.widget_artist,
                state?.artist?.ifBlank { null } ?: "Toca para abrir",
            )
            if (cover != null) views.setImageViewBitmap(R.id.widget_cover, cover)
            else views.setImageViewResource(R.id.widget_cover, R.mipmap.ic_launcher)
            // Fondo teñido con el color de la carátula (oscurecido para que el texto crema se lea).
            // setBackgroundTintList solo es remoto desde Android 12; antes queda el carbón de siempre.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val tint = cover?.let(::tintFrom) ?: 0xFF0B0B0C.toInt()
                views.setColorStateList(R.id.widget_root, "setBackgroundTintList", ColorStateList.valueOf(tint))
            }
            views.setImageViewResource(
                R.id.widget_play,
                if (state?.isPlaying == true) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            )
            views.setOnClickPendingIntent(R.id.widget_prev, mediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS))
            views.setOnClickPendingIntent(R.id.widget_play, mediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            views.setOnClickPendingIntent(R.id.widget_next, mediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT))
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

        private fun tintFrom(cover: Bitmap): Int? {
            val small = Bitmap.createScaledBitmap(cover, 24, 24, true)
            val pixels = IntArray(24 * 24)
            small.getPixels(pixels, 0, 24, 0, 0, 24, 24)
            val dominant = dominantColor(pixels) ?: return null
            // 70 % hacia el carbón: se nota el tono sin perder el estilo oscuro.
            return ColorUtils.blendARGB(dominant, 0xFF0B0B0C.toInt(), 0.7f)
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
