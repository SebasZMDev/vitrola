package com.sebas.musicplayer.ui.sleeptimer

import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.playback.sleep.SleepTimerState
import kotlinx.coroutines.delay

private val PresetMinutes = listOf(5, 10, 15, 30, 45, 60)

/**
 * Acción de la barra superior de "Reproduciendo ahora": botón del temporizador para dormir
 * y todo lo que abre (hoja de opciones, etc.). Autónomo: trae su propio ViewModel.
 * CONTRATO: la firma no cambia sin avisar al orquestador.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerAction() {
    val viewModel: SleepTimerViewModel = viewModel(factory = SleepTimerViewModel.Factory)
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }

    IconButton(onClick = { showSheet = true }) {
        if (state is SleepTimerState.Off) {
            Icon(Icons.Outlined.Bedtime, contentDescription = "Temporizador para dormir")
        } else {
            Icon(
                Icons.Filled.Bedtime,
                contentDescription = "Temporizador para dormir",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }

    if (showSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = sheetState) {
            SleepTimerSheetContent(
                state = state,
                onStartCountdown = { minutes ->
                    viewModel.startCountdown(minutes)
                    Toast.makeText(
                        context,
                        "El temporizador pausará la música en $minutes min",
                        Toast.LENGTH_SHORT,
                    ).show()
                    showSheet = false
                },
                onStartEndOfSong = {
                    viewModel.startEndOfSong()
                    Toast.makeText(
                        context,
                        "La música se pausará al terminar la canción",
                        Toast.LENGTH_SHORT,
                    ).show()
                    showSheet = false
                },
                onAddFiveMinutes = { viewModel.addMinutes(5) },
                onCancel = {
                    viewModel.cancel()
                    showSheet = false
                },
            )
        }
    }
}

@Composable
internal fun SleepTimerSheetContent(
    state: SleepTimerState,
    onStartCountdown: (Int) -> Unit,
    onStartEndOfSong: () -> Unit,
    onAddFiveMinutes: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCustom by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableFloatStateOf(30f) }

    // Con scroll: con un temporizador activo + "Personalizado" abierto no cabe en pantallas bajas.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
    ) {
        Text(
            "Temporizador para dormir",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )

        if (state !is SleepTimerState.Off) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state is SleepTimerState.Countdown) {
                    val remaining = rememberRemainingMs(state)
                    Text("Se pausará en ${formatMmSs(remaining)}", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text("Se pausará al terminar la canción", style = MaterialTheme.typography.titleMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state is SleepTimerState.Countdown) {
                        OutlinedButton(onClick = onAddFiveMinutes) { Text("+5 min") }
                    }
                    OutlinedButton(onClick = onCancel) { Text("Cancelar temporizador") }
                }
            }
        }

        PresetMinutes.forEach { minutes ->
            SheetOption("$minutes minutos") { onStartCountdown(minutes) }
        }
        SheetOption("Al terminar la canción", onClick = onStartEndOfSong)
        SheetOption("Personalizado…") { showCustom = !showCustom }
        if (showCustom) {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text("${customMinutes.toInt()} min", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = customMinutes,
                    onValueChange = { customMinutes = it },
                    valueRange = 1f..120f,
                )
                Button(onClick = { onStartCountdown(customMinutes.toInt()) }) { Text("Iniciar") }
            }
        }
    }
}

/** Opción de la hoja: fondo transparente para que no se vea una franja más oscura que la hoja. */
@Composable
private fun SheetOption(text: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(text) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun rememberRemainingMs(state: SleepTimerState.Countdown): Long {
    var remaining by remember(state) {
        mutableLongStateOf((state.endsAtElapsedRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
    }
    LaunchedEffect(state) {
        while (true) {
            remaining = (state.endsAtElapsedRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            delay(1_000)
        }
    }
    return remaining
}

private fun formatMmSs(ms: Long): String {
    val totalSeconds = (ms + 999) / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Preview(showBackground = true)
@Composable
private fun SleepTimerSheetOffPreview() {
    MaterialTheme {
        Surface {
            SleepTimerSheetContent(SleepTimerState.Off, {}, {}, {}, {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SleepTimerSheetCountdownPreview() {
    MaterialTheme {
        Surface {
            SleepTimerSheetContent(
                SleepTimerState.Countdown(SystemClock.elapsedRealtime() + 754_000L, 900_000L),
                {}, {}, {}, {},
            )
        }
    }
}
