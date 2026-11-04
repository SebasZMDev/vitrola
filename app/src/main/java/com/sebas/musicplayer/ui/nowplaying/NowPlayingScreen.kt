package com.sebas.musicplayer.ui.nowplaying

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.playback.PlayerState
import com.sebas.musicplayer.playback.RepeatMode
import com.sebas.musicplayer.ui.components.SongArtwork
import com.sebas.musicplayer.ui.theme.MusicPlayerTheme

@Composable
fun NowPlayingRoute(
    onBack: () -> Unit,
    viewModel: NowPlayingViewModel = viewModel(factory = NowPlayingViewModel.Factory),
) {
    val state by viewModel.playerState.collectAsStateWithLifecycle()
    NowPlayingScreen(
        state = state,
        onBack = onBack,
        onTogglePlayPause = viewModel::togglePlayPause,
        onNext = viewModel::next,
        onPrevious = viewModel::previous,
        onSeekTo = viewModel::seekTo,
        onToggleShuffle = viewModel::toggleShuffle,
        onCycleRepeat = viewModel::cycleRepeatMode,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    state: PlayerState,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text("Reproduciendo ahora", style = MaterialTheme.typography.titleSmall)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Cerrar")
                    }
                },
            )
        },
    ) { padding ->
        val song = state.currentSong
        if (song == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No hay nada en reproducción",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(16.dp))
                SongArtwork(
                    artworkUri = song.artworkUri,
                    // Cuadrada y lo más grande posible, pero se encoge en pantallas bajas para no empujar los controles.
                    modifier = Modifier.weight(1f, fill = false).aspectRatio(1f, matchHeightConstraintsFirst = true),
                    cornerRadius = 16.dp,
                )
                Spacer(Modifier.height(32.dp))
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = song.album,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(24.dp))
                ProgressSection(
                    positionMs = state.positionMs,
                    durationMs = state.durationMs,
                    onSeekTo = onSeekTo,
                )
                Spacer(Modifier.height(8.dp))
                Controls(
                    state = state,
                    onTogglePlayPause = onTogglePlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ProgressSection(
    positionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val enabled = durationMs > 0
    val fraction = dragFraction
        ?: if (enabled) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val shownMs = if (enabled) (fraction * durationMs).toLong() else positionMs

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = fraction,
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                dragFraction?.let { onSeekTo((it * durationMs).toLong()) }
                dragFraction = null
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatTime(shownMs), style = MaterialTheme.typography.labelMedium)
            Text(formatTime(durationMs), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Controls(
    state: PlayerState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggleShuffle) {
            Icon(
                Icons.Filled.Shuffle,
                contentDescription = "Aleatorio",
                tint = if (state.shuffleEnabled) active else inactive,
            )
        }
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Anterior", modifier = Modifier.size(36.dp))
        }
        FilledIconButton(onClick = onTogglePlayPause, modifier = Modifier.size(72.dp)) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (state.isPlaying) "Pausar" else "Reproducir",
                modifier = Modifier.size(40.dp),
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Siguiente", modifier = Modifier.size(36.dp))
        }
        IconButton(onClick = onCycleRepeat) {
            Icon(
                imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                contentDescription = "Repetir",
                tint = if (state.repeatMode == RepeatMode.OFF) inactive else active,
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Preview(showBackground = true)
@Composable
private fun NowPlayingScreenPreview() {
    MusicPlayerTheme {
        NowPlayingScreen(
            state = PlayerState(
                currentSong = Song(
                    id = 1,
                    uri = Uri.EMPTY,
                    title = "Canción de ejemplo",
                    artist = "Artista",
                    album = "Álbum",
                    albumId = 1,
                    durationMs = 215_000,
                    artworkUri = null,
                ),
                isPlaying = true,
                positionMs = 72_000,
                durationMs = 215_000,
                shuffleEnabled = true,
                repeatMode = RepeatMode.ALL,
            ),
            onBack = {},
            onTogglePlayPause = {},
            onNext = {},
            onPrevious = {},
            onSeekTo = {},
            onToggleShuffle = {},
            onCycleRepeat = {},
        )
    }
}
