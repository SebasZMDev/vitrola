package com.sebas.musicplayer.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Carátula de una canción. El tamaño lo decide quien la usa vía [modifier].
 * Si no hay imagen (null, cargando o error), muestra un placeholder con una nota musical.
 * CONTRATO: la firma no cambia sin avisar al orquestador.
 */
@Composable
fun SongArtwork(
    artworkUri: Uri?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
) {
    val clipped = modifier.clip(RoundedCornerShape(cornerRadius))
    if (artworkUri == null || LocalInspectionMode.current) {
        ArtworkPlaceholder(modifier = clipped)
        return
    }
    // El placeholder queda debajo: se ve mientras carga o si no hay carátula.
    // AsyncImage (sin subcomposición) es lo recomendado por Coil para listas largas.
    Box(modifier = clipped) {
        ArtworkPlaceholder(Modifier.fillMaxSize())
        AsyncImage(
            model = artworkUri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
internal fun ArtworkPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxSize(0.5f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArtworkPlaceholderSmallPreview() {
    SongArtwork(artworkUri = null, modifier = Modifier.size(48.dp))
}

@Preview(showBackground = true)
@Composable
private fun ArtworkPlaceholderLargePreview() {
    SongArtwork(artworkUri = null, modifier = Modifier.size(200.dp), cornerRadius = 16.dp)
}
