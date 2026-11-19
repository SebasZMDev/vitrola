package com.sebas.musicplayer.data.library

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Metadatos corregidos por el usuario; un campo null significa "dejar el original del archivo". */
@Entity(tableName = "song_overrides")
data class SongOverrideEntity(
    @PrimaryKey val songId: Long,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
)
