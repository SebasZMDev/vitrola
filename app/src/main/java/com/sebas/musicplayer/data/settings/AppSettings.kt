package com.sebas.musicplayer.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

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
    val libraryVersion: Int = 0,
) {
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
    )

    private fun write(state: SettingsState) {
        prefs.edit()
            .putStringSet(KEY_FOLDERS, state.excludedFolders)
            .putBoolean(KEY_WHATSAPP, state.excludeWhatsApp)
            .putInt(KEY_MIN_DURATION, state.minDurationSec)
            .putString(KEY_THEME, state.themeMode.name)
            .putBoolean(KEY_OPEN_ON_TAP, state.openPlayerOnTap)
            .apply()
    }

    private companion object {
        const val KEY_FOLDERS = "excluded_folders"
        const val KEY_WHATSAPP = "exclude_whatsapp"
        const val KEY_MIN_DURATION = "min_duration_sec"
        const val KEY_THEME = "theme_mode"
        const val KEY_OPEN_ON_TAP = "open_player_on_tap"
    }
}
