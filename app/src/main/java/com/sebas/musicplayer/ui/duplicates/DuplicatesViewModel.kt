package com.sebas.musicplayer.ui.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.data.library.SongRepository
import com.sebas.musicplayer.domain.library.findDuplicateGroups
import com.sebas.musicplayer.domain.library.suggestedRemovals
import com.sebas.musicplayer.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DuplicatesState(
    val groups: List<List<Song>> = emptyList(),
    val loaded: Boolean = false,
    /** Ids marcados para eliminar. */
    val marked: Set<Long> = emptySet(),
)

class DuplicatesViewModel(
    private val songRepository: SongRepository,
    deletions: Flow<Set<Long>>,
) : ViewModel() {

    private val _state = MutableStateFlow(DuplicatesState())
    val state: StateFlow<DuplicatesState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val songs = runCatching { songRepository.getSongs() }.getOrDefault(emptyList())
            val groups = withContext(Dispatchers.Default) { findDuplicateGroups(songs) }
            _state.value = DuplicatesState(groups, loaded = true)
        }
        // Tras eliminar, las canciones desaparecen de los grupos y los grupos de 1 se van.
        viewModelScope.launch {
            deletions.collect { gone ->
                _state.update { s ->
                    DuplicatesState(
                        groups = s.groups.map { g -> g.filter { it.id !in gone } }.filter { it.size >= 2 },
                        loaded = true,
                        marked = s.marked - gone,
                    )
                }
            }
        }
    }

    fun toggle(songId: Long) = _state.update { s ->
        s.copy(marked = if (songId in s.marked) s.marked - songId else s.marked + songId)
    }

    /** Marca todas menos una por grupo (la más antigua). */
    fun markSuggested() = _state.update { it.copy(marked = suggestedRemovals(it.groups)) }

    fun clearMarks() = _state.update { it.copy(marked = emptySet()) }

    fun markedSongs(): List<Song> {
        val s = _state.value
        return s.groups.flatten().filter { it.id in s.marked }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MusicApp).container
                DuplicatesViewModel(c.songRepository, c.songDeletions)
            }
        }
    }
}
