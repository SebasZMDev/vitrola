package com.sebas.musicplayer.ui.playlists

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.data.playlist.PlaylistRepository
import com.sebas.musicplayer.domain.model.Playlist
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistsViewModel(
    private val application: Application,
    private val repository: PlaylistRepository,
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = repository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String) {
        viewModelScope.launch { repository.create(name.trim()) }
    }

    fun rename(playlistId: Long, name: String) {
        viewModelScope.launch { repository.rename(playlistId, name.trim()) }
    }

    fun delete(playlistId: Long) {
        viewModelScope.launch { repository.delete(playlistId) }
    }

    fun addToPlaylist(playlist: Playlist, songIds: List<Long>) {
        viewModelScope.launch { addAndNotify(playlist.id, playlist.name, songIds) }
    }

    fun createAndAdd(name: String, songIds: List<Long>) {
        viewModelScope.launch {
            val trimmed = name.trim()
            val id = repository.create(trimmed)
            addAndNotify(id, trimmed, songIds)
        }
    }

    private suspend fun addAndNotify(playlistId: Long, name: String, songIds: List<Long>) {
        val existing = repository.observeSongIds(playlistId).first().toSet()
        val missing = songIds.filter { it !in existing }.distinct()
        val message = when {
            missing.isEmpty() -> "Ya está en «$name»"
            else -> {
                repository.addSongs(playlistId, missing)
                if (missing.size == 1) "Agregada a «$name»" else "Agregadas ${missing.size} canciones a «$name»"
            }
        }
        Toast.makeText(application, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MusicApp
                PlaylistsViewModel(app, app.container.playlistRepository)
            }
        }
    }
}
