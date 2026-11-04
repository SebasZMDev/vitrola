package com.sebas.musicplayer.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private val audioPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

private fun requestedPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

private fun Context.hasAudioPermission() =
    checkSelfPermission(audioPermission) == PackageManager.PERMISSION_GRANTED

@Composable
fun AudioPermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasAudioPermission()) }
    var denied by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = context.hasAudioPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = context.hasAudioPermission()
        denied = !granted
    }

    if (granted) {
        content()
    } else {
        PermissionScreen(
            denied = denied,
            onRequest = { launcher.launch(requestedPermissions()) },
            onOpenSettings = {
                val uri = Uri.fromParts("package", context.packageName, null)
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))
            },
        )
    }
}

@Composable
fun PermissionScreen(denied: Boolean, onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.LibraryMusic,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Necesitamos acceso a tu música",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            if (denied) {
                "Sin el permiso no podemos mostrar tus canciones. Puedes activarlo desde los ajustes de la app."
            } else {
                "Para reproducir los archivos de audio de este dispositivo, la app necesita permiso para leerlos."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        if (denied) {
            Button(onClick = onOpenSettings) { Text("Abrir ajustes") }
        } else {
            Button(onClick = onRequest) { Text("Permitir acceso a la música") }
        }
    }
}
