package com.example.prueba.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.prueba.ui.screens.*
import kotlinx.coroutines.launch

/** Ruta concreta de detalle para un songId dado. */
fun songDetailRoute(songId: Long) = "songDetail/$songId"

val NavDark = Color(0xFF15192A)
val NavSelected = Color(0xFFD49A2A)
val NavUnselected = Color(0xFFB8B8B8)

sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    object Home : Dest("home", "Home", Icons.Filled.Home)
    object Search : Dest("search", "Buscar", Icons.Filled.Search)
    object Practice : Dest("practice", "Práctica", Icons.Filled.GraphicEq)
    object Tuner : Dest("tuner", "Afinador", Icons.Filled.MusicNote)
    object Progress : Dest("progress", "Progreso", Icons.Filled.Star)
    object Wilfredo : Dest("wilfredo", "RIFF", Icons.Filled.Person)
    object Login : Dest("login", "Login", Icons.Filled.Person)
    object Splash : Dest("splash", "Splash", Icons.Filled.Home)
    object Onboarding : Dest("onboarding", "Onboarding", Icons.Filled.Star)
    object Camino : Dest("camino", "Mi camino", Icons.Filled.Star)
    object Biblioteca : Dest("biblioteca", "Mi biblioteca", Icons.Filled.MusicNote)
    object SongDetail : Dest("songDetail/{songId}", "Detalle", Icons.Filled.MusicNote)

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    // Ruta base sin argumentos opcionales (p. ej. "practice?ej=acordes" -> "practice"),
    // para que el resaltado de la barra siga funcionando.
    val currentRoute = navBackStackEntry?.destination?.route?.substringBefore("?")
    val showBars = currentRoute != Dest.Login.route &&
        currentRoute != Dest.Splash.route &&
        currentRoute != Dest.Onboarding.route

    val bottomItems = listOf(
        Dest.Home,
        Dest.Search,
        Dest.Practice,
        Dest.Tuner,
        Dest.Progress,
        Dest.Wilfredo
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun go(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = NavDark,
                drawerContentColor = Color.White
            ) {
                Text(
                    text = "Menú",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )

                // El drawer incluye, además de las pestañas, Camino y Biblioteca.
                (bottomItems + Dest.Camino + Dest.Biblioteca).forEach { screen ->
                    NavigationDrawerItem(
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            go(screen.route)
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.label
                            )
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFF3A3120),
                            selectedIconColor = NavSelected,
                            selectedTextColor = NavSelected,
                            unselectedIconColor = NavUnselected,
                            unselectedTextColor = NavUnselected
                        ),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (showBars) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "FretMind",
                                color = Color.White
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = NavDark,
                            titleContentColor = Color.White
                        )
                    )
                }
            },
            bottomBar = {
                if (showBars) {
                    NavigationBar(
                        containerColor = NavDark,
                        tonalElevation = 0.dp
                    ) {
                        bottomItems.forEach { screen ->
                            NavigationBarItem(
                                selected = currentRoute == screen.route,
                                onClick = { go(screen.route) },
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.label
                                    )
                                },
                                label = { Text(screen.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NavSelected,
                                    selectedTextColor = NavSelected,
                                    unselectedIconColor = NavUnselected,
                                    unselectedTextColor = NavUnselected,
                                    indicatorColor = Color(0xFF3A3120)
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Dest.Splash.route,
                modifier = Modifier.padding(padding)
            ) {
                composable(Dest.Splash.route) { SplashScreen(navController) }

                composable(Dest.Login.route) {
                    LoginScreen(
                        // El onboarding solo aparece la primera vez: si el
                        // usuario ya lo completó va directo al Home.
                        onLoginSuccess = { onboardingCompletado ->
                            val destino = if (onboardingCompletado) Dest.Home.route else Dest.Onboarding.route
                            navController.navigate(destino) {
                                popUpTo(Dest.Login.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Dest.Onboarding.route) {
                    OnboardingScreen(
                        onFinished = {
                            navController.navigate(Dest.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Dest.Home.route) {
                    HomeScreen(
                        onNavigate = { route -> go(route) },
                        onLogout = {
                            navController.navigate(Dest.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
                composable(Dest.Search.route) {
                    SearchScreen(onSongClick = { id -> navController.navigate(songDetailRoute(id)) })
                }
                composable(
                    route = "practice?ej={ej}&song={song}",
                    arguments = listOf(
                        navArgument("ej") {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                        navArgument("song") {
                            type = NavType.StringType
                            defaultValue = ""
                        }
                    )
                ) { entry ->
                    val ej = entry.arguments?.getString("ej")?.takeIf { it.isNotBlank() }
                    val song = entry.arguments?.getString("song")?.toLongOrNull()
                    PracticeScreen(ejercicioPreseleccionado = ej, cancionId = song)
                }
                composable(Dest.Tuner.route) { TunerScreen() }
                composable(Dest.Progress.route) {
                    ProgressScreen(onVerCamino = { go(Dest.Camino.route) })
                }
                composable(Dest.Camino.route) {
                    CaminoScreen(onPracticar = { ej -> go("practice?ej=$ej") })
                }
                composable(Dest.Biblioteca.route) {
                    BibliotecaScreen(
                        onSongClick = { id -> navController.navigate(songDetailRoute(id)) },
                        onBuscar = { go(Dest.Search.route) }
                    )
                }
                composable(Dest.Wilfredo.route) { ChatScreen() }
                composable(
                    route = Dest.SongDetail.route,
                    arguments = listOf(navArgument("songId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val songId = backStackEntry.arguments?.getLong("songId") ?: 0L
                    SongDetailScreen(
                        songId = songId,
                        onBack = { navController.popBackStack() },
                        onPracticar = { ejercicioId, cancion ->
                            navController.navigate("practice?ej=$ejercicioId&song=$cancion")
                        }
                    )
                }
            }
        }
    }
}