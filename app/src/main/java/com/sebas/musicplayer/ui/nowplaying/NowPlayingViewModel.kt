package com.sebas.musicplayer.ui.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.data.lyrics.LyricsRepository
import com.sebas.musicplayer.data.settings.AppSettings
import com.sebas.musicplayer.data.settings.PlayerAppearance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import com.sebas.musicplayer.playback.PlayerController
import com.sebas.musicplayer.playback.PlayerState
import com.sebas.musicplayer.data.stats.SongStatsRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class NowPlayingViewModel(
    private val playerController: PlayerController,
    settings: AppSettings,
    private val stats: SongStatsRepository,
    lyricsRepository: LyricsRepository,
    private val gains: com.sebas.musicplayer.playback.gain.SongGainRepository,
) : ViewModel() {

    /** Volumen propio de la canción actual (dB). */
    val songGain: StateFlow<Float> = combine(
        playerController.state.map { it.currentSong?.id }.distinctUntilChanged(),
        gains.gains,
    ) { id, map -> if (id == null) 0f else map[id] ?: 0f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    private val _leveling = kotlinx.coroutines.flow.MutableStateFlow<LevelingUi>(LevelingUi.Idle)
    val leveling: StateFlow<LevelingUi> = _leveling
    private var levelJob: kotlinx.coroutines.Job? = null

    init {
        // Al cambiar de canción el mensaje de "nivelar" ya no aplica.
        viewModelScope.launch {
            playerController.state.map { it.currentSong?.id }.distinctUntilChanged().collect {
                levelJob?.cancel()
                _leveling.value = LevelingUi.Idle
            }
        }
    }

    fun setSongGain(db: Float) {
        val id = playerController.state.value.currentSong?.id ?: return
        gains.set(id, db)
    }

    fun levelSong() {
        val song = playerController.state.value.currentSong ?: return
        levelJob?.cancel()
        _leveling.value = LevelingUi.Working
        levelJob = viewModelScope.launch {
            val db = gains.level(song.id, song.uri)
            _leveling.value = if (db == null) LevelingUi.Failed else LevelingUi.Done(db)
        }
    }

    val playerState: StateFlow<PlayerState> = playerController.state

    /**
     * Letra de la canción actual. Solo se calcula mientras la hoja de letras está abierta
     * (WhileSubscribed): con la hoja cerrada no se escanea ninguna carpeta.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val lyrics: StateFlow<LyricsUi> = combine(
        playerController.state.map { it.currentSong }.distinctUntilChanged { a, b -> a?.id == b?.id },
        settings.state.map { it.lyricsTreeUri }.distinctUntilChanged(),
    ) { song, tree -> song to tree }
        .flatMapLatest { (song, tree) ->
            flow {
                when {
                    tree == null -> emit(LyricsUi.NoFolder)
                    song == null -> emit(LyricsUi.NotFound)
                    else -> {
                        emit(LyricsUi.Loading)
                        val found = lyricsRepository.find(song)
                        emit(if (found == null) LyricsUi.NotFound else LyricsUi.Found(found))
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LyricsUi.Loading)

    /** Velocidades que recorre el botón: 0,75x -> 1x -> 1,25x -> 1,5x -> 2x -> 0,75x. */
    fun cycleSpeed() {
        val steps = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
        val now = playerController.state.value.speed
        val i = steps.indexOfFirst { kotlin.math.abs(it - now) < 0.01f }
        playerController.setSpeed(steps[(i + 1) % steps.size])
    }

    /** Si la canción que suena está en favoritas. */
    val isFavorite: StateFlow<Boolean> = combine(
        playerController.state.map { it.currentSong?.id }.distinctUntilChanged(),
        stats.favoriteIds,
    ) { id, favorites -> id != null && id in favorites }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun toggleFavorite() {
        val id = playerController.state.value.currentSong?.id ?: return
        val make = !isFavorite.value
        viewModelScope.launch { stats.setFavorite(id, make) }
    }

    /** Estilo, fondo y opciones del vinilo elegidos en Configuración. */
    val appearance: StateFlow<PlayerAppearance> = settings.state
        .map { it.appearance }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, settings.current.appearance)

    fun togglePlayPause() = playerController.togglePlayPause()
    fun next() = playerController.next()
    fun previous() = playerController.previous()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun toggleShuffle() = playerController.toggleShuffle()
    fun cycleRepeatMode() = playerController.cycleRepeatMode()
    fun skipToQueueItem(index: Int) = playerController.skipToQueueItem(index)
    fun removeFromQueue(index: Int) = playerController.removeFromQueue(index)
    fun clearQueue() = playerController.clearQueue()
    fun moveQueueItem(from: Int, to: Int) = playerController.moveQueueItem(from, to)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MusicApp
                NowPlayingViewModel(
                    app.container.playerController,
                    app.container.settings,
                    app.container.statsRepository,
                    app.container.lyricsRepository,
                    app.container.songGains,
                )
            }
        }
    }
}
