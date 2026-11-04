package com.sebas.musicplayer.ui.browse

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.domain.library.Album
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.ui.components.SongArtwork
import com.sebas.musicplayer.ui.components.SongMenuButton
import com.sebas.musicplayer.ui.components.SongRow
import com.sebas.musicplayer.ui.components.formatDuration

@Composable
fun AlbumDetailRoute(albumId: Long, onBack: () -> Unit) {
    val viewModel: BrowseViewModel = viewModel(factory = BrowseViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val player by viewModel.playerState.collectAsStateWithLifecycle()
    val album = (state as? BrowseState.Loaded)?.albums?.firstOrNull { it.id == albumId }
    AlbumDetailScreen(
        state = state,
        album = album,
        currentSongId = player.currentSong?.id,
        onBack = onBack,
        onRetry = viewModel::load,
        onPlay = viewModel::play,
        onShuffle = viewModel::shuffle,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlbumDetailScreen(
    state: BrowseState,
    album: Album?,
    currentSongId: Long?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
    onShuffle: (List<Song>) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
    ) { padding ->
        BrowseStatus(state, "Álbum no encontrado", album == null, onRetry, Modifier.padding(padding)) {
            if (album != null) {
                LazyColumn(Modifier.padding(padding)) {
                    item(key = "header") {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            SongArtwork(album.artworkUri, Modifier.size(200.dp), cornerRadius = 16.dp)
                            Text(
                                album.title,
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 16.dp),
                            )
                            Text(
                                album.artist,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "${pluralSongs(album.songs.size)} · ${formatDuration(album.durationMs)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            PlayShuffleButtons(
                                onPlay = { onPlay(album.songs, 0) },
                                onShuffle = { onShuffle(album.songs) },
                                modifier = Modifier.padding(top = 16.dp),
                            )
                        }
                    }
                    itemsIndexed(album.songs, key = { _, song -> song.id }) { index, song ->
                        SongRow(
                            song = song,
                            isCurrent = song.id == currentSongId,
                            onClick = { onPlay(album.songs, index) },
                            showArtwork = false,
                            trailing = { SongMenuButton(song) },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AlbumDetailPreview() {
    val songs = (1L..4L).map { Song(it, Uri.EMPTY, "Canción $it", "Artista", "Álbum", 1L, 185_000L, null) }
    val album = Album(1L, "Álbum", "Artista", Uri.EMPTY, songs)
    MaterialTheme {
        AlbumDetailScreen(BrowseState.Loaded(listOf(album), emptyList()), album, 2L, {}, {}, { _, _ -> }, {})
    }
}
