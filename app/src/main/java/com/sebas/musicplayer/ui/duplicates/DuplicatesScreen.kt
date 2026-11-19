package com.sebas.musicplayer.ui.duplicates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.ui.components.EmptyState
import com.sebas.musicplayer.ui.components.LocalDeleteSongs
import com.sebas.musicplayer.ui.components.SongRow

/** Canciones repetidas (mismo título, artista y duración casi igual) para limpiarlas de a varias. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesRoute(onBack: () -> Unit) {
    val viewModel: DuplicatesViewModel = viewModel(factory = DuplicatesViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deleteSongs = LocalDeleteSongs.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Canciones duplicadas") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
                },
            )
        },
        bottomBar = {
            if (state.groups.isNotEmpty()) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = if (state.marked.isEmpty()) viewModel::markSuggested else viewModel::clearMarks,
                            modifier = Modifier.weight(1f),
                        ) { Text(if (state.marked.isEmpty()) "Marcar sobrantes" else "Desmarcar") }
                        Button(
                            onClick = { deleteSongs(viewModel.markedSongs()) },
                            enabled = state.marked.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = null)
                            Text("Eliminar (${state.marked.size})", Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
    ) { padding ->
        when {
            !state.loaded -> Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
            }

            state.groups.isEmpty() -> EmptyState(
                icon = Icons.Filled.CheckCircle,
                title = "No hay duplicadas",
                message = "No encontré canciones repetidas con el mismo título, artista y duración.",
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 16.dp)) {
                item(key = "hint") {
                    Text(
                        "«Marcar sobrantes» deja sin marcar la canción más antigua de cada grupo. " +
                            "Revisa antes de eliminar: se borra el archivo del teléfono.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(state.groups, key = { group -> group.first().id }) { group ->
                    Column(Modifier.padding(bottom = 8.dp)) {
                        Text(
                            "${group.first().title} · ${group.size} copias",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp),
                        )
                        group.forEach { song ->
                            SongRow(
                                song = song,
                                isCurrent = false,
                                onClick = { viewModel.toggle(song.id) },
                                selected = song.id in state.marked,
                                selectionMode = true,
                            )
                        }
                    }
                }
            }
        }
    }
}
