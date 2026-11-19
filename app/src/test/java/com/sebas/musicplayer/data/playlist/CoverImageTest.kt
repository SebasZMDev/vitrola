package com.sebas.musicplayer.data.playlist

import org.junit.Assert.assertEquals
import org.junit.Test

class CoverImageTest {

    @Test
    fun `una imagen chica no se reduce`() {
        assertEquals(1, CoverImage.sampleSizeFor(500, 800))
        assertEquals(1, CoverImage.sampleSizeFor(600, 600))
    }

    @Test
    fun `reduce a la mayor potencia de 2 sin bajar del tamano objetivo`() {
        // 4000x3000: el lado menor 3000 / 4 = 750 >= 600, pero / 8 = 375 < 600.
        assertEquals(4, CoverImage.sampleSizeFor(4000, 3000))
        // 1200: 1200 / 2 = 600 >= 600 justo.
        assertEquals(2, CoverImage.sampleSizeFor(1200, 5000))
    }

    @Test
    fun `usa el lado menor aunque la foto sea muy alargada`() {
        assertEquals(1, CoverImage.sampleSizeFor(9000, 700))
    }
}
