package com.sebas.musicplayer.data.stats

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Canción marcada con el corazón. */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: Long,
    val addedAt: Long,
)

/** Cuántas veces se escuchó una canción y cuándo fue la última. */
@Entity(tableName = "play_stats")
data class PlayStatEntity(
    @PrimaryKey val songId: Long,
    val playCount: Int,
    val lastPlayedAt: Long,
)
