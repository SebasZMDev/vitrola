package com.sebas.musicplayer.ui.nowplaying

import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sebas.musicplayer.data.settings.PlayerLook
import com.sebas.musicplayer.data.settings.PlayerStyle
import com.sebas.musicplayer.ui.components.SongArtwork
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** Estilos que ocupan un rectángulo en vez de un cuadrado. */
fun PlayerStyle.isScene() = this == PlayerStyle.TURNTABLE || this == PlayerStyle.CASSETTE || this == PlayerStyle.RADIO

/**
 * Escena de un objeto con carátula (tocadiscos, cassette, radio). Se ajusta al rectángulo que le den,
 * y [progress] (0..1) mueve el brazo, la cinta o la aguja del dial.
 */
@Composable
fun PlayerScene(
    style: PlayerStyle,
    artworkUri: Uri?,
    isPlaying: Boolean,
    progress: Float,
    look: PlayerLook,
    modifier: Modifier = Modifier,
) {
    val smooth by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900, easing = LinearEasing), label = "sceneProgress")
    when (style) {
        PlayerStyle.TURNTABLE -> Turntable(artworkUri, isPlaying, { smooth }, look, modifier)
        PlayerStyle.CASSETTE -> Cassette(artworkUri, isPlaying, { smooth }, modifier)
        PlayerStyle.RADIO -> RetroRadio(artworkUri, isPlaying, { smooth }, modifier)
        else -> Unit
    }
}

/** Mayor rectángulo con proporción [aspect] (ancho / alto) que cabe en el espacio disponible. */
@Composable
private fun FittedBox(aspect: Float, modifier: Modifier, content: @Composable (w: Dp, h: Dp) -> Unit) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val w = min(maxWidth.value, maxHeight.value * aspect).dp
        val h = w / aspect
        Box(Modifier.size(w, h)) { content(w, h) }
    }
}

// ───────────────────────────── Vinilo con funda ─────────────────────────────

/** Funda cuadrada con la carátula; el disco asoma por la derecha y gira. */
@Composable
fun SleeveVinyl(
    artworkUri: Uri?,
    isPlaying: Boolean,
    look: PlayerLook,
    modifier: Modifier = Modifier,
) {
    val angle = rememberSpinAngle(isPlaying, if (look.fast45) 270f else 198f)
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        // Funda (s) + el tramo de disco que asoma (0.45 s).
        val s = min(maxHeight.value, maxWidth.value / 1.45f).dp
        Box(Modifier.size(s * 1.45f, s), contentAlignment = Alignment.CenterStart) {
            DiscView(
                style = PlayerStyle.VINYL,
                artworkUri = artworkUri,
                rotation = { angle.value },
                look = look,
                modifier = Modifier.size(s * 0.94f).offset(x = s * 0.51f),
            )
            SongArtwork(
                artworkUri = artworkUri,
                modifier = Modifier.size(s).shadow(14.dp, RoundedCornerShape(6.dp)),
                cornerRadius = 6.dp,
            )
            // Borde de cartón y luz suave encima de la funda.
            Canvas(Modifier.size(s)) {
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent, Color.Black.copy(alpha = 0.18f))),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                )
                drawRoundRect(Color.Black.copy(alpha = 0.25f), cornerRadius = CornerRadius(6.dp.toPx()), style = Stroke(1.dp.toPx()))
            }
        }
    }
}

// ───────────────────────────── Tocadiscos ─────────────────────────────

private val WoodLight = Color(0xFF8A5A33)
private val WoodDark = Color(0xFF4A2D18)

@Composable
private fun Turntable(
    artworkUri: Uri?,
    isPlaying: Boolean,
    progress: () -> Float,
    look: PlayerLook,
    modifier: Modifier,
) {
    val angle = rememberSpinAngle(isPlaying, if (look.fast45) 270f else 198f)
    // El brazo baja al sonar y sube al pausar; al sonar avanza hacia el centro con el progreso.
    val lowered by animateFloatAsState(
        if (isPlaying) 1f else 0f,
        tween(800, easing = FastOutSlowInEasing),
        label = "armLowered",
    )
    FittedBox(aspect = 1.12f, modifier = modifier) { w, h ->
        // Geometría en fracciones del rectángulo (px = fracción × tamaño).
        val platter = Offset(0.41f, 0.52f)
        val recordDiameter = 0.80f // × alto
        val pivot = Offset(0.87f, 0.15f)
        Canvas(Modifier.fillMaxSize()) {
            drawTurntableBase(platter, recordDiameter, pivot)
        }
        val record = h * recordDiameter
        Box(Modifier.fillMaxSize()) {
            DiscView(
                style = PlayerStyle.VINYL,
                artworkUri = artworkUri,
                rotation = { angle.value },
                look = look,
                modifier = Modifier
                    .size(record)
                    .offset(x = w * platter.x - record / 2, y = h * platter.y - record / 2),
            )
        }
        Canvas(Modifier.fillMaxSize()) {
            val p = Offset(size.width * pivot.x, size.height * pivot.y)
            val c = Offset(size.width * platter.x, size.height * platter.y)
            val radius = size.height * recordDiameter / 2f
            // El brazo mide lo justo para que la aguja caiga cerca del borde del disco.
            val outer = Offset(c.x + radius * 0.93f * 0.62f, c.y - radius * 0.93f * 0.78f)
            val length = hypot(outer.x - p.x, outer.y - p.y)
            // Ángulo (grados, sentido horario desde "hacia abajo") con el que la aguja llega a `outer`.
            val onRecord = Math.toDegrees(atan2((p.x - outer.x).toDouble(), (outer.y - p.y).toDouble())).toFloat()
            val rest = onRecord - 68f
            val playing = onRecord + progress() * 17f
            val current = rest + (playing - rest) * lowered
            drawTonearm(p, length, current, lowered)
        }
    }
}

private fun DrawScope.drawTurntableBase(platter: Offset, recordDiameter: Float, pivot: Offset) {
    val w = size.width
    val h = size.height
    val corner = CornerRadius(h * 0.05f)
    // Sombra y caja de madera.
    drawRoundRect(Color.Black.copy(alpha = 0.28f), topLeft = Offset(0f, h * 0.025f), size = size, cornerRadius = corner)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(WoodLight, WoodDark)),
        size = size,
        cornerRadius = corner,
    )
    // Veta de la madera: líneas finas casi horizontales.
    var y = h * 0.04f
    var i = 0
    while (y < h) {
        drawLine(
            Color.Black.copy(alpha = if (i % 3 == 0) 0.10f else 0.05f),
            Offset(w * 0.02f, y),
            Offset(w * 0.98f, y + (if (i % 2 == 0) 2f else -2f)),
            strokeWidth = 1f,
        )
        y += h * 0.032f
        i++
    }
    // Tapa superior de metal cepillado, un poco más pequeña que la caja.
    val inset = h * 0.035f
    drawRoundRect(
        brush = Brush.linearGradient(listOf(Color(0xFFD7D2C8), Color(0xFF9E9A92)), Offset(0f, 0f), Offset(w, h)),
        topLeft = Offset(inset, inset),
        size = Size(w - 2 * inset, h - 2 * inset),
        cornerRadius = CornerRadius(h * 0.035f),
    )
    // Plato: anillo oscuro más grande que el disco.
    val c = Offset(w * platter.x, h * platter.y)
    val r = h * recordDiameter / 2f
    drawCircle(Color.Black.copy(alpha = 0.30f), radius = r * 1.10f, center = c.copy(y = c.y + h * 0.012f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFF3A3A3F), Color(0xFF1C1C1F)), c, r * 1.08f), radius = r * 1.07f, center = c)
    drawCircle(Color.White.copy(alpha = 0.18f), radius = r * 1.07f, center = c, style = Stroke(1.2f))
    // Base del pivote del brazo.
    val p = Offset(w * pivot.x, h * pivot.y)
    drawCircle(Color.Black.copy(alpha = 0.3f), radius = h * 0.075f, center = p.copy(y = p.y + 3f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFFEDEAE3), Color(0xFF8D8982)), p, h * 0.075f), radius = h * 0.07f, center = p)
    // Botón de encendido, detalle de adorno.
    drawCircle(Color(0xFF2A2A2D), radius = h * 0.028f, center = Offset(w * 0.90f, h * 0.88f))
    drawCircle(Color(0xFFE0C9A6), radius = h * 0.010f, center = Offset(w * 0.90f, h * 0.88f))
}

private fun DrawScope.drawTonearm(pivot: Offset, length: Float, degrees: Float, lowered: Float) {
    val thick = size.height * 0.022f
    rotate(degrees, pivot) {
        // Sombra proyectada: más lejos de la tapa cuando el brazo está levantado.
        val lift = size.height * (0.035f * (1f - lowered) + 0.008f)
        drawLine(Color.Black.copy(alpha = 0.25f), pivot + Offset(lift, lift), pivot + Offset(lift, lift + length), thick, StrokeCap.Round)
        // Contrapeso detrás del pivote.
        drawLine(Color(0xFF55555A), pivot, pivot + Offset(0f, -size.height * 0.10f), thick * 2.4f, StrokeCap.Round)
        // Tubo del brazo.
        drawLine(
            Brush.horizontalGradient(listOf(Color(0xFFF2F0EA), Color(0xFF9A978F)), pivot.x - thick, pivot.x + thick),
            pivot,
            pivot + Offset(0f, length),
            thick,
            StrokeCap.Round,
        )
        // Cabezal con la aguja.
        val head = pivot + Offset(0f, length)
        rotate(-14f, head) {
            drawRoundRect(
                Color(0xFF26262A),
                topLeft = head + Offset(-thick * 1.1f, -thick * 0.4f),
                size = Size(thick * 2.2f, thick * 3.4f),
                cornerRadius = CornerRadius(thick * 0.4f),
            )
            drawCircle(Color(0xFFE8B04A), radius = thick * 0.35f, center = head + Offset(0f, thick * 2.9f))
        }
    }
}

// ───────────────────────────── Cassette ─────────────────────────────

@Composable
private fun Cassette(
    artworkUri: Uri?,
    isPlaying: Boolean,
    progress: () -> Float,
    modifier: Modifier,
) {
    val angle = rememberSpinAngle(isPlaying, 220f)
    FittedBox(aspect = 1.6f, modifier = modifier) { w, h ->
        Canvas(Modifier.fillMaxSize()) { drawCassetteBody() }
        // Etiqueta: la carátula recortada en la franja de arriba.
        SongArtwork(
            artworkUri = artworkUri,
            modifier = Modifier
                .offset(x = w * 0.075f, y = h * 0.08f)
                .size(w * 0.85f, h * 0.50f)
                .shadow(2.dp, RoundedCornerShape(4.dp)),
            cornerRadius = 4.dp,
        )
        Canvas(Modifier.fillMaxSize()) {
            // Franja blanca translúcida con líneas de escribir, para que parezca una etiqueta de papel.
            val labelTop = size.height * 0.08f
            val labelH = size.height * 0.50f
            drawRoundRect(
                Color.White.copy(alpha = 0.10f),
                topLeft = Offset(size.width * 0.075f, labelTop),
                size = Size(size.width * 0.85f, labelH),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
            drawRect(
                Color(0xFFE9E4D6),
                topLeft = Offset(size.width * 0.075f, labelTop + labelH * 0.74f),
                size = Size(size.width * 0.85f, labelH * 0.26f),
            )
            for (k in 0..1) {
                drawLine(
                    Color(0xFF8A8576).copy(alpha = 0.55f),
                    Offset(size.width * 0.10f, labelTop + labelH * (0.84f + k * 0.10f)),
                    Offset(size.width * 0.90f, labelTop + labelH * (0.84f + k * 0.10f)),
                    1f,
                )
            }
            // Ventana de la cinta y carretes.
            val windowLeft = size.width * 0.30f
            val windowTop = size.height * 0.62f
            val windowW = size.width * 0.40f
            val windowH = size.height * 0.26f
            drawRoundRect(
                Color(0xFF111113),
                topLeft = Offset(windowLeft, windowTop),
                size = Size(windowW, windowH),
                cornerRadius = CornerRadius(windowH / 2f),
            )
            val reelR = windowH * 0.46f
            val leftC = Offset(windowLeft + windowH / 2f, windowTop + windowH / 2f)
            val rightC = Offset(windowLeft + windowW - windowH / 2f, windowTop + windowH / 2f)
            val p = progress()
            // La cinta pasa del carrete izquierdo al derecho: uno se vacía y el otro se llena.
            val packLeft = reelR * (0.52f + 0.46f * (1f - p))
            val packRight = reelR * (0.52f + 0.46f * p)
            drawReel(leftC, packLeft, reelR * 0.52f, angle.value)
            drawReel(rightC, packRight, reelR * 0.52f, angle.value)
            // Cinta tensa entre carretes, por abajo.
            drawLine(Color(0xFF3B2A1E), leftC + Offset(0f, packLeft), rightC + Offset(0f, packRight), 1.5.dp.toPx())
            // Reflejo en el plástico transparente de la ventana.
            drawRoundRect(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), windowTop, windowTop + windowH * 0.6f),
                topLeft = Offset(windowLeft, windowTop),
                size = Size(windowW, windowH),
                cornerRadius = CornerRadius(windowH / 2f),
            )
            drawRoundRect(Color.White.copy(alpha = 0.18f), topLeft = Offset(windowLeft, windowTop), size = Size(windowW, windowH), cornerRadius = CornerRadius(windowH / 2f), style = Stroke(1.dp.toPx()))
            // Tornillos en las cuatro esquinas.
            val s = size.height * 0.035f
            for (sx in listOf(0.035f, 0.965f)) for (sy in listOf(0.07f, 0.93f)) {
                drawCircle(Color(0xFFBDB6A6), s, Offset(size.width * sx, size.height * sy))
                drawLine(Color(0xFF6B665A), Offset(size.width * sx - s * 0.7f, size.height * sy), Offset(size.width * sx + s * 0.7f, size.height * sy), 1f)
            }
            // Parte inferior trapezoidal con dos agujeros.
            drawRoundRect(
                Color.Black.copy(alpha = 0.35f),
                topLeft = Offset(size.width * 0.20f, size.height * 0.90f),
                size = Size(size.width * 0.60f, size.height * 0.10f),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
        }
    }
}

private fun DrawScope.drawCassetteBody() {
    val h = size.height
    val corner = CornerRadius(h * 0.07f)
    drawRoundRect(Color.Black.copy(alpha = 0.30f), topLeft = Offset(0f, h * 0.03f), size = size, cornerRadius = corner)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF3B3B42), Color(0xFF1B1B20))),
        size = size,
        cornerRadius = corner,
    )
    drawRoundRect(Color.White.copy(alpha = 0.14f), size = size, cornerRadius = corner, style = Stroke(1.2f))
}

private fun DrawScope.drawReel(center: Offset, packRadius: Float, hubRadius: Float, degrees: Float) {
    // Cinta enrollada (marrón) con un brillo.
    drawCircle(Color(0xFF4A3426), packRadius, center)
    drawCircle(Color.White.copy(alpha = 0.10f), packRadius, center, style = Stroke(1f))
    // Eje con dientes: lo único que se ve girar.
    drawCircle(Color(0xFFE8E5DC), hubRadius, center)
    rotate(degrees, center) {
        for (k in 0 until 6) {
            val a = k * PI.toFloat() / 3f
            drawLine(
                Color(0xFF1A1A1D),
                center + Offset(cos(a), sin(a)) * (hubRadius * 0.45f),
                center + Offset(cos(a), sin(a)) * (hubRadius * 0.95f),
                hubRadius * 0.28f,
            )
        }
    }
    drawCircle(Color(0xFF1A1A1D), hubRadius * 0.28f, center)
}

// ───────────────────────────── Radio ─────────────────────────────

@Composable
private fun RetroRadio(
    artworkUri: Uri?,
    isPlaying: Boolean,
    progress: () -> Float,
    modifier: Modifier,
) {
    // La luz del dial respira mientras suena; el altavoz late muy poco.
    val pulse = rememberInfiniteTransition(label = "radioPulse")
    val breathe by pulse.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dialGlow",
    )
    val lamp by animateFloatAsState(if (isPlaying) 1f else 0f, tween(500), label = "lamp")
    val beat by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "speakerBeat",
    )
    FittedBox(aspect = 1.75f, modifier = modifier) { w, h ->
        Canvas(Modifier.fillMaxSize()) { drawRadioBody() }
        // Altavoz circular: la carátula es el cono, con rejilla de aro por encima.
        val speaker = h * 0.74f
        Box(
            Modifier
                .offset(x = w * 0.045f + (h * 0.84f - speaker) / 2f, y = (h - speaker) / 2f)
                .size(speaker)
                .graphicsLayer {
                    val s = if (isPlaying) beat else 1f
                    scaleX = s
                    scaleY = s
                }
                .clip(CircleShape),
        ) {
            SongArtwork(artworkUri, Modifier.fillMaxSize(), cornerRadius = 500.dp)
        }
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width * 0.045f + size.height * 0.42f, size.height / 2f)
            val r = size.height * 0.37f
            // Rejilla: anillos concéntricos que oscurecen un poco la carátula, y un aro metálico.
            var ring = r * 0.18f
            while (ring < r) {
                drawCircle(Color.Black.copy(alpha = 0.22f), ring, c, style = Stroke(1.6f))
                ring += r * 0.085f
            }
            drawCircle(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)), c, r), r, c)
            drawCircle(Color(0xFFCDB98A), r + 3.dp.toPx(), c, style = Stroke(4.dp.toPx()))
            drawCircle(Color.Black.copy(alpha = 0.4f), r + 5.dp.toPx(), c, style = Stroke(1.dp.toPx()))
            drawCircle(Color(0xFFCDB98A), r * 0.07f, c)

            // Ventana del dial.
            val dx = size.width * 0.54f
            val dy = size.height * 0.15f
            val dw = size.width * 0.40f
            val dh = size.height * 0.34f
            val glow = Color(0xFFFFC46B)
            drawRoundRect(Color(0xFF1B140D), Offset(dx, dy), Size(dw, dh), CornerRadius(6.dp.toPx()))
            drawRoundRect(
                Brush.verticalGradient(
                    listOf(glow.copy(alpha = 0.10f + 0.50f * lamp * breathe), glow.copy(alpha = 0.04f + 0.22f * lamp * breathe)),
                    dy,
                    dy + dh,
                ),
                Offset(dx, dy),
                Size(dw, dh),
                CornerRadius(6.dp.toPx()),
            )
            // Escala: marcas largas cada 5.
            val left = dx + dw * 0.06f
            val span = dw * 0.88f
            for (k in 0..24) {
                val x = left + span * k / 24f
                val long = k % 4 == 0
                drawLine(
                    Color(0xFFEFD9A8).copy(alpha = 0.35f + 0.45f * lamp),
                    Offset(x, dy + dh * (if (long) 0.50f else 0.62f)),
                    Offset(x, dy + dh * 0.82f),
                    if (long) 1.6f else 1f,
                )
            }
            // Aguja: avanza con la canción.
            val nx = left + span * progress()
            drawLine(Color(0xFFE5523B), Offset(nx, dy + dh * 0.12f), Offset(nx, dy + dh * 0.90f), 2.dp.toPx(), StrokeCap.Round)
            drawRoundRect(Color.White.copy(alpha = 0.16f), Offset(dx, dy), Size(dw, dh), CornerRadius(6.dp.toPx()), style = Stroke(1.2.dp.toPx()))

            // Perillas y luz piloto.
            val kr = size.height * 0.105f
            for ((index, kx) in listOf(0.64f, 0.84f).withIndex()) {
                val kc = Offset(size.width * kx, size.height * 0.74f)
                drawCircle(Color.Black.copy(alpha = 0.35f), kr * 1.12f, kc.copy(y = kc.y + 3f))
                drawCircle(Brush.radialGradient(listOf(Color(0xFFE6D3A3), Color(0xFF8D7647)), kc, kr), kr, kc)
                val a = Math.toRadians((if (index == 0) -50.0 else 35.0) + progress() * (if (index == 0) 0.0 else 120.0))
                drawLine(
                    Color(0xFF2A1D0F),
                    kc + Offset(cos(a).toFloat(), sin(a).toFloat()) * (kr * 0.25f),
                    kc + Offset(cos(a).toFloat(), sin(a).toFloat()) * (kr * 0.85f),
                    2.dp.toPx(),
                    StrokeCap.Round,
                )
            }
            val lampC = Offset(size.width * 0.945f, size.height * 0.74f)
            drawCircle(glow.copy(alpha = 0.30f * lamp), size.height * 0.06f, lampC)
            drawCircle(Color(0xFF3A2A18).let { lerpColor(it, glow, lamp) }, size.height * 0.028f, lampC)
        }
    }
}

private fun lerpColor(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t)

private fun DrawScope.drawRadioBody() {
    val h = size.height
    val corner = CornerRadius(h * 0.14f)
    drawRoundRect(Color.Black.copy(alpha = 0.30f), topLeft = Offset(0f, h * 0.03f), size = size, cornerRadius = corner)
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF9A6234), Color(0xFF5A351A))),
        size = size,
        cornerRadius = corner,
    )
    // Veta de la madera.
    var y = h * 0.05f
    var i = 0
    while (y < h * 0.97f) {
        drawLine(Color.Black.copy(alpha = if (i % 3 == 0) 0.12f else 0.05f), Offset(size.width * 0.03f, y), Offset(size.width * 0.97f, y + (if (i % 2 == 0) 2f else -2f)), 1f)
        y += h * 0.04f
        i++
    }
    drawRoundRect(Color(0xFFCDB98A).copy(alpha = 0.55f), size = size, cornerRadius = corner, style = Stroke(1.5f))
    drawRoundRect(
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent), 0f, h * 0.35f),
        size = size,
        cornerRadius = corner,
    )
}
