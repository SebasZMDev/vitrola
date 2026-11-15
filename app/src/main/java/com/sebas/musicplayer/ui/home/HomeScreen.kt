package com.sebas.musicplayer.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.sebas.musicplayer.ui.browse.AlbumsTab
import com.sebas.musicplayer.ui.browse.ArtistsTab
import com.sebas.musicplayer.ui.library.SongsTab
import com.sebas.musicplayer.ui.playlists.PlaylistsTab
import kotlinx.coroutines.launch

private val tabs = listOf("Canciones", "Playlists", "Álbumes", "Artistas")

/** Orden antiguo de las pestañas: el índice guardado antes del 2026-11-15 apunta a esta lista. */
private val legacyTabs = listOf("Canciones", "Álbumes", "Artistas", "Playlists")
private const val TAB_KEY = "home_tab_name"

/** Pantalla principal: título, barra de búsqueda y pestañas deslizables. Cada pestaña es de un agente distinto. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (Long) -> Unit,
) {
    val prefs = LocalContext.current.applicationContext.getSharedPreferences("ui_state", 0)
    // Se guarda el nombre de la pestaña (no su posición) para que cambiar el orden no la confunda.
    // Si solo existe el índice del orden antiguo, se traduce a su nombre.
    val savedTab = prefs.getString(TAB_KEY, null)
        ?: legacyTabs.getOrNull(prefs.getInt("home_tab", 0))
    val pagerState = rememberPagerState(
        initialPage = tabs.indexOf(savedTab).coerceAtLeast(0),
        pageCount = { tabs.size },
    )
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page -> prefs.edit { putString(TAB_KEY, tabs[page]) } }
    }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            Column(Modifier.statusBarsPadding()) {
                Text(
                    "Mi música",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
                )
                SearchPill(onClick = onOpenSearch)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                divider = {},
                modifier = Modifier.padding(top = 4.dp),
            ) {
                tabs.forEachIndexed { index, title ->
                    val selected = pagerState.currentPage == index
                    Tab(
                        selected = selected,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = {
                            Text(
                                title,
                                maxLines = 1,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            )
                        },
                    )
                }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f), beyondViewportPageCount = 1) { page ->
                val pageModifier = Modifier.fillMaxSize()
                when (page) {
                    0 -> SongsTab(modifier = pageModifier)
                    1 -> PlaylistsTab(onPlaylistClick = onPlaylistClick, modifier = pageModifier)
                    2 -> AlbumsTab(onAlbumClick = onAlbumClick, modifier = pageModifier)
                    else -> ArtistsTab(onArtistClick = onArtistClick, modifier = pageModifier)
                }
            }
        }
    }
}

/** Parece un campo de búsqueda, pero es un botón: al tocarlo se abre la pantalla de búsqueda con el teclado. */
@Composable
private fun SearchPill(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(48.dp)
            .semantics { role = Role.Button },
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Buscar canciones, álbumes o artistas",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
