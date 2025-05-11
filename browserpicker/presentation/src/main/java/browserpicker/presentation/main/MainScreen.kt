package browserpicker.presentation.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import browserpicker.domain.model.UriSource

/**
 * Main screen container for the app.
 *
 * This is the root composable that contains:
 * - App scaffolding (with bottom navigation)
 * - NavHost for navigation between screens
 *
 * It serves as the container for all other screens in the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNewIntent: (String) -> Unit = {}
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    // Process new intent URI if provided
    LaunchedEffect(onNewIntent) {
        onNewIntent.let { callback ->
            if (callback != {}) {
                // This will be called when a new intent with URI is received
                // The actual URI string will be passed from Activity
            }
        }
    }

    // Process clipboard URI if detected
    val clipboardUri = uiState.clipboardUri.getOrNull()
    LaunchedEffect(clipboardUri) {
        clipboardUri?.let { uri ->
            if (uri.isNotBlank()) {
                // Optionally show a snackbar or prompt to handle clipboard URI
                snackbarHostState.showSnackbar("Found URI in clipboard: $uri")
                // Alternatively, directly handle it:
                // viewModel.handleUri(uri, UriSource.CLIPBOARD)
            }
        }
    }

    // Bottom navigation items
    val navItems = listOf(
        NavItem.Home,
        NavItem.History,
        NavItem.Bookmarks,
        NavItem.Analytics,
        NavItem.Settings
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            MainBottomNavigation(navController = navController, items = navItems)
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController,
                startDestination = NavItem.Home.route
            ) {
                // Home screen
                composable(NavItem.Home.route) {
                    HomeScreen(navController = navController)
                }

                // History screen
                composable(NavItem.History.route) {
                    // TODO: Implement HistoryScreen
                    Text("History Screen")
                }

                // Bookmarks screen
                composable(NavItem.Bookmarks.route) {
                    // TODO: Implement BookmarksScreen
                    Text("Bookmarks Screen")
                }

                // Analytics screen
                composable(NavItem.Analytics.route) {
                    // TODO: Implement AnalyticsScreen
                    Text("Analytics Screen")
                }

                // Settings screen
                composable(NavItem.Settings.route) {
                    // TODO: Implement SettingsScreen
                    Text("Settings Screen")
                }
            }
        }
    }
}

/**
 * Bottom navigation component for the main screen
 */
@Composable
private fun MainBottomNavigation(
    navController: NavController,
    items: List<NavItem>
) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                selected = selected,
                onClick = {
                    if (currentDestination?.route != item.route) {
                        navController.navigate(item.route) {
                            // Pop up to the start destination of the graph to avoid
                            // building up a large stack of destinations on the back stack
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

/**
 * Navigation items for bottom navigation
 */
sealed class NavItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    data object Home : NavItem("home", "Home", Icons.Filled.Home)
    data object History : NavItem("history", "History", Icons.Filled.History)
    data object Bookmarks : NavItem("bookmarks", "Bookmarks", Icons.Filled.Star)
    data object Analytics : NavItem("analytics", "Analytics", Icons.Outlined.Analytics)
    data object Settings : NavItem("settings", "Settings", Icons.Filled.Settings)
}
