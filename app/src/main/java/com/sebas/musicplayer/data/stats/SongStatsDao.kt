package com.sebas.musicplayer.data.stats

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SongStatsDao {
    /** Favoritas, la más reciente primero. */
    @Query("SELECT songId FROM favorites ORDER BY addedAt DESC")
    fun observeFavoriteIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorites(items: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE songId = :songId")
    suspend fun removeFavorite(songId: Long)

    @Query("SELECT * FROM play_stats")
    fun observeStats(): Flow<List<PlayStatEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStatIfMissing(stat: PlayStatEntity): Long

    @Query("UPDATE play_stats SET playCount = playCount + 1, lastPlayedAt = :now WHERE songId = :songId")
    suspend fun bumpStat(songId: Long, now: Long)

    @Query("SELECT * FROM favorites")
    suspend fun getAllFavorites(): List<FavoriteEntity>

    @Query("SELECT * FROM play_stats")
    suspend fun getAllStats(): List<PlayStatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStats(items: List<PlayStatEntity>)

    @Query("DELETE FROM favorites WHERE songId IN (:ids)")
    suspend fun removeFavorites(ids: List<Long>)

    @Query("DELETE FROM play_stats WHERE songId IN (:ids)")
    suspend fun removeStats(ids: List<Long>)
}
