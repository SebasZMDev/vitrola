package com.sebas.musicplayer.data.playlist

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/** Prepara la imagen de portada de una playlist: cuadrada, de tamaño razonable y en JPEG. */
object CoverImage {
    /** Lado de la portada guardada, en píxeles. Sobra para la pantalla de detalle y pesa poco. */
    const val SIZE = 600

    /**
     * Mayor potencia de 2 con la que se puede reducir al decodificar sin que el lado menor baje de [target].
     * Evita cargar fotos de 12 MP enteras en memoria.
     */
    fun sampleSizeFor(width: Int, height: Int, target: Int = SIZE): Int {
        var sample = 1
        val shortSide = minOf(width, height)
        while (shortSide / (sample * 2) >= target) sample *= 2
        return sample
    }

    /** Lee [source], recorta el centro en cuadrado, lo escala a [SIZE] y lo guarda en [target]. false si no se pudo leer. */
    fun saveSquareJpeg(resolver: ContentResolver, source: Uri, target: File): Boolean = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            false
        } else {
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight) }
            val decoded = resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
            if (decoded == null) {
                false
            } else {
                val side = minOf(decoded.width, decoded.height)
                val square = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side)
                val scaled = if (side > SIZE) Bitmap.createScaledBitmap(square, SIZE, SIZE, true) else square
                val ok = target.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                if (scaled !== square) scaled.recycle()
                if (square !== decoded) square.recycle()
                decoded.recycle()
                ok
            }
        }
    } catch (e: Exception) {
        false
    }
}
