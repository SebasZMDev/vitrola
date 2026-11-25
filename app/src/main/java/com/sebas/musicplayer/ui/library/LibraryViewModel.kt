package com.sebas.musicplayer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.data.library.SongRepository
import com.sebas.musicplayer.data.settings.AppSettings
import com.sebas.musicplayer.data.settings.SongSort
import com.sebas.musicplayer.data.settings.SongSortField
import com.sebas.musicplayer.domain.library.sortSongs
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.playback.PlayerController
import com.sebas.musicplayer.playback.PlayerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val settings: AppSettings,
    deletions: Flow<Set<Long>>,
    private val edits: Flow<Unit>,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val playerState: StateFlow<PlayerState> = playerController.state

    /** Canciones tal como llegan del repositorio; [_uiState] las muestra ya ordenadas. */
    private var allSongs: List<Song> = emptyList()

    /** Aquí no hay "Orden original": si estuviera guardado, se usa el orden por defecto. */
    val sort: StateFlow<SongSort> = settings.state.map { it.songSort.orDefault() }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, settings.current.songSort.orDefault())

    private var loaded = false
    private var loading = false
    private var reloadPending = false

    init {
        viewModelScope.launch {
            sort.drop(1).collect { if (loaded) publish() }
        }
        viewModelScope.launch {
            deletions.collect { ids ->
                allSongs = allSongs.filter { it.id !in ids }
                if (loaded) publish()
            }
        }
        // Metadatos corregidos desde cualquier pantalla: recarga sin mostrar el spinner de carga.
        viewModelScope.launch {
            edits.collect { if (loaded) load() }
        }
        // Si cambian carpetas excluidas, WhatsApp o la duración mínima, se vuelve a cargar la lista.
        viewModelScope.launch {
            settings.state.map { it.libraryVersion }.distinctUntilChanged().drop(1).collect {
                if (loaded || loading) load()
            }
        }
    }

    fun loadSongs() {
        if (!loaded) load()
    }

    fun refresh() = load(forceRefresh = true)

    private fun load(forceRefresh: Boolean = false) {
        if (loading) {
            // Una carga en curso empezó con los ajustes anteriores: se repite al terminar.
            reloadPending = true
            return
        }
        loading = true
        if (_uiState.value is LibraryUiState.Success) {
            _isRefreshing.value = true
        } else {
            _uiState.value = LibraryUiState.Loading
        }
        viewModelScope.launch {
            try {
                allSongs = songRepository.getSongs(forceRefresh)
                publish()
                loaded = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error(e.message ?: "No se pudo cargar la música")
            } finally {
                loading = false
                _isRefreshing.value = false
                if (reloadPending) {
                    reloadPending = false
                    load()
                }
            }
        }
    }

    private fun publish() {
        _uiState.value = if (allSongs.isEmpty()) LibraryUiState.Empty else LibraryUiState.Success(sortSongs(allSongs, sort.value) { it })
    }

    fun setSort(value: SongSort) = settings.setSongSort(value)

    private fun SongSort.orDefault() = if (field == SongSortField.ORIGINAL) SongSort() else this

    fun onSongClick(index: Int) {
        val songs = (_uiState.value as? LibraryUiState.Success)?.songs ?: return
        playerController.playQueue(songs, index)
    }

    fun onShuffleClick() {
        val songs = (_uiState.value as? LibraryUiState.Success)?.songs ?: return
        playerController.playQueue(songs.shuffled(), 0)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MusicApp
                LibraryViewModel(
                    app.container.songRepository,
                    app.container.playerController,
                    app.container.settings,
                    app.container.songDeletions,
                    app.container.songEdits,
                )
            }
        }
    }
}
