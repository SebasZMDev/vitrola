package com.sebas.musicplayer.ui.player

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.playback.PlayerState
import com.sebas.musicplayer.ui.components.SongArtwork
import com.sebas.musicplayer.ui.components.rememberArtworkColor
import com.sebas.musicplayer.ui.theme.MusicPlayerTheme

/**
 * Mini reproductor flotante. Tocar abre "Reproduciendo ahora"; deslizar a la izquierda o a la
 * derecha pasa a la canción siguiente o anterior. Se tiñe con el color de la carátula.
 */
@Composable
fun PlayerBar(
    state: PlayerState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val song = state.currentSong ?: return
    val progress = if (state.durationMs > 0) {
        (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val colors = MaterialTheme.colorScheme
    val tint = rememberArtworkColor(song.artworkUri)
    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnPrevious by rememberUpdatedState(onPrevious)

    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = tint.copy(alpha = 0.22f).compositeOver(colors.surfaceContainerHigh),
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    var dragged = 0f
                    val threshold = 56.dp.toPx()
                    detectHorizontalDragGestures(
                        onDragStart = { dragged = 0f },
                        onDragEnd = {
                            if (dragged <= -threshold) currentOnNext()
                            if (dragged >= threshold) currentOnPrevious()
                        },
                        onHorizontalDrag = { _, amount -> dragged += amount },
                    )
                },
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .clickable(onClickLabel = "Abrir reproductor", onClick = onClick)
                        .padding(start = 10.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SongArtwork(artworkUri = song.artworkUri, modifier = Modifier.size(56.dp), cornerRadius = 12.dp)
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(
                            song.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onPlayPause, modifier = Modifier.size(52.dp)) {
                        Icon(
                            if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pausar" else "Reproducir",
                            modifier = Modifier.size(36.dp),
                        )
                    }
                    IconButton(onClick = onNext, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", modifier = Modifier.size(32.dp))
                    }
                }
                Box(Modifier.fillMaxWidth().height(2.dp).background(colors.onSurface.copy(alpha = 0.12f))) {
                    Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(colors.onSurface))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlayerBarPreview() {
    MusicPlayerTheme {
        PlayerBar(
            state = PlayerState(
                currentSong = Song(1, Uri.EMPTY, "Canción de ejemplo", "Artista", "Álbum", 1, 200_000, null),
                isPlaying = true,
                positionMs = 60_000,
                durationMs = 200_000,
            ),
            onPlayPause = {},
            onNext = {},
            onPrevious = {},
        )
    }
}
