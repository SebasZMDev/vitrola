package com.sebas.musicplayer.ui.search

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.domain.library.Album
import com.sebas.musicplayer.domain.library.Artist
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.ui.components.SongArtwork
import com.sebas.musicplayer.ui.components.SongMenuButton
import com.sebas.musicplayer.ui.components.SongRow

/**
 * Pantalla de búsqueda (ruta propia). [onBack] vuelve a Home.
 * CONTRATO: la firma no cambia sin avisar al orquestador.
 */
@Composable
fun SearchRoute(onBack: () -> Unit, onAlbumClick: (Long) -> Unit, onArtistClick: (String) -> Unit) {
    val viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory)
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    SearchContent(
        query = query,
        results = results,
        currentSongId = playerState.currentSong?.id,
        onQueryChange = viewModel::onQueryChange,
        onBack = onBack,
        onSongClick = viewModel::onSongClick,
        onAlbumClick = onAlbumClick,
        onArtistClick = onArtistClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchContent(
    query: String,
    results: SearchResults,
    currentSongId: Long?,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onSongClick: (Int) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    var autoFocused by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!autoFocused) {
            autoFocused = true
            focusRequester.requestFocus()
        }
    }
    HideKeyboardOnScroll(listState) { keyboard?.hide() }

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.union(WindowInsets.ime),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                title = {
                    TextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Buscar canciones, álbumes, artistas") },
                        singleLine = true,
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Borrar")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )
                },
            )
        },
    ) { padding ->
        val trimmed = query.trim()
        when {
            trimmed.isEmpty() -> Message("Busca en tu música", Modifier.padding(padding))
            results.isEmpty -> Message("Sin resultados para «$trimmed»", Modifier.padding(padding))
            else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(padding)) {
                if (results.songs.isNotEmpty()) {
                    item(key = "h_songs", contentType = "header") { SectionHeader("Canciones") }
                    itemsIndexed(results.songs, key = { _, s -> "s${s.id}" }, contentType = { _, _ -> "song" }) { index, song ->
                        SongRow(
                            song = song,
                            isCurrent = song.id == currentSongId,
                            onClick = { onSongClick(index) },
                            trailing = { SongMenuButton(song) },
                        )
                    }
                }
                if (results.albums.isNotEmpty()) {
                    item(key = "h_albums", contentType = "header") { SectionHeader("Álbumes") }
                    itemsIndexed(results.albums, key = { _, a -> "a${a.id}" }, contentType = { _, _ -> "album" }) { _, album ->
                        AlbumResultRow(album, onClick = { onAlbumClick(album.id) })
                    }
                }
                if (results.artists.isNotEmpty()) {
                    item(key = "h_artists", contentType = "header") { SectionHeader("Artistas") }
                    itemsIndexed(results.artists, key = { _, a -> "r${a.name}" }, contentType = { _, _ -> "artist" }) { _, artist ->
                        ArtistResultRow(artist, onClick = { onArtistClick(artist.name) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HideKeyboardOnScroll(listState: LazyListState, hide: () -> Unit) {
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { if (it) hide() }
    }
}

@Composable
private fun Message(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun AlbumResultRow(album: Album, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SongArtwork(artworkUri = album.artworkUri, modifier = Modifier.size(48.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(album.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${album.artist} · ${album.songs.size} canciones",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ArtistResultRow(artist: Artist, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    artist.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(artist.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${artist.songs.size} canciones",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchContentPreview() {
    val songs = List(3) {
        Song(it.toLong(), Uri.EMPTY, "Canción $it", "Artista", "Álbum", 1L, 200_000L, null)
    }
    val album = Album(1L, "Álbum", "Artista", null, songs)
    MaterialTheme {
        SearchContent(
            query = "a",
            results = SearchResults(songs, listOf(album), listOf(Artist("Artista", songs, listOf(album)))),
            currentSongId = 1L,
            onQueryChange = {}, onBack = {}, onSongClick = {}, onAlbumClick = {}, onArtistClick = {},
        )
    }
}
