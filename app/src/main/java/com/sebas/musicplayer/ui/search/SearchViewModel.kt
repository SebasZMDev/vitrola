package com.sebas.musicplayer.ui.search

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
import com.sebas.musicplayer.domain.library.normalizedForSearch
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.playback.PlayerController
import com.sebas.musicplayer.playback.PlayerState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchResults(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
) {
    val isEmpty: Boolean get() = songs.isEmpty() && albums.isEmpty() && artists.isEmpty()
}

private const val MAX_SONGS = 50
private const val MAX_GROUPS = 20

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val songRepository: SongRepository,
    private val playerController: PlayerController,
    deletions: kotlinx.coroutines.flow.Flow<Set<Long>>,
) : ViewModel() {

    private val allSongs = MutableStateFlow<List<Song>?>(null)
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val playerState: StateFlow<PlayerState> = playerController.state

    val results: StateFlow<SearchResults> = combine(allSongs, _query.debounce(200)) { songs, query -> songs to query }
        .mapLatest { (songs, query) -> search(songs.orEmpty(), query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResults())

    init {
        viewModelScope.launch { deletions.collect { ids -> allSongs.value = allSongs.value?.filter { it.id !in ids } } }
        viewModelScope.launch {
            allSongs.value = try {
                songRepository.getSongs()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    /** [songs] llega en el orden visible. */
    fun onSongClick(songs: List<Song>, index: Int) = playerController.playQueue(songs, index)

    private fun search(songs: List<Song>, rawQuery: String): SearchResults {
        val q = rawQuery.trim().normalizedForSearch()
        if (q.isEmpty()) return SearchResults()
        return SearchResults(
            songs = songs.filter {
                it.title.normalizedForSearch().contains(q) ||
                    it.artist.normalizedForSearch().contains(q) ||
                    it.album.normalizedForSearch().contains(q)
            }.take(MAX_SONGS),
            albums = groupAlbums(songs).filter {
                it.title.normalizedForSearch().contains(q) || it.artist.normalizedForSearch().contains(q)
            }.take(MAX_GROUPS),
            artists = groupArtists(songs).filter { it.name.normalizedForSearch().contains(q) }.take(MAX_GROUPS),
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MusicApp
                SearchViewModel(app.container.songRepository, app.container.playerController, app.container.songDeletions)
            }
        }
    }
}
