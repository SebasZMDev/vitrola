package com.sebas.musicplayer.playback.gain

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Sube o baja el volumen de lo que suena, dentro de la cadena de audio de ExoPlayer.
 * No usa player.volume (lo usa el temporizador para el fundido) y a diferencia de él puede subir, no solo bajar.
 * [gainDb] se puede cambiar en cualquier momento; se aplica desde el siguiente trozo de audio.
 */
@OptIn(UnstableApi::class)
class GainAudioProcessor : BaseAudioProcessor() {

    @Volatile var gainDb: Float = 0f
        set(value) {
            field = value.coerceIn(GainMath.MIN_DB, GainMath.MAX_DB)
            factor = GainMath.factor(field)
        }

    @Volatile private var factor = 1f

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val enc = inputAudioFormat.encoding
        if (enc != C.ENCODING_PCM_16BIT && enc != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat // mismo formato: solo cambia la amplitud
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val out = replaceOutputBuffer(size)
        val f = factor
        val input = inputBuffer.order(ByteOrder.nativeOrder())
        if (inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT) {
            while (input.hasRemaining()) out.putFloat(GainMath.apply(input.getFloat(), f))
        } else if (f == 1f) {
            out.put(input)
        } else {
            while (input.hasRemaining()) {
                val s = GainMath.apply(input.getShort() / 32768f, f)
                out.putShort((s * 32767f).toInt().coerceIn(-32768, 32767).toShort())
            }
        }
        out.flip()
    }
}
