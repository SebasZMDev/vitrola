package com.sebas.musicplayer.domain.model

import android.net.Uri

/**
 * Una canción del dispositivo. Modelo de dominio compartido por todas las capas.
 * CONTRATO: no cambiar sin avisar al orquestador.
 */
data class Song(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val artworkUri: Uri?,
    /** Género puesto por el usuario (el del archivo casi nunca sirve); null = sin género. */
    val genre: String? = null,
    /** Cuándo se agregó al teléfono, en segundos (MediaStore DATE_ADDED). */
    val dateAdded: Long = 0,
)
