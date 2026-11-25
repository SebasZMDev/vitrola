package com.sebas.musicplayer.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.data.library.OverridingSongRepository
import com.sebas.musicplayer.data.library.SongEdit
import com.sebas.musicplayer.domain.library.bulkEdits
import com.sebas.musicplayer.domain.library.splitTitleEdits
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.playback.PlayerController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/** Acciones sobre varias canciones marcadas a la vez (ver [SelectionBar]). */
class SelectionViewModel(
    private val playerController: PlayerController,
    private val editor: OverridingSongRepository,
    private val edits: MutableSharedFlow<Unit>,
) : ViewModel() {

    fun playNext(songs: List<Song>) {
        // En orden inverso para que queden en el mismo orden de la lista.
        songs.asReversed().forEach(playerController::playNext)
    }

    fun addToQueue(songs: List<Song>) = songs.forEach(playerController::addToQueue)

    fun apply(songs: List<Song>, artist: String, album: String, genre: String) =
        save(bulkEdits(songs, artist, album, genre))

    /** Separa "Artista - Título" del nombre de cada canción. */
    fun splitTitle(songs: List<Song>) = save(splitTitleEdits(songs))

    /** Quita las correcciones y vuelve a los metadatos del archivo. */
    fun restore(songs: List<Song>) {
        val ids = songs.map { it.id }.distinct()
        viewModelScope.launch {
            editor.clearEdits(ids)
            refresh(ids)
        }
    }

    private fun save(list: List<SongEdit>) {
        viewModelScope.launch {
            editor.applyEdits(list)
            refresh(list.map { it.songId })
        }
    }

    /** Avisa a las listas y, si la canción que suena fue editada, también al reproductor. */
    private suspend fun refresh(ids: List<Long>) {
        edits.emit(Unit)
        val current = playerController.state.value.currentSong?.id ?: return
        if (current !in ids) return
        editor.getSongs(false).firstOrNull { it.id == current }?.let(playerController::updateSongInfo)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MusicApp
                val c = app.container
                SelectionViewModel(c.playerController, c.songEditor, c.songEdits)
            }
        }
    }
}
