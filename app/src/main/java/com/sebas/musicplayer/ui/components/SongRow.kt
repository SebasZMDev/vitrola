package com.sebas.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sebas.musicplayer.domain.model.Song

/**
 * Se ejecuta después de tocar cualquier fila de canción (ya iniciada la reproducción). La app lo usa
 * para abrir la pantalla del reproductor si el usuario lo activó en Configuración.
 */
val LocalOnSongTap = compositionLocalOf<() -> Unit> { {} }

/**
 * Fila estándar de una canción. Úsala en todas las listas de canciones para que se vean igual.
 * [trailing] suele ser `{ SongMenuButton(song) }`.
 * Sin carátula ([showArtwork] = false) se puede pasar [trackNumber] para mostrar el número de pista.
 * La canción actual se marca con las barritas de [PlayingIndicator].
 * CONTRATO compartido: no cambiar la firma sin avisar al orquestador.
 */
@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
    trackNumber: Int? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val titleColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val onSongTap = LocalOnSongTap.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onClick()
                onSongTap()
            }
            .padding(start = 16.dp, end = if (trailing != null) 4.dp else 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val hasLeading = showArtwork || trackNumber != null
        if (showArtwork) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                SongArtwork(artworkUri = song.artworkUri, modifier = Modifier.size(48.dp))
                if (isCurrent) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        PlayingIndicator(color = Color.White)
                    }
                }
            }
        } else if (trackNumber != null) {
            Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                if (isCurrent) {
                    PlayingIndicator()
                } else {
                    Text(
                        trackNumber.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = if (hasLeading) 14.dp else 0.dp)) {
            Text(
                song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${song.artist} · ${formatDuration(song.durationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        trailing?.invoke()
    }
}

/** 185000 -> "3:05"; con horas, "1:02:03". */
fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
