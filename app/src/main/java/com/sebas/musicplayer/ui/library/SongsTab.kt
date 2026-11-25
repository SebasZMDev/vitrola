package com.sebas.musicplayer.ui.library

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.data.settings.SongSort
import com.sebas.musicplayer.data.settings.SongSortField
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.ui.components.EmptyState
import com.sebas.musicplayer.ui.components.SelectableSong
import com.sebas.musicplayer.ui.components.SelectableSongRow
import com.sebas.musicplayer.ui.components.SelectionBar
import com.sebas.musicplayer.ui.components.SongSelection
import com.sebas.musicplayer.ui.components.rememberSongSelection
import com.sebas.musicplayer.ui.components.SortButton

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
    // Solo recompone cuando cambia la canción, no con cada tick de posición (cada 500 ms).
    val currentSongId by remember { derivedStateOf { playerState.currentSong?.id } }

    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val selection = rememberSongSelection()

    LaunchedEffect(Unit) { viewModel.loadSongs() }

    SongsContent(
        sort = sort,
        selection = selection,
        onSortChange = viewModel::setSort,
        uiState = uiState,
        isRefreshing = isRefreshing,
        currentSongId = currentSongId,
        onRefresh = viewModel::refresh,
        onSongClick = viewModel::onSongClick,
        onShuffleClick = viewModel::onShuffleClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongsContent(
    sort: SongSort,
    selection: SongSelection,
    onSortChange: (SongSort) -> Unit,
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

            // Con scroll para que el gesto de "tirar para actualizar" también funcione aquí.
            LibraryUiState.Empty -> LazyColumn(Modifier.fillMaxSize()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.LibraryMusic,
                        title = "Aún no hay música",
                        message = "Copia canciones a tu teléfono y desliza hacia abajo para actualizar.",
                        modifier = Modifier.fillParentMaxSize(),
                        action = { FilledTonalButton(onClick = onRefresh) { Text("Buscar de nuevo") } },
                    )
                }
            }

            is LibraryUiState.Error -> LazyColumn(Modifier.fillMaxSize()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.CloudOff,
                        title = "No se pudo cargar tu música",
                        message = uiState.message,
                        modifier = Modifier.fillParentMaxSize(),
                        action = { FilledTonalButton(onClick = onRefresh) { Text("Reintentar") } },
                    )
                }
            }

            is LibraryUiState.Success -> Column(Modifier.fillMaxSize()) {
                val items = remember(uiState.songs) { uiState.songs.map { SelectableSong(it.id, it) } }
                if (selection.active) SelectionBar(selection, items)
                LazyColumn(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    item(key = "header", contentType = "header") {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (uiState.songs.size == 1) "1 canción" else "${uiState.songs.size} canciones",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            SortButton(sort, onSortChange, SongSortField.entries - SongSortField.ORIGINAL)
                        }
                    }
                    itemsIndexed(items, key = { _, item -> item.key }, contentType = { _, _ -> "song" }) { index, item ->
                        SelectableSongRow(
                            selection = selection,
                            item = item,
                            isCurrent = item.song.id == currentSongId,
                            onPlay = { onSongClick(index) },
                        )
                    }
                }
            }
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
        SongsContent(SongSort(), rememberSongSelection(), {}, LibraryUiState.Success(songs), false, 2L, {}, {}, {})
    }
}
