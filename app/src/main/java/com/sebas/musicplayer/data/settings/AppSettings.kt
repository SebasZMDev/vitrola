package com.sebas.musicplayer.data.settings

import android.content.Context
import com.sebas.musicplayer.domain.library.decodeSort
import com.sebas.musicplayer.domain.library.encodeSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Cómo se ve la carátula en la pantalla de reproducción. */
enum class PlayerStyle { COVER, VINYL, CD, TURNTABLE, CASSETTE, RADIO }

/** Fondo de la pantalla de reproducción. OLED es negro puro y solo se aplica con el tema oscuro. */
enum class PlayerBackground { GLOW, BLUR, OLED }

/** Criterio para ordenar la lista de canciones. */
enum class SongSortField(val label: String) {
    ORIGINAL("Orden original"),
    TITLE("Título"),
    ARTIST("Artista"),
    ALBUM("Álbum"),
    DURATION("Duración"),
    RECENT("Recientes"),
}

data class SongSort(val field: SongSortField = SongSortField.TITLE, val ascending: Boolean = true)

enum class VinylColor { BLACK, RED, BLUE, COVER }

enum class LabelSize(val fraction: Float) { SMALL(0.28f), MEDIUM(0.36f), LARGE(0.46f) }

/** Opciones del vinilo (también el del tocadiscos). */
data class PlayerLook(
    val vinylColor: VinylColor = VinylColor.BLACK,
    val grooves: Boolean = true,
    val sheen: Boolean = true,
    val fast45: Boolean = false,
    val labelSize: LabelSize = LabelSize.MEDIUM,
    /** Vinilo dentro de una funda con la carátula (solo en "Vinilo clásico"). */
    val sleeve: Boolean = false,
)

/** Todo lo que decide cómo se ve la pantalla de reproducción. */
data class PlayerAppearance(
    val style: PlayerStyle = PlayerStyle.COVER,
    val background: PlayerBackground = PlayerBackground.GLOW,
    val look: PlayerLook = PlayerLook(),
)

/**
 * Ajustes del usuario. [libraryVersion] sube cada vez que cambia algo que altera qué canciones
 * se ven (carpetas, WhatsApp, duración mínima); no se guarda en disco.
 */
data class SettingsState(
    val excludedFolders: Set<String> = emptySet(),
    val excludeWhatsApp: Boolean = true,
    val minDurationSec: Int = DEFAULT_MIN_DURATION_SEC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val openPlayerOnTap: Boolean = false,
    val playerStyle: PlayerStyle = PlayerStyle.COVER,
    val playerBackground: PlayerBackground = PlayerBackground.GLOW,
    val look: PlayerLook = PlayerLook(),
    val songSort: SongSort = SongSort(),
    /** Orden elegido en cada lista (clave de la lista -> orden). */
    val listSorts: Map<String, SongSort> = emptyMap(),
    /** Carpeta (URI de árbol SAF) donde están las letras .lrc; null = sin configurar. */
    val lyricsTreeUri: String? = null,
    val libraryVersion: Int = 0,
) {
    val appearance: PlayerAppearance get() = PlayerAppearance(playerStyle, playerBackground, look)

    /** Una carpeta queda excluida si ella o alguna carpeta superior está en la lista. */
    fun isFolderExcluded(folder: String): Boolean = excludedFolders.any { folder.startsWith(it, ignoreCase = true) }

    companion object {
        const val DEFAULT_MIN_DURATION_SEC = 30
    }
}

/** Ajustes guardados en SharedPreferences y expuestos como flujo. */
class AppSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    val current: SettingsState get() = _state.value

    fun setFolderExcluded(folder: String, excluded: Boolean) = update(libraryChanged = true) {
        it.copy(excludedFolders = if (excluded) it.excludedFolders + folder else it.excludedFolders - folder)
    }

    fun setExcludeWhatsApp(value: Boolean) = update(libraryChanged = true) { it.copy(excludeWhatsApp = value) }

    fun setMinDurationSec(value: Int) = update(libraryChanged = true) { it.copy(minDurationSec = value) }

    fun setThemeMode(value: ThemeMode) = update(libraryChanged = false) { it.copy(themeMode = value) }

    fun setOpenPlayerOnTap(value: Boolean) = update(libraryChanged = false) { it.copy(openPlayerOnTap = value) }

    fun setPlayerStyle(value: PlayerStyle) = update(libraryChanged = false) { it.copy(playerStyle = value) }

    fun setPlayerBackground(value: PlayerBackground) = update(libraryChanged = false) { it.copy(playerBackground = value) }

    fun setSongSort(value: SongSort) = update(libraryChanged = false) { it.copy(songSort = value) }

    fun setListSort(key: String, sort: SongSort) = update(libraryChanged = false) {
        it.copy(listSorts = it.listSorts + (key to sort))
    }

    fun setLyricsTreeUri(value: String?) = update(libraryChanged = false) { it.copy(lyricsTreeUri = value) }

    fun setLook(transform: (PlayerLook) -> PlayerLook) = update(libraryChanged = false) { it.copy(look = transform(it.look)) }

    private fun update(libraryChanged: Boolean, transform: (SettingsState) -> SettingsState) {
        synchronized(this) {
            val old = _state.value
            var new = transform(old)
            if (new == old) return
            if (libraryChanged) new = new.copy(libraryVersion = old.libraryVersion + 1)
            write(new)
            _state.value = new
        }
    }

    private fun read() = SettingsState(
        excludedFolders = prefs.getStringSet(KEY_FOLDERS, emptySet()).orEmpty().toSet(),
        excludeWhatsApp = prefs.getBoolean(KEY_WHATSAPP, true),
        minDurationSec = prefs.getInt(KEY_MIN_DURATION, SettingsState.DEFAULT_MIN_DURATION_SEC),
        themeMode = prefs.getString(KEY_THEME, null)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
            ?: ThemeMode.SYSTEM,
        openPlayerOnTap = prefs.getBoolean(KEY_OPEN_ON_TAP, false),
        playerStyle = prefs.getString(KEY_PLAYER_STYLE, null)
            ?.let { name -> PlayerStyle.entries.firstOrNull { it.name == name } }
            ?: PlayerStyle.COVER,
        playerBackground = prefs.enumOf(KEY_PLAYER_BG, PlayerBackground.GLOW),
        songSort = SongSort(prefs.enumOf(KEY_SORT_FIELD, SongSortField.TITLE), prefs.getBoolean(KEY_SORT_ASC, true)),
        listSorts = readListSorts(),
        lyricsTreeUri = prefs.getString(KEY_LYRICS_TREE, null),
        look = PlayerLook(
            vinylColor = prefs.enumOf(KEY_VINYL_COLOR, VinylColor.BLACK),
            grooves = prefs.getBoolean(KEY_GROOVES, true),
            sheen = prefs.getBoolean(KEY_SHEEN, true),
            fast45 = prefs.getBoolean(KEY_FAST45, false),
            labelSize = prefs.enumOf(KEY_LABEL_SIZE, LabelSize.MEDIUM),
            sleeve = prefs.getBoolean(KEY_SLEEVE, false),
        ),
    )

    private fun readListSorts(): Map<String, SongSort> = prefs.all.entries.mapNotNull { (key, value) ->
        if (!key.startsWith(KEY_LIST_SORT_PREFIX)) return@mapNotNull null
        decodeSort(value as? String)?.let { key.removePrefix(KEY_LIST_SORT_PREFIX) to it }
    }.toMap()

    private inline fun <reified E : Enum<E>> android.content.SharedPreferences.enumOf(key: String, default: E): E =
        getString(key, null)?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

    private fun write(state: SettingsState) {
        prefs.edit()
            .putStringSet(KEY_FOLDERS, state.excludedFolders)
            .putBoolean(KEY_WHATSAPP, state.excludeWhatsApp)
            .putInt(KEY_MIN_DURATION, state.minDurationSec)
            .putString(KEY_THEME, state.themeMode.name)
            .putBoolean(KEY_OPEN_ON_TAP, state.openPlayerOnTap)
            .putString(KEY_PLAYER_STYLE, state.playerStyle.name)
            .putString(KEY_PLAYER_BG, state.playerBackground.name)
            .putString(KEY_VINYL_COLOR, state.look.vinylColor.name)
            .putBoolean(KEY_GROOVES, state.look.grooves)
            .putBoolean(KEY_SHEEN, state.look.sheen)
            .putBoolean(KEY_FAST45, state.look.fast45)
            .putString(KEY_LABEL_SIZE, state.look.labelSize.name)
            .putBoolean(KEY_SLEEVE, state.look.sleeve)
            .putString(KEY_SORT_FIELD, state.songSort.field.name)
            .putBoolean(KEY_SORT_ASC, state.songSort.ascending)
            .putString(KEY_LYRICS_TREE, state.lyricsTreeUri)
            .apply {
                state.listSorts.forEach { (key, sort) -> putString(KEY_LIST_SORT_PREFIX + key, encodeSort(sort)) }
            }
            .apply()
    }

    private companion object {
        const val KEY_FOLDERS = "excluded_folders"
        const val KEY_WHATSAPP = "exclude_whatsapp"
        const val KEY_MIN_DURATION = "min_duration_sec"
        const val KEY_THEME = "theme_mode"
        const val KEY_OPEN_ON_TAP = "open_player_on_tap"
        const val KEY_PLAYER_STYLE = "player_style"
        const val KEY_PLAYER_BG = "player_background"
        const val KEY_VINYL_COLOR = "vinyl_color"
        const val KEY_GROOVES = "vinyl_grooves"
        const val KEY_SHEEN = "vinyl_sheen"
        const val KEY_FAST45 = "vinyl_fast45"
        const val KEY_LABEL_SIZE = "vinyl_label_size"
        const val KEY_SLEEVE = "vinyl_sleeve"
        const val KEY_SORT_FIELD = "song_sort_field"
        const val KEY_SORT_ASC = "song_sort_asc"
        const val KEY_LYRICS_TREE = "lyrics_tree_uri"
        const val KEY_LIST_SORT_PREFIX = "list_sort_"
    }
}
