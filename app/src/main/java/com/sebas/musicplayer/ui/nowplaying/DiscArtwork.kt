package com.sebas.musicplayer.ui.nowplaying

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.sebas.musicplayer.data.settings.PlayerStyle
import com.sebas.musicplayer.ui.components.SongArtwork
import kotlin.math.exp

/** 33 rpm = 198 grados por segundo. */
private const val SPIN_DEGREES_PER_SECOND = 198f

/** Constante de tiempo de arranque y frenado: el disco tiene inercia en vez de parar en seco. */
private const val SPIN_INERTIA_PER_SECOND = 3.5f

/**
 * Disco que gira mientras suena y se frena con inercia al pausar (conserva el ángulo).
 * El ángulo se lee solo al dibujar, así que girar no recompone nada.
 */
@Composable
fun SpinningDisc(
    style: PlayerStyle,
    artworkUri: Uri?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    var angle by remember { mutableFloatStateOf(0f) }
    var speed by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        val target = if (isPlaying) SPIN_DEGREES_PER_SECOND else 0f
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
            last = now
            speed += (target - speed) * (1f - exp(-SPIN_INERTIA_PER_SECOND * dt))
            angle = (angle + speed * dt) % 360f
            if (!isPlaying && speed < 0.5f) {
                speed = 0f
                break // ya está quieto: no hace falta seguir pidiendo fotogramas
            }
        }
    }

    DiscView(style = style, artworkUri = artworkUri, rotation = { angle }, modifier = modifier)
}

/**
 * Dibujo del disco. [rotation] (grados) se lee en la capa gráfica. El brillo (reflejo de la luz)
 * no gira: es lo que hace que se note que el disco se mueve.
 */
@Composable
fun DiscView(
    style: PlayerStyle,
    artworkUri: Uri?,
    rotation: () -> Float,
    modifier: Modifier = Modifier,
) {
    val holeColor = MaterialTheme.colorScheme.background
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        // La sombra crece con el tamaño: grande en el reproductor, discreta en las miniaturas.
        val elevation = (side * 0.06f).coerceIn(3.dp, 20.dp)
        Box(Modifier.size(side).shadow(elevation = elevation, shape = CircleShape), contentAlignment = Alignment.Center) {
            // Parte que gira: superficie + carátula.
            Box(
                Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation() },
                contentAlignment = Alignment.Center,
            ) {
                if (style == PlayerStyle.CD) {
                    // CD: la carátula ocupa todo el disco.
                    SongArtwork(artworkUri, Modifier.fillMaxSize().clip(CircleShape), cornerRadius = 500.dp)
                } else {
                    Canvas(Modifier.fillMaxSize()) { drawVinylSurface() }
                    SongArtwork(artworkUri, Modifier.size(side * 0.36f), cornerRadius = 500.dp)
                }
            }
            // Parte fija: reflejos, aro central y agujero.
            Canvas(Modifier.fillMaxSize()) {
                if (style == PlayerStyle.CD) drawCdOverlay(holeColor) else drawVinylOverlay(holeColor)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVinylSurface() {
    val c = center
    val r = size.minDimension / 2f
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFF2B2B30), Color(0xFF0D0D0F)), center = c, radius = r),
        radius = r,
        center = c,
    )
    // Surcos: anillos finos, y cada tanto uno más marcado (separación entre pistas).
    var ring = r * 0.40f
    var i = 0
    while (ring < r * 0.97f) {
        val track = i % 8 == 0
        drawCircle(
            color = Color.White.copy(alpha = if (track) 0.11f else 0.035f),
            radius = ring,
            center = c,
            style = Stroke(width = if (track) 1.6.dp.toPx() else 0.7.dp.toPx()),
        )
        ring += r * 0.0225f
        i++
    }
    drawCircle(Color.White.copy(alpha = 0.10f), radius = r * 0.985f, center = c, style = Stroke(1.dp.toPx()))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVinylOverlay(holeColor: Color) {
    val c = center
    val r = size.minDimension / 2f
    drawCircle(brush = sheen(c, alpha = 0.15f), radius = r * 0.99f, center = c)
    // Aro alrededor de la etiqueta y agujero del eje.
    drawCircle(Color.Black.copy(alpha = 0.55f), radius = r * 0.36f, center = c, style = Stroke(2.dp.toPx()))
    drawCircle(holeColor, radius = r * 0.03f, center = c)
    drawCircle(Color.Black.copy(alpha = 0.5f), radius = r * 0.03f, center = c, style = Stroke(1.dp.toPx()))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCdOverlay(holeColor: Color) {
    val c = center
    val r = size.minDimension / 2f
    // Arcoíris del reflejo de un CD.
    val rainbow = listOf(
        Color(0xFFFF4D4D), Color(0xFFFFD84D), Color(0xFF4DFF88), Color(0xFF4DD2FF),
        Color(0xFF7A4DFF), Color(0xFFFF4DD2), Color(0xFFFF4D4D),
    ).map { it.copy(alpha = 0.22f) }
    drawCircle(brush = Brush.sweepGradient(rainbow, center = c), radius = r, center = c)
    drawCircle(brush = sheen(c, alpha = 0.30f), radius = r, center = c)
    // Borde del disco.
    drawCircle(Color.White.copy(alpha = 0.55f), radius = r * 0.985f, center = c, style = Stroke(1.5.dp.toPx()))
    // Zona central de plástico: aro plateado, anillo transparente y agujero.
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFFF2F3F8), Color(0xFFB9BCC8)), center = c, radius = r * 0.30f),
        radius = r * 0.30f,
        center = c,
    )
    drawCircle(Color.Black.copy(alpha = 0.25f), radius = r * 0.30f, center = c, style = Stroke(1.dp.toPx()))
    drawCircle(Color.White.copy(alpha = 0.45f), radius = r * 0.19f, center = c, style = Stroke(r * 0.04f))
    drawCircle(holeColor, radius = r * 0.075f, center = c)
    drawCircle(Color.Black.copy(alpha = 0.35f), radius = r * 0.075f, center = c, style = Stroke(1.dp.toPx()))
}

/**
 * Dos destellos opuestos, como la luz sobre una superficie curva.
 * Los tramos sin brillo son blanco con alfa 0 (no Color.Transparent, que es negro con alfa 0 y deja
 * una línea de costura al interpolar), y hay varias paradas cerca del ángulo 0 por la misma razón.
 */
private fun sheen(center: Offset, alpha: Float): Brush {
    val off = Color.White.copy(alpha = 0f)
    val on = Color.White.copy(alpha = alpha)
    return Brush.sweepGradient(
        colorStops = arrayOf(
            0.00f to off,
            0.04f to off,
            0.07f to on,
            0.14f to off,
            0.50f to off,
            0.57f to on,
            0.64f to off,
            0.96f to off,
            1.00f to off,
        ),
        center = center,
    )
}
