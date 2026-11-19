package com.sebas.musicplayer.ui.components

import android.content.Context
import android.net.Uri
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val NO_COLOR = 0
private val colorCache = LruCache<String, Int>(128)

/**
 * Color dominante de una carátula, para teñir fondos. Mientras carga (o si no hay carátula)
 * devuelve el color primario del tema. El cambio entre colores va animado.
 */
@Composable
fun rememberArtworkColor(artworkUri: Uri?): Color {
    val fallback = MaterialTheme.colorScheme.primary
    val context = LocalContext.current.applicationContext
    val initial = artworkUri?.let { colorCache.get(it.toString()) }?.takeIf { it != NO_COLOR }?.let(::Color)
    val target by produceState(initialValue = initial ?: fallback, artworkUri, fallback) {
        if (artworkUri == null) {
            value = fallback
            return@produceState
        }
        val key = artworkUri.toString()
        val argb = colorCache.get(key) ?: (extractColor(context, artworkUri) ?: NO_COLOR).also { colorCache.put(key, it) }
        value = if (argb == NO_COLOR) fallback else Color(argb)
    }
    val animated by animateColorAsState(target, tween(600), label = "artworkColor")
    return animated
}

/** Resplandor del color de la carátula que se desvanece hacia abajo. Va detrás del contenido. */
@Composable
fun ArtworkGlow(color: Color, modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val alpha = if (dark) 0.55f else 0.35f
    Box(modifier.background(Brush.verticalGradient(listOf(color.copy(alpha = alpha), Color.Transparent))))
}

private suspend fun extractColor(context: Context, uri: Uri): Int? {
    val request = ImageRequest.Builder(context).data(uri).size(48).allowHardware(false).build()
    val result = SingletonImageLoader.get(context).execute(request) as? SuccessResult ?: return null
    return withContext(Dispatchers.Default) {
        runCatching {
            val bitmap = result.image.toBitmap()
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            dominantColor(pixels)
        }.getOrNull()
    }
}

/** Promedio ponderado por lo "vivo" de cada píxel, para que no gane el gris o el negro del fondo. */
internal fun dominantColor(pixels: IntArray): Int? {
    if (pixels.isEmpty()) return null
    val hsv = FloatArray(3)
    var r = 0.0
    var g = 0.0
    var b = 0.0
    var total = 0.0
    for (pixel in pixels) {
        android.graphics.Color.colorToHSV(pixel, hsv)
        val weight = hsv[1] * hsv[1] * hsv[2] + 0.02
        r += android.graphics.Color.red(pixel) * weight
        g += android.graphics.Color.green(pixel) * weight
        b += android.graphics.Color.blue(pixel) * weight
        total += weight
    }
    val average = android.graphics.Color.rgb((r / total).toInt(), (g / total).toInt(), (b / total).toInt())
    android.graphics.Color.colorToHSV(average, hsv)
    // Carátulas casi grises se quedan grises; al resto se le asegura algo de color y de luz.
    if (hsv[1] > 0.08f) hsv[1] = hsv[1].coerceIn(0.3f, 0.8f)
    hsv[2] = hsv[2].coerceIn(0.45f, 0.8f)
    return android.graphics.Color.HSVToColor(hsv)
}
