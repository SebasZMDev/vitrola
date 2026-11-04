package com.sebas.musicplayer.data.library

import com.sebas.musicplayer.domain.model.Song

/**
 * Fuente de canciones. La UI depende de esta interfaz, nunca de la implementación.
 * CONTRATO: no cambiar sin avisar al orquestador.
 */
interface SongRepository {
    /** Devuelve todas las canciones de música del dispositivo, ordenadas por título. Corre fuera del hilo principal.
     * Si [forceRefresh] es false, puede devolver una copia en caché. */
    suspend fun getSongs(forceRefresh: Boolean = false): List<Song>
}
