# Listas unificadas — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Que las 6 listas de canciones tengan la misma selección múltiple y el mismo ordenamiento.

**Architecture:** Lógica pura (orden, edición en bloque) en `domain/library` con tests; piezas Compose compartidas en `ui/components` que cada pantalla coloca en su propio diseño. Cada pantalla solo aporta su lista visible y su clave de orden.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), SharedPreferences, Room, JUnit.

**Spec:** `docs/superpowers/specs/2026-11-25-listas-unificadas-design.md`

## Global Constraints
- Rama base `generos-explorar-menu`. Sin push ni merge.
- Raíz del código: `app/src/main/java/com/sebas/musicplayer/` (rutas relativas a ella). Tests en `app/src/test/java/com/sebas/musicplayer/`.
- Tests: `JAVA_HOME=/e/Programas/AndroidStudio/jbr ./gradlew testDebugUnitTest` (en Bash). Un error RMI de KSP es transitorio: reintentar.
- UI en español, con los textos exactos de este plan. Sin dependencias nuevas.
- Metadatos solo en Room, nunca en el archivo.
- Ordenar nunca reescribe la playlist.
- Para archivos con contenido largo usar la herramienta Write, no heredocs.

## Review Focus
1. Playlist con la misma canción dos veces: marcar una no marca la otra; quitar varias posiciones quita exactamente esas.
2. Lista ordenada: tocar una fila reproduce esa canción y la cola sigue el orden visible.
3. Canción que desaparece mientras está marcada (eliminada): el contador no la cuenta.
4. Preferencia guardada ilegible o de un criterio inexistente: la lista abre en su orden por defecto.
5. Editar el álbum desde la vista de ese álbum: la pantalla puede quedar sin álbum; no debe cerrarse la app (muestra "Álbum no encontrado").

---

### Task 1: Orden compartido, "Recientes" y preferencia por lista

**Files:**
- Create: `domain/library/SongSorting.kt`; test `domain/library/SongSortingTest.kt`
- Modify: `domain/model/Song.kt`, `data/library/MediaStoreSongRepository.kt`, `data/settings/AppSettings.kt`, `ui/library/LibraryViewModel.kt` (quitar `sorted`), `ui/library/SongsTab.kt`
- Create: `ui/components/SortButton.kt`

**Interfaces — Produces:**
- `Song.dateAdded: Long = 0` (último parámetro; segundos de `MediaStore.Audio.Media.DATE_ADDED`).
- `enum SongSortField`: `ORIGINAL("Orden original")` primero, luego los actuales, y `RECENT("Recientes")` al final.
- `fun <T> sortSongs(items: List<T>, sort: SongSort, song: (T) -> Song): List<T>`
- `fun nextSort(current: SongSort, field: SongSortField): SongSort`
- `val ListDefaultSort = SongSort(SongSortField.ORIGINAL, true)`
- `fun encodeSort(sort: SongSort): String` / `fun decodeSort(value: String?): SongSort?` (formato `"RECENT:false"`)
- `SettingsState.listSorts: Map<String, SongSort>`; `AppSettings.setListSort(key: String, sort: SongSort)`; en prefs, una entrada `list_sort_<key>` por lista.
- `@Composable fun SortButton(sort: SongSort, onChange: (SongSort) -> Unit, fields: List<SongSortField> = SongSortField.entries)`
- `@Composable fun rememberListSort(key: String): Pair<SongSort, (SongSort) -> Unit>` (lee `(context.applicationContext as MusicApp).container.settings`; sin dato = `ListDefaultSort`). Vive en `SortButton.kt`.

- [ ] **Step 1: Tests que fallan** (`Uri` se obtiene como en `LibraryGroupingTest`)
  - `originalKeepsOrder`: ids [3,1,2] con `ListDefaultSort` → [3,1,2]; con `ascending=false` → [2,1,3].
  - `recentDescendingPutsNewestFirst`: dateAdded 10, 30, 20 → 30, 20, 10.
  - `titleIgnoresCase`: "b", "A" → "A", "b".
  - `tiesInArtistBreakByTitle`.
  - `worksWithWrappedItemsAndDuplicates`: lista de pares (posición, song) con la misma canción dos veces conserva ambas posiciones.
  - `nextSortInvertsActive`; `nextSortRecentStartsDescending` (`ascending == false`); `nextSortOtherStartsAscending`.
  - `decodeRoundTrips`; `decodeRejectsGarbage`: `null`, `""`, `"FOO:true"`, `"TITLE"` → `null`.
- [ ] **Step 2:** Correr tests; deben fallar por símbolos inexistentes.
- [ ] **Step 3:** Implementar. Comparadores iguales a los de `sorted` actual; `RECENT` por `dateAdded` desempatando por título. Añadir `DATE_ADDED` a la proyección y al `Song`. En Canciones, `fields = SongSortField.entries - SongSortField.ORIGINAL`, y si la preferencia guardada fuera `ORIGINAL` usar `SongSort()`. `SortButton` sale de `SongsTab.kt` y usa `nextSort`.
- [ ] **Step 4:** Tests en verde.
- [ ] **Step 5:** Commit `Orden compartido con "Recientes" y preferencia por lista`.

### Task 2: Selección compartida y Canciones sobre ella

**Files:**
- Create: `domain/library/BulkEdits.kt`; test `domain/library/BulkEditsTest.kt`
- Create: `ui/components/SongSelection.kt`, `ui/components/SelectionViewModel.kt`
- Modify: `ui/library/SongsTab.kt`, `ui/library/LibraryViewModel.kt` (eliminar toda la lógica de selección y edición en bloque)

**Interfaces — Produces:**
- `fun commonMetadata(songs: List<Song>): Triple<String, String, String>`, `fun bulkEdits(songs: List<Song>, artist: String, album: String, genre: String): List<SongEdit>`, `fun splitTitleEdits(songs: List<Song>): List<SongEdit>` — mismo comportamiento que hoy en `LibraryViewModel`; las tres deduplican por `song.id`.
- `data class SelectableSong(val key: Long, val song: Song)` (key = id; en playlist, la posición).
- `class SongSelection`: `val keys: Set<Long>`, `val active: Boolean`, `fun toggle(key: Long)`, `fun selectAll(all: Collection<Long>)`, `fun retain(existing: Collection<Long>)`, `fun clear()`. `toggle` sobre selección vacía la inicia.
- `@Composable fun rememberSongSelection(): SongSelection` (sobrevive rotación con `rememberSaveable`).
- `@Composable fun SelectionBar(selection: SongSelection, items: List<SelectableSong>, modifier: Modifier = Modifier, onRemoveFromPlaylist: ((List<SelectableSong>) -> Unit)? = null)` — incluye barra, menú, diálogos (`AddToPlaylistDialog`, `EditMetadataDialog`), `BackHandler`, y llama `retain` cuando cambia `items`. Las acciones usan las marcadas en orden visible. Se llama solo cuando `selection.active`.
- `@Composable fun SelectableSongRow(selection: SongSelection, item: SelectableSong, isCurrent: Boolean, onPlay: () -> Unit, showArtwork: Boolean = true, trackNumber: Int? = null, menuExtra: (@Composable (dismiss: () -> Unit) -> Unit)? = null)` — pulsación larga marca; en selección tocar marca y se oculta el ⋮.
- `SelectionViewModel` (con `Factory`): reproducir a continuación (en orden inverso, como hoy), a la cola, aplicar/separar/restaurar; tras editar emite `songEdits` y llama `playerController.updateSongInfo` si la canción que suena fue editada.

Textos de la barra: los actuales de Canciones; el extra es "Quitar de la playlist". "Eliminar del teléfono" usa `LocalDeleteSongs` con `distinctBy { id }` y no limpia la selección (si el usuario cancela, sigue marcada). Las demás acciones la limpian.

- [ ] **Step 1: Tests que fallan:** `commonMetadataBlankWhenDiffers`; `bulkEditsEmptyWhenAllBlank`; `bulkEditsOnlyFilledFields` (artist `" X "` → `"X"`, album y genre null); `bulkEditsDedupes`; `splitTitleSkipsWithoutSeparator` ("A - B" → artist A, title B; "Solo" no produce edición).
- [ ] **Step 2:** Correr; fallan.
- [ ] **Step 3:** Implementar lógica pura, luego los composables, y migrar `SongsTab`.
- [ ] **Step 4:** Tests en verde y `./gradlew assembleDebug` compila.
- [ ] **Step 5:** Commit `Selección múltiple compartida`.

### Task 3: Playlist

**Files:** Modify `ui/playlists/PlaylistDetailRoute.kt`, `ui/playlists/PlaylistDetailViewModel.kt`; create test `ui/playlists/RemovalOrderTest.kt`

**Interfaces:** Consumes Task 1 y 2. Produces `fun removalOrder(positions: Collection<Int>): List<Int>` (únicas, de mayor a menor) y `PlaylistDetailViewModel.removeAll(positions: Collection<Int>)`, que llama `repository.removeAt` en ese orden dentro de una sola corrutina (cada borrado desplaza las posiciones siguientes).

- [ ] **Step 1:** Test `removalGoesFromLast`: `removalOrder(listOf(1, 4, 1, 2)) == listOf(4, 2, 1)`. Correr; falla.
- [ ] **Step 2:** Implementar. Clave de orden `"playlist:$playlistId"`. `visible = sortSongs(state.entries, sort) { it.song }`; clave de selección = `position.toLong()`. Reproducir, aleatorio y tocar fila usan `visible`. `SortButton` en la cabecera, bajo los botones, alineado a la derecha (oculto si está vacía). `SelectionBar` reemplaza el `topBar` con `Modifier.statusBarsPadding()`. "Quitar de la playlist" → `removeAll` + `clear`; además `clear` si cambia el número de canciones en `state.entries` (las posiciones se desplazan). Se conserva "Quitar de la playlist" en el ⋮. Portada y su selector siguen usando el orden original.
- [ ] **Step 3:** Tests en verde y compila.
- [ ] **Step 4:** Commit `Playlist: selección y orden`.

### Task 4: Búsqueda y listas automáticas

**Files:** Modify `ui/search/SearchScreen.kt`, `ui/search/SearchViewModel.kt`, `ui/smartlists/SmartListScreen.kt`

**Interfaces:** Consumes Task 1 y 2. `SearchViewModel.onSongClick(songs: List<Song>, index: Int)`.

- [ ] **Step 1:** Búsqueda: clave `"search"`; ordenar los resultados ya recortados a 50; `SortButton` a la derecha del encabezado "Canciones"; `SelectionBar` en lugar de la barra de búsqueda (`statusBarsPadding`); al iniciar selección ocultar teclado; `clear` cuando cambia el texto (no al recomponer ni rotar); en selección, tocar álbum o artista no navega.
- [ ] **Step 2:** Listas automáticas: clave `"smart:${kind.route}"`; `SortButton` bajo `PlayShuffleButtons`; `SelectionBar` como `topBar`; reproducir y aleatorio usan el orden visible.
- [ ] **Step 3:** Tests en verde y compila.
- [ ] **Step 4:** Commit `Búsqueda y listas automáticas: selección y orden`.

### Task 5: Álbum, género y artista; APK

**Files:** Modify `ui/browse/AlbumDetailScreen.kt`, `ui/browse/ArtistDetailScreen.kt`

**Interfaces:** Consumes Task 1 y 2.

- [ ] **Step 1:** Álbum/género: clave `"album:${album.id}"`; ordenar pares (número = índice original + 1, canción) para que el número acompañe a la canción; `SortButton` bajo los botones; `SelectionBar` como `topBar`; reproducir/aleatorio/tocar usan el orden visible.
- [ ] **Step 2:** Artista: clave `"artist:${artist.name}"`; `SortButton` junto al título "Canciones"; igual selección y reproducción. La fila de álbumes no cambia.
- [ ] **Step 3:** Con `Grep`, confirmar que `SongRow(` solo queda en `SongSelection.kt`, `DuplicatesScreen.kt` y su definición.
- [ ] **Step 4:** Tests en verde; `./gradlew assembleRelease`; firmar el APK con `zipalign` y `apksigner` usando `~/.android/debug.keystore` (contraseña estándar de debug) y dejarlo en `app/build/outputs/apk/release/Vitrola.apk`; verificar con `apksigner verify`. `assembleRelease` borra el APK firmado anterior.
- [ ] **Step 5:** Commit `Álbum y artista: selección y orden`.
