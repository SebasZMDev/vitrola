package com.sebas.musicplayer.data.library

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.sebas.musicplayer.domain.model.Song
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Decorador que guarda en memoria la lista de canciones tras la primera carga.
 * Un [Mutex] garantiza una sola consulta aunque varias pantallas pidan a la vez.
 * Si la carga falla no se cachea nada y la excepción se propaga.
 * Un [ContentObserver] invalida la caché cuando cambia la música del teléfono.
 */
class CachingSongRepository(
    private val delegate: SongRepository,
    contentResolver: ContentResolver,
) : SongRepository {

    private val mutex = Mutex()

    @Volatile
    private var cache: List<Song>? = null

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = invalidate()
        override fun onChange(selfChange: Boolean, uri: Uri?) = invalidate()
    }

    init {
        // Vive lo mismo que la app: no hace falta desregistrarlo.
        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
    }

    /** Sube con cada invalidación; evita guardar una carga que empezó antes de un cambio en MediaStore. */
    private val generation = AtomicInteger(0)

    fun invalidate() {
        generation.incrementAndGet()
        cache = null
    }

    override suspend fun getSongs(forceRefresh: Boolean): List<Song> {
        if (!forceRefresh) cache?.let { return it }
        return mutex.withLock {
            if (!forceRefresh) cache?.let { return it }
            val startedAt = generation.get()
            delegate.getSongs(forceRefresh).also { songs ->
                // Si MediaStore cambió durante la consulta, devuelve el resultado pero no lo cachea.
                if (generation.get() == startedAt) cache = songs
            }
        }
    }
}
