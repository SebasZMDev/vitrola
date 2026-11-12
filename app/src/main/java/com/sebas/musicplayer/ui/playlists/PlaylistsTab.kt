package com.sebas.musicplayer.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.domain.model.Playlist
import com.sebas.musicplayer.ui.components.EmptyState

/** Pestaña "Playlists" de HomeScreen. Sin Scaffold: solo el contenido. */
@Composable
fun PlaylistsTab(onPlaylistClick: (Long) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: PlaylistsViewModel = viewModel(factory = PlaylistsViewModel.Factory)
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Playlist?>(null) }
    var deleting by remember { mutableStateOf<Playlist?>(null) }

    PlaylistsContent(
        playlists = playlists,
        onNewPlaylist = { showCreate = true },
        onPlaylistClick = onPlaylistClick,
        onRename = { renaming = it },
        onDelete = { deleting = it },
        modifier = modifier,
    )

    if (showCreate) {
        PlaylistNameDialog(
            title = "Nueva playlist",
            confirmLabel = "Crear",
            onConfirm = { viewModel.create(it); showCreate = false },
            onDismiss = { showCreate = false },
        )
    }
    renaming?.let { playlist ->
        PlaylistNameDialog(
            title = "Renombrar playlist",
            confirmLabel = "Guardar",
            initialName = playlist.name,
            onConfirm = { viewModel.rename(playlist.id, it); renaming = null },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { playlist ->
        DeletePlaylistDialog(
            playlistName = playlist.name,
            onConfirm = { viewModel.delete(playlist.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun PlaylistsContent(
    playlists: List<Playlist>,
    onNewPlaylist: () -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onRename: (Playlist) -> Unit,
    onDelete: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (playlists.isEmpty()) {
        EmptyState(
            icon = Icons.AutoMirrored.Filled.QueueMusic,
            title = "Aún no tienes playlists",
            message = "Crea una y agrégale canciones desde el menú ⋮ de cualquier canción.",
            modifier = modifier,
            action = {
                Button(onClick = onNewPlaylist) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Crear playlist", Modifier.padding(start = 8.dp))
                }
            },
        )
        return
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)) {
        item(key = "new", contentType = "new") { NewPlaylistRow(onNewPlaylist) }
        items(playlists, key = { it.id }, contentType = { "playlist" }) { playlist ->
            PlaylistRow(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist.id) },
                onRename = { onRename(playlist) },
                onDelete = { onDelete(playlist) },
            )
        }
    }
}

@Composable
private fun NewPlaylistRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            "Nueva playlist",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaylistTile(size = 56.dp, cornerRadius = 12.dp)
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(
                playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                songCountLabel(playlist.songCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Renombrar") }, onClick = { expanded = false; onRename() })
                DropdownMenuItem(text = { Text("Eliminar") }, onClick = { expanded = false; onDelete() })
            }
        }
    }
}

/** "Carátula" de una playlist: cuadro con degradado del tema y el ícono de lista. */
@Composable
internal fun PlaylistTile(size: Dp, cornerRadius: Dp, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.QueueMusic,
            contentDescription = null,
            tint = colors.onPrimaryContainer,
            modifier = Modifier.size(size * 0.45f),
        )
    }
}

internal fun songCountLabel(count: Int): String = if (count == 1) "1 canción" else "$count canciones"

@Preview(showBackground = true)
@Composable
private fun PlaylistsContentPreview() {
    MaterialTheme {
        PlaylistsContent(
            playlists = listOf(Playlist(1, "Gym", 12), Playlist(2, "Road trip", 1)),
            onNewPlaylist = {},
            onPlaylistClick = {},
            onRename = {},
            onDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaylistsEmptyPreview() {
    MaterialTheme {
        PlaylistsContent(
            playlists = emptyList(),
            onNewPlaylist = {},
            onPlaylistClick = {},
            onRename = {},
            onDelete = {},
        )
    }
}
