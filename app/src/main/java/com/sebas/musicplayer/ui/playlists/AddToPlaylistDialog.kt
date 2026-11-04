package com.sebas.musicplayer.ui.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.domain.model.Playlist

/**
 * Diálogo para elegir (o crear) una playlist y agregarle [songIds].
 * CONTRATO: la firma no cambia sin avisar al orquestador.
 */
@Composable
fun AddToPlaylistDialog(songIds: List<Long>, onDismiss: () -> Unit) {
    val viewModel: PlaylistsViewModel = viewModel(factory = PlaylistsViewModel.Factory)
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var creating by rememberSaveable { mutableStateOf(false) }

    if (creating) {
        PlaylistNameDialog(
            title = "Nueva playlist",
            confirmLabel = "Crear",
            onConfirm = { name ->
                viewModel.createAndAdd(name, songIds)
                onDismiss()
            },
            onDismiss = onDismiss,
        )
    } else {
        AddToPlaylistContent(
            playlists = playlists,
            onNewPlaylist = { creating = true },
            onPlaylistClick = { playlist ->
                viewModel.addToPlaylist(playlist, songIds)
                onDismiss()
            },
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun AddToPlaylistContent(
    playlists: List<Playlist>,
    onNewPlaylist: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar a playlist") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                item {
                    DialogRow(icon = Icons.Default.Add, text = "Nueva playlist", onClick = onNewPlaylist)
                }
                items(playlists, key = { it.id }) { playlist ->
                    DialogRow(
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        text = playlist.name,
                        onClick = { onPlaylistClick(playlist) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun DialogRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp), maxLines = 1)
    }
}

@Preview
@Composable
private fun AddToPlaylistContentPreview() {
    MaterialTheme {
        AddToPlaylistContent(
            playlists = listOf(Playlist(1, "Gym", 12), Playlist(2, "Road trip", 40)),
            onNewPlaylist = {},
            onPlaylistClick = {},
            onDismiss = {},
        )
    }
}
