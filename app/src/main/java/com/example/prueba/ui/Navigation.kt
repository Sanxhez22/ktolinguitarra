package com.example.prueba.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.prueba.ui.screens.*
import kotlinx.coroutines.launch

sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    object Home : Dest("home", "Home", Icons.Filled.Home)
    object Search : Dest("search", "Buscar", Icons.Filled.Search)
    object Practice : Dest("practice", "Práctica", Icons.Filled.PlayArrow)
    object Progress : Dest("progress", "Progreso", Icons.Filled.Star)
    object Profile : Dest("profile", "Perfil", Icons.Filled.Settings)

    // propenso a dar error"
    object Chat : Dest(route = "chat", label = "Wilfredo", icon = Icons.Filled.Person)
    object Tuner : Dest(route = "tuner", label = "Afinador", icon = Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomItems = listOf(Dest.Home, Dest.Search, Dest.Tuner, Dest.Practice, Dest.Progress, Dest.Profile)

    // Drawer
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun go(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Menú",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )

                val drawerItems = listOf(
                    Dest.Home, Dest.Practice, Dest.Progress, Dest.Profile, Dest.Chat, Dest.Tuner
                )

                drawerItems.forEach { screen ->
                    NavigationDrawerItem(
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route,
                        icon = { Icon(screen.icon, contentDescription = null) },
                        onClick = {
                            scope.launch { drawerState.close() }
                            go(screen.route)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("FretMind") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menú")
                        }
                    }
                )
            },

            //  AQUÍ YA NO ESTÁ VACÍO: ahora sí pinta Home/Buscar/Práctica/Progreso/Perfil
            bottomBar = {
                NavigationBar {
                    bottomItems.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = { go(screen.route) },
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) }
                        )
                    }
                }
            },

            floatingActionButton = {
                FloatingActionButton(
                    onClick = { go(Dest.Chat.route) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Dest.Chat.icon,
                        contentDescription = "Wilfredo",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "splash",
                modifier = Modifier.padding(padding)
            ) {
                composable("splash") { SplashScreen(navController) }

                composable(Dest.Home.route) { HomeScreen() }
                composable(Dest.Search.route) { SearchScreen() }
                composable(Dest.Practice.route) { PracticeScreen() }
                composable(Dest.Progress.route) { ProgressScreen() }
                composable(Dest.Profile.route) { ProfileScreen() }

                composable(Dest.Chat.route) { ChatScreen() }
                composable(Dest.Chat.route.replace("chat", "tuner")) { TunerScreen() }
            }
        }
    }
}