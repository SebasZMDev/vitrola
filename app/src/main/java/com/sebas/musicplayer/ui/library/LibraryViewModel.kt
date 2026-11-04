package com.sebas.musicplayer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.data.library.SongRepository
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.playback.PlayerController
import com.sebas.musicplayer.playback.PlayerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data class Success(val songs: List<Song>) : LibraryUiState
    data object Empty : LibraryUiState
    data class Error(val message: String) : LibraryUiState
}

class LibraryViewModel(
    private val songRepository: SongRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val playerState: StateFlow<PlayerState> = playerController.state

    private var loaded = false
    private var loading = false

    fun loadSongs() {
        if (!loaded) load()
    }

    fun refresh() = load()

    private fun load() {
        if (loading) return
        loading = true
        if (_uiState.value is LibraryUiState.Success) {
            _isRefreshing.value = true
        } else {
            _uiState.value = LibraryUiState.Loading
        }
        viewModelScope.launch {
            try {
                val songs = songRepository.getSongs()
                _uiState.value = if (songs.isEmpty()) LibraryUiState.Empty else LibraryUiState.Success(songs)
                loaded = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error(e.message ?: "No se pudo cargar la música")
            } finally {
                loading = false
                _isRefreshing.value = false
            }
        }
    }

    fun onSongClick(index: Int) {
        val songs = (_uiState.value as? LibraryUiState.Success)?.songs ?: return
        playerController.playQueue(songs, index)
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun next() = playerController.next()
    fun previous() = playerController.previous()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MusicApp
                LibraryViewModel(app.container.songRepository, app.container.playerController)
            }
        }
    }
}
