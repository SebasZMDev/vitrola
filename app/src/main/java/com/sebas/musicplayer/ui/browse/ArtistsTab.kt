package com.sebas.musicplayer.ui.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.domain.library.Artist

@Composable
fun ArtistsTab(onArtistClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: BrowseViewModel = viewModel(factory = BrowseViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    ArtistsContent(state, onArtistClick, viewModel::load, modifier)
}

@Composable
internal fun ArtistsContent(
    state: BrowseState,
    onArtistClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artists = (state as? BrowseState.Loaded)?.artists.orEmpty()
    BrowseStatus(state, "No hay artistas", artists.isEmpty(), onRetry, modifier) {
        LazyColumn(modifier.fillMaxSize()) {
            items(artists, key = { it.name }) { artist ->
                ArtistRow(artist, onClick = { onArtistClick(artist.name) })
            }
        }
    }
}

@Composable
private fun ArtistRow(artist: Artist, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InitialCircle(artist.name, 48.dp, 20.sp)
        Column(Modifier.padding(start = 16.dp)) {
            Text(artist.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${pluralSongs(artist.songs.size)} · ${pluralAlbums(artist.albums.size)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ArtistsContentPreview() {
    val artists = listOf(Artist("Soda Stereo", emptyList(), emptyList()), Artist("Zoé", emptyList(), emptyList()))
    MaterialTheme { ArtistsContent(BrowseState.Loaded(emptyList(), artists), {}, {}) }
}
