package com.sebas.musicplayer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.playback.PlayerController
import com.sebas.musicplayer.ui.playlists.AddToPlaylistDialog

/**
 * Botón ⋮ con las acciones de una canción: reproducir siguiente, agregar a la cola, agregar a playlist.
 * Autónomo (trae su propio ViewModel), así cualquier lista lo usa con `trailing = { SongMenuButton(song) }`.
 * [extraItems] permite a una pantalla agregar acciones propias (por ejemplo, "Quitar de la playlist");
 * recibe una lambda para cerrar el menú.
 * CONTRATO compartido: no cambiar la firma sin avisar al orquestador.
 */
@Composable
fun SongMenuButton(
    song: Song,
    extraItems: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
) {
    val viewModel: SongActionsViewModel = viewModel(factory = SongActionsViewModel.Factory)
    var expanded by remember { mutableStateOf(false) }
    var showPlaylistDialog by rememberSaveable { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Reproducir siguiente") },
                leadingIcon = { Icon(Icons.Default.SkipNext, contentDescription = null) },
                onClick = { expanded = false; viewModel.playNext(song) },
            )
            DropdownMenuItem(
                text = { Text("Agregar a la cola") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                onClick = { expanded = false; viewModel.addToQueue(song) },
            )
            DropdownMenuItem(
                text = { Text("Agregar a playlist") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                onClick = { expanded = false; showPlaylistDialog = true },
            )
            extraItems?.invoke { expanded = false }
        }
    }

    if (showPlaylistDialog) {
        AddToPlaylistDialog(songIds = listOf(song.id), onDismiss = { showPlaylistDialog = false })
    }
}

class SongActionsViewModel(private val playerController: PlayerController) : ViewModel() {
    fun playNext(song: Song) = playerController.playNext(song)
    fun addToQueue(song: Song) = playerController.addToQueue(song)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MusicApp
                SongActionsViewModel(app.container.playerController)
            }
        }
    }
}
