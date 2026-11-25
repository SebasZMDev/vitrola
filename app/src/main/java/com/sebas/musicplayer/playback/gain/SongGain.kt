package com.sebas.musicplayer.playback.gain

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Volumen propio de una canción, en dB (0 = como viene en el archivo). Sin fila = 0.
 * [isAuto]: lo puso "nivelar la biblioteca"; si es false es manual (también un 0) y no se pisa.
 */
@Entity(tableName = "song_gain")
data class SongGainEntity(
    @PrimaryKey val songId: Long,
    val gainDb: Float,
    @ColumnInfo(defaultValue = "0") val isAuto: Boolean = false,
)

/** Medición guardada de una canción, para no repetirla. */
@Entity(tableName = "song_loudness")
data class SongLoudnessEntity(
    @PrimaryKey val songId: Long,
    val loudnessDb: Float,
    val peak: Float,
)

@Dao
interface SongGainDao {
    @Query("SELECT * FROM song_gain")
    suspend fun getAll(): List<SongGainEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<SongGainEntity>)

    @Query("DELETE FROM song_gain WHERE isAuto = 1")
    suspend fun deleteAuto()

    @Query("SELECT * FROM song_loudness WHERE songId = :songId")
    suspend fun getLoudness(songId: Long): SongLoudnessEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLoudness(item: SongLoudnessEntity)
}

/**
 * Volúmenes por canción. Se cargan una vez en memoria: el servicio los consulta al cambiar de canción.
 * Las escrituras van en fila ([writes], por orden de llegada) para que no se pisen.
 */
class SongGainRepository(
    private val dao: SongGainDao,
    private val measure: suspend (Uri) -> Loudness?,
    private val targetDb: () -> Float = { GainMath.TARGET_DB },
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writes = Mutex()
    private val _gains = MutableStateFlow<Map<Long, Float>>(emptyMap())
    val gains: StateFlow<Map<Long, Float>> = _gains.asStateFlow()

    /** Cambios de [set] aún sin guardar, por canción: mandan sobre lo que se esté escribiendo. */
    private val pending = HashMap<Long, Int>()

    init {
        // UNDISPATCHED: toma su turno ya, antes que cualquier otra escritura.
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            writes.withLock {
                val stored = dao.getAll().associate { it.songId to it.gainDb }
                _gains.update { stored + it } // si alguien ya cambió algo mientras cargaba, gana lo nuevo
            }
        }
    }

    fun gainOf(songId: Long): Float = _gains.value[songId] ?: 0f

    /** Ajuste manual: en memoria al instante (se oye ya) y en la base en segundo plano. Un 0 también se guarda. */
    fun set(songId: Long, db: Float) {
        val v = ((db.coerceIn(GainMath.MIN_DB, GainMath.MAX_DB)) * 2f).let { kotlin.math.round(it) } / 2f
        synchronized(pending) {
            pending[songId] = (pending[songId] ?: 0) + 1
            _gains.update { it + (songId to v) }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            writes.withLock {
                try {
                    dao.upsertAll(listOf(SongGainEntity(songId, v)))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Sin guardar: se sigue oyendo el valor nuevo hasta que otra escritura publique lo de la base.
                } finally {
                    synchronized(pending) {
                        val left = pending.getValue(songId) - 1
                        if (left == 0) pending.remove(songId) else pending[songId] = left
                    }
                }
            }
        }
    }

    /** Mide la canción y le pone el volumen sugerido (queda manual). Devuelve ese volumen, o null si no se pudo medir. */
    suspend fun level(songId: Long, uri: Uri): Float? {
        val loudness = measure(uri) ?: return null
        val db = GainMath.suggest(loudness.loudnessDb, loudness.peak, targetDb())
        set(songId, db)
        return db
    }

    /** La medición guardada o, si no hay, la mide y la guarda. null si no se pudo medir. */
    suspend fun measureCached(songId: Long, uri: Uri): Loudness? {
        dao.getLoudness(songId)?.let { return Loudness(it.loudnessDb, it.peak) }
        return measure(uri)?.also { dao.upsertLoudness(SongLoudnessEntity(songId, it.loudnessDb, it.peak)) }
    }

    /** Aplica el nivelado de la biblioteca sin tocar los ajustes manuales que haya en este momento. */
    suspend fun applyAuto(plan: LevelPlan) = write {
        val manual = dao.getAll().filterNot { it.isAuto }.mapTo(HashSet()) { it.songId }
        val rows = plan.gains.filterKeys { it !in manual }.map { (id, db) -> SongGainEntity(id, db, isAuto = true) }
        dao.upsertAll(rows)
        publish { current -> current + rows.filter { it.songId !in pending }.associate { it.songId to it.gainDb } }
    }

    /** Quita las ganancias automáticas; las manuales quedan. */
    suspend fun removeAuto() = write {
        val auto = dao.getAll().filter { it.isAuto }.map { it.songId }
        dao.deleteAuto()
        publish { current -> current - auto.filter { it !in pending }.toSet() }
    }

    /** Para el respaldo: lo importado cuenta como manual. */
    suspend fun importAll(items: List<SongGainEntity>) = write {
        val rows = items.map { it.copy(isAuto = false) }
        dao.upsertAll(rows)
        publish { current -> current + rows.filter { it.songId !in pending }.associate { it.songId to it.gainDb } }
    }

    /** Todas las filas, esperando antes las escrituras en curso. */
    suspend fun getAll(): List<SongGainEntity> = writes.withLock { dao.getAll() }

    /** Una escritura a la vez; una vez empezada no se corta, para que memoria y base no se separen. */
    private suspend fun write(block: suspend () -> Unit) = writes.withLock {
        withContext(NonCancellable) { block() }
    }

    private fun publish(transform: (Map<Long, Float>) -> Map<Long, Float>) {
        synchronized(pending) { _gains.update(transform) }
    }
}
