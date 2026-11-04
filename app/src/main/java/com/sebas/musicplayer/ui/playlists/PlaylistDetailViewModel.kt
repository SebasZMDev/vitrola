package com.sebas.musicplayer.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.data.library.SongRepository
import com.sebas.musicplayer.data.playlist.PlaylistRepository
import com.sebas.musicplayer.domain.model.Playlist
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.playback.PlayerController
import com.sebas.musicplayer.playback.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Canción resuelta junto con su posición original en la playlist (la que usa `removeAt`). */
data class PlaylistEntry(val position: Int, val song: Song)

data class PlaylistDetailState(
    val loaded: Boolean = false,
    val playlist: Playlist? = null,
    val entries: List<PlaylistEntry> = emptyList(),
) {
    val songs: List<Song> get() = entries.map { it.song }
    val totalDurationMs: Long get() = entries.sumOf { it.song.durationMs }
}

class PlaylistDetailViewModel(
    private val playlistId: Long,
    private val repository: PlaylistRepository,
    private val songRepository: SongRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val songsById = MutableStateFlow<Map<Long, Song>?>(null)

    val state: StateFlow<PlaylistDetailState> = combine(
        repository.observePlaylist(playlistId),
        repository.observeSongIds(playlistId),
        songsById,
    ) { playlist, ids, songs ->
        if (songs == null) {
            PlaylistDetailState()
        } else {
            val entries = ids.mapIndexedNotNull { position, id -> songs[id]?.let { PlaylistEntry(position, it) } }
            PlaylistDetailState(loaded = true, playlist = playlist, entries = entries)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaylistDetailState())

    val playerState: StateFlow<PlayerState> = playerController.state

    init {
        viewModelScope.launch { songsById.value = songRepository.getSongs().associateBy { it.id } }
    }

    fun play(songs: List<Song>, index: Int) = playerController.playQueue(songs, index)

    fun playShuffled(songs: List<Song>) = playerController.playQueue(songs.shuffled(), 0)

    fun removeAt(position: Int) {
        viewModelScope.launch { repository.removeAt(playlistId, position) }
    }

    fun rename(name: String) {
        viewModelScope.launch { repository.rename(playlistId, name.trim()) }
    }

    fun delete() {
        viewModelScope.launch { repository.delete(playlistId) }
    }

    companion object {
        fun factory(playlistId: Long) = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MusicApp
                val container = app.container
                PlaylistDetailViewModel(playlistId, container.playlistRepository, container.songRepository, container.playerController)
            }
        }
    }
}
