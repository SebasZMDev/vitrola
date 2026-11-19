package com.sebas.musicplayer.data.library

import android.net.Uri
import com.sebas.musicplayer.domain.model.Song
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class OverridingSongRepositoryTest {

    private class FakeDao : SongOverrideDao {
        val rows = mutableMapOf<Long, SongOverrideEntity>()
        override suspend fun getAll() = rows.values.toList()
        override suspend fun upsertAll(items: List<SongOverrideEntity>) { items.forEach { rows[it.songId] = it } }
        override suspend fun deleteByIds(ids: List<Long>) { ids.forEach { rows.remove(it) } }
    }

    private val uri = mockk<Uri>()

    private fun song(id: Long, title: String = "Título $id", artist: String = "Canal", album: String = "Descargas") =
        Song(id, uri, title, artist, album, 1L, 200_000L, null)

    private fun repo(dao: FakeDao, vararg songs: Song) = OverridingSongRepository(
        delegate = object : SongRepository {
            override suspend fun getSongs(forceRefresh: Boolean) = songs.toList()
        },
        dao = dao,
    )

    @Test
    fun `sin correcciones devuelve las canciones tal cual`() = runBlocking {
        val s = song(1)
        assertEquals(listOf(s), repo(FakeDao(), s).getSongs())
    }

    @Test
    fun `la correccion sustituye solo los campos editados`() = runBlocking {
        val dao = FakeDao()
        val r = repo(dao, song(1), song(2))
        r.applyEdits(listOf(SongEdit(1, artist = "Bad Bunny")))
        val songs = r.getSongs().associateBy { it.id }
        assertEquals("Bad Bunny", songs.getValue(1).artist)
        assertEquals("Descargas", songs.getValue(1).album)
        assertEquals("Canal", songs.getValue(2).artist)
    }

    @Test
    fun `una segunda edicion se fusiona con la anterior`() = runBlocking {
        val dao = FakeDao()
        val r = repo(dao, song(1))
        r.applyEdits(listOf(SongEdit(1, artist = "Bad Bunny")))
        r.applyEdits(listOf(SongEdit(1, album = "Un Verano Sin Ti")))
        val s = r.getSongs().single()
        assertEquals("Bad Bunny", s.artist)
        assertEquals("Un Verano Sin Ti", s.album)
    }

    @Test
    fun `restaurar vuelve a los metadatos originales`() = runBlocking {
        val dao = FakeDao()
        val original = song(1)
        val r = repo(dao, original)
        r.applyEdits(listOf(SongEdit(1, title = "Otro", artist = "X", album = "Y")))
        r.clearEdits(listOf(1))
        assertEquals(original, r.getSongs().single())
    }
}
