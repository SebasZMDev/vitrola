package com.sebas.musicplayer.ui.playlists

import org.junit.Assert.assertEquals
import org.junit.Test

class RemovalOrderTest {

    @Test
    fun removalGoesFromLast() {
        assertEquals(listOf(4, 2, 1), removalOrder(listOf(1, 4, 1, 2)))
    }
}
