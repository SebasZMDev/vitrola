package com.sebas.musicplayer.ui.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private val sections = listOf("Géneros", "Álbumes", "Artistas")
private const val SECTION_KEY = "explore_section"

/** Álbumes, artistas y géneros en una sola pestaña, con un selector arriba que recuerda la última elección. */
@Composable
fun ExploreTab(onAlbumClick: (Long) -> Unit, onArtistClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val viewModel: BrowseViewModel = viewModel(factory = BrowseViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val prefs = LocalContext.current.applicationContext.getSharedPreferences("ui_state", 0)
    var section by remember { mutableStateOf(prefs.getString(SECTION_KEY, null)?.takeIf { it in sections } ?: sections[0]) }

    Column(modifier) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            sections.forEachIndexed { index, name ->
                SegmentedButton(
                    selected = section == name,
                    onClick = { section = name; prefs.edit { putString(SECTION_KEY, name) } },
                    shape = SegmentedButtonDefaults.itemShape(index, sections.size),
                ) { Text(name, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
            }
        }
        val loaded = state as? BrowseState.Loaded
        when (section) {
            "Géneros" -> AlbumGrid(
                state, loaded?.genres.orEmpty(),
                "Aún no hay géneros. Ponlos al editar una canción o importando un JSON.",
                onAlbumClick, viewModel::load, showCount = true,
            )
            "Álbumes" -> AlbumsContent(state, onAlbumClick, viewModel::load)
            else -> ArtistsContent(state, onArtistClick, viewModel::load)
        }
    }
}
