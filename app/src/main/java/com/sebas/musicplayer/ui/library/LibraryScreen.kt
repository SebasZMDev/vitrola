package com.sebas.musicplayer.ui.library

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.playback.PlayerState
import com.sebas.musicplayer.ui.components.SongArtwork
import com.sebas.musicplayer.ui.player.PlayerBar
import com.sebas.musicplayer.ui.theme.MusicPlayerTheme

@Composable
fun LibraryRoute(
    onOpenNowPlaying: () -> Unit = {},
    viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadSongs() }

    LibraryScreen(
        uiState = uiState,
        playerState = playerState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        onSongClick = viewModel::onSongClick,
        onPlayPause = viewModel::togglePlayPause,
        onNext = viewModel::next,
        onPrevious = viewModel::previous,
        onPlayerBarClick = onOpenNowPlaying,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    playerState: PlayerState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSongClick: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onPlayerBarClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val count = (uiState as? LibraryUiState.Success)?.songs?.size
                    Column {
                        Text("Mi música")
                        if (count != null) {
                            Text(
                                if (count == 1) "1 canción" else "$count canciones",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            PlayerBar(
                state = playerState,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onClick = onPlayerBarClick,
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when (uiState) {
                LibraryUiState.Loading -> CenteredContent { CircularProgressIndicator() }
                LibraryUiState.Empty -> CenteredContent {
                    Text("No se encontró música en el dispositivo", textAlign = TextAlign.Center)
                }
                is LibraryUiState.Error -> CenteredContent {
                    Text(uiState.message, textAlign = TextAlign.Center)
                    Button(onClick = onRefresh) { Text("Reintentar") }
                }
                is LibraryUiState.Success -> SongList(
                    songs = uiState.songs,
                    currentSongId = playerState.currentSong?.id,
                    onSongClick = onSongClick,
                )
            }
        }
    }
}

@Composable
private fun CenteredContent(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

@Composable
private fun SongList(songs: List<Song>, currentSongId: Long?, onSongClick: (Int) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            SongRow(song = song, isCurrent = song.id == currentSongId, onClick = { onSongClick(index) })
        }
    }
}

@Composable
private fun SongRow(song: Song, isCurrent: Boolean, onClick: () -> Unit) {
    val titleColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SongArtwork(artworkUri = song.artworkUri, modifier = Modifier.size(44.dp))
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${song.artist} · ${formatDuration(song.durationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Preview(showBackground = true)
@Composable
private fun LibraryScreenPreview() {
    val songs = List(8) {
        Song(it.toLong(), Uri.EMPTY, "Canción de ejemplo ${it + 1}", "Artista", "Álbum", 1, 185_000L + it * 7_000, null)
    }
    MusicPlayerTheme {
        LibraryScreen(
            uiState = LibraryUiState.Success(songs),
            playerState = PlayerState(currentSong = songs[2], isPlaying = true, positionMs = 50_000, durationMs = 199_000),
            isRefreshing = false,
            onRefresh = {},
            onSongClick = {},
            onPlayPause = {},
            onNext = {},
            onPrevious = {},
            onPlayerBarClick = {},
        )
    }
}
