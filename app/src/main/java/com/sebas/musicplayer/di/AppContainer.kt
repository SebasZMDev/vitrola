package com.sebas.musicplayer.di

import android.content.Context
import com.sebas.musicplayer.data.library.CachingSongRepository
import com.sebas.musicplayer.data.library.FolderRepository
import com.sebas.musicplayer.data.library.MediaStoreFolderRepository
import com.sebas.musicplayer.data.library.MediaStoreSongRepository
import com.sebas.musicplayer.data.library.OverridingSongRepository
import com.sebas.musicplayer.data.playlist.db.MusicDatabase
import com.sebas.musicplayer.data.library.SongRepository
import com.sebas.musicplayer.data.playlist.PlaylistRepository
import com.sebas.musicplayer.data.playlist.RoomPlaylistRepository
import com.sebas.musicplayer.data.settings.AppSettings
import com.sebas.musicplayer.playback.MediaControllerPlayerController
import com.sebas.musicplayer.playback.PlayerController
import com.sebas.musicplayer.playback.effects.AudioEffects
import com.sebas.musicplayer.playback.effects.EqualizerAudioEffects
import com.sebas.musicplayer.playback.sleep.SleepTimer
import com.sebas.musicplayer.playback.sleep.SleepTimerController
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Inyección de dependencias manual. Único lugar donde se eligen las implementaciones.
 * Si el proyecto crece, se reemplaza por Hilt sin tocar la UI.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val settings: AppSettings by lazy { AppSettings(appContext) }

    /** Repositorio completo: MediaStore + caché + metadatos corregidos por el usuario. */
    private val overridingRepository: OverridingSongRepository by lazy {
        val resolver = appContext.contentResolver
        OverridingSongRepository(
            delegate = CachingSongRepository(
                delegate = MediaStoreSongRepository(resolver, settings),
                contentResolver = resolver,
                settingsVersion = { settings.current.libraryVersion },
            ),
            dao = MusicDatabase.get(appContext).songOverrideDao(),
        )
    }

    val songRepository: SongRepository get() = overridingRepository

    /** Edición de metadatos (artista, álbum, título) sin tocar los archivos. */
    val songEditor: OverridingSongRepository get() = overridingRepository

    /** Avisa que se cambiaron metadatos; las listas abiertas se recargan. */
    val songEdits = MutableSharedFlow<Unit>(extraBufferCapacity = 4)

    /** Ids de canciones eliminadas del teléfono; las listas abiertas las quitan al instante. */
    val songDeletions = MutableSharedFlow<Set<Long>>(extraBufferCapacity = 8)

    val folderRepository: FolderRepository by lazy { MediaStoreFolderRepository(appContext.contentResolver) }

    val playerController: PlayerController by lazy {
        MediaControllerPlayerController(appContext)
    }

    val sleepTimer: SleepTimer by lazy { SleepTimerController() }

    val audioEffects: AudioEffects by lazy { EqualizerAudioEffects(appContext) }

    val playlistRepository: PlaylistRepository by lazy {
        RoomPlaylistRepository(appContext)
    }
}
