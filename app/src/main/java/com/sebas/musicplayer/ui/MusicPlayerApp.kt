package com.sebas.musicplayer.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import com.sebas.musicplayer.ui.components.LocalIsPlaying
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sebas.musicplayer.ui.browse.AlbumDetailRoute
import com.sebas.musicplayer.ui.browse.ArtistDetailRoute
import com.sebas.musicplayer.ui.equalizer.EqualizerRoute
import com.sebas.musicplayer.ui.home.HomeScreen
import com.sebas.musicplayer.ui.nowplaying.NowPlayingRoute
import com.sebas.musicplayer.ui.permissions.AudioPermissionGate
import com.sebas.musicplayer.ui.player.PlayerBar
import com.sebas.musicplayer.ui.player.PlayerBarViewModel
import com.sebas.musicplayer.ui.playlists.PlaylistDetailRoute
import com.sebas.musicplayer.ui.search.SearchRoute
import com.sebas.musicplayer.ui.theme.MusicPlayerTheme

private object Routes {
    const val HOME = "home"
    const val NOW_PLAYING = "now_playing"
    const val SEARCH = "search"
    const val ALBUM = "album/{albumId}"
    const val ARTIST = "artist/{artistName}"
    const val PLAYLIST = "playlist/{playlistId}"
    const val EQUALIZER = "equalizer"

    fun album(id: Long) = "album/$id"
    fun artist(name: String) = "artist/${Uri.encode(name)}"
    fun playlist(id: Long) = "playlist/$id"
}

@Composable
fun MusicPlayerApp() {
    MusicPlayerTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            AudioPermissionGate { AppShell() }
        }
    }
}

/** NavHost + mini reproductor global debajo (oculto en "Reproduciendo ahora"). */
@Composable
private fun AppShell() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val playerBarViewModel: PlayerBarViewModel = viewModel(factory = PlayerBarViewModel.Factory)
    val playerState by playerBarViewModel.playerState.collectAsStateWithLifecycle()

    // playerState cambia cada 500 ms con la posición; derivedStateOf evita recomponer toda la
    // pantalla salvo que de verdad cambie la visibilidad. Solo PlayerBar lee la posición.
    val hasSong by remember { derivedStateOf { playerState.currentSong != null } }
    val showPlayerBar = hasSong && backStackEntry?.destination?.route != Routes.NOW_PLAYING
    val isPlaying by remember { derivedStateOf { playerState.isPlaying } }

    CompositionLocalProvider(LocalIsPlaying provides isPlaying) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Con el mini reproductor visible, él se encarga del inset de la barra de navegación;
        // las pantallas de arriba no deben sumarlo otra vez.
        val contentModifier = Modifier.weight(1f).let {
            if (showPlayerBar) it.consumeWindowInsets(WindowInsets.navigationBars) else it
        }
        Box(modifier = contentModifier) {
            AppNavHost(navController)
        }
        AnimatedVisibility(
            visible = showPlayerBar,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            PlayerBar(
                state = playerState,
                onPlayPause = playerBarViewModel::togglePlayPause,
                onNext = playerBarViewModel::next,
                onPrevious = playerBarViewModel::previous,
                onClick = { navController.navigate(Routes.NOW_PLAYING) { launchSingleTop = true } },
            )
        }
    }
    }
}

@Composable
private fun AppNavHost(navController: NavHostController) {
    val openAlbum: (Long) -> Unit = { navController.navigate(Routes.album(it)) }
    val openArtist: (String) -> Unit = { navController.navigate(Routes.artist(it)) }
    val back: () -> Unit = { navController.popBackIfNotHome() }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(
            route = Routes.HOME,
            // Home se queda quieta debajo mientras otra pantalla entra o sale encima.
            exitTransition = { ExitTransition.KeepUntilTransitionsFinished },
            popEnterTransition = { EnterTransition.None },
        ) {
            HomeScreen(
                onOpenSearch = { navController.navigate(Routes.SEARCH) { launchSingleTop = true } },
                onAlbumClick = openAlbum,
                onArtistClick = openArtist,
                onPlaylistClick = { navController.navigate(Routes.playlist(it)) },
            )
        }
        pushScreen(Routes.SEARCH) {
            SearchRoute(onBack = back, onAlbumClick = openAlbum, onArtistClick = openArtist)
        }
        pushScreen(Routes.ALBUM, navArgument("albumId") { type = NavType.LongType }) { args ->
            AlbumDetailRoute(albumId = args.getLong("albumId"), onBack = back)
        }
        pushScreen(Routes.ARTIST, navArgument("artistName") { type = NavType.StringType }) { args ->
            ArtistDetailRoute(
                artistName = args.getString("artistName").orEmpty(),
                onBack = back,
                onAlbumClick = openAlbum,
            )
        }
        pushScreen(Routes.PLAYLIST, navArgument("playlistId") { type = NavType.LongType }) { args ->
            PlaylistDetailRoute(playlistId = args.getLong("playlistId"), onBack = back)
        }
        composable(
            route = Routes.NOW_PLAYING,
            enterTransition = { slideInVertically { it } + fadeIn() },
            exitTransition = { slideOutVertically { it } + fadeOut() },
            popEnterTransition = { slideInVertically { it } + fadeIn() },
            popExitTransition = { slideOutVertically { it } + fadeOut() },
        ) {
            NowPlayingRoute(
                onBack = back,
                onOpenEqualizer = { navController.navigate(Routes.EQUALIZER) { launchSingleTop = true } },
            )
        }
        pushScreen(Routes.EQUALIZER) {
            EqualizerRoute(onBack = back)
        }
    }
}

/** Pantalla de detalle que entra desde la derecha. */
private fun NavGraphBuilder.pushScreen(
    route: String,
    vararg arguments: androidx.navigation.NamedNavArgument,
    content: @Composable (android.os.Bundle) -> Unit,
) {
    composable(
        route = route,
        arguments = arguments.toList(),
        enterTransition = { slideInHorizontally { it } + fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally { it } + fadeOut() },
    ) { entry ->
        content(entry.arguments ?: android.os.Bundle.EMPTY)
    }
}

/** Evita que un doble toque en "atrás" saque también Home y deje la pantalla vacía. */
private fun NavHostController.popBackIfNotHome() {
    if (currentDestination?.route != Routes.HOME) popBackStack()
}
