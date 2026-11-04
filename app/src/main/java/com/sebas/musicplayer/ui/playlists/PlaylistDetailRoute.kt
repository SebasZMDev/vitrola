package com.sebas.musicplayer.ui.playlists

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.domain.model.Playlist
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.ui.components.SongMenuButton
import com.sebas.musicplayer.ui.components.SongRow
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
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    val missing = state.loaded && state.playlist == null
    LaunchedEffect(missing) { if (missing) onBack() }

    PlaylistDetailContent(
        state = state,
        currentSongId = playerState.currentSong?.id,
        onBack = onBack,
        onRename = { showRename = true },
        onDelete = { showDelete = true },
        onPlay = { viewModel.play(state.songs, 0) },
        onShuffle = { viewModel.playShuffled(state.songs) },
        onSongClick = { index -> viewModel.play(state.songs, index) },
        onRemove = viewModel::removeAt,
    )

    val playlist = state.playlist
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
    onBack: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onSongClick: (Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.playlist?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                            DropdownMenuItem(text = { Text("Eliminar") }, onClick = { menuExpanded = false; onDelete() })
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val isEmpty = state.entries.isEmpty()
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "${songCountLabel(state.entries.size)} · ${formatDuration(state.totalDurationMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onPlay, enabled = !isEmpty) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text("Reproducir", Modifier.padding(start = 8.dp))
                    }
                    OutlinedButton(onClick = onShuffle, enabled = !isEmpty) {
                        Icon(Icons.Default.Shuffle, contentDescription = null)
                        Text("Aleatorio", Modifier.padding(start = 8.dp))
                    }
                }
            }
            if (isEmpty) {
                if (state.loaded) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Esta playlist está vacía",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxWidth()) {
                    items(state.entries, key = { it.position }) { entry ->
                        val song = entry.song
                        SongRow(
                            song = song,
                            isCurrent = song.id == currentSongId,
                            onClick = { onSongClick(state.entries.indexOf(entry)) },
                            trailing = {
                                SongMenuButton(song, extraItems = { dismiss ->
                                    DropdownMenuItem(
                                        text = { Text("Quitar de la playlist") },
                                        onClick = { dismiss(); onRemove(entry.position) },
                                    )
                                })
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun previewSong(id: Long, title: String) = Song(id, Uri.EMPTY, title, "Artista", "Álbum", 0, 185_000, null)

@Preview(showBackground = true)
@Composable
private fun PlaylistDetailPreview() {
    MaterialTheme {
        PlaylistDetailContent(
            state = PlaylistDetailState(
                loaded = true,
                playlist = Playlist(1, "Gym", 2),
                entries = listOf(PlaylistEntry(0, previewSong(1, "Canción uno")), PlaylistEntry(2, previewSong(2, "Canción dos"))),
            ),
            currentSongId = 1,
            onBack = {}, onRename = {}, onDelete = {}, onPlay = {}, onShuffle = {}, onSongClick = {}, onRemove = {},
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
            onBack = {}, onRename = {}, onDelete = {}, onPlay = {}, onShuffle = {}, onSongClick = {}, onRemove = {},
        )
    }
}
