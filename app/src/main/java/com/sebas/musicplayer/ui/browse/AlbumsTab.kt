package com.sebas.musicplayer.ui.browse

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.domain.library.Album
import com.sebas.musicplayer.ui.components.SongArtwork

@Composable
fun AlbumsTab(onAlbumClick: (Long) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: BrowseViewModel = viewModel(factory = BrowseViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    AlbumsContent(state, onAlbumClick, viewModel::load, modifier)
}

@Composable
internal fun AlbumsContent(
    state: BrowseState,
    onAlbumClick: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val albums = (state as? BrowseState.Loaded)?.albums.orEmpty()
    BrowseStatus(state, "No hay álbumes", albums.isEmpty(), onRetry, modifier) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(albums, key = { it.id }) { album ->
                AlbumCard(album, onClick = { onAlbumClick(album.id) })
            }
        }
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        SongArtwork(album.artworkUri, Modifier.fillMaxWidth().aspectRatio(1f), cornerRadius = 12.dp)
        Text(
            album.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AlbumsContentPreview() {
    val albums = (1L..4L).map { Album(it, "Álbum $it", "Artista $it", Uri.EMPTY, emptyList()) }
    MaterialTheme { AlbumsContent(BrowseState.Loaded(albums, emptyList()), {}, {}) }
}
