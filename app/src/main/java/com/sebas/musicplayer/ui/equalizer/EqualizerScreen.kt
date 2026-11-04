package com.sebas.musicplayer.ui.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.playback.effects.EqBand
import com.sebas.musicplayer.playback.effects.EqualizerInfo
import com.sebas.musicplayer.playback.effects.EqualizerSettings
import java.util.Locale

/**
 * Pantalla del ecualizador (ruta propia "equalizer", con su Scaffold y flecha atrás).
 * CONTRATO: la firma no cambia sin avisar al orquestador.
 */
@Composable
fun EqualizerRoute(onBack: () -> Unit) {
    val viewModel: EqualizerViewModel = viewModel(factory = EqualizerViewModel.Factory)
    val info by viewModel.info.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    EqualizerScreen(
        info = info,
        settings = settings,
        onBack = onBack,
        onEnabledChange = viewModel::setEnabled,
        onPresetSelected = viewModel::usePreset,
        onBandLevelChange = viewModel::setBandLevel,
        onBassBoostChange = viewModel::setBassBoost,
        onReset = viewModel::reset,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    info: EqualizerInfo?,
    settings: EqualizerSettings,
    onBack: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onPresetSelected: (Int) -> Unit,
    onBandLevelChange: (band: Int, levelMb: Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onReset: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ecualizador") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (info != null) {
                        Switch(
                            checked = settings.enabled,
                            onCheckedChange = onEnabledChange,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (info == null) {
            Box(
                Modifier.padding(padding).fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "El ecualizador no está disponible en este dispositivo",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            EqualizerContent(
                info = info,
                settings = settings,
                onPresetSelected = onPresetSelected,
                onBandLevelChange = onBandLevelChange,
                onBassBoostChange = onBassBoostChange,
                onReset = onReset,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun EqualizerContent(
    info: EqualizerInfo,
    settings: EqualizerSettings,
    onPresetSelected: (Int) -> Unit,
    onBandLevelChange: (Int, Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = settings.enabled
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = settings.presetIndex == null,
                    onClick = { onBandLevelChange(0, settings.bandLevelsMb.getOrElse(0) { 0 }) },
                    enabled = enabled,
                    label = { Text("Personalizado") },
                )
            }
            itemsIndexed(info.presets) { index, name ->
                FilterChip(
                    selected = settings.presetIndex == index,
                    onClick = { onPresetSelected(index) },
                    enabled = enabled,
                    label = { Text(name) },
                )
            }
        }

        info.bands.forEach { band ->
            BandRow(
                band = band,
                levelMb = settings.bandLevelsMb.getOrElse(band.index) { 0 },
                enabled = enabled,
                onChange = { onBandLevelChange(band.index, it) },
            )
        }

        if (info.bassBoostSupported) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Refuerzo de graves", style = MaterialTheme.typography.titleSmall)
                    Text("${settings.bassBoostStrength / 10}%", style = MaterialTheme.typography.bodyMedium)
                }
                Slider(
                    value = settings.bassBoostStrength.toFloat(),
                    onValueChange = { onBassBoostChange(it.toInt()) },
                    valueRange = 0f..1000f,
                    enabled = enabled,
                )
            }
        }

        OutlinedButton(
            onClick = onReset,
            enabled = enabled,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) { Text("Restablecer") }
    }
}

@Composable
private fun BandRow(band: EqBand, levelMb: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            formatFrequency(band.centerFreqHz),
            modifier = Modifier.width(64.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = levelMb.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = band.minLevelMb.toFloat()..band.maxLevelMb.toFloat(),
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        Text(
            formatDb(levelMb),
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun formatFrequency(hz: Int): String = when {
    hz < 1000 -> "$hz Hz"
    hz % 1000 == 0 -> "${hz / 1000} kHz"
    else -> String.format(Locale.US, "%.1f kHz", hz / 1000f)
}

private fun formatDb(mb: Int): String = String.format(Locale.US, "%+.1f dB", mb / 100f)

@Preview(showBackground = true)
@Composable
private fun EqualizerScreenPreview() {
    MaterialTheme {
        EqualizerScreen(
            info = EqualizerInfo(
                bands = listOf(60, 230, 910, 3600, 14000).mapIndexed { i, hz -> EqBand(i, hz, -1500, 1500) },
                presets = listOf("Normal", "Rock", "Jazz"),
                bassBoostSupported = true,
            ),
            settings = EqualizerSettings(
                enabled = true,
                presetIndex = null,
                bandLevelsMb = listOf(300, 0, -150, 200, 500),
                bassBoostStrength = 400,
            ),
            onBack = {},
            onEnabledChange = {},
            onPresetSelected = {},
            onBandLevelChange = { _, _ -> },
            onBassBoostChange = {},
            onReset = {},
        )
    }
}
