package com.sebas.musicplayer.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Edita artista y álbum de varias canciones a la vez. Los cambios se guardan en la app
 * (no se escriben en los archivos), así que se pueden deshacer con "Restaurar originales".
 */
@Composable
fun EditMetadataDialog(
    count: Int,
    initialArtist: String,
    initialAlbum: String,
    onApply: (artist: String, album: String) -> Unit,
    onSplitFromTitle: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
) {
    var artist by remember { mutableStateOf(initialArtist) }
    var album by remember { mutableStateOf(initialAlbum) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (count == 1) "Editar canción" else "Editar $count canciones") },
        text = {
            Column {
                Text(
                    "Deja un campo en blanco para no cambiarlo. No se modifica el archivo de audio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artista") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Álbum") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedButton(onClick = onSplitFromTitle, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text("Usar «Artista - Título» del nombre")
                }
                TextButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                    Text("Restaurar originales")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(artist, album) }) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
