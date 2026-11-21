package com.sebas.musicplayer.playback.gain

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext

/** Decodifica una canción entera (sin reproducirla) y mide su volumen. Tarda unos segundos. */
class LoudnessAnalyzer(private val context: Context) {

    /** null si el archivo no se pudo leer o es silencio. */
    suspend fun measure(uri: Uri): Loudness? = withContext(Dispatchers.Default) {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return@withContext null
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val dec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec = dec
            dec.configure(format, null, null, 0)
            dec.start()

            var meter: LoudnessMeter? = null
            var isFloat = false
            var buf = FloatArray(16384)
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            while (true) {
                coroutineContext.ensureActive()
                if (!inputDone) {
                    val inIdx = dec.dequeueInputBuffer(10_000)
                    if (inIdx >= 0) {
                        val n = extractor.readSampleData(dec.getInputBuffer(inIdx)!!, 0)
                        if (n < 0) {
                            dec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            dec.queueInputBuffer(inIdx, 0, n, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIdx = dec.dequeueOutputBuffer(info, 10_000)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val f = dec.outputFormat
                        isFloat = f.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                            f.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                        meter = LoudnessMeter(
                            f.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                            f.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                        )
                    }
                    outIdx >= 0 -> {
                        val out = dec.getOutputBuffer(outIdx)!!.order(ByteOrder.nativeOrder())
                        out.position(info.offset)
                        out.limit(info.offset + info.size)
                        val count = if (isFloat) info.size / 4 else info.size / 2
                        if (buf.size < count) buf = FloatArray(count)
                        if (isFloat) out.asFloatBuffer().get(buf, 0, count)
                        else { val sb = out.asShortBuffer(); for (i in 0 until count) buf[i] = sb.get(i) / 32768f }
                        val m = meter ?: LoudnessMeter(
                            format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                            format.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                        ).also { meter = it }
                        m.add(buf, count)
                        dec.releaseOutputBuffer(outIdx, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                }
            }
            meter?.result()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }
}
