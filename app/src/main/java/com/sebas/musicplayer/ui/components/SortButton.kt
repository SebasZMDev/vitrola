package com.sebas.musicplayer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sebas.musicplayer.MusicApp
import com.sebas.musicplayer.data.settings.SongSort
import com.sebas.musicplayer.data.settings.SongSortField
import com.sebas.musicplayer.domain.library.ListDefaultSort
import com.sebas.musicplayer.domain.library.nextSort

/** Botón con el criterio de orden actual y un menú para cambiarlo. */
@Composable
fun SortButton(
    sort: SongSort,
    onChange: (SongSort) -> Unit,
    fields: List<SongSortField> = SongSortField.entries,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(sort.field.label, modifier = Modifier.padding(start = 6.dp))
            Icon(
                if (sort.ascending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = if (sort.ascending) "Ascendente" else "Descendente",
                modifier = Modifier.padding(start = 4.dp).size(16.dp),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            fields.forEach { field ->
                DropdownMenuItem(
                    text = { Text(field.label) },
                    trailingIcon = {
                        if (field == sort.field) {
                            Icon(
                                if (sort.ascending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    onClick = {
                        open = false
                        onChange(nextSort(sort, field))
                    },
                )
            }
        }
    }
}

/** Orden guardado de la lista [key] y la función para cambiarlo; sin dato es [ListDefaultSort]. */
@Composable
fun rememberListSort(key: String): Pair<SongSort, (SongSort) -> Unit> {
    val context = LocalContext.current
    val settings = remember(context) { (context.applicationContext as MusicApp).container.settings }
    val state by settings.state.collectAsStateWithLifecycle()
    val onChange = remember(settings, key) { { sort: SongSort -> settings.setListSort(key, sort) } }
    return (state.listSorts[key] ?: ListDefaultSort) to onChange
}
