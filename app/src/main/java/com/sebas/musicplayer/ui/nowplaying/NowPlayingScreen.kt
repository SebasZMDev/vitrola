package com.sebas.musicplayer.ui.nowplaying

import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.data.settings.PlayerStyle
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.playback.PlayerState
import com.sebas.musicplayer.playback.RepeatMode
import com.sebas.musicplayer.ui.components.EmptyState
import com.sebas.musicplayer.ui.components.SongArtwork
import com.sebas.musicplayer.ui.components.formatDuration
import com.sebas.musicplayer.ui.components.rememberArtworkColor
import com.sebas.musicplayer.ui.sleeptimer.SleepTimerAction
import com.sebas.musicplayer.ui.theme.MusicPlayerTheme

@Composable
fun NowPlayingRoute(
    onBack: () -> Unit,
    onOpenEqualizer: () -> Unit,
    viewModel: NowPlayingViewModel = viewModel(factory = NowPlayingViewModel.Factory),
) {
    val state by viewModel.playerState.collectAsStateWithLifecycle()
    val playerStyle by viewModel.playerStyle.collectAsStateWithLifecycle()
    NowPlayingScreen(
        state = state,
        playerStyle = playerStyle,
        onBack = onBack,
        onTogglePlayPause = viewModel::togglePlayPause,
        onNext = viewModel::next,
        onPrevious = viewModel::previous,
        onSeekTo = viewModel::seekTo,
        onCycleRepeat = viewModel::cycleRepeatMode,
        onSkipToQueueItem = viewModel::skipToQueueItem,
        onRemoveFromQueue = viewModel::removeFromQueue,
        onMoveQueueItem = viewModel::moveQueueItem,
        onOpenEqualizer = onOpenEqualizer,
    )
}

/**
 * Pantalla completa del reproductor. El fondo toma el color de la carátula.
 * Gestos: deslizar hacia abajo cierra; deslizar la carátula a los lados cambia de canción.
 */
@Composable
fun NowPlayingScreen(
    state: PlayerState,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onCycleRepeat: () -> Unit,
    onSkipToQueueItem: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onMoveQueueItem: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    onOpenEqualizer: () -> Unit = {},
    playerStyle: PlayerStyle = PlayerStyle.COVER,
) {
    var showQueue by remember { mutableStateOf(false) }
    if (showQueue) {
        QueueSheet(
            state = state,
            onDismiss = { showQueue = false },
            onSkipToItem = onSkipToQueueItem,
            onRemove = onRemoveFromQueue,
            onMove = onMoveQueueItem,
        )
    }

    val song = state.currentSong
    val colors = MaterialTheme.colorScheme
    val glow = rememberArtworkColor(song?.artworkUri)
    val glowAlpha = if (colors.background.luminance() < 0.5f) 0.6f else 0.4f
    val currentOnBack by rememberUpdatedState(onBack)

    Box(modifier.fillMaxSize().background(colors.background)) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to glow.copy(alpha = glowAlpha), 0.8f to Color.Transparent),
            ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .pointerInput(Unit) {
                    var dragged = 0f
                    val threshold = 96.dp.toPx()
                    detectVerticalDragGestures(
                        onDragStart = { dragged = 0f },
                        onDragEnd = { if (dragged >= threshold) currentOnBack() },
                        onVerticalDrag = { _, amount -> dragged += amount },
                    )
                },
        ) {
            Header(album = song?.album, onBack = onBack)
            if (song == null) {
                EmptyState(
                    icon = Icons.Filled.MusicNote,
                    title = "No hay nada en reproducción",
                    message = "Elige una canción de tu biblioteca para empezar.",
                )
            } else {
                Column(modifier = Modifier.weight(1f).padding(horizontal = 28.dp)) {
                    Artwork(
                        style = playerStyle,
                        artworkUri = song.artworkUri,
                        isPlaying = state.isPlaying,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp),
                    )
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().basicMarquee(),
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    )
                    Spacer(Modifier.height(16.dp))
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
                        onCycleRepeat = onCycleRepeat,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SleepTimerAction()
                    IconButton(onClick = onOpenEqualizer) {
                        Icon(Icons.Default.Equalizer, contentDescription = "Ecualizador")
                    }
                    IconButton(onClick = { showQueue = true }) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Cola")
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(album: String?, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Cerrar", modifier = Modifier.size(32.dp))
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "REPRODUCIENDO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!album.isNullOrBlank()) {
                Text(
                    album,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
        // Mismo ancho que el botón de la izquierda para que el título quede centrado.
        Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun Artwork(
    style: PlayerStyle,
    artworkUri: Uri?,
    isPlaying: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // La carátula "respira": se encoge un poco en pausa y vuelve con rebote al reproducir.
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.86f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "artworkScale",
    )
    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnPrevious by rememberUpdatedState(onPrevious)

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // Cuadrada y lo más grande posible; en pantallas bajas se encoge para no empujar los controles.
        val side = min(maxWidth, maxHeight)
        // Deslizar a los lados cambia de canción, sea cual sea el estilo.
        val swipe = Modifier.pointerInput(Unit) {
            var dragged = 0f
            val threshold = 64.dp.toPx()
            detectHorizontalDragGestures(
                onDragStart = { dragged = 0f },
                onDragEnd = {
                    if (dragged <= -threshold) currentOnNext()
                    if (dragged >= threshold) currentOnPrevious()
                },
                onHorizontalDrag = { _, amount -> dragged += amount },
            )
        }
        if (style == PlayerStyle.COVER) {
            SongArtwork(
                artworkUri = artworkUri,
                modifier = Modifier
                    .size(side)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .shadow(elevation = 24.dp, shape = RoundedCornerShape(20.dp))
                    .then(swipe),
                cornerRadius = 20.dp,
            )
        } else {
            // Disco: en pausa se frena con inercia (no hace falta encogerlo).
            SpinningDisc(
                style = style,
                artworkUri = artworkUri,
                isPlaying = isPlaying,
                modifier = Modifier.size(side).then(swipe),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val onSurface = MaterialTheme.colorScheme.onSurface
    val sliderColors = SliderDefaults.colors(
        thumbColor = onSurface,
        activeTrackColor = onSurface,
        inactiveTrackColor = onSurface.copy(alpha = 0.2f),
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = fraction,
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                dragFraction?.let { onSeekTo((it * durationMs).toLong()) }
                dragFraction = null
            },
            enabled = enabled,
            colors = sliderColors,
            thumb = { Box(Modifier.size(14.dp).background(onSurface, CircleShape)) },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(4.dp),
                    colors = sliderColors,
                    enabled = enabled,
                    drawStopIndicator = null,
                    thumbTrackGapSize = 0.dp,
                    trackInsideCornerSize = 0.dp,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            Text(formatDuration(shownMs), style = MaterialTheme.typography.labelMedium, color = labelColor)
            Text(formatDuration(durationMs), style = MaterialTheme.typography.labelMedium, color = labelColor)
        }
    }
}

@Composable
private fun Controls(
    state: PlayerState,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    // Un solo botón: normal -> repetir una -> mix. Mix = aleatorio (con repetir todo, ver el servicio).
    val mode = when {
        state.shuffleEnabled -> PlayMode.MIX
        state.repeatMode == RepeatMode.ONE -> PlayMode.REPEAT_ONE
        else -> PlayMode.NORMAL
    }
    val next = when (mode) {
        PlayMode.NORMAL -> PlayMode.REPEAT_ONE
        PlayMode.REPEAT_ONE -> PlayMode.MIX
        PlayMode.MIX -> PlayMode.NORMAL
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToggleControl(
            icon = when (mode) {
                PlayMode.NORMAL -> Icons.Filled.Repeat
                PlayMode.REPEAT_ONE -> Icons.Filled.RepeatOne
                PlayMode.MIX -> Icons.Filled.Shuffle
            },
            description = mode.label,
            active = mode != PlayMode.NORMAL,
            onClick = {
                onCycleRepeat()
                Toast.makeText(context, next.label, Toast.LENGTH_SHORT).show()
            },
        )
        IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Anterior", modifier = Modifier.size(40.dp))
        }
        Surface(
            onClick = onTogglePlayPause,
            shape = CircleShape,
            color = colors.onSurface,
            contentColor = colors.surface,
            modifier = Modifier.size(72.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Crossfade(targetState = state.isPlaying, label = "playPause") { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "Pausar" else "Reproducir",
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
        }
        IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Siguiente", modifier = Modifier.size(40.dp))
        }
        // Mismo ancho que el botón de modo, para que anterior/play/siguiente queden centrados.
        Spacer(Modifier.size(48.dp))
    }
}

private enum class PlayMode(val label: String) {
    NORMAL("Reproducción normal"),
    REPEAT_ONE("Repetir una canción"),
    MIX("Mix: aleatorio sin parar"),
}

/** Botón de modo (aleatorio, repetir): activo se pinta con el acento y lleva un punto debajo. */
@Composable
private fun ToggleControl(icon: ImageVector, description: String, active: Boolean, onClick: () -> Unit) {
    val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    IconButton(onClick = onClick) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, tint = tint)
            if (active) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .graphicsLayer { translationY = 8.dp.toPx() }
                        .size(4.dp)
                        .background(tint, CircleShape),
                )
            }
        }
    }
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
            onCycleRepeat = {},
            onSkipToQueueItem = {},
            onRemoveFromQueue = {},
            onMoveQueueItem = { _, _ -> },
        )
    }
}
