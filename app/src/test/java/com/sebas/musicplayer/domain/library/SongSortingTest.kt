package com.sebas.musicplayer.domain.library

import android.net.Uri
import com.sebas.musicplayer.data.settings.SongSort
import com.sebas.musicplayer.data.settings.SongSortField
import com.sebas.musicplayer.domain.model.Song
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SongSortingTest {
    private val uri = mockk<Uri>()

    private fun song(id: Long, title: String = "t$id", artist: String = "A", dateAdded: Long = 0) =
        Song(id, uri, title, artist, "Al", 1, 1000, null, dateAdded = dateAdded)

    private fun ids(songs: List<Song>, sort: SongSort) = sortSongs(songs, sort) { it }.map { it.id }

    @Test
    fun originalKeepsOrder() {
        val songs = listOf(song(3), song(1), song(2))
        assertEquals(listOf(3L, 1L, 2L), ids(songs, ListDefaultSort))
        assertEquals(listOf(2L, 1L, 3L), ids(songs, ListDefaultSort.copy(ascending = false)))
    }

    @Test
    fun recentDescendingPutsNewestFirst() {
        val songs = listOf(song(1, dateAdded = 10), song(2, dateAdded = 30), song(3, dateAdded = 20))
        val sorted = sortSongs(songs, SongSort(SongSortField.RECENT, false)) { it }
        assertEquals(listOf(30L, 20L, 10L), sorted.map { it.dateAdded })
    }

    @Test
    fun titleIgnoresCase() {
        val songs = listOf(song(1, title = "b"), song(2, title = "A"))
        assertEquals(listOf(2L, 1L), ids(songs, SongSort(SongSortField.TITLE, true)))
    }

    @Test
    fun tiesInArtistBreakByTitle() {
        val songs = listOf(song(1, title = "z", artist = "x"), song(2, title = "a", artist = "X"))
        assertEquals(listOf(2L, 1L), ids(songs, SongSort(SongSortField.ARTIST, true)))
    }

    @Test
    fun worksWithWrappedItemsAndDuplicates() {
        val repeated = song(1, title = "b")
        val items = listOf(0 to repeated, 1 to song(2, title = "a"), 2 to repeated)
        val sorted = sortSongs(items, SongSort(SongSortField.TITLE, true)) { it.second }
        assertEquals(listOf(1, 0, 2), sorted.map { it.first })
    }

    @Test
    fun nextSortInvertsActive() {
        val current = SongSort(SongSortField.TITLE, true)
        val inverted = nextSort(current, SongSortField.TITLE)
        assertEquals(SongSort(SongSortField.TITLE, false), inverted)
        assertEquals(current, nextSort(inverted, SongSortField.TITLE))
    }

    @Test
    fun nextSortRecentStartsDescending() {
        val next = nextSort(ListDefaultSort, SongSortField.RECENT)
        assertEquals(SongSortField.RECENT, next.field)
        assertFalse(next.ascending)
    }

    @Test
    fun nextSortOtherStartsAscending() {
        val next = nextSort(SongSort(SongSortField.RECENT, false), SongSortField.ARTIST)
        assertEquals(SongSortField.ARTIST, next.field)
        assertTrue(next.ascending)
    }

    @Test
    fun decodeRoundTrips() {
        assertEquals("RECENT:false", encodeSort(SongSort(SongSortField.RECENT, false)))
        SongSortField.entries.forEach { field ->
            listOf(true, false).forEach { ascending ->
                val sort = SongSort(field, ascending)
                assertEquals(sort, decodeSort(encodeSort(sort)))
            }
        }
    }

    @Test
    fun decodeRejectsGarbage() {
        listOf(null, "", "FOO:true", "TITLE", "TITLE:yes", "TITLE:true:x").forEach { assertNull(decodeSort(it)) }
    }
}
