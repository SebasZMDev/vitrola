package com.sebas.musicplayer.ui.library

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.data.settings.SongSort
import com.sebas.musicplayer.data.settings.SongSortField
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.ui.components.LocalDeleteSongs
import com.sebas.musicplayer.ui.playlists.AddToPlaylistDialog
import com.sebas.musicplayer.ui.components.EmptyState
import com.sebas.musicplayer.ui.components.SongMenuButton
import com.sebas.musicplayer.ui.components.SongRow

/**
 * Pestaña "Canciones" de HomeScreen. Sin Scaffold ni barra superior: solo el contenido.
 * CONTRATO: la firma no cambia sin avisar al orquestador.
 */
@Composable
fun SongsTab(modifier: Modifier = Modifier) {
    val viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    // Solo recompone cuando cambia la canción, no con cada tick de posición (cada 500 ms).
    val currentSongId by remember { derivedStateOf { playerState.currentSong?.id } }

    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val deleteSongs = LocalDeleteSongs.current
    var addingToPlaylist by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadSongs() }
    BackHandler(enabled = selected.isNotEmpty()) { viewModel.clearSelection() }

    if (addingToPlaylist) {
        AddToPlaylistDialog(
            songIds = viewModel.selectedSongs().map { it.id },
            onDismiss = { addingToPlaylist = false; viewModel.clearSelection() },
        )
    }

    SongsContent(
        sort = sort,
        selected = selected,
        onSortChange = viewModel::setSort,
        onLongClick = viewModel::startSelection,
        onToggle = viewModel::toggleSelected,
        onClearSelection = viewModel::clearSelection,
        onSelectAll = viewModel::selectAll,
        onPlayNext = viewModel::playNextSelected,
        onAddToQueue = viewModel::addSelectedToQueue,
        onAddToPlaylist = { addingToPlaylist = true },
        onDelete = { deleteSongs(viewModel.selectedSongs()) },
        uiState = uiState,
        isRefreshing = isRefreshing,
        currentSongId = currentSongId,
        onRefresh = viewModel::refresh,
        onSongClick = viewModel::onSongClick,
        onShuffleClick = viewModel::onShuffleClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongsContent(
    sort: SongSort,
    selected: Set<Long>,
    onSortChange: (SongSort) -> Unit,
    onLongClick: (Long) -> Unit,
    onToggle: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit,
    uiState: LibraryUiState,
    isRefreshing: Boolean,
    currentSongId: Long?,
    onRefresh: () -> Unit,
    onSongClick: (Int) -> Unit,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier) {
        when (uiState) {
            LibraryUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            // Con scroll para que el gesto de "tirar para actualizar" también funcione aquí.
            LibraryUiState.Empty -> LazyColumn(Modifier.fillMaxSize()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.LibraryMusic,
                        title = "Aún no hay música",
                        message = "Copia canciones a tu teléfono y desliza hacia abajo para actualizar.",
                        modifier = Modifier.fillParentMaxSize(),
                        action = { FilledTonalButton(onClick = onRefresh) { Text("Buscar de nuevo") } },
                    )
                }
            }

            is LibraryUiState.Error -> LazyColumn(Modifier.fillMaxSize()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.CloudOff,
                        title = "No se pudo cargar tu música",
                        message = uiState.message,
                        modifier = Modifier.fillParentMaxSize(),
                        action = { FilledTonalButton(onClick = onRefresh) { Text("Reintentar") } },
                    )
                }
            }

            is LibraryUiState.Success -> Column(Modifier.fillMaxSize()) {
                val selecting = selected.isNotEmpty()
                if (selecting) {
                    SelectionBar(
                        count = selected.size,
                        onClose = onClearSelection,
                        onSelectAll = onSelectAll,
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onAddToPlaylist = onAddToPlaylist,
                        onDelete = onDelete,
                    )
                }
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    item(key = "header", contentType = "header") {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (uiState.songs.size == 1) "1 canción" else "${uiState.songs.size} canciones",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            SortButton(sort, onSortChange)
                        }
                    }
                    itemsIndexed(uiState.songs, key = { _, song -> song.id }, contentType = { _, _ -> "song" }) { index, song ->
                        SongRow(
                            song = song,
                            isCurrent = song.id == currentSongId,
                            onClick = { if (selecting) onToggle(song.id) else onSongClick(index) },
                            onLongClick = { if (selecting) onToggle(song.id) else onLongClick(song.id) },
                            selected = song.id in selected,
                            selectionMode = selecting,
                            trailing = if (selecting) null else ({ SongMenuButton(song) }),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortButton(sort: SongSort, onChange: (SongSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(sort.field.label, modifier = Modifier.padding(start = 6.dp))
            Icon(
                if (sort.ascending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = if (sort.ascending) "Ascendente" else "Descendente",
                modifier = Modifier.padding(start = 4.dp).size(16.dp),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SongSortField.entries.forEach { field ->
                DropdownMenuItem(
                    text = { Text(field.label) },
                    trailingIcon = {
                        if (field == sort.field) {
                            Icon(
                                if (sort.ascending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    // Tocar el criterio activo invierte el sentido; uno nuevo empieza ascendente.
                    onClick = {
                        open = false
                        onChange(if (field == sort.field) sort.copy(ascending = !sort.ascending) else SongSort(field, true))
                    },
                )
            }
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Cancelar selección") }
            Text(
                if (count == 1) "1 seleccionada" else "$count seleccionadas",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onSelectAll) { Icon(Icons.Filled.SelectAll, contentDescription = "Seleccionar todas") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar del teléfono") }
            var menu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Más acciones") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Reproducir a continuación") },
                        leadingIcon = { Icon(Icons.Filled.SkipNext, contentDescription = null) },
                        onClick = { menu = false; onPlayNext() },
                    )
                    DropdownMenuItem(
                        text = { Text("Agregar a la cola") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null) },
                        onClick = { menu = false; onAddToQueue() },
                    )
                    DropdownMenuItem(
                        text = { Text("Agregar a playlist") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                        onClick = { menu = false; onAddToPlaylist() },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SongsContentPreview() {
    val songs = List(5) {
        Song(it.toLong(), Uri.EMPTY, "Canción $it", "Artista", "Álbum", 1L, 200_000L, null)
    }
    MaterialTheme {
        SongsContent(SongSort(), emptySet(), {}, {}, {}, {}, {}, {}, {}, {}, {}, LibraryUiState.Success(songs), false, 2L, {}, {}, {})
    }
}
