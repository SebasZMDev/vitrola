package com.sebas.musicplayer.playback.gain

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.tanh

/** Volumen por canción: cuentas puras (sin Android) para poder probarlas. */
object GainMath {
    const val MIN_DB = -6f
    const val MAX_DB = 6f

    /** Volumen al que "nivelar" lleva cada canción (RMS en dBFS, aprox. -14 LUFS de las plataformas). */
    const val TARGET_DB = -14f

    /** Cuánto se deja pasar del pico al subir: el limitador suave se come esos 3 dB sin que se note. */
    private const val LIMITER_ALLOWANCE_DB = 3f

    /** Desde aquí el limitador empieza a redondear la onda en vez de cortarla seco. */
    private const val KNEE = 0.8f

    fun factor(db: Float): Float = 10f.pow(db / 20f)

    /** Una muestra (-1..1) por [factor]. Si sube, un limitador suave evita la distorsión por recorte. */
    fun apply(sample: Float, factor: Float): Float {
        val y = sample * factor
        if (factor <= 1f) return y
        val a = abs(y)
        if (a <= KNEE) return y
        return sign(y) * (KNEE + (1f - KNEE) * tanh((a - KNEE) / (1f - KNEE)))
    }

    /** Ganancia para llevar una canción medida a [targetDb], sin subirla más de lo que su pico permite. */
    fun suggest(loudnessDb: Float, peak: Float, targetDb: Float = TARGET_DB): Float {
        val wanted = targetDb - loudnessDb
        val headroom = if (peak > 0f) -20f * log10(peak) + LIMITER_ALLOWANCE_DB else MAX_DB
        val db = minOf(wanted, headroom).coerceIn(MIN_DB, MAX_DB)
        return (db * 2f).roundToInt() / 2f
    }

    /** "+2,5 dB", "0 dB", "-3 dB". */
    fun label(db: Float): String {
        val r = (db * 2f).roundToInt() / 2f
        val txt = r.toString().removeSuffix(".0").replace('.', ',')
        return if (r > 0f) "+$txt dB" else "$txt dB"
    }
}

data class Loudness(val loudnessDb: Float, val peak: Float)

/**
 * Mide qué tan fuerte suena una canción: RMS por bloques de 400 ms, ignorando silencios y partes muy
 * bajas (como hace la norma de las plataformas, simplificada). Recibe muestras -1..1 intercaladas.
 */
class LoudnessMeter(sampleRate: Int, private val channels: Int) {
    private val blockSize = (sampleRate * 0.4).toInt().coerceAtLeast(1) * channels
    private val blocks = ArrayList<Double>()
    private var sum = 0.0
    private var count = 0
    private var peak = 0f

    fun add(samples: FloatArray, size: Int) {
        for (i in 0 until size) {
            val s = samples[i]
            val a = abs(s)
            if (a > peak) peak = a
            sum += s * s
            if (++count == blockSize) {
                blocks += sum / count
                sum = 0.0
                count = 0
            }
        }
    }

    /** null si todo era silencio (o la canción duraba menos de un bloque). */
    fun result(): Loudness? {
        val absGate = 10.0.pow(-70 / 10.0)
        val loud = blocks.filter { it > absGate }
        if (loud.isEmpty()) return null
        val relGate = loud.average() * 10.0.pow(-10 / 10.0)
        val kept = loud.filter { it > relGate }
        return Loudness((10 * log10(kept.average())).toFloat(), peak)
    }
}
