package com.sebas.musicplayer.data.backup

import android.net.Uri
import com.sebas.musicplayer.domain.model.Song
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupModelsTest {

    private val uri = mockk<Uri>()

    private fun song(id: Long, title: String, artist: String, durationSec: Int) =
        Song(id, uri, title, artist, "Álbum", 1L, durationSec * 1000L, null)

    @Test
    fun `encuentra la cancion por titulo y artista sin importar mayusculas ni espacios`() {
        val matcher = SongMatcher(listOf(song(10, "  Titi Me Preguntó ", "Bad Bunny", 243)))
        assertEquals(10L, matcher.find(SongKey("titi me preguntó", "BAD BUNNY", 243)))
    }

    @Test
    fun `tolera hasta 2 segundos de diferencia en la duracion`() {
        val matcher = SongMatcher(listOf(song(1, "A", "B", 200)))
        assertEquals(1L, matcher.find(SongKey("A", "B", 202)))
        assertNull(matcher.find(SongKey("A", "B", 203)))
    }

    @Test
    fun `entre dos versiones elige la de duracion mas parecida`() {
        val matcher = SongMatcher(listOf(song(1, "Tema", "X", 180), song(2, "Tema", "X", 240)))
        assertEquals(2L, matcher.find(SongKey("Tema", "X", 241)))
    }

    @Test
    fun `una cancion que no esta devuelve null`() {
        val matcher = SongMatcher(listOf(song(1, "A", "B", 100)))
        assertNull(matcher.find(SongKey("Otra", "B", 100)))
    }

    @Test
    fun `el resumen solo menciona lo que paso`() {
        val simple = ImportReport(2, 0, 5, 7, 1, 0).summary()
        assertEquals("Importado: 2 playlists, 5 favoritas, 7 con historial, 1 editadas", simple)
        val full = ImportReport(1, 3, 0, 0, 0, 4).summary()
        assertEquals(
            "Importado: 1 playlists, 0 favoritas, 0 con historial, 0 editadas. " +
                "3 playlists ya existían y se dejaron igual. 4 canciones no están en este teléfono",
            full,
        )
    }
}
