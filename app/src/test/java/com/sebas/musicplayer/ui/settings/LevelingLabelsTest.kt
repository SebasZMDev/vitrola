package com.sebas.musicplayer.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class LevelingLabelsTest {

    @Test
    fun singularAndPlural() {
        assertEquals("1 canción nivelada", leveledLabel(1))
        assertEquals("0 canciones niveladas", leveledLabel(0))
        assertEquals("2 canciones niveladas", leveledLabel(2))
        assertEquals("1 no se pudo medir", unmeasuredLabel(1))
        assertEquals("2 no se pudieron medir", unmeasuredLabel(2))
    }
}
