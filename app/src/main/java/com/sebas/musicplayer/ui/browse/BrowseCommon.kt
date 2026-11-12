package com.sebas.musicplayer.ui.browse

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sebas.musicplayer.domain.library.Artist
import com.sebas.musicplayer.ui.components.EmptyState

@Composable
internal fun BrowseStatus(
    state: BrowseState,
    emptyMessage: String,
    isEmpty: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    emptyIcon: ImageVector = Icons.Filled.LibraryMusic,
    content: @Composable () -> Unit,
) {
    when {
        state is BrowseState.Loading -> Box(modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        state is BrowseState.Error -> EmptyState(
            icon = Icons.Filled.CloudOff,
            title = "No se pudo cargar la biblioteca",
            modifier = modifier,
            action = { FilledTonalButton(onClick = onRetry) { Text("Reintentar") } },
        )
        isEmpty -> EmptyState(icon = emptyIcon, title = emptyMessage, modifier = modifier)
        else -> content()
    }
}

@Composable
internal fun InitialCircle(name: String, size: Dp, fontSize: TextUnit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?",
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = colors.onPrimaryContainer,
        )
    }
}

/**
 * Foto redonda del artista: la carátula de uno de sus álbumes y, debajo, su inicial
 * (que es lo que se ve mientras carga o si no hay carátula).
 */
@Composable
internal fun ArtistAvatar(artist: Artist, size: Dp, fontSize: TextUnit, modifier: Modifier = Modifier) {
    val artworkUri: Uri? = artist.albums.firstNotNullOfOrNull { it.artworkUri }
        ?: artist.songs.firstNotNullOfOrNull { it.artworkUri }
    Box(modifier.size(size)) {
        InitialCircle(artist.name, size, fontSize)
        if (artworkUri != null) {
            AsyncImage(
                model = artworkUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/** Par de botones "Reproducir" y "Aleatorio" a todo el ancho. Lo usan todas las cabeceras de listas. */
@Composable
internal fun PlayShuffleButtons(
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onPlay, enabled = enabled, modifier = Modifier.weight(1f).height(48.dp)) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Text("Reproducir", maxLines = 1, modifier = Modifier.padding(start = 8.dp))
        }
        FilledTonalButton(onClick = onShuffle, enabled = enabled, modifier = Modifier.weight(1f).height(48.dp)) {
            Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("Aleatorio", maxLines = 1, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

internal fun pluralSongs(n: Int) = if (n == 1) "1 canción" else "$n canciones"
internal fun pluralAlbums(n: Int) = if (n == 1) "1 álbum" else "$n álbumes"
