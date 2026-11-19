package com.sebas.musicplayer.ui.playlists

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.ui.components.SongArtwork

/** Elegir de dónde sale la portada de la playlist. */
@Composable
fun PlaylistCoverDialog(
    hasCustomCover: Boolean,
    canUseSong: Boolean,
    onGallery: () -> Unit,
    onFromSong: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Portada de la playlist") },
        text = {
            Column {
                CoverOption(Icons.Filled.Image, "Elegir de la galería", onGallery)
                if (canUseSong) CoverOption(Icons.Filled.LibraryMusic, "Usar la carátula de una canción", onFromSong)
                if (hasCustomCover) CoverOption(Icons.Filled.DeleteOutline, "Quitar portada (volver al collage)", onRemove)
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun CoverOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
    }
}

/** Lista las canciones de la playlist que tienen carátula, para usar una como portada. */
@Composable
fun PickSongCoverDialog(songs: List<Song>, onPick: (Uri) -> Unit, onDismiss: () -> Unit) {
    // Una por carátula distinta: un álbum entero comparte la misma.
    val choices = songs.filter { it.artworkUri != null }.distinctBy { it.artworkUri }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elige una carátula") },
        text = {
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(choices, key = { it.id }) { song ->
                    ListItem(
                        headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = { SongArtwork(song.artworkUri, Modifier.size(48.dp)) },
                        modifier = Modifier.clickable { song.artworkUri?.let(onPick) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
