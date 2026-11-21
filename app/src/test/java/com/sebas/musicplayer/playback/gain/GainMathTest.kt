package com.sebas.musicplayer.playback.gain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class GainMathTest {

    @Test
    fun `decibeles a factor`() {
        assertEquals(1f, GainMath.factor(0f), 1e-6f)
        assertEquals(0.5012f, GainMath.factor(-6f), 1e-3f)
        assertEquals(1.9953f, GainMath.factor(6f), 1e-3f)
    }

    @Test
    fun `aplicar ganancia baja y recorta sin pasarse del maximo`() {
        assertEquals(0.5f, GainMath.apply(1f, 0.5f), 1e-6f)
        // Con ganancia positiva el limitador suave nunca deja pasar de 1.
        val loud = GainMath.apply(0.9f, 2f)
        assertTrue(loud <= 1f && loud > 0.9f)
        assertEquals(-GainMath.apply(0.9f, 2f), GainMath.apply(-0.9f, 2f), 1e-6f)
        // Por debajo del umbral, ganancia limpia.
        assertEquals(0.4f, GainMath.apply(0.2f, 2f), 1e-6f)
    }

    @Test
    fun `medidor da el rms de una senoidal`() {
        val meter = LoudnessMeter(sampleRate = 44100, channels = 1)
        val amp = 0.5f
        val samples = FloatArray(44100 * 3) { (amp * sin(2 * PI * 440 * it / 44100)).toFloat() }
        meter.add(samples, samples.size)
        val r = meter.result()!!
        // RMS de senoidal = amp/sqrt(2) -> 20log10(0.3536) = -9.03 dBFS
        assertEquals(-9.03f, r.loudnessDb, 0.1f)
        assertEquals(0.5f, r.peak, 0.01f)
    }

    @Test
    fun `silencio no cuenta`() {
        val meter = LoudnessMeter(sampleRate = 1000, channels = 1)
        meter.add(FloatArray(5000), 5000)
        assertEquals(null, meter.result())
    }

    @Test
    fun `sugerencia baja lo fuerte y sube lo bajo hasta donde da el pico`() {
        // Muy fuerte: -8 dB hacia -14 = -6.
        assertEquals(-6f, GainMath.suggest(loudnessDb = -8f, peak = 1f), 0.01f)
        // Muy fuerte, más allá del rango: se queda en -6.
        assertEquals(-6f, GainMath.suggest(loudnessDb = -2f, peak = 1f), 0.01f)
        // Bajo con aire: -18 -> +4.
        assertEquals(4f, GainMath.suggest(loudnessDb = -18f, peak = 0.3f), 0.01f)
        // Bajo pero con picos al tope: solo hasta 3 dB (lo que el limitador aguanta bien).
        assertEquals(3f, GainMath.suggest(loudnessDb = -20f, peak = 1f), 0.01f)
        // Redondeo a medio dB.
        assertEquals(-1.5f, GainMath.suggest(loudnessDb = -12.4f, peak = 1f), 0.01f)
    }
}
