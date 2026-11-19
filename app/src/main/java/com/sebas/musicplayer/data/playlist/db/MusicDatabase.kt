package com.sebas.musicplayer.data.playlist.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sebas.musicplayer.data.library.SongOverrideDao
import com.sebas.musicplayer.data.library.SongOverrideEntity

@Database(
    entities = [PlaylistEntity::class, PlaylistSongEntity::class, SongOverrideEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun songOverrideDao(): SongOverrideDao

    companion object {
        /** v2: metadatos corregidos por el usuario (artista, álbum, título). */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `song_overrides` (`songId` INTEGER NOT NULL, `title` TEXT, " +
                        "`artist` TEXT, `album` TEXT, PRIMARY KEY(`songId`))",
                )
            }
        }

        @Volatile
        private var instance: MusicDatabase? = null

        fun get(context: Context): MusicDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, MusicDatabase::class.java, "music.db")
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}
