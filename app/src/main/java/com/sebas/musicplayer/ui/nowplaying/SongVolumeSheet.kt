package com.sebas.musicplayer.ui.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sebas.musicplayer.playback.gain.GainMath

/** Estado del botón "nivelar". */
sealed interface LevelingUi {
    data object Idle : LevelingUi
    data object Working : LevelingUi
    data class Done(val db: Float) : LevelingUi
    data object Failed : LevelingUi
}

/**
 * Hoja para darle a la canción actual su propio volumen (-6 a +6 dB), a mano o midiéndola.
 * Se guarda por canción: la próxima vez que suene ya viene ajustada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongVolumeSheet(
    songTitle: String,
    gainDb: Float,
    leveling: LevelingUi,
    onGainChange: (Float) -> Unit,
    onLevel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    // El deslizador se mueve suelto y solo guarda al soltar (no escribe en la base 60 veces por segundo).
    var dragging by remember { mutableFloatStateOf(Float.NaN) }
    val shown = if (dragging.isNaN()) gainDb else dragging

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text("VOLUMEN DE ESTA CANCIÓN", style = MaterialTheme.typography.labelMedium, color = colors.secondary)
            Text(songTitle, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Spacer(Modifier.height(16.dp))
            Text(
                GainMath.label(shown),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (shown == 0f) colors.onSurface else colors.secondary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Slider(
                value = shown,
                onValueChange = { dragging = (it * 2f).let(Math::round) / 2f },
                onValueChangeFinished = {
                    if (!dragging.isNaN()) onGainChange(dragging)
                    dragging = Float.NaN
                },
                valueRange = GainMath.MIN_DB..GainMath.MAX_DB,
                steps = ((GainMath.MAX_DB - GainMath.MIN_DB) * 2).toInt() - 1,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Más baja", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Text("Más alta", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onLevel, enabled = leveling != LevelingUi.Working) {
                    if (leveling == LevelingUi.Working) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(if (leveling == LevelingUi.Working) "Midiendo…" else "Nivelar automático")
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onGainChange(0f) }, enabled = gainDb != 0f) { Text("Original") }
            }
            val hint = when (leveling) {
                is LevelingUi.Done -> "Listo: la dejé en ${GainMath.label(leveling.db)} para que suene parejo con el resto."
                LevelingUi.Failed -> "No pude leer este archivo para medirlo. Ajústala a mano."
                LevelingUi.Working -> "Escuchando la canción entera, tarda unos segundos."
                LevelingUi.Idle -> "Nivelar mide la canción y la lleva al volumen de las demás."
            }
            Text(hint, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
