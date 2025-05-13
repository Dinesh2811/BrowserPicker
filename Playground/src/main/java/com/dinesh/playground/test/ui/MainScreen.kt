package com.dinesh.playground.test.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.dinesh.playground.test.navigation.AppDestination
import com.dinesh.playground.test.navigation.BrowserAnalyticsScreen
import com.dinesh.playground.test.navigation.FolderDetailsScreen
import com.dinesh.playground.test.navigation.HomeScreen
import com.dinesh.playground.test.navigation.PreferencesScreen
import com.dinesh.playground.test.navigation.UriHistoryScreen
import com.dinesh.playground.test.ui.screens.BrowserAnalyticsScreen
import com.dinesh.playground.test.ui.screens.FolderDetailsScreen
import com.dinesh.playground.test.ui.screens.HomeScreen
import com.dinesh.playground.test.ui.screens.NestedFolderDetailsScreen1
import com.dinesh.playground.test.ui.screens.NestedFolderDetailsScreen2
import com.dinesh.playground.test.ui.screens.NestedFolderDetailsScreen3
import com.dinesh.playground.test.ui.screens.NestedPreferencesScreen1
import com.dinesh.playground.test.ui.screens.NestedPreferencesScreen2
import com.dinesh.playground.test.ui.screens.NestedPreferencesScreen3
import com.dinesh.playground.test.ui.screens.NestedUriHistoryScreen1
import com.dinesh.playground.test.ui.screens.NestedUriHistoryScreen2
import com.dinesh.playground.test.ui.screens.NestedUriHistoryScreen3
import com.dinesh.playground.test.ui.screens.PreferencesScreen
import com.dinesh.playground.test.ui.screens.UriHistoryScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

// Helper data class for bottom navigation items
data class BottomNavItem(
    // Replace with actual @StringRes Int if you have them
    val label: String, // Using String for simplicity now
    val icon: ImageVector,
    val route: AppDestination // Use the sealed interface type
)

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    HandleBackPress(navController) // Extract back press logic

    // Define bottom navigation items using the serializable routes
    val bottomNavItems = remember { // Use remember for constant list
        listOf(
            BottomNavItem("Home", Icons.Default.Home, HomeScreen),
            BottomNavItem("History", Icons.Default.History, UriHistoryScreen),
            BottomNavItem("Prefs", Icons.Default.Settings, PreferencesScreen),
            BottomNavItem("Folder", Icons.Default.Folder, FolderDetailsScreen),
            BottomNavItem("Analytics", Icons.Default.Analytics, BrowserAnalyticsScreen)
        )
    }

    Scaffold(
        bottomBar = {
            AppBottomNavigation(navController, bottomNavItems)
        }
    ) { innerPadding ->
        AppNavHost(navController, modifier = Modifier.padding(innerPadding))
    }
}

@Composable
private fun AppBottomNavigation(
    navController: NavHostController,
    items: List<BottomNavItem>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    // Attempt to get the current type-safe route object
    val currentRouteObject = navBackStackEntry?.toRoute<AppDestination>()

    // Function to handle navigation clicks
    val navigateToTopLevelDestination = { destination: AppDestination ->
        navController.navigate(destination) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true // Save state when popping
            }
            launchSingleTop = true // Avoid multiple copies
            restoreState = true // Restore state when re-selecting
        }
    }

    NavigationBar {
        items.forEach { item ->
            // Use the currentRouteObject for comparison
            val isSelected = currentRouteObject == item.route

            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navigateToTopLevelDestination(item.route)
                    }
                },
                alwaysShowLabel = true
            )
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HomeScreen, // Use the serializable object
        modifier = modifier
    ) {
        // --- Top Level Screens ---
        composable<HomeScreen> { HomeScreen() }
        composable<UriHistoryScreen> {
            UriHistoryScreen(
                onNavigateToNested1 = { navController.navigate(UriHistoryScreen.NestedScreen1) },
                onNavigateToNested2 = { navController.navigate(UriHistoryScreen.NestedScreen2) },
                onNavigateToNested3 = { navController.navigate(UriHistoryScreen.NestedScreen3) }
            )
        }
        composable<PreferencesScreen> {
            PreferencesScreen(
                onNavigateToNested1 = { navController.navigate(PreferencesScreen.NestedScreen1) },
                onNavigateToNested2 = { navController.navigate(PreferencesScreen.NestedScreen2) },
                onNavigateToNested3 = { navController.navigate(PreferencesScreen.NestedScreen3) }
            )
        }
        composable<FolderDetailsScreen> {
            FolderDetailsScreen(
                onNavigateToNested1 = { navController.navigate(FolderDetailsScreen.NestedScreen1) },
                onNavigateToNested2 = { navController.navigate(FolderDetailsScreen.NestedScreen2) },
                onNavigateToNested3 = { navController.navigate(FolderDetailsScreen.NestedScreen3) }
            )
        }
        composable<BrowserAnalyticsScreen> { BrowserAnalyticsScreen() }

        // --- Nested Screens ---
        composable<UriHistoryScreen.NestedScreen1> { NestedUriHistoryScreen1(onNavigateBack = { navController.popBackStack() }) }
        composable<UriHistoryScreen.NestedScreen2> { NestedUriHistoryScreen2(onNavigateBack = { navController.popBackStack() }) }
        composable<UriHistoryScreen.NestedScreen3> { NestedUriHistoryScreen3(onNavigateBack = { navController.popBackStack() }) }

        composable<PreferencesScreen.NestedScreen1> { NestedPreferencesScreen1(onNavigateBack = { navController.popBackStack() }) }
        composable<PreferencesScreen.NestedScreen2> { NestedPreferencesScreen2(onNavigateBack = { navController.popBackStack() }) }
        composable<PreferencesScreen.NestedScreen3> { NestedPreferencesScreen3(onNavigateBack = { navController.popBackStack() }) }

        composable<FolderDetailsScreen.NestedScreen1> { NestedFolderDetailsScreen1(onNavigateBack = { navController.popBackStack() }) }
        composable<FolderDetailsScreen.NestedScreen2> { NestedFolderDetailsScreen2(onNavigateBack = { navController.popBackStack() }) }
        composable<FolderDetailsScreen.NestedScreen3> { NestedFolderDetailsScreen3(onNavigateBack = { navController.popBackStack() }) }
    }
}

@Composable
private fun HandleBackPress(navController: NavHostController) {
    val context = LocalContext.current
    var backPressedOnce by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    // Check if the current destination is the start destination (HomeScreen)
    // Use toRoute to get the type-safe object for comparison
    val isAtHome = currentBackStackEntry?.toRoute<AppDestination>() == HomeScreen

    // Check if the back stack *below* home is empty
    // We can approximate this by checking if the previous back stack entry is null
    // or if navigating back would pop the start destination.
    val canPop = navController.previousBackStackEntry != null


    BackHandler(enabled = isAtHome && !canPop) { // Only enable double-tap-exit if at Home AND cannot pop further back
        if (backPressedOnce) {
            (context as? Activity)?.finish() // Exit app
        } else {
            backPressedOnce = true
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            scope.launch {
                delay(2.seconds)
                backPressedOnce = false
            }
        }
    }
}
