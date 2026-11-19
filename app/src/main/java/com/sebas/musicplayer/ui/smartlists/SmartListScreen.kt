package com.sebas.musicplayer.ui.smartlists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.ui.browse.PlayShuffleButtons
import com.sebas.musicplayer.ui.components.EmptyState
import com.sebas.musicplayer.ui.components.SongMenuButton
import com.sebas.musicplayer.ui.components.SongRow

/** Pantalla de una lista automática: Favoritas, Recientes o Más escuchadas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartListRoute(kindRoute: String?, onBack: () -> Unit) {
    val kind = SmartListKind.fromRoute(kindRoute)
    val viewModel: SmartListViewModel = viewModel(key = "smart-${kind.route}", factory = SmartListViewModel.factory(kind))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    // Solo recompone cuando cambia la canción, no con cada tick de posición.
    val currentSongId by remember { derivedStateOf { playerState.currentSong?.id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(kind.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás") }
                },
            )
        },
    ) { padding ->
        if (state.loaded && state.songs.isEmpty()) {
            EmptyState(
                icon = when (kind) {
                    SmartListKind.FAVORITES -> Icons.Filled.Favorite
                    SmartListKind.RECENT -> Icons.Filled.History
                    SmartListKind.TOP -> Icons.Filled.TrendingUp
                },
                title = "Aún no hay nada aquí",
                message = kind.emptyHint,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
            Column(Modifier.fillMaxSize().padding(padding)) {
                PlayShuffleButtons(
                    onPlay = { viewModel.play(state.songs, 0) },
                    onShuffle = { viewModel.playShuffled(state.songs) },
                    enabled = state.songs.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                    itemsIndexed(state.songs, key = { _, s -> s.id }, contentType = { _, _ -> "song" }) { index, song ->
                        SongRow(
                            song = song,
                            isCurrent = song.id == currentSongId,
                            onClick = { viewModel.play(state.songs, index) },
                            trailing = { SongMenuButton(song) },
                        )
                    }
                }
            }
        }
    }
}
