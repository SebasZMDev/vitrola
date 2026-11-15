package com.sebas.musicplayer.di

import android.content.Context
import com.sebas.musicplayer.data.library.CachingSongRepository
import com.sebas.musicplayer.data.library.FolderRepository
import com.sebas.musicplayer.data.library.MediaStoreFolderRepository
import com.sebas.musicplayer.data.library.MediaStoreSongRepository
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

/**
 * Inyección de dependencias manual. Único lugar donde se eligen las implementaciones.
 * Si el proyecto crece, se reemplaza por Hilt sin tocar la UI.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val settings: AppSettings by lazy { AppSettings(appContext) }

    val songRepository: SongRepository by lazy {
        val resolver = appContext.contentResolver
        CachingSongRepository(
            delegate = MediaStoreSongRepository(resolver, settings),
            contentResolver = resolver,
            settingsVersion = { settings.current.libraryVersion },
        )
    }

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
