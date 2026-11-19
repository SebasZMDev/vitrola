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
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.playback.PlayerController
import com.sebas.musicplayer.playback.PlayerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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
) : ViewModel() {

    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val playerState: StateFlow<PlayerState> = playerController.state

    /** Canciones tal como llegan del repositorio; [_uiState] las muestra ya ordenadas. */
    private var allSongs: List<Song> = emptyList()

    val sort: StateFlow<SongSort> = settings.state.map { it.songSort }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, settings.current.songSort)

    /** Ids marcados en el modo selección; vacío = modo normal. */
    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected.asStateFlow()

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
                _selected.update { it - ids }
                if (loaded) publish()
            }
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
                _selected.update { sel -> sel.filterTo(HashSet()) { id -> allSongs.any { it.id == id } } }
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
        _uiState.value = if (allSongs.isEmpty()) LibraryUiState.Empty else LibraryUiState.Success(sorted(allSongs, sort.value))
    }

    fun setSort(value: SongSort) = settings.setSongSort(value)

    fun startSelection(songId: Long) { _selected.value = setOf(songId) }

    fun toggleSelected(songId: Long) = _selected.update { if (songId in it) it - songId else it + songId }

    fun selectAll() {
        _selected.value = (_uiState.value as? LibraryUiState.Success)?.songs?.mapTo(HashSet()) { it.id } ?: emptySet()
    }

    fun clearSelection() { _selected.value = emptySet() }

    fun selectedSongs(): List<Song> {
        val ids = _selected.value
        return (_uiState.value as? LibraryUiState.Success)?.songs?.filter { it.id in ids }.orEmpty()
    }

    fun playNextSelected() {
        // En orden inverso para que queden en el mismo orden de la lista.
        selectedSongs().asReversed().forEach(playerController::playNext)
        clearSelection()
    }

    fun addSelectedToQueue() {
        selectedSongs().forEach(playerController::addToQueue)
        clearSelection()
    }

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
                )
            }
        }
    }
}

/** Ordena con comparación sin mayúsculas; en empate de artista/álbum/duración desempata por título. */
fun sorted(songs: List<Song>, sort: SongSort): List<Song> {
    val byTitle = compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
    val base: Comparator<Song> = when (sort.field) {
        SongSortField.TITLE -> byTitle
        SongSortField.ARTIST -> compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.artist }.then(byTitle)
        SongSortField.ALBUM -> compareBy<Song, String>(String.CASE_INSENSITIVE_ORDER) { it.album }.then(byTitle)
        SongSortField.DURATION -> compareBy<Song> { it.durationMs }.then(byTitle)
    }
    return songs.sortedWith(if (sort.ascending) base else base.reversed())
}
