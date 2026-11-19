package com.sebas.musicplayer.ui.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

private const val BAR_COUNT = 32

/** 30 cuadros/s; se resta un poco para no perder un fotograma por redondeo en pantallas de 60 Hz. */
private const val MIN_FRAME_NANOS = 30_000_000L

/**
 * Barras tipo ecualizador, ADORNO: se mueven con el compás de la canción en curso pero NO leen el
 * audio real (eso exigiría el permiso de micrófono). Cada canción tiene su propio "baile" (fase por
 * semilla). Al pausar bajan con suavidad y el bucle de fotogramas se detiene (costo cero en pausa).
 * El tiempo se lee solo al dibujar: animar no recompone nada.
 */
@Composable
fun SoundBars(
    isPlaying: Boolean,
    seed: Long,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val time = remember { mutableFloatStateOf(0f) }
    val energy = remember { mutableFloatStateOf(0f) }
    var t by time
    var e by energy

    LaunchedEffect(isPlaying) {
        val target = if (isPlaying) 1f else 0f
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            // Tope de ~30 cuadros/s: en pantallas de 90/120 Hz saltamos fotogramas (a la vista es igual).
            if (now - last < MIN_FRAME_NANOS) continue
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
            last = now
            e += (target - e) * (1f - exp(-5f * dt))
            if (isPlaying) t += dt
            if (!isPlaying && e < 0.01f) {
                e = 0f
                break
            }
        }
    }

    val phases = remember(seed) {
        val rnd = java.util.Random(seed)
        FloatArray(BAR_COUNT * 2) { rnd.nextFloat() * (2f * PI.toFloat()) }
    }

    Canvas(modifier) {
        val gap = size.width / BAR_COUNT * 0.35f
        val barW = (size.width - gap * (BAR_COUNT - 1)) / BAR_COUNT
        val energyNow = energy.floatValue
        val timeNow = time.floatValue
        val brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0.25f)))
        for (i in 0 until BAR_COUNT) {
            // Dos ondas por barra a distinta velocidad: parece un espectro, no un vaivén regular.
            val a = sin(timeNow * (2.1f + i % 5 * 0.55f) + phases[i * 2])
            val b = sin(timeNow * (3.7f + i % 3 * 0.9f) + phases[i * 2 + 1])
            val level = (0.5f + 0.32f * a + 0.18f * b).coerceIn(0.08f, 1f)
            // Más movimiento en graves (izquierda) que en agudos (derecha).
            val tilt = 1f - 0.45f * i / BAR_COUNT
            val h = (size.height * (0.06f + (level * tilt) * 0.94f * energyNow)).coerceAtLeast(2f)
            drawRoundRect(
                brush = brush,
                topLeft = Offset(i * (barW + gap), size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f),
            )
        }
    }
}
