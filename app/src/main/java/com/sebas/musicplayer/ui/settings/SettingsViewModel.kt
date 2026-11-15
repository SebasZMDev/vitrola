package com.sebas.musicplayer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.data.library.FolderRepository
import com.sebas.musicplayer.data.library.MusicFolder
import com.sebas.musicplayer.data.settings.AppSettings
import com.sebas.musicplayer.data.settings.SettingsState
import com.sebas.musicplayer.data.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: AppSettings,
    private val folderRepository: FolderRepository,
    val versionName: String,
) : ViewModel() {

    val state: StateFlow<SettingsState> = settings.state

    /** null mientras carga. */
    private val _folders = MutableStateFlow<List<MusicFolder>?>(null)
    val folders: StateFlow<List<MusicFolder>?> = _folders

    init {
        // La lista de carpetas depende de la duración mínima (carpetas solo con sonidos cortos no cuentan).
        viewModelScope.launch {
            settings.state.map { it.minDurationSec }.distinctUntilChanged().collect { minSec ->
                _folders.value = try {
                    folderRepository.getFolders(minSec)
                } catch (e: SecurityException) {
                    emptyList()
                }
            }
        }
    }

    fun setFolderExcluded(folder: String, excluded: Boolean) = settings.setFolderExcluded(folder, excluded)
    fun setExcludeWhatsApp(value: Boolean) = settings.setExcludeWhatsApp(value)
    fun setMinDurationSec(value: Int) = settings.setMinDurationSec(value)
    fun setThemeMode(value: ThemeMode) = settings.setThemeMode(value)
    fun setOpenPlayerOnTap(value: Boolean) = settings.setOpenPlayerOnTap(value)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MusicApp
                val version = runCatching {
                    app.packageManager.getPackageInfo(app.packageName, 0).versionName
                }.getOrNull() ?: "?"
                SettingsViewModel(app.container.settings, app.container.folderRepository, version)
            }
        }
    }
}
