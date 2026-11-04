package com.sebas.musicplayer.ui.player

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.playback.PlayerState
import com.sebas.musicplayer.ui.components.SongArtwork
import com.sebas.musicplayer.ui.theme.MusicPlayerTheme

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
    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 3.dp) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Row(
                modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SongArtwork(artworkUri = song.artworkUri, modifier = Modifier.size(40.dp), cornerRadius = 6.dp)
                Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior") }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pausar" else "Reproducir",
                    )
                }
                IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, contentDescription = "Siguiente") }
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
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
