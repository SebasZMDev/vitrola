package com.sebas.musicplayer.playback.gain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryLevelTest {

    @Test
    fun medianOddAndEven() {
        assertEquals(-12f, medianTarget(listOf(-20f, -10f, -12f))!!, 1e-6f)
        assertEquals(-15f, medianTarget(listOf(-20f, -10f))!!, 1e-6f)
    }

    @Test
    fun medianEmptyIsNull() {
        assertNull(medianTarget(emptyList()))
    }

    @Test
    fun suggestUsesTarget() {
        assertEquals(3f, GainMath.suggest(loudnessDb = -18f, peak = 0.1f, targetDb = -15f), 1e-6f)
    }

    @Test
    fun planSkipsManualButCountsThemInMedian() {
        val measures = mapOf(
            1L to Loudness(-20f, 0.1f),
            2L to Loudness(-12f, 0.1f),
            3L to Loudness(-10f, 0.1f),
        )
        val plan = planLevel(measures, manualIds = setOf(2L))!!
        assertEquals(-12f, plan.targetDb, 1e-6f)
        assertEquals(mapOf(1L to 6f, 3L to -2f), plan.gains)
    }

    @Test
    fun planEmptyIsNull() {
        assertNull(planLevel(emptyMap(), emptySet()))
    }

    @Test
    fun planGainsStayWithinRange() {
        val measures = listOf(-60f, -40f, -14f, -3f, 0f)
            .mapIndexed { i, db -> i.toLong() to Loudness(db, 0.01f) }
            .toMap()
        val gains = planLevel(measures, emptySet())!!.gains
        assertEquals(measures.keys, gains.keys)
        assertTrue(gains.values.all { it in GainMath.MIN_DB..GainMath.MAX_DB })
    }
}
