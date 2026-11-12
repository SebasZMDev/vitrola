package com.sebas.musicplayer.domain.library

import android.net.Uri
import com.sebas.musicplayer.domain.model.Song
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryGroupingTest {
    private val uri = mockk<Uri>()

    private fun song(id: Long, title: String = "t$id", artist: String = "A", album: String = "Al", albumId: Long = 1, durationMs: Long = 1000) =
        Song(id, uri, title, artist, album, albumId, durationMs, null)

    @Test
    fun `groupAlbums junta por albumId y suma duracion`() {
        val albums = groupAlbums(listOf(song(1, albumId = 1, durationMs = 100), song(2, albumId = 1, durationMs = 200), song(3, albumId = 2, album = "Otro")))
        assertEquals(2, albums.size)
        assertEquals(300L, albums.first { it.id == 1L }.durationMs)
    }

    @Test
    fun `album con varios artistas se marca Varios artistas`() {
        val album = groupAlbums(listOf(song(1, artist = "X"), song(2, artist = "Y"))).single()
        assertEquals(VARIOUS_ARTISTS, album.artist)
    }

    @Test
    fun `album con un solo artista lo conserva`() {
        assertEquals("X", groupAlbums(listOf(song(1, artist = "X"), song(2, artist = "X"))).single().artist)
    }

    @Test
    fun `albumes ordenados por titulo sin importar mayusculas`() {
        val titles = groupAlbums(listOf(song(1, album = "beta", albumId = 1), song(2, album = "Alfa", albumId = 2), song(3, album = "Zeta", albumId = 3))).map { it.title }
        assertEquals(listOf("Alfa", "beta", "Zeta"), titles)
    }

    @Test
    fun `groupArtists agrupa ordena y trae sus albumes`() {
        val artists = groupArtists(listOf(song(1, artist = "b", albumId = 1), song(2, artist = "A", albumId = 2, album = "X"), song(3, artist = "A", albumId = 3, album = "Y")))
        assertEquals(listOf("A", "b"), artists.map { it.name })
        assertEquals(2, artists.first().albums.size)
        assertEquals(2, artists.first().songs.size)
    }

    @Test
    fun `listas vacias dan resultados vacios`() {
        assertTrue(groupAlbums(emptyList()).isEmpty())
        assertTrue(groupArtists(emptyList()).isEmpty())
    }

    @Test
    fun `normalizedForSearch quita tildes y mayusculas`() {
        assertEquals("cancion", "Canción".normalizedForSearch())
        assertEquals("nino", "NIÑO".normalizedForSearch())
    }
}
