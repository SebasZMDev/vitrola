package com.sebas.musicplayer.data.library

import com.sebas.musicplayer.data.settings.SettingsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryFilterTest {

    private fun clause(
        minSec: Int = 30,
        whatsApp: Boolean = true,
        folders: Set<String> = emptySet(),
    ) = LibraryFilter.clause("path", "music", "dur", minSec, whatsApp, folders)

    @Test
    fun `por defecto filtra solo musica, duracion y WhatsApp`() {
        val c = clause()
        assertEquals(
            "music != 0 AND dur >= ? AND COALESCE(path, '') NOT LIKE ? COLLATE NOCASE",
            c.selection,
        )
        assertEquals(listOf("30000", "%WhatsApp%"), c.args)
    }

    @Test
    fun `sin WhatsApp ni carpetas queda solo musica y duracion`() {
        val c = clause(minSec = 0, whatsApp = false)
        assertEquals("music != 0 AND dur >= ?", c.selection)
        assertEquals(listOf("0"), c.args)
    }

    @Test
    fun `cada carpeta excluida agrega un NOT LIKE con prefijo`() {
        val c = clause(whatsApp = false, folders = setOf("Music/Rock/", "Download/"))
        assertEquals(2, c.selection.split("NOT LIKE").size - 1)
        // Orden estable (alfabético) para que selection y args coincidan siempre.
        assertEquals(listOf("30000", "Download/%", "Music/Rock/%"), c.args)
    }

    @Test
    fun `escapa porcentaje guion bajo y barra invertida en las rutas`() {
        assertEquals("Music/100\\%/mi\\_carpeta/", LibraryFilter.escapeLike("Music/100%/mi_carpeta/"))
        assertEquals("a\\\\b", LibraryFilter.escapeLike("a\\b"))
        val c = clause(whatsApp = false, folders = setOf("Music/mi_carpeta/"))
        assertEquals("Music/mi\\_carpeta/%", c.args.last())
    }

    @Test
    fun `la cantidad de signos de pregunta coincide con los argumentos`() {
        val c = clause(folders = setOf("A/", "B/", "C/"))
        assertEquals(c.args.size, c.selection.count { it == '?' })
    }

    @Test
    fun `folderOf usa la ruta relativa tal cual y quita el archivo de la ruta completa`() {
        assertEquals("Music/Rock/", LibraryFilter.folderOf("Music/Rock/", isFullFilePath = false))
        assertEquals(
            "/storage/emulated/0/Music/Rock/",
            LibraryFilter.folderOf("/storage/emulated/0/Music/Rock/cancion.mp3", isFullFilePath = true),
        )
        assertEquals("", LibraryFilter.folderOf(null, isFullFilePath = false))
        assertEquals("", LibraryFilter.folderOf("cancion.mp3", isFullFilePath = true))
    }

    @Test
    fun `una carpeta excluida tambien excluye sus subcarpetas`() {
        val s = SettingsState(excludedFolders = setOf("Music/Rock/"))
        assertTrue(s.isFolderExcluded("Music/Rock/"))
        assertTrue(s.isFolderExcluded("Music/Rock/Live/"))
        assertTrue(s.isFolderExcluded("music/rock/"))
        assertFalse(s.isFolderExcluded("Music/Rockabilly/"))
        assertFalse(s.isFolderExcluded("Music/"))
    }
}
