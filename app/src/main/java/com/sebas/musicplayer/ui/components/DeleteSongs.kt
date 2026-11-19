package com.sebas.musicplayer.ui.components

import android.app.Activity
import android.app.RecoverableSecurityException
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.domain.model.Song

/**
 * Pide eliminar canciones del teléfono (archivo incluido). Cualquier pantalla lo usa con
 * `LocalDeleteSongs.current(listOf(song))`; [ProvideSongDeleter] se encarga de confirmar con el
 * sistema, borrar, sacarlas de la cola y avisar a las listas abiertas.
 */
val LocalDeleteSongs = compositionLocalOf<(List<Song>) -> Unit> { {} }

@Composable
fun ProvideSongDeleter(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as MusicApp).container

    var confirm by remember { mutableStateOf<List<Song>?>(null) }
    var inFlight by remember { mutableStateOf<List<Song>>(emptyList()) }
    val legacyDone = remember { mutableListOf<Song>() }

    fun finish(songs: List<Song>) {
        if (songs.isEmpty()) return
        val ids = songs.map { it.id }.toSet()
        // Fuera de la cola, de atrás hacia adelante para no mover los índices pendientes.
        val queue = container.playerController.state.value.queue
        queue.indices.reversed().filter { queue[it].id in ids }.forEach { container.playerController.removeFromQueue(it) }
        container.songDeletions.tryEmit(ids)
        Toast.makeText(context, if (songs.size == 1) "Canción eliminada" else "${songs.size} canciones eliminadas", Toast.LENGTH_SHORT).show()
    }

    lateinit var launch: (IntentSenderRequest) -> Unit

    // Android 6-10: se borra de a una; en 10 el sistema puede pedir permiso por archivo.
    fun deleteLegacy(songs: List<Song>) {
        var index = 0
        while (index < songs.size) {
            val song = songs[index]
            try {
                context.contentResolver.delete(song.uri, null, null)
                legacyDone += song
                index++
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                    inFlight = songs.drop(index)
                    launch(IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build())
                } else {
                    Toast.makeText(context, "No se pudo eliminar", Toast.LENGTH_SHORT).show()
                    finish(legacyDone.toList()); legacyDone.clear()
                }
                return
            }
        }
        finish(legacyDone.toList()); legacyDone.clear()
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) { finish(legacyDone.toList()); legacyDone.clear() }
            return@rememberLauncherForActivityResult
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) finish(inFlight) else deleteLegacy(inFlight)
    }
    launch = { launcher.launch(it) }

    val request: (List<Song>) -> Unit = remember {
        { songs ->
            if (songs.isNotEmpty()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // El sistema ya pide confirmación con el nombre de las canciones.
                    inFlight = songs
                    val sender = MediaStore.createDeleteRequest(context.contentResolver, songs.map { it.uri }).intentSender
                    launcher.launch(IntentSenderRequest.Builder(sender).build())
                } else {
                    confirm = songs
                }
            }
        }
    }

    CompositionLocalProvider(LocalDeleteSongs provides request) { content() }

    confirm?.let { songs ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(if (songs.size == 1) "¿Eliminar esta canción?" else "¿Eliminar ${songs.size} canciones?") },
            text = { Text("Se borra el archivo del teléfono y no se puede deshacer.") },
            confirmButton = { TextButton(onClick = { confirm = null; deleteLegacy(songs) }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancelar") } },
        )
    }
}
