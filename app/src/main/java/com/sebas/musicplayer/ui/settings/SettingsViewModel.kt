package com.sebas.musicplayer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.content.ContentResolver
import android.net.Uri
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.data.backup.BackupJson
import com.sebas.musicplayer.data.backup.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import com.sebas.musicplayer.data.library.FolderRepository
import com.sebas.musicplayer.data.library.MusicFolder
import com.sebas.musicplayer.data.settings.AppSettings
import com.sebas.musicplayer.data.settings.PlayerBackground
import com.sebas.musicplayer.data.settings.PlayerLook
import com.sebas.musicplayer.data.settings.PlayerStyle
import com.sebas.musicplayer.data.settings.SettingsState
import com.sebas.musicplayer.data.settings.ThemeMode
import com.sebas.musicplayer.playback.gain.LevelingState
import com.sebas.musicplayer.playback.gain.LibraryLeveler
import com.sebas.musicplayer.playback.gain.SongGainRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: AppSettings,
    private val folderRepository: FolderRepository,
    val versionName: String,
    private val backupManager: BackupManager,
    private val resolver: ContentResolver,
    private val songEdits: MutableSharedFlow<Unit>,
    private val leveler: LibraryLeveler,
    private val songGains: SongGainRepository,
) : ViewModel() {

    val state: StateFlow<SettingsState> = settings.state

    val leveling: StateFlow<LevelingState> = leveler.state

    fun startLeveling() {
        leveler.start()
    }

    fun cancelLeveling() {
        leveler.cancel()
    }

    /** Corre en el nivelador, no en [viewModelScope]: salir de la pantalla no lo deja a medias. */
    fun removeLeveling() {
        leveler.remove {
            songGains.removeAuto()
            settings.setLevelTarget(null)
        }
    }

    /** Guarda el respaldo en el archivo [uri] elegido por el usuario; [onDone] recibe el mensaje a mostrar. */
    fun exportBackup(uri: Uri, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val message = try {
                val json = BackupJson.encode(backupManager.export())
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                        ?: error("No se pudo abrir el archivo")
                }
                "Respaldo guardado"
            } catch (e: Exception) {
                "No se pudo guardar el respaldo: ${e.message}"
            }
            onDone(message)
        }
    }

    /** Lee el respaldo del archivo [uri] y lo suma a lo que ya hay; no borra nada. */
    fun importBackup(uri: Uri, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val message = try {
                val text = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: error("No se pudo abrir el archivo")
                }
                val report = backupManager.import(BackupJson.decode(text))
                songEdits.emit(Unit)
                report.summary()
            } catch (e: IllegalArgumentException) {
                e.message ?: "El archivo no es un respaldo válido"
            } catch (e: Exception) {
                "No se pudo importar el respaldo: ${e.message}"
            }
            onDone(message)
        }
    }

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
    fun setPlayerStyle(value: PlayerStyle) = settings.setPlayerStyle(value)
    fun setPlayerBackground(value: PlayerBackground) = settings.setPlayerBackground(value)
    fun updateLook(transform: (PlayerLook) -> PlayerLook) = settings.setLook(transform)

    /** Guarda la carpeta de letras y pide a Android que recuerde el permiso (sobrevive a reinicios). */
    fun setLyricsFolder(uri: Uri?) {
        settings.current.lyricsTreeUri?.let { old ->
            runCatching { resolver.releasePersistableUriPermission(Uri.parse(old), android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        if (uri != null) {
            runCatching { resolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        settings.setLyricsTreeUri(uri?.toString())
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MusicApp
                val version = runCatching {
                    app.packageManager.getPackageInfo(app.packageName, 0).versionName
                }.getOrNull() ?: "?"
                SettingsViewModel(
                    app.container.settings,
                    app.container.folderRepository,
                    version,
                    app.container.backupManager,
                    app.contentResolver,
                    app.container.songEdits,
                    app.container.libraryLeveler,
                    app.container.songGains,
                )
            }
        }
    }
}
