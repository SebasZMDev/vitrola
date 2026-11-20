package com.sebas.musicplayer.data.playlist.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sebas.musicplayer.data.library.SongOverrideDao
import com.sebas.musicplayer.data.library.SongOverrideEntity
import com.sebas.musicplayer.data.stats.FavoriteEntity
import com.sebas.musicplayer.data.stats.PlayStatEntity
import com.sebas.musicplayer.data.stats.SongStatsDao
import com.sebas.musicplayer.playback.gain.SongGainDao
import com.sebas.musicplayer.playback.gain.SongGainEntity

@Database(
    entities = [
        PlaylistEntity::class, PlaylistSongEntity::class, SongOverrideEntity::class,
        FavoriteEntity::class, PlayStatEntity::class, SongGainEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun songOverrideDao(): SongOverrideDao
    abstract fun songStatsDao(): SongStatsDao
    abstract fun songGainDao(): SongGainDao

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

        /** v3: favoritas e historial de reproducción. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `favorites` (`songId` INTEGER NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`songId`))")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `play_stats` (`songId` INTEGER NOT NULL, `playCount` INTEGER NOT NULL, " +
                        "`lastPlayedAt` INTEGER NOT NULL, PRIMARY KEY(`songId`))",
                )
            }
        }

        /** v4: portada personalizada de playlist (ruta del archivo). */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `playlists` ADD COLUMN `coverPath` TEXT")
            }
        }

        /** v5: volumen propio por canción. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `song_gain` (`songId` INTEGER NOT NULL, `gainDb` REAL NOT NULL, PRIMARY KEY(`songId`))")
            }
        }

        @Volatile
        private var instance: MusicDatabase? = null

        fun get(context: Context): MusicDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, MusicDatabase::class.java, "music.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                .also { instance = it }
        }
    }
}
