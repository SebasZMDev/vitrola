package com.sebas.musicplayer.widget

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader

private const val WIDTH = 420
private const val SIDE = 300

/**
 * La funda con el vinilo asomando. Se dibuja una sola vez por canción (en el hilo del widget)
 * y el resultado se reutiliza: no hay animación ni trabajo por cuadro.
 */
internal fun sleeveArt(cover: Bitmap?): Bitmap {
    val out = Bitmap.createBitmap(WIDTH, SIDE, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    // Disco: negro, surcos, borde latón para separarlo del fondo y etiqueta.
    val cy = SIDE / 2f
    val radius = SIDE * 0.47f
    val cx = WIDTH - radius - 2f
    paint.color = 0xFF111113.toInt()
    canvas.drawCircle(cx, cy, radius, paint)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f
    paint.color = 0x40EDE6D3
    var groove = radius * 0.5f
    while (groove < radius - 6f) {
        canvas.drawCircle(cx, cy, groove, paint)
        groove += 10f
    }
    paint.color = 0xCCD2B57A.toInt()
    canvas.drawCircle(cx, cy, radius - 1f, paint)
    paint.style = Paint.Style.FILL
    paint.color = 0xFFD2B57A.toInt()
    canvas.drawCircle(cx, cy, radius * 0.38f, paint)

    // Sombra suave de la funda sobre el disco.
    val sleeve = RectF(0f, 0f, SIDE.toFloat(), SIDE.toFloat())
    paint.color = 0x30000000
    for (step in 1..3) {
        canvas.drawRoundRect(RectF(sleeve).apply { offset(step * 3f, 0f) }, 26f, 26f, paint)
    }

    // Funda: carátula recortada al cuadrado con esquinas redondeadas.
    paint.color = 0xFF1E4F57.toInt()
    if (cover != null) {
        val scale = SIDE / minOf(cover.width, cover.height).toFloat()
        paint.color = 0xFF000000.toInt()
        paint.shader = BitmapShader(cover, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(Matrix().apply {
                setScale(scale, scale)
                postTranslate((SIDE - cover.width * scale) / 2f, (SIDE - cover.height * scale) / 2f)
            })
        }
    }
    canvas.drawRoundRect(sleeve, 26f, 26f, paint)
    paint.shader = null

    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2f
    paint.color = 0x55D2B57A
    sleeve.inset(1f, 1f)
    canvas.drawRoundRect(sleeve, 25f, 25f, paint)
    return out
}
