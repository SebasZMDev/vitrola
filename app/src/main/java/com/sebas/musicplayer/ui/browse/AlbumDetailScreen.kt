package com.sebas.musicplayer.ui.browse

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.data.settings.SongSort
import com.sebas.musicplayer.domain.library.Album
import com.sebas.musicplayer.domain.library.ListDefaultSort
import com.sebas.musicplayer.domain.library.sortSongs
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.ui.components.ArtworkGlow
import com.sebas.musicplayer.ui.components.SongArtwork
import com.sebas.musicplayer.ui.components.SelectableSong
import com.sebas.musicplayer.ui.components.SelectableSongRow
import com.sebas.musicplayer.ui.components.SelectionBar
import com.sebas.musicplayer.ui.components.SortButton
import com.sebas.musicplayer.ui.components.formatDuration
import com.sebas.musicplayer.ui.components.rememberArtworkColor
import com.sebas.musicplayer.ui.components.rememberListSort
import com.sebas.musicplayer.ui.components.rememberSongSelection

@Composable
fun AlbumDetailRoute(albumId: Long, onBack: () -> Unit) {
    val viewModel: BrowseViewModel = viewModel(factory = BrowseViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val player by viewModel.playerState.collectAsStateWithLifecycle()
    // Solo recompone cuando cambia la canción, no con cada tick de posición (cada 500 ms).
    val currentSongId by remember { derivedStateOf { player.currentSong?.id } }
    // Los géneros reutilizan esta pantalla (sus ids son negativos).
    val loaded = state as? BrowseState.Loaded
    val album = (loaded?.albums?.firstOrNull { it.id == albumId } ?: loaded?.genres?.firstOrNull { it.id == albumId })
    val (sort, onSortChange) = rememberListSort("album:$albumId")
    AlbumDetailScreen(
        state = state,
        album = album,
        currentSongId = currentSongId,
        onBack = onBack,
        onRetry = viewModel::load,
        onPlay = viewModel::play,
        onShuffle = viewModel::shuffle,
        sort = sort,
        onSortChange = onSortChange,
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
    sort: SongSort,
    onSortChange: (SongSort) -> Unit,
) {
    // (número de pista, canción) en el orden visible: el número acompaña a su canción.
    val tracks = remember(album, sort) {
        sortSongs(album?.songs.orEmpty().mapIndexed { index, song -> index + 1 to song }, sort) { it.second }
    }
    val items = remember(tracks) { tracks.map { (_, song) -> SelectableSong(song.id, song) } }
    val visible = remember(tracks) { tracks.map { it.second } }
    val selection = rememberSongSelection()
    val glow = rememberArtworkColor(album?.artworkUri)
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ArtworkGlow(glow, Modifier.fillMaxWidth().height(380.dp))
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // Solo con el álbum cargado: la barra descarta las marcas que no estén en [items].
                if (selection.active && album != null) {
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
            BrowseStatus(state, "Álbum no encontrado", album == null, onRetry, Modifier.padding(padding)) {
                if (album != null) {
                    LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
                        item(key = "header", contentType = "header") {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                SongArtwork(
                                    album.artworkUri,
                                    Modifier.size(220.dp).shadow(20.dp, RoundedCornerShape(20.dp)),
                                    cornerRadius = 20.dp,
                                )
                                Text(
                                    album.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 20.dp),
                                )
                                Text(
                                    album.artist,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                                Text(
                                    "${pluralSongs(album.songs.size)} · ${formatDuration(album.durationMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                                PlayShuffleButtons(
                                    onPlay = { onPlay(visible, 0) },
                                    onShuffle = { onShuffle(visible) },
                                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                                )
                                Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.CenterEnd) {
                                    SortButton(sort, onSortChange)
                                }
                            }
                        }
                        itemsIndexed(items, key = { _, item -> item.key }, contentType = { _, _ -> "song" }) { index, item ->
                            SelectableSongRow(
                                selection = selection,
                                item = item,
                                isCurrent = item.song.id == currentSongId,
                                onPlay = { onPlay(visible, index) },
                                showArtwork = false,
                                trackNumber = tracks[index].first,
                            )
                        }
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
        AlbumDetailScreen(BrowseState.Loaded(listOf(album), emptyList()), album, 2L, {}, {}, { _, _ -> }, {}, ListDefaultSort, {})
    }
}
