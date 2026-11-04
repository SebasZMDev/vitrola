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
)
