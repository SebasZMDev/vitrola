package com.sebas.musicplayer.domain.model

/**
 * Playlist creada por el usuario. Guarda ids de MediaStore ([Song.id]), no copias de las canciones.
 * CONTRATO: no cambiar sin avisar al orquestador.
 */
data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int,
)
