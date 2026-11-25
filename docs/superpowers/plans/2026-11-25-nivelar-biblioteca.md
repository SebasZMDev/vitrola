# Nivelar toda la biblioteca — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development.

**Goal:** Nivelar el volumen de todas las canciones hacia la mediana de la biblioteca.

**Architecture:** Cuentas puras en `playback/gain` con tests; Room guarda mediciones y si cada ganancia es automática; un `LibraryLeveler` en el contenedor corre el proceso y Configuración lo muestra.

**Tech Stack:** Kotlin, Compose, Room, corrutinas, JUnit.

**Spec:** `docs/superpowers/specs/2026-11-25-nivelar-biblioteca-design.md`

## Global Constraints
- Raíz: `app/src/main/java/com/sebas/musicplayer/`; tests en `app/src/test/java/com/sebas/musicplayer/`.
- Tests: `JAVA_HOME=/e/Programas/AndroidStudio/jbr ./gradlew testDebugUnitTest`. Error RMI de KSP: reintentar.
- Sin dependencias nuevas. Los archivos de audio no se modifican. UI en español.
- ±6 dB, pasos de 0,5, tope por pico: las reglas actuales de `GainMath`.
- Sin push ni merge.

## Review Focus
1. Un volumen existente o puesto a mano (incluido "Original" = 0) nunca lo pisa el nivelado, ni si se cambia mientras corre.
2. Cancelar a mitad: no cambia ninguna ganancia y las mediciones hechas se conservan.
3. Biblioteca vacía o ningún archivo medible: termina sin objetivo ni cambios.
4. Migración 6→7 sin perder datos.
5. Tocar el botón dos veces: una sola ejecución.

---

### Task 1: Datos y cálculo
**Files:** Modify `playback/gain/GainMath.kt`, `playback/gain/SongGain.kt`, `data/playlist/db/MusicDatabase.kt`, `data/settings/AppSettings.kt`, `di/AppContainer.kt`, `data/backup/BackupManager.kt` (solo si compila distinto); Create `playback/gain/LibraryLevel.kt`; test `playback/gain/LibraryLevelTest.kt`; schema `7.json`.

**Produces:**
- `GainMath.suggest(loudnessDb: Float, peak: Float, targetDb: Float = TARGET_DB): Float`
- `fun medianTarget(values: Collection<Float>): Float?` (par: promedio de los dos centrales; vacío: null)
- `data class LevelPlan(val targetDb: Float, val gains: Map<Long, Float>)`; `fun planLevel(measures: Map<Long, Loudness>, manualIds: Set<Long>): LevelPlan?` (la mediana usa todas las mediciones; `gains` excluye las manuales)
- Room v7, `MIGRATION_6_7`: `song_gain.isAuto INTEGER NOT NULL DEFAULT 0` (`@ColumnInfo(defaultValue = "0")`) y tabla `song_loudness(songId PK, loudnessDb REAL, peak REAL)`.
- `SongGainRepository`: `suspend fun measureCached(songId: Long, uri: Uri): Loudness?`; `suspend fun applyAuto(plan: LevelPlan)` (relee las manuales al aplicar; reemplaza las automáticas anteriores de las canciones del plan); `suspend fun removeAuto()`.
- `set()` guarda siempre una fila manual, también con 0. `level()` (una canción) usa `settings.current.levelTargetDb ?: GainMath.TARGET_DB` y queda manual. `importAll` queda manual.
- Todas las escrituras (set, applyAuto, removeAuto, importAll) van serializadas para que no se pisen (p. ej. un `Mutex`), y la memoria queda igual que la base.
- `SettingsState.levelTargetDb: Float? = null`, `AppSettings.setLevelTarget(value: Float?)`, persistido.

- [ ] Tests que fallan: `medianOddAndEven` ([-20,-10,-12] → -12; [-20,-10] → -15); `medianEmptyIsNull`; `suggestUsesTarget` (loudness -18, peak 0.1, target -15 → 3.0); `planSkipsManualButCountsThemInMedian`; `planEmptyIsNull`; `planGainsStayWithinRange` (todas en -6..6).
- [ ] Verlos fallar, implementar, tests y `assembleDebug` en verde.
- [ ] Commit `Datos y cálculo para nivelar la biblioteca`.

### Task 2: Proceso, Configuración y APK
**Files:** Create `playback/gain/LibraryLeveler.kt`, test `playback/gain/LibraryLevelerTest.kt`; Modify `di/AppContainer.kt`, `ui/settings/SettingsViewModel.kt`, `ui/settings/SettingsScreen.kt`.

**Consumes:** Task 1.
**Produces:** `sealed interface LevelingState { Idle; Running(done: Int, total: Int); Done(leveled: Int, failed: Int); Failed }` y `class LibraryLeveler(scope, songs: suspend () -> List<Song>, measure: suspend (Song) -> Loudness?, manualIds: suspend () -> Set<Long>, apply: suspend (LevelPlan) -> Unit)` con `val state: StateFlow<LevelingState>`, `fun start()`, `fun cancel()`. Mide de a una canción; `apply` solo al terminar; una excepción en una canción cuenta como `failed`. Una sola instancia en el contenedor (sobrevive a la pantalla). `apply` del contenedor = `applyAuto` + `setLevelTarget(plan.targetDb)`.

UI, sección "Volumen" (después de "Reproducción"): botón "Nivelar toda la biblioteca"; corriendo: barra de progreso, "120 de 480" y "Cancelar"; al terminar "N canciones niveladas" y, si hay, "M no se pudieron medir"; error: "No se pudo nivelar". Texto de apoyo: "Ajusta cada canción a un volumen parecido. No modifica los archivos. Puede tardar." Botón "Quitar nivelado" (cancela, `removeAuto`, `setLevelTarget(null)`), visible si `levelTargetDb != null`.

- [ ] Tests que fallan (con fakes y `kotlinx-coroutines-test` solo si ya está; si no, `runBlocking` y esperar el estado): `appliesOnlyAfterMeasuringAll`; `cancelAppliesNothing`; `unreadableSongCountsAsFailed`; `emptyLibraryDoesNotApply`; `secondStartIgnoredWhileRunning`.
- [ ] Implementar; tests en verde; `assembleRelease`; firmar con `zipalign` + `apksigner` y `~/.android/debug.keystore` (contraseña de debug) como `app/build/outputs/apk/release/Vitrola.apk`; `apksigner verify`.
- [ ] Commit `Nivelar toda la biblioteca desde Configuración`.
