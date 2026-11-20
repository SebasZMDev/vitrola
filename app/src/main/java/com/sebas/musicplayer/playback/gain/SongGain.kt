package com.sebas.musicplayer.playback.gain

import android.net.Uri
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Volumen propio de una canción, en dB (0 = como viene en el archivo). Sin fila = 0. */
@Entity(tableName = "song_gain")
data class SongGainEntity(
    @PrimaryKey val songId: Long,
    val gainDb: Float,
)

@Dao
interface SongGainDao {
    @Query("SELECT * FROM song_gain")
    suspend fun getAll(): List<SongGainEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SongGainEntity>)

    @Query("DELETE FROM song_gain WHERE songId = :songId")
    suspend fun delete(songId: Long)
}

/**
 * Volúmenes por canción. Se cargan una vez en memoria: el servicio los consulta al cambiar de canción.
 */
class SongGainRepository(private val dao: SongGainDao, private val analyzer: LoudnessAnalyzer) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _gains = MutableStateFlow<Map<Long, Float>>(emptyMap())
    val gains: StateFlow<Map<Long, Float>> = _gains.asStateFlow()

    init {
        scope.launch {
            val stored = dao.getAll().associate { it.songId to it.gainDb }
            _gains.update { stored + it } // si alguien ya cambió algo mientras cargaba, gana lo nuevo
        }
    }

    fun gainOf(songId: Long): Float = _gains.value[songId] ?: 0f

    /** Guarda en memoria al instante (se oye ya) y en la base en segundo plano. */
    fun set(songId: Long, db: Float) {
        val v = ((db.coerceIn(GainMath.MIN_DB, GainMath.MAX_DB)) * 2f).let { kotlin.math.round(it) } / 2f
        _gains.update { if (v == 0f) it - songId else it + (songId to v) }
        scope.launch { if (v == 0f) dao.delete(songId) else dao.upsertAll(listOf(SongGainEntity(songId, v))) }
    }

    /** Mide la canción y le pone el volumen sugerido. Devuelve ese volumen, o null si no se pudo medir. */
    suspend fun level(songId: Long, uri: Uri): Float? {
        val loudness = analyzer.measure(uri) ?: return null
        val db = GainMath.suggest(loudness.loudnessDb, loudness.peak)
        set(songId, db)
        return db
    }

    /** Para el respaldo. */
    suspend fun importAll(items: List<SongGainEntity>) {
        dao.upsertAll(items)
        _gains.update { it + items.associate { e -> e.songId to e.gainDb } }
    }

    suspend fun getAll(): List<SongGainEntity> = dao.getAll()
}
