package com.sebas.musicplayer.ui.browse

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.domain.library.Album
import com.sebas.musicplayer.domain.library.Artist
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.ui.components.SongArtwork
import com.sebas.musicplayer.ui.components.SongMenuButton
import com.sebas.musicplayer.ui.components.SongRow

@Composable
fun ArtistDetailRoute(artistName: String, onBack: () -> Unit, onAlbumClick: (Long) -> Unit) {
    val viewModel: BrowseViewModel = viewModel(factory = BrowseViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val player by viewModel.playerState.collectAsStateWithLifecycle()
    // Solo recompone cuando cambia la canción, no con cada tick de posición (cada 500 ms).
    val currentSongId by remember { derivedStateOf { player.currentSong?.id } }
    val artist = (state as? BrowseState.Loaded)?.artists?.firstOrNull { it.name == artistName }
    ArtistDetailScreen(
        state = state,
        artist = artist,
        artistName = artistName,
        currentSongId = currentSongId,
        onBack = onBack,
        onRetry = viewModel::load,
        onAlbumClick = onAlbumClick,
        onPlay = viewModel::play,
        onShuffle = viewModel::shuffle,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArtistDetailScreen(
    state: BrowseState,
    artist: Artist?,
    artistName: String,
    currentSongId: Long?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
    onShuffle: (List<Song>) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(artistName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
            )
        },
    ) { padding ->
        BrowseStatus(state, "Artista no encontrado", artist == null, onRetry, Modifier.padding(padding)) {
            if (artist != null) {
                LazyColumn(Modifier.padding(padding)) {
                    item(key = "header") {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            InitialCircle(artist.name, 120.dp, 48.sp)
                            Text(
                                artist.name,
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 16.dp),
                            )
                            Text(
                                "${pluralSongs(artist.songs.size)} · ${pluralAlbums(artist.albums.size)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            PlayShuffleButtons(
                                onPlay = { onPlay(artist.songs, 0) },
                                onShuffle = { onShuffle(artist.songs) },
                                modifier = Modifier.padding(top = 16.dp),
                            )
                        }
                    }
                    if (artist.albums.isNotEmpty()) {
                        item(key = "albums-title") { SectionTitle("Álbumes") }
                        item(key = "albums") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(artist.albums, key = { it.id }) { album ->
                                    Column(Modifier.width(140.dp).clickable { onAlbumClick(album.id) }) {
                                        SongArtwork(album.artworkUri, Modifier.size(140.dp), cornerRadius = 12.dp)
                                        Text(
                                            album.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 6.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item(key = "songs-title") { SectionTitle("Canciones") }
                    itemsIndexed(artist.songs, key = { _, song -> song.id }) { index, song ->
                        SongRow(
                            song = song,
                            isCurrent = song.id == currentSongId,
                            onClick = { onPlay(artist.songs, index) },
                            trailing = { SongMenuButton(song) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun ArtistDetailPreview() {
    val songs = (1L..3L).map { Song(it, Uri.EMPTY, "Canción $it", "Artista", "Álbum", 1L, 185_000L, null) }
    val album = Album(1L, "Álbum", "Artista", Uri.EMPTY, songs)
    val artist = Artist("Artista", songs, listOf(album))
    MaterialTheme {
        ArtistDetailScreen(
            BrowseState.Loaded(listOf(album), listOf(artist)), artist, "Artista", 1L, {}, {}, {}, { _, _ -> }, {},
        )
    }
}
