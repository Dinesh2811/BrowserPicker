package browserpicker.presentation.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import browserpicker.presentation.navigation.BrowserPickerNavHost
import browserpicker.presentation.navigation.NavRoutes

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
    val uiState by viewModel.uiState.collectAsState()
    
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf("Home", "History", "Bookmarks", "Blocked")
    
    // Effect to handle URI from intent
    LaunchedEffect(Unit) {
        onNewIntent.invoke("") // This is a placeholder for real implementation
    }
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { /* Icon based on index */ },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                            when (index) {
                                0 -> navController.navigate(NavRoutes.HOME)
                                1 -> navController.navigate(NavRoutes.URI_HISTORY)
                                2 -> navController.navigate(NavRoutes.BOOKMARKS)
                                3 -> navController.navigate(NavRoutes.BLOCKED_URLS)
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        BrowserPickerNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
