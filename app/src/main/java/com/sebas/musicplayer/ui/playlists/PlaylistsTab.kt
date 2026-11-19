package com.sebas.musicplayer.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import com.sebas.musicplayer.ui.components.SongArtwork

/** Pestaña "Playlists" de HomeScreen. Sin Scaffold: solo el contenido. */
@Composable
fun PlaylistsTab(onPlaylistClick: (Long) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: PlaylistsViewModel = viewModel(factory = PlaylistsViewModel.Factory)
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val covers by viewModel.covers.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Playlist?>(null) }
    var deleting by remember { mutableStateOf<Playlist?>(null) }

    PlaylistsContent(
        playlists = playlists,
        covers = covers,
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
    covers: Map<Long, List<Uri>>,
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
    // Cuadrícula como la de Álbumes: tarjetas cuadradas grandes, 2 por fila en un celular.
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "new", contentType = "new") { NewPlaylistCard(onNewPlaylist) }
        items(playlists, key = { it.id }, contentType = { "playlist" }) { playlist ->
            PlaylistCard(
                playlist = playlist,
                artworks = covers[playlist.id].orEmpty(),
                onClick = { onPlaylistClick(playlist.id) },
                onRename = { onRename(playlist) },
                onDelete = { onDelete(playlist) },
            )
        }
    }
}

@Composable
private fun NewPlaylistCard(onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
        }
        Text(
            "Nueva playlist",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 8.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    artworks: List<Uri>,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        PlaylistTile(size = null, cornerRadius = 12.dp, artworks = artworks, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
        Row(Modifier.padding(start = 2.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    playlist.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    songCountLabel(playlist.songCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Box {
                IconButton(onClick = { expanded = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Renombrar") }, onClick = { expanded = false; onRename() })
                    DropdownMenuItem(text = { Text("Eliminar") }, onClick = { expanded = false; onDelete() })
                }
            }
        }
    }
}

/** "Carátula" de una playlist: cuadro con degradado del tema y el ícono de lista. */
@Composable
internal fun PlaylistTile(size: Dp?, cornerRadius: Dp, modifier: Modifier = Modifier, artworks: List<Uri> = emptyList()) {
    val colors = MaterialTheme.colorScheme
    if (artworks.isNotEmpty()) {
        val base = if (size != null) modifier.size(size) else modifier
        Box(base.clip(RoundedCornerShape(cornerRadius))) {
            if (artworks.size == 1) {
                SongArtwork(artworks[0], Modifier.fillMaxSize(), cornerRadius = 0.dp)
            } else {
                // 2x2; con 2 o 3 carátulas se repiten para no dejar huecos.
                val cells = List(4) { artworks[it % artworks.size] }
                Column(Modifier.fillMaxSize()) {
                    for (row in 0..1) {
                        Row(Modifier.weight(1f).fillMaxWidth()) {
                            for (col in 0..1) {
                                SongArtwork(cells[row * 2 + col], Modifier.weight(1f).fillMaxHeight(), cornerRadius = 0.dp)
                            }
                        }
                    }
                }
            }
        }
        return
    }
    Box(
        modifier = (if (size != null) modifier.size(size) else modifier)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.QueueMusic,
            contentDescription = null,
            tint = colors.onPrimaryContainer,
            // Con tamaño fijo, el ícono es el 45 %; en la tarjeta (tamaño libre) se usa 48 dp.
            modifier = Modifier.size(if (size != null) size * 0.45f else 48.dp),
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
            covers = emptyMap(),
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
            covers = emptyMap(),
            onNewPlaylist = {},
            onPlaylistClick = {},
            onRename = {},
            onDelete = {},
        )
    }
}
