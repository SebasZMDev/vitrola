package com.sebas.musicplayer.ui.nowplaying

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.sebas.musicplayer.domain.model.Song
import com.sebas.musicplayer.playback.PlayerState
import com.sebas.musicplayer.ui.components.SongArtwork
import com.sebas.musicplayer.ui.components.formatDuration
import com.sebas.musicplayer.ui.theme.MusicPlayerTheme
import kotlin.math.roundToInt

private const val NO_DRAG = -1
private const val EDGE_SCROLL_ZONE_PX = 120f
private const val EDGE_SCROLL_STEP_PX = 24f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    state: PlayerState,
    onDismiss: () -> Unit,
    onSkipToItem: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        QueueSheetContent(
            state = state,
            onSkipToItem = onSkipToItem,
            onRemove = { index ->
                if (state.queue.size <= 1) onDismiss()
                onRemove(index)
            },
            onMove = onMove,
            onClear = {
                onDismiss()
                onClear()
            },
        )
    }
}

@Composable
fun QueueSheetContent(
    state: PlayerState,
    onSkipToItem: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    onClear: () -> Unit = {},
) {
    var confirmClear by remember { mutableStateOf(false) }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("¿Vaciar la cola?") },
            text = { Text("Se quitarán las ${state.queue.size} canciones y se detendrá la reproducción.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; onClear() }) { Text("Vaciar") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } },
        )
    }
    val listState = rememberLazyListState()
    var draggingIndex by remember { mutableIntStateOf(NO_DRAG) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        if (state.currentIndex in state.queue.indices) listState.scrollToItem(state.currentIndex)
    }

    LaunchedEffect(draggingIndex) {
        if (draggingIndex == NO_DRAG) return@LaunchedEffect
        while (true) {
            val step = listState.edgeScrollStep(draggingIndex, dragOffset)
            if (step != 0f) dragOffset += listState.scrollBy(step)
            withFrameNanos { }
        }
    }

    val remainingMs = if (state.currentIndex in state.queue.indices) {
        (state.queue.drop(state.currentIndex).sumOf { it.durationMs } - state.positionMs).coerceAtLeast(0)
    } else {
        state.queue.sumOf { it.durationMs }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Cola", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "${state.queue.size} canciones · ${formatDuration(remainingMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.shuffleEnabled) {
                Text(
                    text = "Aleatorio activado: el orden de reproducción puede variar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = { confirmClear = true }, enabled = state.queue.isNotEmpty()) {
            Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Vaciar", modifier = Modifier.padding(start = 6.dp))
        }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp),
        ) {
            itemsIndexed(state.queue, key = { index, song -> "$index-${song.id}" }) { index, song ->
                val dragging = draggingIndex == index
                QueueRow(
                    song = song,
                    isCurrent = index == state.currentIndex,
                    modifier = Modifier
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (dragging) dragOffset else 0f
                            shadowElevation = if (dragging) 8.dp.toPx() else 0f
                        },
                    onClick = { onSkipToItem(index) },
                    onRemove = { onRemove(index) },
                    onDragStart = {
                        draggingIndex = index
                        dragOffset = 0f
                    },
                    onDrag = { dragOffset += it },
                    onDragEnd = {
                        val target = listState.dropIndex(index, dragOffset, state.queue.lastIndex)
                        draggingIndex = NO_DRAG
                        dragOffset = 0f
                        if (target != index) onMove(index, target)
                    },
                    onDragCancel = {
                        draggingIndex = NO_DRAG
                        dragOffset = 0f
                    },
                )
            }
        }
    }
}

private fun LazyListState.edgeScrollStep(index: Int, offset: Float): Float {
    val item = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return 0f
    val top = item.offset + offset
    val bottom = top + item.size
    return when {
        top < layoutInfo.viewportStartOffset + EDGE_SCROLL_ZONE_PX -> -EDGE_SCROLL_STEP_PX
        bottom > layoutInfo.viewportEndOffset - EDGE_SCROLL_ZONE_PX -> EDGE_SCROLL_STEP_PX
        else -> 0f
    }
}

private fun LazyListState.dropIndex(index: Int, offset: Float, lastIndex: Int): Int {
    val size = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }?.size ?: return index
    return (index + (offset / size).roundToInt()).coerceIn(0, lastIndex)
}

@Composable
private fun QueueRow(
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)
    val background = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.DragHandle,
            contentDescription = "Reordenar",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(8.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { currentOnDragStart() },
                        onDrag = { change, amount ->
                            change.consume()
                            currentOnDrag(amount.y)
                        },
                        onDragEnd = { currentOnDragEnd() },
                        onDragCancel = { currentOnDragCancel() },
                    )
                },
        )
        SongArtwork(
            artworkUri = song.artworkUri,
            modifier = Modifier.size(40.dp),
            cornerRadius = 6.dp,
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.SemiBold else null,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = "Quitar de la cola")
        }
    }
}

private fun previewSong(id: Long, title: String, artist: String) = Song(
    id = id,
    uri = Uri.EMPTY,
    title = title,
    artist = artist,
    album = "Álbum",
    albumId = 1,
    durationMs = 200_000 + id * 10_000,
    artworkUri = null,
)

@Preview(showBackground = true)
@Composable
private fun QueueSheetContentPreview() {
    val songs = listOf(
        previewSong(1, "Primera canción", "Artista A"),
        previewSong(2, "Segunda canción", "Artista B"),
        previewSong(3, "Tercera canción", "Artista C"),
        previewSong(2, "Segunda canción", "Artista B"),
        previewSong(4, "Cuarta canción", "Artista D"),
    )
    MusicPlayerTheme {
        QueueSheetContent(
            state = PlayerState(
                currentSong = songs[1],
                positionMs = 45_000,
                shuffleEnabled = true,
                queue = songs,
                currentIndex = 1,
            ),
            onSkipToItem = {},
            onRemove = {},
            onMove = { _, _ -> },
        )
    }
}
