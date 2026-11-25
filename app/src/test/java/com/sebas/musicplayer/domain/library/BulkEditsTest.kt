package com.sebas.musicplayer.domain.library

import android.net.Uri
import com.sebas.musicplayer.data.library.SongEdit
import com.sebas.musicplayer.domain.model.Song
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class BulkEditsTest {
    private val uri = mockk<Uri>()

    private fun song(id: Long, title: String = "t$id", artist: String = "A", album: String = "Al", genre: String? = null) =
        Song(id, uri, title, artist, album, 1, 1000, null, genre = genre)

    @Test
    fun commonMetadataBlankWhenDiffers() {
        val songs = listOf(song(1, artist = "A", genre = "Pop"), song(2, artist = "B", genre = "Pop"))
        assertEquals(Triple("", "Al", "Pop"), commonMetadata(songs))
    }

    @Test
    fun bulkEditsEmptyWhenAllBlank() {
        assertEquals(emptyList<SongEdit>(), bulkEdits(listOf(song(1)), "", "  ", ""))
    }

    @Test
    fun bulkEditsOnlyFilledFields() {
        assertEquals(listOf(SongEdit(1, artist = "X")), bulkEdits(listOf(song(1)), " X ", "", " "))
    }

    @Test
    fun bulkEditsDedupes() {
        val songs = listOf(song(1), song(2), song(1))
        assertEquals(listOf(1L, 2L), bulkEdits(songs, "X", "", "").map { it.songId })
        assertEquals(
            listOf(1L),
            splitTitleEdits(listOf(song(1, title = "A - B"), song(1, title = "A - B"))).map { it.songId },
        )
        assertEquals(Triple("A", "Al", ""), commonMetadata(listOf(song(1), song(1))))
    }

    @Test
    fun splitTitleSkipsWithoutSeparator() {
        val songs = listOf(song(1, title = "A - B"), song(2, title = "Solo"))
        assertEquals(listOf(SongEdit(1, title = "B", artist = "A")), splitTitleEdits(songs))
    }
}
