package com.sebas.musicplayer.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupJsonTest {

    private val key = SongKey("Canción", "Artista", 200)

    @Test
    fun `ida y vuelta conserva los volumenes por cancion`() {
        val data = BackupData(
            exportedAt = 5L,
            playlists = listOf(BackupPlaylist("Lista", listOf(key))),
            favorites = emptyList(),
            stats = emptyList(),
            overrides = emptyList(),
            gains = listOf(BackupGain(key, -2.5f)),
        )
        val back = BackupJson.decode(BackupJson.encode(data))
        assertEquals(listOf(BackupGain(key, -2.5f)), back.gains)
        assertEquals(data.playlists, back.playlists)
    }

    @Test
    fun `el genero de overrides se lee y la biblioteca sale en el json`() {
        val data = BackupData(
            exportedAt = 1L, playlists = emptyList(), favorites = emptyList(), stats = emptyList(),
            overrides = listOf(BackupOverride(key, "Limpio", null, null, "Rock")),
            library = listOf(BackupSong(key, "Canción (sub español)", "Artista", "Álbum", null)),
        )
        val text = BackupJson.encode(data)
        assertTrue(text.contains("\"currentTitle\": \"Canción (sub español)\""))
        assertEquals(data.overrides, BackupJson.decode(text).overrides)
    }

    @Test
    fun `respaldo viejo sin volumenes se sigue leyendo`() {
        val old = """{"app":"Vitrola","version":1,"exportedAt":1,"playlists":[],"favorites":[],"stats":[],"overrides":[]}"""
        assertTrue(BackupJson.decode(old).gains.isEmpty())
    }
}
