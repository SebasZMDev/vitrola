package com.sebas.musicplayer.playback

import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

/**
 * "Siguiente" está disponible siempre que haya cola: en la última canción vuelve a la primera.
 * Sin esto, en reproducción normal o repetir una el botón no hacía nada y la notificación lo ocultaba.
 * La reproducción automática no cambia: al terminar la cola en modo normal, la música se detiene.
 */
@OptIn(UnstableApi::class)
class WrapAroundPlayer(player: Player) : ForwardingPlayer(player) {

    /** Por cada oyente, otro que corrige los comandos disponibles que informa el reproductor real. */
    private val fixers = mutableMapOf<Player.Listener, Player.Listener>()

    override fun addListener(listener: Player.Listener) {
        super.addListener(listener)
        // Se registra después: su aviso llega último y es el que queda.
        val fixer = object : Player.Listener {
            override fun onAvailableCommandsChanged(availableCommands: Player.Commands) {
                listener.onAvailableCommandsChanged(getAvailableCommands())
            }
        }
        fixers.put(listener, fixer)?.let { super.removeListener(it) }
        super.addListener(fixer)
    }

    override fun removeListener(listener: Player.Listener) {
        super.removeListener(listener)
        fixers.remove(listener)?.let { super.removeListener(it) }
    }

    override fun getAvailableCommands(): Player.Commands {
        val commands = super.getAvailableCommands()
        if (mediaItemCount == 0) return commands
        return commands.buildUpon()
            .addAll(Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .build()
    }

    override fun isCommandAvailable(command: Int): Boolean = availableCommands.contains(command)

    override fun seekToNext() {
        if (hasNextMediaItem() || mediaItemCount == 0) super.seekToNext() else seekToStart()
    }

    override fun seekToNextMediaItem() {
        if (hasNextMediaItem() || mediaItemCount == 0) super.seekToNextMediaItem() else seekToStart()
    }

    private fun seekToStart() = seekToDefaultPosition(currentTimeline.getFirstWindowIndex(shuffleModeEnabled))
}
