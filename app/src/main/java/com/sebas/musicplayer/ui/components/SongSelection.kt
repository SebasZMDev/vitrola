package com.sebas.musicplayer.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.domain.library.commonMetadata
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.ui.library.EditMetadataDialog
import com.sebas.musicplayer.ui.playlists.AddToPlaylistDialog

/** Una fila que se puede marcar. [key] es el id de la canción; en una playlist, la posición. */
data class SelectableSong(val key: Long, val song: Song)

/** Estado del modo selección de una lista: sin claves marcadas = modo normal. */
@Stable
class SongSelection(initial: Set<Long> = emptySet()) {
    var keys: Set<Long> by mutableStateOf(initial)
        private set

    val active: Boolean get() = keys.isNotEmpty()

    /** Marca o desmarca; sobre una selección vacía, la inicia. */
    fun toggle(key: Long) {
        keys = if (key in keys) keys - key else keys + key
    }

    fun selectAll(all: Collection<Long>) {
        keys = all.toSet()
    }

    /** Descarta las claves que ya no están en la lista. */
    fun retain(existing: Collection<Long>) {
        if (keys.isNotEmpty()) keys = keys intersect existing.toSet()
    }

    fun clear() {
        keys = emptySet()
    }
}

@Composable
fun rememberSongSelection(): SongSelection = rememberSaveable(
    saver = Saver<SongSelection, LongArray>(
        save = { it.keys.toLongArray() },
        restore = { SongSelection(it.toSet()) },
    ),
) { SongSelection() }

/**
 * Barra del modo selección, con sus diálogos. Se llama solo cuando [selection] está activa.
 * [items] son las filas visibles, en orden: las acciones se aplican a las marcadas en ese orden.
 * Con [onRemoveFromPlaylist] el menú ofrece además "Quitar de la playlist".
 */
@Composable
fun SelectionBar(
    selection: SongSelection,
    items: List<SelectableSong>,
    modifier: Modifier = Modifier,
    onRemoveFromPlaylist: ((List<SelectableSong>) -> Unit)? = null,
) {
    val viewModel: SelectionViewModel = viewModel(factory = SelectionViewModel.Factory)
    val deleteSongs = LocalDeleteSongs.current
    var addingToPlaylist by rememberSaveable { mutableStateOf(false) }
    var editingMetadata by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }

    LaunchedEffect(items) { selection.retain(items.map { it.key }) }
    BackHandler { selection.clear() }

    fun marked() = items.filter { it.key in selection.keys }
    fun songs() = marked().map { it.song }

    if (addingToPlaylist) {
        AddToPlaylistDialog(
            songIds = songs().map { it.id },
            onDismiss = { addingToPlaylist = false; selection.clear() },
        )
    }

    if (editingMetadata) {
        val unique = songs().distinctBy { it.id }
        val (artist, album, genre) = commonMetadata(unique)
        EditMetadataDialog(
            count = unique.size,
            initialArtist = artist,
            initialAlbum = album,
            initialGenre = genre,
            onApply = { a, b, g ->
                editingMetadata = false
                // Con todo en blanco no hay nada que aplicar: la selección sigue.
                if (a.isNotBlank() || b.isNotBlank() || g.isNotBlank()) {
                    viewModel.apply(songs(), a, b, g)
                    selection.clear()
                }
            },
            onSplitFromTitle = { editingMetadata = false; viewModel.splitTitle(songs()); selection.clear() },
            onRestore = { editingMetadata = false; viewModel.restore(songs()); selection.clear() },
            onDismiss = { editingMetadata = false },
        )
    }

    val count = selection.keys.size
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = selection::clear) { Icon(Icons.Filled.Close, contentDescription = "Cancelar selección") }
            Text(
                if (count == 1) "1 seleccionada" else "$count seleccionadas",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { selection.selectAll(items.map { it.key }) }) {
                Icon(Icons.Filled.SelectAll, contentDescription = "Seleccionar todas")
            }
            // No limpia la selección: si el usuario cancela el borrado, sigue marcada.
            IconButton(onClick = { deleteSongs(songs().distinctBy { it.id }) }) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar del teléfono")
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Más acciones") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Reproducir a continuación") },
                        leadingIcon = { Icon(Icons.Filled.SkipNext, contentDescription = null) },
                        onClick = { menu = false; viewModel.playNext(songs()); selection.clear() },
                    )
                    DropdownMenuItem(
                        text = { Text("Agregar a la cola") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                        onClick = { menu = false; viewModel.addToQueue(songs()); selection.clear() },
                    )
                    DropdownMenuItem(
                        text = { Text("Agregar a playlist") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                        onClick = { menu = false; addingToPlaylist = true },
                    )
                    DropdownMenuItem(
                        text = { Text("Editar artista / álbum") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = { menu = false; editingMetadata = true },
                    )
                    if (onRemoveFromPlaylist != null) {
                        DropdownMenuItem(
                            text = { Text("Quitar de la playlist") },
                            leadingIcon = { Icon(Icons.Filled.RemoveCircleOutline, contentDescription = null) },
                            onClick = { menu = false; onRemoveFromPlaylist(marked()); selection.clear() },
                        )
                    }
                }
            }
        }
    }
}

/**
 * [SongRow] con selección múltiple: la pulsación larga marca; con la selección activa, tocar marca
 * y se oculta el ⋮. [menuExtra] agrega acciones propias de la pantalla al menú de la canción.
 */
@Composable
fun SelectableSongRow(
    selection: SongSelection,
    item: SelectableSong,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    showArtwork: Boolean = true,
    trackNumber: Int? = null,
    menuExtra: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
) {
    val selecting = selection.active
    SongRow(
        song = item.song,
        isCurrent = isCurrent,
        onClick = { if (selection.active) selection.toggle(item.key) else onPlay() },
        showArtwork = showArtwork,
        trackNumber = trackNumber,
        trailing = if (selecting) null else ({ SongMenuButton(item.song, menuExtra) }),
        onLongClick = { selection.toggle(item.key) },
        selected = item.key in selection.keys,
        selectionMode = selecting,
    )
}
