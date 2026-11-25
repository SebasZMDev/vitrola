package com.sebas.musicplayer.playback.gain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DecodeGuardTest {

    private var now = 1_000L
    private fun guard(durationMs: Long = 0) = DecodeGuard(durationMs) { now }

    @Test
    fun givesUpAfterConsecutiveEmptyOutputsOnceInputIsDone() {
        val guard = guard()
        repeat(DecodeGuard.MAX_EMPTY_OUTPUTS - 1) { guard.emptyOutput(inputDone = true) }
        assertFalse(guard.timedOut())
        guard.progress()
        repeat(DecodeGuard.MAX_EMPTY_OUTPUTS - 1) { guard.emptyOutput(inputDone = true) }
        assertFalse(guard.timedOut())
        guard.emptyOutput(inputDone = true)
        assertTrue(guard.timedOut())
    }

    @Test
    fun givesUpAfterALongStallBeforeInputIsDone() {
        val guard = guard()
        repeat(1000) { guard.emptyOutput(inputDone = false) }
        now += DecodeGuard.STALL_MS - 1
        guard.emptyOutput(inputDone = false)
        assertFalse(guard.timedOut())
        now += 1
        guard.emptyOutput(inputDone = false)
        assertTrue(guard.timedOut())
    }

    @Test
    fun overallLimitHasAFloor() {
        val guard = guard(durationMs = 1_000)
        now += DecodeGuard.MIN_LIMIT_MS - 1
        guard.progress()
        assertFalse(guard.timedOut())
        now += 1
        assertTrue(guard.timedOut())
    }

    @Test
    fun overallLimitScalesWithDuration() {
        val hour = 3_600_000L
        val guard = guard(durationMs = hour)
        now += 2 * hour - 1
        assertFalse(guard.timedOut())
        now += 1
        assertTrue(guard.timedOut())
    }
}
