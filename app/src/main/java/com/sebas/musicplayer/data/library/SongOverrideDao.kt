package com.sebas.musicplayer.data.library

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SongOverrideDao {
    @Query("SELECT * FROM song_overrides")
    suspend fun getAll(): List<SongOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SongOverrideEntity>)

    @Query("DELETE FROM song_overrides WHERE songId IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
