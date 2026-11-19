package com.sebas.musicplayer.data.playlist.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    /** Ruta de la portada elegida por el usuario (archivo propio de la app); null = collage automático. */
    val coverPath: String? = null,
)
