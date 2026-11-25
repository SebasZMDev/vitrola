package com.sebas.musicplayer.playback

import kotlin.random.Random
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ShuffleOrderTest {
    @Test
    fun startsWithCurrentAndKeepsEverySong() {
        val order = shuffleOrderFrom(count = 10, current = 7, random = Random(1))
        assertEquals(7, order.first())
        assertArrayEquals(IntArray(10) { it }, order.sortedArray())
    }

    @Test
    fun eachActivationMixesAgain() {
        val random = Random(1)
        assertNotEquals(shuffleOrderFrom(20, 0, random).toList(), shuffleOrderFrom(20, 0, random).toList())
    }
}
