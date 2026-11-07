package com.musicfy.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import com.musicfy.app.AppVM
import kotlinx.coroutines.flow.merge

val Green = Color(0xFF3DF2A0)
val Blue = Color(0xFF0EA9D8)
val Bg = Color(0xFF07070B)
val SurfaceDark = Color(0xFF14141C)

@Composable
fun MusicFyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Green, onPrimary = Color.Black,
            secondary = Blue, background = Bg, onBackground = Color.White,
            surface = SurfaceDark, onSurface = Color.White,
            surfaceVariant = Color(0xFF22222C), onSurfaceVariant = Color(0xFFB3B3C0),
        ),
        content = content
    )
}

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val LIBRARY = "library"
    const val ALBUM = "album/{id}"
    const val ARTIST = "artist/{id}"
}

@Composable
fun App(vm: AppVM) {
    val nav = rememberNavController()
    var showPlayer by remember { mutableStateOf(false) }
    val current by vm.player.current.collectAsState()
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        merge(vm.store.errors, vm.player.errors).collect { snack.showSnackbar(it) }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Bg,
            snackbarHost = { SnackbarHost(snack) },
            bottomBar = {
                Column {
                    if (current != null) MiniPlayer(vm) { showPlayer = true }
                    BottomBar(nav)
                }
            }
        ) { pad ->
            NavHost(nav, Routes.HOME, Modifier.padding(pad)) {
                composable(Routes.HOME) { HomeScreen(vm) }
                composable(Routes.SEARCH) { SearchScreen(vm, nav) }
                composable(Routes.LIBRARY) { LibraryScreen(vm) }
                composable(
                    Routes.ALBUM,
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { AlbumScreen(vm, nav, it.arguments?.getString("id").orEmpty()) }
                composable(
                    Routes.ARTIST,
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { ArtistScreen(vm, nav, it.arguments?.getString("id").orEmpty()) }
            }
        }

        AnimatedVisibility(
            visible = showPlayer,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) { NowPlaying(vm) { showPlayer = false } }
    }

    // Android back button: first closes the full-screen player, otherwise the NavHost
    // pops the screen stack (album -> search -> home -> exit).
    BackHandler(enabled = showPlayer) { showPlayer = false }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
private fun BottomBar(nav: NavHostController) {
    val tabs = listOf(
        Tab(Routes.HOME, "Home", Icons.Default.Home),
        Tab(Routes.SEARCH, "Search", Icons.Default.Search),
        Tab(Routes.LIBRARY, "Library", Icons.Default.LibraryMusic),
    )
    val entry by nav.currentBackStackEntryAsState()
    NavigationBar(containerColor = Bg) {
        tabs.forEach { t ->
            val selected = entry?.destination?.hierarchy?.any { it.route == t.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    nav.navigate(t.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(t.icon, contentDescription = t.label) },
                label = { Text(t.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    indicatorColor = Green,
                    selectedTextColor = Green,
                )
            )
        }
    }
}
