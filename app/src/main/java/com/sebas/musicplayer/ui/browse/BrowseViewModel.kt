package com.sebas.musicplayer.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.data.library.SongRepository
import com.sebas.musicplayer.domain.library.Album
import com.sebas.musicplayer.domain.library.Artist
import com.sebas.musicplayer.domain.library.groupAlbums
import com.sebas.musicplayer.domain.library.groupArtists
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.playback.PlayerController
import com.sebas.musicplayer.playback.PlayerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface BrowseState {
    data object Loading : BrowseState
    data object Error : BrowseState
    data class Loaded(val albums: List<Album>, val artists: List<Artist>) : BrowseState
}

class BrowseViewModel(
    private val repository: SongRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val _state = MutableStateFlow<BrowseState>(BrowseState.Loading)
    val state: StateFlow<BrowseState> = _state

    val playerState: StateFlow<PlayerState> = playerController.state

    init {
        load()
    }

    fun load() {
        _state.value = BrowseState.Loading
        viewModelScope.launch {
            _state.value = try {
                val songs = repository.getSongs()
                withContext(Dispatchers.Default) {
                    BrowseState.Loaded(groupAlbums(songs), groupArtists(songs))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                BrowseState.Error
            }
        }
    }

    fun play(songs: List<Song>, startIndex: Int = 0) = playerController.playQueue(songs, startIndex)

    fun shuffle(songs: List<Song>) = playerController.playQueue(songs.shuffled(), 0)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MusicApp
                BrowseViewModel(app.container.songRepository, app.container.playerController)
            }
        }
    }
}
