package com.sebas.musicplayer.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.sebas.musicplayer.ui.components.ZmSignature
import com.sebas.musicplayer.data.settings.LabelSize
import com.sebas.musicplayer.data.settings.PlayerBackground
import com.sebas.musicplayer.data.settings.PlayerLook
import com.sebas.musicplayer.data.settings.PlayerStyle
import com.sebas.musicplayer.data.settings.VinylColor
import com.sebas.musicplayer.ui.nowplaying.PlayerScene
import com.sebas.musicplayer.ui.nowplaying.isScene
import com.sebas.musicplayer.ui.components.SongArtwork
import com.sebas.musicplayer.ui.nowplaying.DiscView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sebas.musicplayer.data.library.MusicFolder
import com.sebas.musicplayer.data.settings.SettingsState
import com.sebas.musicplayer.data.settings.ThemeMode

private val durationOptions = listOf(
    0 to "Sin mínimo",
    15 to "15 s",
    30 to "30 s",
    60 to "1 min",
    120 to "2 min",
)

@Composable
fun SettingsRoute(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        folders = folders,
        versionName = viewModel.versionName,
        onBack = onBack,
        onThemeMode = viewModel::setThemeMode,
        onOpenPlayerOnTap = viewModel::setOpenPlayerOnTap,
        onPlayerStyle = viewModel::setPlayerStyle,
        onPlayerBackground = viewModel::setPlayerBackground,
        onLook = viewModel::updateLook,
        onExcludeWhatsApp = viewModel::setExcludeWhatsApp,
        onMinDuration = viewModel::setMinDurationSec,
        onFolderExcluded = viewModel::setFolderExcluded,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    state: SettingsState,
    folders: List<MusicFolder>?,
    versionName: String,
    onBack: () -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onOpenPlayerOnTap: (Boolean) -> Unit,
    onPlayerStyle: (PlayerStyle) -> Unit,
    onPlayerBackground: (PlayerBackground) -> Unit,
    onLook: ((PlayerLook) -> PlayerLook) -> Unit,
    onExcludeWhatsApp: (Boolean) -> Unit,
    onMinDuration: (Int) -> Unit,
    onFolderExcluded: (String, Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item { SectionTitle("Apariencia") }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("Tema", style = MaterialTheme.typography.bodyLarge)
                    ChipRow {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = state.themeMode == mode,
                                onClick = { onThemeMode(mode) },
                                label = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> "Del sistema"
                                            ThemeMode.LIGHT -> "Claro"
                                            ThemeMode.DARK -> "Oscuro"
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }

            item { SectionTitle("Reproducción") }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("Estilo del reproductor", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Cómo se ve la carátula mientras suena la música.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PlayerStyle.entries.forEach { style ->
                            PlayerStyleCard(style, selected = state.playerStyle == style, onClick = { onPlayerStyle(style) })
                        }
                    }
                }
            }
            if (state.playerStyle == PlayerStyle.VINYL || state.playerStyle == PlayerStyle.TURNTABLE) {
                item { VinylOptions(state.playerStyle, state.look, onLook) }
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Fondo del reproductor", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "El negro puro solo se aplica con el tema oscuro (ahorra batería en pantallas OLED).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ChipRow {
                        PlayerBackground.entries.forEach { bg ->
                            FilterChip(
                                selected = state.playerBackground == bg,
                                onClick = { onPlayerBackground(bg) },
                                label = {
                                    Text(
                                        when (bg) {
                                            PlayerBackground.GLOW -> "Color de la carátula"
                                            PlayerBackground.BLUR -> "Carátula borrosa"
                                            PlayerBackground.OLED -> "Negro puro"
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }
            item {
                SwitchRow(
                    title = "Abrir el reproductor al tocar una canción",
                    subtitle = "Si lo apagas, la canción suena y el reproductor sigue abajo en miniatura.",
                    checked = state.openPlayerOnTap,
                    onChange = onOpenPlayerOnTap,
                )
            }

            item { SectionTitle("Biblioteca") }
            item {
                SwitchRow(
                    title = "Excluir audios de WhatsApp",
                    subtitle = "Notas de voz y audios recibidos.",
                    checked = state.excludeWhatsApp,
                    onChange = onExcludeWhatsApp,
                )
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("Duración mínima", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Los audios más cortos no aparecen (tonos, alertas).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ChipRow {
                        durationOptions.forEach { (seconds, label) ->
                            FilterChip(
                                selected = state.minDurationSec == seconds,
                                onClick = { onMinDuration(seconds) },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }

            item { SectionTitle("Carpetas") }
            item {
                Text(
                    "Apaga una carpeta para ocultar su música. También se ocultan sus subcarpetas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            when {
                folders == null -> item {
                    Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
                folders.isEmpty() -> item {
                    Text(
                        "No se encontraron carpetas con música.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                else -> items(folders, key = { it.path }) { folder ->
                    FolderRow(folder, state, onFolderExcluded)
                }
            }

            item { SectionTitle("Acerca de") }
            item {
                Text(
                    "Versión $versionName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            item { ZmSignature(Modifier.padding(top = 24.dp, bottom = 32.dp)) }
        }
    }
}

@Composable
private fun FolderRow(folder: MusicFolder, state: SettingsState, onFolderExcluded: (String, Boolean) -> Unit) {
    val ownExcluded = folder.path in state.excludedFolders
    val excluded = state.isFolderExcluded(folder.path)
    // Excluida por una carpeta superior: su interruptor no se puede cambiar desde aquí.
    val byParent = excluded && !ownExcluded
    val name = folder.path.trimEnd('/').substringAfterLast('/').ifEmpty { folder.path }
    val count = if (folder.songCount == 1) "1 canción" else "${folder.songCount} canciones"
    SwitchRow(
        title = name,
        subtitle = if (byParent) "Oculta por una carpeta superior" else "${folder.path} · $count",
        checked = !excluded,
        enabled = !byParent,
        onChange = { included -> onFolderExcluded(folder.path, !included) },
    )
}

/** Vista previa de un estilo de reproductor; el elegido lleva un borde del color de acento. */
@Composable
private fun PlayerStyleCard(style: PlayerStyle, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        modifier = Modifier
            .width(108.dp)
            .clip(shape)
            .border(2.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            style == PlayerStyle.COVER -> SongArtwork(artworkUri = null, modifier = Modifier.size(84.dp), cornerRadius = 12.dp)
            style.isScene() -> PlayerScene(
                style = style,
                artworkUri = null,
                isPlaying = false,
                progress = 0.35f,
                look = PlayerLook(),
                modifier = Modifier.size(width = 92.dp, height = 84.dp),
            )
            else -> DiscView(style = style, artworkUri = null, rotation = { 0f }, modifier = Modifier.size(84.dp))
        }
        Text(
            when (style) {
                PlayerStyle.COVER -> "Carátula"
                PlayerStyle.VINYL -> "Vinilo clásico"
                PlayerStyle.CD -> "CD"
                PlayerStyle.TURNTABLE -> "Tocadiscos"
                PlayerStyle.CASSETTE -> "Cassette"
                PlayerStyle.RADIO -> "Radio"
            },
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Opciones del vinilo: aparecen solo con el estilo que las usa. */
@Composable
private fun VinylOptions(style: PlayerStyle, look: PlayerLook, onLook: ((PlayerLook) -> PlayerLook) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Color del vinilo", style = MaterialTheme.typography.bodyLarge)
        ChipRow {
            VinylColor.entries.forEach { c ->
                FilterChip(
                    selected = look.vinylColor == c,
                    onClick = { onLook { it.copy(vinylColor = c) } },
                    label = {
                        Text(
                            when (c) {
                                VinylColor.BLACK -> "Negro"
                                VinylColor.RED -> "Rojo"
                                VinylColor.BLUE -> "Azul"
                                VinylColor.COVER -> "De la carátula"
                            },
                        )
                    },
                )
            }
        }
        Text("Etiqueta", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp))
        ChipRow {
            LabelSize.entries.forEach { size ->
                FilterChip(
                    selected = look.labelSize == size,
                    onClick = { onLook { it.copy(labelSize = size) } },
                    label = { Text(when (size) { LabelSize.SMALL -> "Pequeña"; LabelSize.MEDIUM -> "Media"; LabelSize.LARGE -> "Grande" }) },
                )
            }
        }
        Text("Velocidad", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp))
        ChipRow {
            FilterChip(selected = !look.fast45, onClick = { onLook { it.copy(fast45 = false) } }, label = { Text("33 rpm") })
            FilterChip(selected = look.fast45, onClick = { onLook { it.copy(fast45 = true) } }, label = { Text("45 rpm") })
        }
    }
    SwitchRow("Surcos", "Los anillos finos del disco.", look.grooves, { v -> onLook { it.copy(grooves = v) } })
    SwitchRow("Reflejo de luz", "El brillo fijo que hace notar el giro.", look.sheen, { v -> onLook { it.copy(sheen = v) } })
    if (style == PlayerStyle.VINYL) {
        SwitchRow("Funda con la carátula", "El disco asoma por un lado de su funda.", look.sleeve, { v -> onLook { it.copy(sleeve = v) } })
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}
