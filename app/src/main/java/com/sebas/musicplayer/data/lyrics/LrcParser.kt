package com.sebas.musicplayer.data.lyrics

/** Una línea de letra. [timeMs] es -1 en las letras sin tiempos. */
data class LyricLine(val timeMs: Long, val text: String)

/** Letra de una canción: sincronizada (con tiempos) o texto plano. */
data class Lyrics(val lines: List<LyricLine>, val synced: Boolean) {
    /** Índice de la línea que toca en [positionMs] (la última cuyo tiempo ya pasó); -1 antes de la primera. */
    fun indexAt(positionMs: Long): Int {
        if (!synced) return -1
        var lo = 0
        var hi = lines.lastIndex
        var found = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lines[mid].timeMs <= positionMs) {
                found = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        return found
    }
}

/**
 * Lee archivos .lrc: `[mm:ss.xx] texto`, varias marcas por línea (`[00:10.00][00:50.00] estribillo`),
 * `[offset:±ms]`, y quita las marcas por palabra `<mm:ss.xx>`. Las etiquetas `[ar:...]`, `[ti:...]` se ignoran.
 * Si el archivo no tiene tiempos, devuelve el texto plano (sin sincronía).
 */
object LrcParser {
    private val TIME = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val OFFSET = Regex("""^\[offset:\s*([+-]?\d+)\s*]""", RegexOption.IGNORE_CASE)
    private val WORD_TIME = Regex("""<\d{1,3}:\d{1,2}(?:[.:]\d{1,3})?>""")

    fun parse(raw: String): Lyrics? {
        var offset = 0L
        val synced = ArrayList<LyricLine>()
        val plain = ArrayList<LyricLine>()
        for (rawLine in raw.removePrefix("﻿").lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val offsetMatch = OFFSET.find(line)
            if (offsetMatch != null) {
                offset = offsetMatch.groupValues[1].toLong()
                continue
            }
            val times = ArrayList<Long>()
            var pos = 0
            while (true) {
                val m = TIME.find(line, pos) ?: break
                if (m.range.first != pos) break
                times += toMillis(m)
                pos = m.range.last + 1
            }
            if (times.isEmpty()) {
                // Etiquetas de metadatos como [ar:Artista] no son letra.
                if (!line.startsWith("[")) plain += LyricLine(-1, line)
                continue
            }
            val text = line.substring(pos).replace(WORD_TIME, "").trim()
            times.forEach { synced += LyricLine((it - offset).coerceAtLeast(0L), text) }
        }
        return when {
            synced.isNotEmpty() -> Lyrics(synced.sortedBy { it.timeMs }, synced = true)
            plain.isNotEmpty() -> Lyrics(plain, synced = false)
            else -> null
        }
    }

    private fun toMillis(m: MatchResult): Long {
        val minutes = m.groupValues[1].toLong()
        val seconds = m.groupValues[2].toLong()
        val fraction = m.groupValues[3]
        val ms = when (fraction.length) {
            0 -> 0L
            1 -> fraction.toLong() * 100
            2 -> fraction.toLong() * 10
            else -> fraction.take(3).toLong()
        }
        return (minutes * 60 + seconds) * 1000 + ms
    }
}
