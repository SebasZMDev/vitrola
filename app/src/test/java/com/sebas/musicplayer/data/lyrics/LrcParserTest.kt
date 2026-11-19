package com.sebas.musicplayer.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun `lee tiempos y texto ordenados`() {
        val l = LrcParser.parse("[ar:Alguien]\n[00:12.50]Segunda\n[00:05.00]Primera\n")!!
        assertTrue(l.synced)
        assertEquals(listOf("Primera", "Segunda"), l.lines.map { it.text })
        assertEquals(listOf(5000L, 12500L), l.lines.map { it.timeMs })
    }

    @Test
    fun `varias marcas en una linea repiten el texto`() {
        val l = LrcParser.parse("[00:10.00][00:50.00]Estribillo")!!
        assertEquals(listOf(10_000L, 50_000L), l.lines.map { it.timeMs })
        assertEquals(listOf("Estribillo", "Estribillo"), l.lines.map { it.text })
    }

    @Test
    fun `fracciones de uno dos y tres digitos`() {
        val l = LrcParser.parse("[00:01.5]a\n[00:02.25]b\n[00:03.125]c")!!
        assertEquals(listOf(1500L, 2250L, 3125L), l.lines.map { it.timeMs })
    }

    @Test
    fun `offset adelanta o atrasa`() {
        val l = LrcParser.parse("[offset:500]\n[00:10.00]x")!!
        assertEquals(9500L, l.lines.single().timeMs)
    }

    @Test
    fun `quita marcas por palabra y el BOM`() {
        val l = LrcParser.parse("﻿[00:01.00]<00:01.00>Hola <00:01.40>mundo")!!
        assertEquals("Hola mundo", l.lines.single().text)
    }

    @Test
    fun `sin tiempos devuelve texto plano`() {
        val l = LrcParser.parse("Linea uno\n\nLinea dos")!!
        assertFalse(l.synced)
        assertEquals(listOf("Linea uno", "Linea dos"), l.lines.map { it.text })
    }

    @Test
    fun `vacio devuelve null`() {
        assertNull(LrcParser.parse("[ti:Solo etiqueta]\n\n"))
    }

    @Test
    fun `indexAt devuelve la linea vigente`() {
        val l = LrcParser.parse("[00:05.00]a\n[00:10.00]b\n[00:20.00]c")!!
        assertEquals(-1, l.indexAt(1000))
        assertEquals(0, l.indexAt(5000))
        assertEquals(1, l.indexAt(19_999))
        assertEquals(2, l.indexAt(60_000))
    }
}
