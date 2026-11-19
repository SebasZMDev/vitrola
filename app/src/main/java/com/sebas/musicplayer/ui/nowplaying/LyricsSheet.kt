package com.sebas.musicplayer.ui.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sebas.musicplayer.data.lyrics.Lyrics

/** Estado de la letra de la canción actual. */
sealed interface LyricsUi {
    data object Loading : LyricsUi
    data object NoFolder : LyricsUi
    data object NotFound : LyricsUi
    data class Found(val lyrics: Lyrics) : LyricsUi
}

/**
 * Hoja con la letra. Si tiene tiempos, resalta la línea actual (latón) y la mantiene centrada;
 * tocar una línea salta a ese momento. La posición se lee dentro de derivedStateOf: la hoja solo se
 * recompone cuando cambia de línea, no cada 500 ms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSheet(
    ui: LyricsUi,
    positionMs: () -> Long,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Box(Modifier.fillMaxWidth().height(520.dp), contentAlignment = Alignment.Center) {
            when (ui) {
                LyricsUi.Loading -> CircularProgressIndicator()
                LyricsUi.NoFolder -> Message("Aún no elegiste la carpeta de letras.\nVe a Configuración > Letras.")
                LyricsUi.NotFound -> Message(
                    "No encontré letra para esta canción.\nBusco un .lrc con el mismo nombre que el archivo de audio.",
                )
                is LyricsUi.Found -> LyricsList(ui.lyrics, positionMs, onSeek)
            }
        }
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(32.dp),
    )
}

@Composable
private fun LyricsList(lyrics: Lyrics, positionMs: () -> Long, onSeek: (Long) -> Unit) {
    val listState = rememberLazyListState()
    val current by remember(lyrics) { derivedStateOf { lyrics.indexAt(positionMs()) } }

    LaunchedEffect(current) {
        if (lyrics.synced && current >= 0) {
            // Deja la línea actual hacia el tercio superior para ver lo que viene.
            listState.animateScrollToItem(current, scrollOffset = -180)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(lyrics.lines) { i, line ->
            val active = lyrics.synced && i == current
            Text(
                text = line.text.ifBlank { "♪" },
                style = if (active) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    !lyrics.synced -> MaterialTheme.colorScheme.onSurface
                    active -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (lyrics.synced) Modifier.clickable { onSeek(line.timeMs) } else Modifier),
            )
        }
    }
}
