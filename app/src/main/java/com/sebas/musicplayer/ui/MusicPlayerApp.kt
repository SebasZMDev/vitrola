package com.sebas.musicplayer.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sebas.musicplayer.ui.library.LibraryRoute
import com.sebas.musicplayer.ui.nowplaying.NowPlayingRoute
import com.sebas.musicplayer.ui.permissions.AudioPermissionGate
import com.sebas.musicplayer.ui.theme.MusicPlayerTheme

private const val ROUTE_LIBRARY = "library"
private const val ROUTE_NOW_PLAYING = "now_playing"

@Composable
fun MusicPlayerApp() {
    MusicPlayerTheme {
        AudioPermissionGate {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = ROUTE_LIBRARY) {
                composable(
                    route = ROUTE_LIBRARY,
                    // La biblioteca se queda quieta debajo mientras "Reproduciendo ahora" sube o baja encima.
                    enterTransition = { fadeIn() },
                    exitTransition = { ExitTransition.KeepUntilTransitionsFinished },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { fadeOut() },
                ) {
                    LibraryRoute(
                        onOpenNowPlaying = {
                            navController.navigate(ROUTE_NOW_PLAYING) { launchSingleTop = true }
                        },
                    )
                }
                composable(
                    route = ROUTE_NOW_PLAYING,
                    enterTransition = { slideInVertically { it } + fadeIn() },
                    exitTransition = { slideOutVertically { it } + fadeOut() },
                    popEnterTransition = { slideInVertically { it } + fadeIn() },
                    popExitTransition = { slideOutVertically { it } + fadeOut() },
                ) {
                    NowPlayingRoute(
                        onBack = {
                            // Evita que un doble toque saque también la biblioteca y deje la pantalla vacía.
                            if (navController.currentDestination?.route == ROUTE_NOW_PLAYING) {
                                navController.popBackStack()
                            }
                        },
                    )
                }
            }
        }
    }
}
