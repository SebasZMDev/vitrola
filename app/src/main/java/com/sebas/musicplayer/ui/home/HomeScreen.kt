package com.sebas.musicplayer.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.sebas.musicplayer.ui.browse.AlbumsTab
import com.sebas.musicplayer.ui.browse.ArtistsTab
import com.sebas.musicplayer.ui.library.SongsTab
import com.sebas.musicplayer.ui.playlists.PlaylistsTab
import kotlinx.coroutines.launch

private val tabs = listOf("Canciones", "Álbumes", "Artistas", "Playlists")

/** Pantalla principal: barra superior con búsqueda + pestañas deslizables. Cada pestaña es de un agente distinto. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (Long) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi música") },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title, maxLines = 1) },
                    )
                }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f), beyondViewportPageCount = 1) { page ->
                val pageModifier = Modifier.fillMaxSize()
                when (page) {
                    0 -> SongsTab(modifier = pageModifier)
                    1 -> AlbumsTab(onAlbumClick = onAlbumClick, modifier = pageModifier)
                    2 -> ArtistsTab(onArtistClick = onArtistClick, modifier = pageModifier)
                    else -> PlaylistsTab(onPlaylistClick = onPlaylistClick, modifier = pageModifier)
                }
            }
        }
    }
}
