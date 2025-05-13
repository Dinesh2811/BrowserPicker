package browserpicker.presentation.features.main

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import browserpicker.presentation.test.navigation.BrowserPickerNavHost
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@Composable
fun MainScreen(
    onNewIntent: (String) -> Unit = {}
) {
    val navController = rememberNavController()
    HandleBackPress(navController)

    val bottomNavItems = remember {
        listOf(
            BottomNavItem("Home", Icons.Default.Home, HomeScreen),
            BottomNavItem("History", Icons.Default.History, UriHistoryScreen),
            BottomNavItem("Preferences", Icons.AutoMirrored.Filled.ListAlt, PreferencesScreen),
            BottomNavItem("Folder", Icons.Default.Folder, FolderDetailsScreen),
            BottomNavItem("Analytics", Icons.Default.Analytics, BrowserAnalyticsScreen)
        )
    }

    Scaffold(
        bottomBar = {
            AppBottomNavigation(navController, bottomNavItems)
        }
    ) { innerPadding ->
        BrowserPickerNavHost(navController = navController, modifier = Modifier.padding(innerPadding))
    }
}

@Composable
private fun AppBottomNavigation(
    navController: NavHostController,
    items: List<BottomNavItem>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Function to handle navigation clicks
    val navigateToTopLevelDestination = { destination: AppDestination ->
        navController.navigate(destination) {
            // Correctly pop up to the start destination of the graph
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true // Save state when popping
            }
            launchSingleTop = true // Avoid multiple copies
            restoreState = true // Restore state when re-selecting
        }
    }

    NavigationBar {
        items.forEach { item ->
            // Use the qualified name of the serializable object for comparison
            val isSelected = currentDestination?.hierarchy?.any { navDest ->
                navDest.route == item.route::class.qualifiedName
            } == true

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
private fun HandleBackPress(navController: NavHostController) {
    val context = LocalContext.current
    var backPressedOnce by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    // Check if the current destination is the start destination (HomeScreen)
    // by comparing the route string with the qualified name of the HomeScreen object
    val isAtHome = currentBackStackEntry?.destination?.route == HomeScreen::class.qualifiedName

    // Check if the back stack *below* home is empty
    // We can approximate this by checking if the previous back stack entry is null
    // or if navigating back would pop the start destination.
    // A more robust check would involve inspecting the back stack entries themselves,
    // but this approximation is usually sufficient for bottom navigation setups.
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

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: AppDestination
)
