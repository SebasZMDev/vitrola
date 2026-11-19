package com.sebas.musicplayer.playback.session

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.playback.toMediaItem
import com.sebas.musicplayer.playback.toSong
import org.json.JSONArray
import org.json.JSONObject

/** Cola guardada: canciones, posición actual y modos. */
data class SavedQueue(
    val songs: List<Song>,
    val index: Int,
    val positionMs: Long,
    val shuffle: Boolean,
    val repeatMode: Int,
)

/**
 * Guarda la cola en SharedPreferences como JSON para restaurarla al reabrir la app.
 * Solo guarda los datos de las canciones (no el archivo); si una canción ya no existe,
 * el reproductor la salta al intentar reproducirla.
 */
class QueueStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Huella (ids en orden) de la cola ya escrita en disco; null = aún no escribimos nada. */
    private var savedSignature: Long? = null

    fun save(player: Player) {
        val count = player.mediaItemCount
        if (count == 0) {
            prefs.edit().remove(KEY_QUEUE).apply()
            savedSignature = null
            return
        }
        // Lo barato primero: una huella de los ids. Si la cola es la misma que ya escribimos
        // (play/pausa, cambio de canción, modos) solo actualizamos los datos pequeños y nos
        // saltamos reconstruir y escribir el JSON entero (miles de canciones = decenas de ms).
        var signature = count.toLong()
        for (i in 0 until count) signature = signature * 31 + player.getMediaItemAt(i).mediaId.hashCode()
        val edit = prefs.edit()
        if (signature != savedSignature) {
            val songs = (0 until count).mapNotNull { player.getMediaItemAt(it).toSong() }
            // Si algún item no se pudo convertir, los índices cambian: no guardamos una cola inconsistente.
            if (songs.size != count) return
            val array = JSONArray()
            songs.forEach { array.put(it.toJson()) }
            edit.putString(KEY_QUEUE, array.toString())
            savedSignature = signature
        }
        edit.putInt(KEY_INDEX, player.currentMediaItemIndex)
            .putLong(KEY_POSITION, player.currentPosition.coerceAtLeast(0L))
            .putBoolean(KEY_SHUFFLE, player.shuffleModeEnabled)
            .putInt(KEY_REPEAT, player.repeatMode)
            .apply()
    }

    fun load(): SavedQueue? {
        val json = prefs.getString(KEY_QUEUE, null) ?: return null
        val songs = runCatching {
            val array = JSONArray(json)
            (0 until array.length()).map { array.getJSONObject(it).toSong() }
        }.getOrNull()?.takeIf { it.isNotEmpty() } ?: return null
        return SavedQueue(
            songs = songs,
            index = prefs.getInt(KEY_INDEX, 0).coerceIn(0, songs.lastIndex),
            positionMs = prefs.getLong(KEY_POSITION, 0L),
            shuffle = prefs.getBoolean(KEY_SHUFFLE, false),
            repeatMode = prefs.getInt(KEY_REPEAT, Player.REPEAT_MODE_OFF),
        )
    }

    fun toMediaItems(queue: SavedQueue): List<MediaItem> = queue.songs.map { it.toMediaItem() }

    private fun Song.toJson() = JSONObject().apply {
        put("id", id)
        put("uri", uri.toString())
        put("title", title)
        put("artist", artist)
        put("album", album)
        put("albumId", albumId)
        put("durationMs", durationMs)
        put("artwork", artworkUri?.toString() ?: JSONObject.NULL)
    }

    private fun JSONObject.toSong() = Song(
        id = getLong("id"),
        uri = Uri.parse(getString("uri")),
        title = getString("title"),
        artist = getString("artist"),
        album = getString("album"),
        albumId = getLong("albumId"),
        durationMs = getLong("durationMs"),
        artworkUri = if (isNull("artwork")) null else Uri.parse(getString("artwork")),
    )

    private companion object {
        const val PREFS = "queue_store"
        const val KEY_QUEUE = "queue"
        const val KEY_INDEX = "index"
        const val KEY_POSITION = "position"
        const val KEY_SHUFFLE = "shuffle"
        const val KEY_REPEAT = "repeat"
    }
}
