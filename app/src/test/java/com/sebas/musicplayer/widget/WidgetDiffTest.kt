package com.sebas.musicplayer.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetDiffTest {
    private val base = WidgetState(1, "T", "A", null, true, 1_000, 200_000, 1, 10, "N", false)

    @Test fun `solo avanza la posicion`() =
        assertTrue(onlyPositionChanged(base, base.copy(positionMs = 16_000)))

    @Test fun `cambiar de cancion o pausar redibuja todo`() {
        assertFalse(onlyPositionChanged(base, base.copy(songId = 2, positionMs = 0)))
        assertFalse(onlyPositionChanged(base, base.copy(isPlaying = false, positionMs = 16_000)))
    }

    @Test fun `sin estado anterior o actual redibuja todo`() {
        assertFalse(onlyPositionChanged(null, base))
        assertFalse(onlyPositionChanged(base, null))
    }
}
