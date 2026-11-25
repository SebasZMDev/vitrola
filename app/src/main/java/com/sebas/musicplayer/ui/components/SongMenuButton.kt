package com.sebas.musicplayer.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.data.library.OverridingSongRepository
import com.sebas.musicplayer.data.library.SongEdit
import com.sebas.musicplayer.data.stats.SongStatsRepository
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.playback.PlayerController
import com.sebas.musicplayer.ui.playlists.AddToPlaylistDialog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Botón ⋮ con las acciones de una canción. Arriba, una fila de íconos (favorita, siguiente, cola,
 * playlist); abajo, en texto, Editar, las acciones propias de la pantalla y Eliminar.
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
    var expanded by remember { mutableStateOf(false) }
    var showPlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
        }
        // Cada fila de la lista tiene un botón: el ViewModel y el menú solo se crean al abrirlo,
        // no al hacer scroll (antes se componían para todas las filas visibles).
        if (expanded) {
            val viewModel: SongActionsViewModel = viewModel(factory = SongActionsViewModel.Factory)
            val context = LocalContext.current
            val deleteSongs = LocalDeleteSongs.current
            val favoriteIds by viewModel.favoriteIds.collectAsState()
            val isFavorite = song.id in favoriteIds
            DropdownMenu(expanded = true, onDismissRequest = { expanded = false }) {
                Row(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    QuickAction(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        if (isFavorite) "Quitar" else "Favorita",
                    ) { expanded = false; viewModel.setFavorite(song.id, !isFavorite) }
                    QuickAction(Icons.Default.SkipNext, "Siguiente") {
                        expanded = false
                        viewModel.playNext(song)
                        Toast.makeText(context, "Sonará a continuación", Toast.LENGTH_SHORT).show()
                    }
                    QuickAction(Icons.AutoMirrored.Filled.QueueMusic, "A la cola") {
                        expanded = false
                        viewModel.addToQueue(song)
                        Toast.makeText(context, "Agregada a la cola", Toast.LENGTH_SHORT).show()
                    }
                    QuickAction(Icons.AutoMirrored.Filled.PlaylistAdd, "Playlist") {
                        expanded = false; showPlaylistDialog = true
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(
                    text = { Text("Editar") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = { expanded = false; showEditDialog = true },
                )
                extraItems?.invoke { expanded = false }
                DropdownMenuItem(
                    text = { Text("Eliminar del teléfono") },
                    leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                    onClick = { expanded = false; deleteSongs(listOf(song)) },
                )
            }
        }
    }

    if (showPlaylistDialog) {
        AddToPlaylistDialog(songIds = listOf(song.id), onDismiss = { showPlaylistDialog = false })
    }

    if (showEditDialog) {
        val viewModel: SongActionsViewModel = viewModel(factory = SongActionsViewModel.Factory)
        EditSongDialog(
            song = song,
            onApply = { t, a, al, g -> viewModel.edit(song, t, a, al, g); showEditDialog = false },
            onRestore = { viewModel.restore(song); showEditDialog = false },
            onDismiss = { showEditDialog = false },
        )
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.width(64.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label)
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Título, artista, álbum y género de una canción. Se guarda en la app, no en el archivo. */
@Composable
private fun EditSongDialog(
    song: Song,
    onApply: (title: String, artist: String, album: String, genre: String) -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(song.title) }
    var artist by rememberSaveable { mutableStateOf(song.artist) }
    var album by rememberSaveable { mutableStateOf(song.album) }
    var genre by rememberSaveable { mutableStateOf(song.genre.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar canción") },
        text = {
            Column {
                Field("Título", title) { title = it }
                Field("Artista", artist) { artist = it }
                Field("Álbum", album) { album = it }
                Field("Género", genre) { genre = it }
                TextButton(onClick = onRestore, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text("Restaurar originales")
                }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(title, artist, album, genre) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

class SongActionsViewModel(
    private val playerController: PlayerController,
    private val stats: SongStatsRepository,
    private val editor: OverridingSongRepository,
    private val edits: MutableSharedFlow<Unit>,
) : ViewModel() {
    fun playNext(song: Song) = playerController.playNext(song)
    fun addToQueue(song: Song) = playerController.addToQueue(song)

    val favoriteIds: StateFlow<Set<Long>> = stats.favoriteIds
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    fun setFavorite(songId: Long, favorite: Boolean) {
        viewModelScope.launch { stats.setFavorite(songId, favorite) }
    }

    /** Guarda solo los campos que cambiaron (vacío = no tocar) y avisa a las listas y al reproductor. */
    fun edit(song: Song, title: String, artist: String, album: String, genre: String) {
        fun changed(new: String, old: String?) = new.trim().takeIf { it.isNotEmpty() && it != old }
        val e = SongEdit(
            song.id,
            title = changed(title, song.title),
            artist = changed(artist, song.artist),
            album = changed(album, song.album),
            genre = changed(genre, song.genre),
        )
        if (e.title == null && e.artist == null && e.album == null && e.genre == null) return
        viewModelScope.launch {
            editor.applyEdits(listOf(e))
            refresh(song.id)
        }
    }

    fun restore(song: Song) {
        viewModelScope.launch {
            editor.clearEdits(listOf(song.id))
            refresh(song.id)
        }
    }

    private suspend fun refresh(id: Long) {
        edits.emit(Unit)
        editor.getSongs(false).firstOrNull { it.id == id }?.let(playerController::updateSongInfo)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MusicApp
                val c = app.container
                SongActionsViewModel(c.playerController, c.statsRepository, c.songEditor, c.songEdits)
            }
        }
    }
}
