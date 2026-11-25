package com.sebas.musicplayer.playback.gain

/** Mediana de los volúmenes medidos (con cantidad par, el promedio de los dos centrales); null si no hay. */
fun medianTarget(values: Collection<Float>): Float? {
    if (values.isEmpty()) return null
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2f
}

/** Resultado de nivelar la biblioteca: objetivo común y ganancia de cada canción sin ajuste manual. */
data class LevelPlan(val targetDb: Float, val gains: Map<Long, Float>)

/** La mediana usa todas las mediciones; las canciones de [manualIds] no reciben ganancia. null sin mediciones. */
fun planLevel(measures: Map<Long, Loudness>, manualIds: Set<Long>): LevelPlan? {
    val target = medianTarget(measures.values.map { it.loudnessDb }) ?: return null
    val gains = measures.filterKeys { it !in manualIds }
        .mapValues { (_, m) -> GainMath.suggest(m.loudnessDb, m.peak, target) }
    return LevelPlan(target, gains)
}
