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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.sp
import com.sebas.musicplayer.ui.browse.ArtistAvatar
import com.sebas.musicplayer.ui.components.EmptyState
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
import androidx.compose.runtime.derivedStateOf
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
import com.sebas.musicplayer.data.settings.SongSort
import com.sebas.musicplayer.domain.library.ListDefaultSort
import com.sebas.musicplayer.domain.library.sortSongs
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.ui.components.SelectableSong
import com.sebas.musicplayer.ui.components.SelectableSongRow
import com.sebas.musicplayer.ui.components.SelectionBar
import com.sebas.musicplayer.ui.components.SongArtwork
import com.sebas.musicplayer.ui.components.SongSelection
import com.sebas.musicplayer.ui.components.SortButton
import com.sebas.musicplayer.ui.components.rememberListSort
import com.sebas.musicplayer.ui.components.rememberSongSelection

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
    // Solo recompone cuando cambia la canción, no con cada tick de posición (cada 500 ms).
    val currentSongId by remember { derivedStateOf { playerState.currentSong?.id } }

    val (sort, onSortChange) = rememberListSort("search")
    // Canciones en el orden visible: se ordenan los resultados ya recortados.
    val items = remember(results.songs, sort) {
        sortSongs(results.songs, sort) { it }.map { SelectableSong(it.id, it) }
    }
    val visible = remember(items) { items.map { it.song } }
    val selection = rememberSongSelection()

    SearchContent(
        query = query,
        results = results,
        items = items,
        selection = selection,
        sort = sort,
        onSortChange = onSortChange,
        currentSongId = currentSongId,
        // Las marcas se borran al cambiar el texto, no al recomponer ni al rotar.
        onQueryChange = { text ->
            if (text != query) selection.clear()
            viewModel.onQueryChange(text)
        },
        onBack = onBack,
        onSongClick = { index -> viewModel.onSongClick(visible, index) },
        onAlbumClick = onAlbumClick,
        onArtistClick = onArtistClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchContent(
    query: String,
    results: SearchResults,
    items: List<SelectableSong>,
    selection: SongSelection,
    sort: SongSort,
    onSortChange: (SongSort) -> Unit,
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
    // Solo con resultados a la vista: la barra descarta las marcas que no estén en [items].
    val selecting = selection.active && items.isNotEmpty()
    LaunchedEffect(selecting) { if (selecting) keyboard?.hide() }

    Scaffold(
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.union(WindowInsets.ime),
        topBar = {
            if (selecting) {
                SelectionBar(selection = selection, items = items, modifier = Modifier.statusBarsPadding())
            } else Row(
                modifier = Modifier.statusBarsPadding().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                }
                val fieldColor = MaterialTheme.colorScheme.surfaceContainerHigh
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Canciones, álbumes o artistas", maxLines = 1) },
                    singleLine = true,
                    shape = CircleShape,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
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
                        focusedContainerColor = fieldColor,
                        unfocusedContainerColor = fieldColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                )
            }
        },
    ) { padding ->
        val trimmed = query.trim()
        when {
            trimmed.isEmpty() -> EmptyState(
                icon = Icons.Filled.Search,
                title = "Busca en tu música",
                message = "Escribe el nombre de una canción, un álbum o un artista.",
                modifier = Modifier.padding(padding),
            )
            results.isEmpty -> EmptyState(
                icon = Icons.Filled.SearchOff,
                title = "Sin resultados",
                message = "No encontramos nada para «$trimmed». Revisa cómo está escrito.",
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                if (results.songs.isNotEmpty()) {
                    item(key = "h_songs", contentType = "header") {
                        Row(verticalAlignment = Alignment.Bottom) {
                            SectionHeader("Canciones", Modifier.weight(1f))
                            Box(Modifier.padding(end = 8.dp)) { SortButton(sort, onSortChange) }
                        }
                    }
                    itemsIndexed(items, key = { _, item -> "s${item.key}" }, contentType = { _, _ -> "song" }) { index, item ->
                        SelectableSongRow(
                            selection = selection,
                            item = item,
                            isCurrent = item.song.id == currentSongId,
                            onPlay = { onSongClick(index) },
                        )
                    }
                }
                if (results.albums.isNotEmpty()) {
                    item(key = "h_albums", contentType = "header") { SectionHeader("Álbumes") }
                    itemsIndexed(results.albums, key = { _, a -> "a${a.id}" }, contentType = { _, _ -> "album" }) { _, album ->
                        AlbumResultRow(album, onClick = { if (!selecting) onAlbumClick(album.id) })
                    }
                }
                if (results.artists.isNotEmpty()) {
                    item(key = "h_artists", contentType = "header") { SectionHeader("Artistas") }
                    itemsIndexed(results.artists, key = { _, a -> "r${a.name}" }, contentType = { _, _ -> "artist" }) { _, artist ->
                        ArtistResultRow(artist, onClick = { if (!selecting) onArtistClick(artist.name) })
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
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun AlbumResultRow(album: Album, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SongArtwork(artworkUri = album.artworkUri, modifier = Modifier.size(48.dp))
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(album.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "Álbum · ${album.artist}",
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
        ArtistAvatar(artist, 48.dp, 20.sp)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(artist.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (artist.songs.size == 1) "Artista · 1 canción" else "Artista · ${artist.songs.size} canciones",
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
            items = songs.map { SelectableSong(it.id, it) },
            selection = rememberSongSelection(),
            sort = ListDefaultSort,
            onSortChange = {},
            currentSongId = 1L,
            onQueryChange = {}, onBack = {}, onSongClick = {}, onAlbumClick = {}, onArtistClick = {},
        )
    }
}
