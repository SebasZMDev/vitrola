package com.sebas.musicplayer.playback.effects

import kotlinx.coroutines.flow.StateFlow

/**
 * Ecualizador (android.media.audiofx) aplicado a la sesión de audio del reproductor.
 * Singleton de la app (AppContainer). PlaybackService llama a [attach] con el audioSessionId
 * de su ExoPlayer al crearse y a [detach] al destruirse. Los ajustes se guardan y se
 * vuelven a aplicar en cada attach.
 * CONTRATO: no cambiar sin avisar al orquestador.
 */
interface AudioEffects {
    /** Capacidades del ecualizador del dispositivo; null si aún no hay sesión o el dispositivo no lo soporta. */
    val info: StateFlow<EqualizerInfo?>
    val settings: StateFlow<EqualizerSettings>

    fun setEnabled(enabled: Boolean)

    /** Aplica un preset del sistema (índice en [EqualizerInfo.presets]); actualiza bandLevelsMb. */
    fun usePreset(index: Int)

    /** Ajuste manual de una banda en milibeles; pasa a modo personalizado (presetIndex = null). */
    fun setBandLevel(band: Int, levelMb: Int)

    /** 0..1000. Se ignora si el dispositivo no soporta BassBoost. */
    fun setBassBoost(strength: Int)

    /** Todo a 0 dB, sin preset, bass boost 0. Mantiene enabled. */
    fun reset()

    fun attach(audioSessionId: Int)
    fun detach()
}

data class EqualizerInfo(
    val bands: List<EqBand>,
    val presets: List<String>,
    val bassBoostSupported: Boolean,
)

data class EqBand(
    val index: Int,
    val centerFreqHz: Int,
    val minLevelMb: Int,
    val maxLevelMb: Int,
)

data class EqualizerSettings(
    val enabled: Boolean = false,
    /** null = personalizado. */
    val presetIndex: Int? = null,
    /** Un valor por banda, en milibeles. Vacío hasta conocer las bandas del dispositivo. */
    val bandLevelsMb: List<Int> = emptyList(),
    val bassBoostStrength: Int = 0,
)
