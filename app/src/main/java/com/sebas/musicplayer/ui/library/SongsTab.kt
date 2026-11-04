package com.sebas.musicplayer.ui.library

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.ui.components.SongMenuButton
import com.sebas.musicplayer.ui.components.SongRow

/**
 * Pestaña "Canciones" de HomeScreen. Sin Scaffold ni barra superior: solo el contenido.
 * CONTRATO: la firma no cambia sin avisar al orquestador.
 */
@Composable
fun SongsTab(modifier: Modifier = Modifier) {
    val viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadSongs() }

    SongsContent(
        uiState = uiState,
        isRefreshing = isRefreshing,
        currentSongId = playerState.currentSong?.id,
        onRefresh = viewModel::refresh,
        onSongClick = viewModel::onSongClick,
        onShuffleClick = viewModel::onShuffleClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongsContent(
    uiState: LibraryUiState,
    isRefreshing: Boolean,
    currentSongId: Long?,
    onRefresh: () -> Unit,
    onSongClick: (Int) -> Unit,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier) {
        when (uiState) {
            LibraryUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            LibraryUiState.Empty -> CenteredMessage("No se encontró música en el dispositivo")

            is LibraryUiState.Error -> CenteredMessage(uiState.message) {
                TextButton(onClick = onRefresh) { Text("Reintentar") }
            }

            is LibraryUiState.Success -> LazyColumn(Modifier.fillMaxSize()) {
                item(key = "header", contentType = "header") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${uiState.songs.size} canciones",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FilledTonalButton(onClick = onShuffleClick) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Aleatorio")
                        }
                    }
                }
                itemsIndexed(uiState.songs, key = { _, song -> song.id }, contentType = { _, _ -> "song" }) { index, song ->
                    SongRow(
                        song = song,
                        isCurrent = song.id == currentSongId,
                        onClick = { onSongClick(index) },
                        trailing = { SongMenuButton(song) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String, action: (@Composable () -> Unit)? = null) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            action?.invoke()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SongsContentPreview() {
    val songs = List(5) {
        Song(it.toLong(), Uri.EMPTY, "Canción $it", "Artista", "Álbum", 1L, 200_000L, null)
    }
    MaterialTheme {
        SongsContent(LibraryUiState.Success(songs), false, 2L, {}, {}, {})
    }
}
