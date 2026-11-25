package com.sebas.musicplayer.playback

import kotlin.random.Random

/** Orden aleatorio nuevo de [count] canciones que empieza en [current] (la que está sonando). */
fun shuffleOrderFrom(count: Int, current: Int, random: Random = Random.Default): IntArray =
    (listOf(current) + (0 until count).filter { it != current }.shuffled(random)).toIntArray()
