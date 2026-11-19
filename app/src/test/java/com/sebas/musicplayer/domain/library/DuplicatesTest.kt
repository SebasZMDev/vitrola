package com.sebas.musicplayer.domain.library

import android.net.Uri
import com.sebas.musicplayer.domain.model.Song
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicatesTest {

    private val uri = mockk<Uri>()

    private fun song(id: Long, title: String, artist: String = "X", durationSec: Int = 200) =
        Song(id, uri, title, artist, "Álbum", 1L, durationSec * 1000L, null)

    @Test
    fun `agrupa mismo titulo y artista aunque cambien mayusculas y espacios`() {
        val groups = findDuplicateGroups(listOf(song(1, "Tema"), song(2, "  tema  "), song(3, "Otro")))
        assertEquals(1, groups.size)
        assertEquals(listOf(1L, 2L), groups.single().map { it.id })
    }

    @Test
    fun `no junta versiones de duracion muy distinta`() {
        val groups = findDuplicateGroups(listOf(song(1, "Tema", durationSec = 200), song(2, "Tema", durationSec = 330)))
        assertTrue(groups.isEmpty())
    }

    @Test
    fun `distinto artista no es duplicada`() {
        assertTrue(findDuplicateGroups(listOf(song(1, "Tema", "A"), song(2, "Tema", "B"))).isEmpty())
    }

    @Test
    fun `tres copias forman un solo grupo ordenado por id`() {
        val groups = findDuplicateGroups(listOf(song(9, "Tema", durationSec = 201), song(3, "Tema"), song(5, "Tema", durationSec = 199)))
        assertEquals(listOf(3L, 5L, 9L), groups.single().map { it.id })
    }

    @Test
    fun `sugiere eliminar todas menos la mas antigua de cada grupo`() {
        val groups = findDuplicateGroups(listOf(song(1, "A"), song(2, "A"), song(3, "B"), song(4, "B"), song(5, "B")))
        assertEquals(setOf(2L, 4L, 5L), suggestedRemovals(groups))
    }
}
