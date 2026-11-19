package com.sebas.musicplayer.ui.smartlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.data.library.SongRepository
import com.sebas.musicplayer.data.stats.SongStatsRepository
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.playback.PlayerController
import com.sebas.musicplayer.playback.PlayerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Las listas automáticas que arma la app a partir de lo que escuchas. */
enum class SmartListKind(val route: String, val title: String, val emptyHint: String, val limit: Int) {
    FAVORITES(
        "favorites", "Favoritas",
        "Toca el corazón en el reproductor o usa «Agregar a favoritas» en el menú ⋮ de una canción.",
        Int.MAX_VALUE,
    ),
    RECENT("recent", "Recientes", "Aquí aparecerán las canciones que vayas escuchando.", 50),
    TOP("top", "Más escuchadas", "Se llena a medida que escuchas música; una canción cuenta tras 20 segundos.", 50),
    ;

    companion object {
        fun fromRoute(route: String?) = entries.firstOrNull { it.route == route } ?: FAVORITES
    }
}

data class SmartListState(val songs: List<Song> = emptyList(), val loaded: Boolean = false)

class SmartListViewModel(
    private val kind: SmartListKind,
    private val songRepository: SongRepository,
    stats: SongStatsRepository,
    private val playerController: PlayerController,
    songEdits: Flow<Unit>,
    deletions: Flow<Set<Long>>,
) : ViewModel() {

    private val songsById = MutableStateFlow<Map<Long, Song>>(emptyMap())
    private val loadedOnce = MutableStateFlow(false)

    val playerState: StateFlow<PlayerState> = playerController.state

    private val ids: Flow<List<Long>> = when (kind) {
        SmartListKind.FAVORITES -> stats.favoriteIds
        SmartListKind.RECENT -> stats.recentIds
        SmartListKind.TOP -> stats.topIds
    }

    /** Los ids que ya no existen en el teléfono simplemente no se muestran. */
    val state: StateFlow<SmartListState> = combine(ids, songsById, loadedOnce) { list, byId, loaded ->
        SmartListState(list.mapNotNull { byId[it] }.take(kind.limit), loaded)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SmartListState())

    init {
        reload()
        viewModelScope.launch { songEdits.collect { reload() } }
        viewModelScope.launch { deletions.collect { gone -> songsById.value = songsById.value - gone } }
    }

    private fun reload() {
        viewModelScope.launch {
            songsById.value = runCatching { songRepository.getSongs() }.getOrDefault(emptyList()).associateBy { it.id }
            loadedOnce.value = true
        }
    }

    fun play(songs: List<Song>, index: Int) = playerController.playQueue(songs, index)
    fun playShuffled(songs: List<Song>) = playerController.playQueue(songs.shuffled(), 0)

    companion object {
        fun factory(kind: SmartListKind) = viewModelFactory {
            initializer {
                val c = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MusicApp).container
                SmartListViewModel(kind, c.songRepository, c.statsRepository, c.playerController, c.songEdits, c.songDeletions)
            }
        }
    }
}
