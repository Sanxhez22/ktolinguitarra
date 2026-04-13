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
import androidx.navigation.compose.*
import com.example.prueba.ui.screens.*
import kotlinx.coroutines.launch

val NavDark = Color(0xFF15192A)
val NavSelected = Color(0xFFD49A2A)
val NavUnselected = Color(0xFFB8B8B8)

sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    object Home : Dest("home", "Home", Icons.Filled.Home)
    object Search : Dest("search", "Buscar", Icons.Filled.Search)
    object Practice : Dest("practice", "Práctica", Icons.Filled.GraphicEq)
    object Tuner : Dest("tuner", "Afinador", Icons.Filled.MusicNote)
    object Progress : Dest("progress", "Progreso", Icons.Filled.Star)
    object Wilfredo : Dest("wilfredo", "Wilfredo", Icons.Filled.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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

                bottomItems.forEach { screen ->
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
                TopAppBar(
                    title = {
                        Text(
                            text = "FretMind",
                            color = Color.White
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NavDark,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
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
                            label = {
                                Text(screen.label)
                            },
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
                composable(Dest.Tuner.route) { TunerScreen() }
                composable(Dest.Progress.route) { ProgressScreen() }
                composable(Dest.Wilfredo.route) { ChatScreen() }
            }
        }
    }
}