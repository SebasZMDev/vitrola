package com.sebas.musicplayer.playback.effects

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EqualizerAudioEffects(context: Context) : AudioEffects {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _info = MutableStateFlow<EqualizerInfo?>(null)
    override val info: StateFlow<EqualizerInfo?> = _info.asStateFlow()

    private val _settings = MutableStateFlow(load())
    override val settings: StateFlow<EqualizerSettings> = _settings.asStateFlow()

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    override fun setEnabled(enabled: Boolean) {
        commit { it.copy(enabled = enabled) }
        runCatching {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled && _settings.value.bassBoostStrength > 0
        }
    }

    override fun usePreset(index: Int) {
        val eq = equalizer
        if (eq == null) {
            commit { it.copy(presetIndex = index) }
            return
        }
        try {
            eq.usePreset(index.toShort())
            val levels = List(eq.numberOfBands.toInt()) { eq.getBandLevel(it.toShort()).toInt() }
            commit { it.copy(presetIndex = index, bandLevelsMb = levels) }
        } catch (_: RuntimeException) {
            commit { it.copy(presetIndex = index) }
        }
    }

    override fun setBandLevel(band: Int, levelMb: Int) {
        val range = _info.value?.bands?.getOrNull(band)
        val level = if (range != null) levelMb.coerceIn(range.minLevelMb, range.maxLevelMb) else levelMb
        commit { current ->
            val levels = current.bandLevelsMb.toMutableList()
            if (band in levels.indices) levels[band] = level
            current.copy(presetIndex = null, bandLevelsMb = levels)
        }
        runCatching { equalizer?.setBandLevel(band.toShort(), level.toShort()) }
    }

    override fun setBassBoost(strength: Int) {
        val value = strength.coerceIn(0, MAX_BASS)
        commit { it.copy(bassBoostStrength = value) }
        runCatching {
            bassBoost?.let {
                it.setStrength(value.toShort())
                it.enabled = value > 0 && _settings.value.enabled
            }
        }
    }

    override fun reset() {
        val bands = _settings.value.bandLevelsMb.size
        commit { it.copy(presetIndex = null, bandLevelsMb = List(bands) { 0 }, bassBoostStrength = 0) }
        runCatching {
            equalizer?.let { eq ->
                for (b in 0 until eq.numberOfBands) eq.setBandLevel(b.toShort(), 0)
            }
            bassBoost?.let {
                it.setStrength(0)
                it.enabled = false
            }
        }
    }

    override fun attach(audioSessionId: Int) {
        release()
        val eq = try {
            Equalizer(0, audioSessionId)
        } catch (_: RuntimeException) {
            _info.value = null
            return
        }
        equalizer = eq
        val bass = try {
            BassBoost(0, audioSessionId)
        } catch (_: RuntimeException) {
            null
        }
        bassBoost = bass

        try {
            val range = eq.bandLevelRange
            val bands = List(eq.numberOfBands.toInt()) { i ->
                EqBand(
                    index = i,
                    centerFreqHz = eq.getCenterFreq(i.toShort()) / 1000,
                    minLevelMb = range[0].toInt(),
                    maxLevelMb = range[1].toInt(),
                )
            }
            val presets = List(eq.numberOfPresets.toInt()) { eq.getPresetName(it.toShort()) }
            val bassSupported = bass?.strengthSupported == true
            _info.value = EqualizerInfo(bands, presets, bassSupported)

            if (_settings.value.bandLevelsMb.size != bands.size) {
                commit { it.copy(bandLevelsMb = List(bands.size) { 0 }) }
            }
            apply(eq, bass, bands, presets.size)
        } catch (_: RuntimeException) {
            release()
            _info.value = null
        }
    }

    override fun detach() = release()

    private fun apply(eq: Equalizer, bass: BassBoost?, bands: List<EqBand>, presetCount: Int) {
        val s = _settings.value
        val preset = s.presetIndex
        if (preset != null && preset in 0 until presetCount) {
            eq.usePreset(preset.toShort())
            val levels = List(bands.size) { eq.getBandLevel(it.toShort()).toInt() }
            commit { it.copy(bandLevelsMb = levels) }
        } else {
            bands.forEach { b ->
                val level = s.bandLevelsMb[b.index].coerceIn(b.minLevelMb, b.maxLevelMb)
                eq.setBandLevel(b.index.toShort(), level.toShort())
            }
        }
        eq.enabled = s.enabled
        if (bass != null && _info.value?.bassBoostSupported == true) {
            bass.setStrength(s.bassBoostStrength.toShort())
            bass.enabled = s.enabled && s.bassBoostStrength > 0
        }
    }

    private fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        equalizer = null
        bassBoost = null
    }

    private fun commit(transform: (EqualizerSettings) -> EqualizerSettings) {
        _settings.update(transform)
        save(_settings.value)
    }

    private fun load(): EqualizerSettings {
        val levels = prefs.getString(KEY_BANDS, "").orEmpty()
            .split(',')
            .mapNotNull { it.trim().toIntOrNull() }
        return EqualizerSettings(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            presetIndex = prefs.getInt(KEY_PRESET, -1).takeIf { it >= 0 },
            bandLevelsMb = levels,
            bassBoostStrength = prefs.getInt(KEY_BASS, 0).coerceIn(0, MAX_BASS),
        )
    }

    private fun save(s: EqualizerSettings) {
        prefs.edit {
            putBoolean(KEY_ENABLED, s.enabled)
            putInt(KEY_PRESET, s.presetIndex ?: -1)
            putString(KEY_BANDS, s.bandLevelsMb.joinToString(","))
            putInt(KEY_BASS, s.bassBoostStrength)
        }
    }

    private companion object {
        const val PREFS = "audio_effects"
        const val KEY_ENABLED = "enabled"
        const val KEY_PRESET = "presetIndex"
        const val KEY_BANDS = "bandLevels"
        const val KEY_BASS = "bassBoost"
        const val MAX_BASS = 1000
    }
}
