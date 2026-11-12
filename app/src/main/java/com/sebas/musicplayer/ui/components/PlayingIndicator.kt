package com.sebas.musicplayer.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * ¿Está sonando la música ahora mismo? Lo provee AppShell una sola vez para que las filas
 * no tengan que observar el reproductor (solo cambia al pausar o reanudar).
 */
val LocalIsPlaying = compositionLocalOf { false }

/** Tres barritas tipo ecualizador que marcan la canción actual; se mueven solo si está sonando. */
@Composable
fun PlayingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    animate: Boolean = LocalIsPlaying.current,
) {
    if (animate) {
        val transition = rememberInfiniteTransition(label = "playing")
        val a = transition.animateFloat(
            0.25f, 1f, infiniteRepeatable(tween(430, easing = LinearEasing), RepeatMode.Reverse), label = "a",
        )
        val b = transition.animateFloat(
            1f, 0.3f, infiniteRepeatable(tween(560, easing = LinearEasing), RepeatMode.Reverse), label = "b",
        )
        val c = transition.animateFloat(
            0.4f, 0.9f, infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse), label = "c",
        )
        Bars(a, b, c, color, modifier)
    } else {
        val a = remember { mutableFloatStateOf(0.45f) }
        val b = remember { mutableFloatStateOf(0.8f) }
        val c = remember { mutableFloatStateOf(0.6f) }
        Bars(a, b, c, color, modifier)
    }
}

@Composable
private fun Bars(a: State<Float>, b: State<Float>, c: State<Float>, color: Color, modifier: Modifier) {
    // Los valores se leen dentro del dibujo: la animación no recompone nada.
    Canvas(modifier.size(16.dp)) {
        val barWidth = size.width / 5f
        listOf(a, b, c).forEachIndexed { i, state ->
            val barHeight = size.height * state.value
            drawRoundRect(
                color = color,
                topLeft = Offset(i * 2 * barWidth, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}
