package com.sebas.musicplayer.ui.browse

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.data.settings.SongSort
import com.sebas.musicplayer.domain.library.Album
import com.sebas.musicplayer.domain.library.Artist
import com.sebas.musicplayer.domain.library.ListDefaultSort
import com.sebas.musicplayer.domain.library.sortSongs
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.ui.components.ArtworkGlow
import com.sebas.musicplayer.ui.components.SongArtwork
import com.sebas.musicplayer.ui.components.SelectableSong
import com.sebas.musicplayer.ui.components.SelectableSongRow
import com.sebas.musicplayer.ui.components.SelectionBar
import com.sebas.musicplayer.ui.components.SortButton
import com.sebas.musicplayer.ui.components.rememberArtworkColor
import com.sebas.musicplayer.ui.components.rememberListSort
import com.sebas.musicplayer.ui.components.rememberSongSelection

@Composable
fun ArtistDetailRoute(artistName: String, onBack: () -> Unit, onAlbumClick: (Long) -> Unit) {
    val viewModel: BrowseViewModel = viewModel(factory = BrowseViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val player by viewModel.playerState.collectAsStateWithLifecycle()
    // Solo recompone cuando cambia la canción, no con cada tick de posición (cada 500 ms).
    val currentSongId by remember { derivedStateOf { player.currentSong?.id } }
    val artist = (state as? BrowseState.Loaded)?.artists?.firstOrNull { it.name == artistName }
    val (sort, onSortChange) = rememberListSort("artist:$artistName")
    ArtistDetailScreen(
        state = state,
        artist = artist,
        currentSongId = currentSongId,
        onBack = onBack,
        onRetry = viewModel::load,
        onAlbumClick = onAlbumClick,
        onPlay = viewModel::play,
        onShuffle = viewModel::shuffle,
        sort = sort,
        onSortChange = onSortChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArtistDetailScreen(
    state: BrowseState,
    artist: Artist?,
    currentSongId: Long?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    onPlay: (List<Song>, Int) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    sort: SongSort,
    onSortChange: (SongSort) -> Unit,
) {
    // Filas en el orden visible; reproducir y aleatorio usan este mismo orden.
    val items = remember(artist, sort) {
        sortSongs(artist?.songs.orEmpty(), sort) { it }.map { SelectableSong(it.id, it) }
    }
    val visible = remember(items) { items.map { it.song } }
    val selection = rememberSongSelection()
    val glow = rememberArtworkColor(artist?.albums?.firstNotNullOfOrNull { it.artworkUri })
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ArtworkGlow(glow, Modifier.fillMaxWidth().height(340.dp))
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // Solo con el artista cargado: la barra descarta las marcas que no estén en [items].
                if (selection.active && artist != null) {
                    SelectionBar(selection = selection, items = items, modifier = Modifier.statusBarsPadding())
                } else {
                    TopAppBar(
                        title = {},
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    )
                }
            },
        ) { padding ->
            BrowseStatus(
                state, "Artista no encontrado", artist == null, onRetry, Modifier.padding(padding),
                emptyIcon = Icons.Filled.Person,
            ) {
                if (artist != null) {
                    LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
                        item(key = "header", contentType = "header") {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                ArtistAvatar(artist, 148.dp, 56.sp)
                                Text(
                                    artist.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 20.dp),
                                )
                                Text(
                                    "${pluralSongs(artist.songs.size)} · ${pluralAlbums(artist.albums.size)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                                PlayShuffleButtons(
                                    onPlay = { onPlay(visible, 0) },
                                    onShuffle = { onShuffle(visible) },
                                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                                )
                            }
                        }
                        if (artist.albums.isNotEmpty()) {
                            item(key = "albums-title", contentType = "title") { SectionTitle("Álbumes") }
                            item(key = "albums", contentType = "albums") {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    items(artist.albums, key = { it.id }) { album ->
                                        Column(
                                            Modifier
                                                .width(148.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable { onAlbumClick(album.id) }
                                                .padding(4.dp),
                                        ) {
                                            SongArtwork(album.artworkUri, Modifier.size(140.dp), cornerRadius = 12.dp)
                                            Text(
                                                album.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 8.dp),
                                            )
                                            Text(
                                                pluralSongs(album.songs.size),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 4.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item(key = "songs-title", contentType = "title") {
                            Row(
                                Modifier.fillMaxWidth().padding(end = 8.dp),
                                verticalAlignment = Alignment.Bottom,
                            ) {
                                Box(Modifier.weight(1f)) { SectionTitle("Canciones") }
                                SortButton(sort, onSortChange)
                            }
                        }
                        itemsIndexed(items, key = { _, item -> item.key }, contentType = { _, _ -> "song" }) { index, item ->
                            SelectableSongRow(
                                selection = selection,
                                item = item,
                                isCurrent = item.song.id == currentSongId,
                                onPlay = { onPlay(visible, index) },
                            )
                        }
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
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
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
            BrowseState.Loaded(listOf(album), listOf(artist)), artist, 1L, {}, {}, {}, { _, _ -> }, {}, ListDefaultSort, {},
        )
    }
}
