package com.sebas.musicplayer.playback.gain

/**
 * Decide cuándo dejar de esperar a un decodificador atascado. Se rinde si:
 * - ya se entregó toda la entrada y llegan [MAX_EMPTY_OUTPUTS] salidas vacías seguidas;
 * - antes de eso, pasan [STALL_MS] sin ningún avance;
 * - se supera el tiempo total: el doble de la duración, con un mínimo de [MIN_LIMIT_MS].
 */
class DecodeGuard(durationMs: Long, private val nowMs: () -> Long) {
    private val start = nowMs()
    private val limitMs = maxOf(MIN_LIMIT_MS, durationMs * 2)
    private var lastProgress = start
    private var emptyAfterInput = 0
    private var stalled = false

    /** Se entregó entrada o salió audio. */
    fun progress() {
        lastProgress = nowMs()
        emptyAfterInput = 0
    }

    /** Se pidió salida y no llegó nada útil. */
    fun emptyOutput(inputDone: Boolean) {
        if (inputDone) {
            if (++emptyAfterInput >= MAX_EMPTY_OUTPUTS) stalled = true
        } else if (nowMs() - lastProgress >= STALL_MS) {
            stalled = true
        }
    }

    fun timedOut(): Boolean = stalled || nowMs() - start >= limitMs

    companion object {
        const val MAX_EMPTY_OUTPUTS = 200
        const val STALL_MS = 30_000L
        const val MIN_LIMIT_MS = 180_000L
    }
}
