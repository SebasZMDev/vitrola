package com.sebas.musicplayer.ui.playlists

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.sebas.musicplayer.ui.browse.PlayShuffleButtons
import com.sebas.musicplayer.ui.components.ArtworkGlow
import com.sebas.musicplayer.ui.components.EmptyState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.data.settings.SongSort
import com.sebas.musicplayer.domain.library.ListDefaultSort
import com.sebas.musicplayer.domain.library.sortSongs
import com.sebas.musicplayer.domain.model.Playlist
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.ui.components.SelectableSong
import com.sebas.musicplayer.ui.components.SelectableSongRow
import com.sebas.musicplayer.ui.components.SelectionBar
import com.sebas.musicplayer.ui.components.SongSelection
import com.sebas.musicplayer.ui.components.SortButton
import com.sebas.musicplayer.ui.components.rememberListSort
import com.sebas.musicplayer.ui.components.rememberSongSelection
import com.sebas.musicplayer.ui.components.formatDuration

/** Pantalla completa de una playlist (ruta propia, con su Scaffold y flecha atrás). */
@Composable
fun PlaylistDetailRoute(playlistId: Long, onBack: () -> Unit) {
    val viewModel: PlaylistDetailViewModel = viewModel(
        key = "playlist-$playlistId",
        factory = PlaylistDetailViewModel.factory(playlistId),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    // Solo recompone cuando cambia la canción, no con cada tick de posición (cada 500 ms).
    val currentSongId by remember { derivedStateOf { playerState.currentSong?.id } }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showCover by remember { mutableStateOf(false) }
    var pickingSongCover by remember { mutableStateOf(false) }
    val (sort, onSortChange) = rememberListSort("playlist:$playlistId")
    // Filas en el orden visible. La clave es la posición original: ordenar no reescribe la playlist.
    val items = remember(state.entries, sort) {
        sortSongs(state.entries, sort) { it.song }.map { SelectableSong(it.position.toLong(), it.song) }
    }
    val visible = remember(items) { items.map { it.song } }
    val selection = rememberSongSelection()
    // Si cambia el número de canciones, las posiciones se desplazan y las marcas ya no valen. El número
    // se guarda y solo se mira con la lista cargada, para no borrar una selección restaurada.
    var knownSize by rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(state.loaded, state.entries.size) {
        if (state.loaded) {
            if (knownSize != null && knownSize != state.entries.size) selection.clear()
            knownSize = state.entries.size
        }
    }
    val context = LocalContext.current
    val onCoverResult: (Boolean) -> Unit = { ok ->
        Toast.makeText(context, if (ok) "Portada actualizada" else "No se pudo usar esa imagen", Toast.LENGTH_SHORT).show()
    }
    // Selector de fotos del sistema: no pide permisos.
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setCover(uri, onCoverResult)
    }

    val missing = state.loaded && state.playlist == null
    LaunchedEffect(missing) { if (missing) onBack() }

    PlaylistDetailContent(
        state = state,
        currentSongId = currentSongId,
        onBack = onBack,
        onRename = { showRename = true },
        onChangeCover = { showCover = true },
        onDelete = { showDelete = true },
        items = items,
        selection = selection,
        sort = sort,
        onSortChange = onSortChange,
        onPlay = { viewModel.play(visible, 0) },
        onShuffle = { viewModel.playShuffled(visible) },
        onSongClick = { index -> viewModel.play(visible, index) },
        onRemove = viewModel::removeAt,
        onRemoveSelected = viewModel::removeAll,
    )

    val playlist = state.playlist
    if (showCover && playlist != null) {
        PlaylistCoverDialog(
            hasCustomCover = playlist.coverPath != null,
            canUseSong = state.songs.any { it.artworkUri != null },
            onGallery = {
                showCover = false
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onFromSong = { showCover = false; pickingSongCover = true },
            onRemove = { showCover = false; viewModel.clearCover() },
            onDismiss = { showCover = false },
        )
    }
    if (pickingSongCover) {
        PickSongCoverDialog(
            songs = state.songs,
            onPick = { uri -> pickingSongCover = false; viewModel.setCover(uri, onCoverResult) },
            onDismiss = { pickingSongCover = false },
        )
    }
    if (showRename && playlist != null) {
        PlaylistNameDialog(
            title = "Renombrar playlist",
            confirmLabel = "Guardar",
            initialName = playlist.name,
            onConfirm = { viewModel.rename(it); showRename = false },
            onDismiss = { showRename = false },
        )
    }
    if (showDelete && playlist != null) {
        DeletePlaylistDialog(
            playlistName = playlist.name,
            onConfirm = { showDelete = false; viewModel.delete() },
            onDismiss = { showDelete = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistDetailContent(
    state: PlaylistDetailState,
    currentSongId: Long?,
    items: List<SelectableSong>,
    selection: SongSelection,
    sort: SongSort,
    onSortChange: (SongSort) -> Unit,
    onBack: () -> Unit,
    onRename: () -> Unit,
    onChangeCover: () -> Unit,
    onDelete: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onSongClick: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onRemoveSelected: (List<Int>) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    ArtworkGlow(MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth().height(340.dp))
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            // Solo con la lista cargada: la barra descarta las marcas que no estén en [items].
            if (selection.active && state.loaded) {
                SelectionBar(
                    selection = selection,
                    items = items,
                    modifier = Modifier.statusBarsPadding(),
                    onRemoveFromPlaylist = { marked -> onRemoveSelected(marked.map { it.key.toInt() }) },
                )
            } else {
                TopAppBar(
                    title = {},
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(text = { Text("Renombrar") }, onClick = { menuExpanded = false; onRename() })
                                DropdownMenuItem(text = { Text("Cambiar portada") }, onClick = { menuExpanded = false; onChangeCover() })
                                DropdownMenuItem(text = { Text("Eliminar") }, onClick = { menuExpanded = false; onDelete() })
                            }
                        }
                    },
                )
            }
        },
    ) { padding ->
        val isEmpty = state.entries.isEmpty()
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            item(key = "header", contentType = "header") {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val customCover = state.playlist?.coverPath
                    PlaylistTile(
                        size = 180.dp,
                        cornerRadius = 20.dp,
                        artworks = if (customCover != null) listOf(Uri.fromFile(File(customCover))) else coverArtworks(state.songs.take(12)),
                    )
                    Text(
                        state.playlist?.name.orEmpty(),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                    Text(
                        "${songCountLabel(state.entries.size)} · ${formatDuration(state.totalDurationMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    PlayShuffleButtons(
                        onPlay = onPlay,
                        onShuffle = onShuffle,
                        enabled = !isEmpty,
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    )
                    if (!isEmpty) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            SortButton(sort, onSortChange)
                        }
                    }
                }
            }
            if (isEmpty && state.loaded) {
                item(key = "empty", contentType = "empty") {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                        title = "Esta playlist está vacía",
                        message = "Agrega canciones desde el menú ⋮ de cualquier canción.",
                        modifier = Modifier.fillParentMaxHeight(0.45f),
                    )
                }
            }
            itemsIndexed(items, key = { _, item -> item.key }, contentType = { _, _ -> "song" }) { index, item ->
                SelectableSongRow(
                    selection = selection,
                    item = item,
                    isCurrent = item.song.id == currentSongId,
                    onPlay = { onSongClick(index) },
                    menuExtra = { dismiss ->
                        DropdownMenuItem(
                            text = { Text("Quitar de la playlist") },
                            leadingIcon = { Icon(Icons.Filled.RemoveCircleOutline, contentDescription = null) },
                            onClick = { dismiss(); onRemove(item.key.toInt()) },
                        )
                    },
                )
            }
        }
    }
    }
}

private fun previewSong(id: Long, title: String) = Song(id, Uri.EMPTY, title, "Artista", "Álbum", 0, 185_000, null)

@Preview(showBackground = true)
@Composable
private fun PlaylistDetailPreview() {
    val songs = listOf(previewSong(1, "Canción uno"), previewSong(2, "Canción dos"))
    MaterialTheme {
        PlaylistDetailContent(
            state = PlaylistDetailState(
                loaded = true,
                playlist = Playlist(1, "Gym", 2),
                entries = listOf(PlaylistEntry(0, songs[0]), PlaylistEntry(2, songs[1])),
            ),
            currentSongId = 1,
            items = listOf(SelectableSong(0, songs[0]), SelectableSong(2, songs[1])),
            selection = rememberSongSelection(),
            sort = ListDefaultSort,
            onSortChange = {},
            onBack = {}, onRename = {}, onChangeCover = {}, onDelete = {}, onPlay = {}, onShuffle = {}, onSongClick = {}, onRemove = {},
            onRemoveSelected = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaylistDetailEmptyPreview() {
    MaterialTheme {
        PlaylistDetailContent(
            state = PlaylistDetailState(loaded = true, playlist = Playlist(1, "Vacía", 0)),
            currentSongId = null,
            items = emptyList(),
            selection = rememberSongSelection(),
            sort = ListDefaultSort,
            onSortChange = {},
            onBack = {}, onRename = {}, onChangeCover = {}, onDelete = {}, onPlay = {}, onShuffle = {}, onSongClick = {}, onRemove = {},
            onRemoveSelected = {},
        )
    }
}
