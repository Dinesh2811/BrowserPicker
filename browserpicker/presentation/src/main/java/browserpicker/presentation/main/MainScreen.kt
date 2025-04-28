package browserpicker.presentation.main

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import browserpicker.domain.model.FolderType
import browserpicker.domain.model.UriStatus
import browserpicker.presentation.navigation.* // Import navigation routes
import browserpicker.presentation.picker.BrowserPickerSheetContent
import kotlinx.coroutines.launch
import timber.log.Timber

// Data class for Bottom Navigation Items
data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any // Use Any for @Serializable route objects/classes
)

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter") // Content padding handled by NavHost modifier
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
    // Callback for when browser needs to be launched after selection in sheet
    // launchBrowserIntent: (uri: String, packageName: String) -> Unit
) {
    val navController = rememberNavController()
    val mainState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    // Bottom Sheet State
    val sheetState = rememberStandardBottomSheetState(
        // Allow hiding completely
        skipHiddenState = false,
        // Initial state: hidden if no context, expanded if context exists (needs logic)
        initialValue = if (mainState.pickerContext != null) SheetValue.Expanded else SheetValue.Hidden
    )

    // If picker context appears, expand the sheet
    LaunchedEffect(mainState.pickerContext) {
        if (mainState.pickerContext != null) {
            launch { // Use specific launch for suspend calls
                try {
                    sheetState.expand()
                } catch (e: Exception) { Timber.e(e, "Error expanding sheet on context change") }
            }
        }
        // Consider auto-hiding if context becomes null and sheet is expanded?
        // else if (sheetState.isVisible) {
        //     launch { sheetState.hide() }
        // }
    }

    val bottomNavItems = listOf(
        BottomNavItem("History", Icons.Default.History, History),
        BottomNavItem("Bookmarks", Icons.Default.Bookmarks, Rules(UriStatus.BOOKMARKED.value)),
        BottomNavItem("Blocked", Icons.Default.Block, Rules(UriStatus.BLOCKED.value)),
        BottomNavItem("Folders", Icons.Default.Folder, Folders(FolderType.BOOKMARK.value)), // Maybe separate Bookmark/Block folders?
        BottomNavItem("Stats", Icons.Default.QueryStats, Stats),
        // BottomNavItem("Settings", Icons.Default.Settings, Settings), // Add later
    )

    // Snackbar Host State
    val snackbarHostState = remember { SnackbarHostState() }
    // Show messages from MainViewModel (global messages)
    LaunchedEffect(mainState.userMessages) {
        mainState.userMessages.firstOrNull()?.let { message ->
            val result = snackbarHostState.showSnackbar(
                message = message.message,
                duration = SnackbarDuration.Short
            )
            // TODO: Add action button if needed based on result
            viewModel.clearMessage(message.id) // Consume message
        }
    }


    BottomSheetScaffold(
        scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState),
        sheetContent = {
            BrowserPickerSheetContent(
                browsers = mainState.availableBrowsers,
                context = mainState.pickerContext,
                onBrowserSelected = { browser, context ->
                    viewModel.handleBrowserSelected(browser, context, sheetState, coroutineScope)
                    // TODO: Observe state/event from VM to trigger actual Intent launch
                    // launchBrowserIntent(context.uriString, browser.packageName)
                },
                onSetPreferenceSelected = { browser, context ->
                    viewModel.handleSetPreferenceSelected(browser, context, sheetState, coroutineScope)
                    // TODO: Observe state/event from VM to trigger actual Intent launch
                    // launchBrowserIntent(context.uriString, browser.packageName)
                },
                onBookmarkSelected = { browser, folderId, context ->
                    viewModel.handleBookmarkSelected(browser, folderId, context, sheetState, coroutineScope)
                    // TODO: Observe state/event from VM to trigger actual Intent launch
                    // launchBrowserIntent(context.uriString, browser.packageName)
                },
                onBlockSelected = { context ->
                    viewModel.handleBlockSelected(context, sheetState, coroutineScope)
                    // No intent launch needed here
                }
            )
        },
        sheetPeekHeight = 0.dp, // Hide sheet when collapsed
        // You can add a drag handle if desired: sheetDragHandle = { ... }
        snackbarHost = { SnackbarHost(snackbarHostState) } // Host for global messages
    ) { scaffoldPadding -> // Padding provided by BottomSheetScaffold

        // Inner Scaffold for Bottom Navigation and main content area
        Scaffold(
            bottomBar = {
                NavigationBar { // Use NavigationBar (Material 3)
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    bottomNavItems.forEach { item ->
                        // Need a way to check if the current route matches the item's route.
                        // Type-safe routes make this slightly trickier than string comparison.
                        // We might need to compare the route *type* or a stable identifier.
                        // Simple approach: Compare serialized form or route class name (less robust).
                        // Better: Define a common interface or property for routes if needed.
                        // Workaround: Check hierarchy for route class/object instance.
                        val selected = currentDestination?.hierarchy?.any {
                            // This comparison works for objects and classes (if using ==)
                            // For classes with args, need more sophisticated check based on route pattern/type
                            // Or use a custom extension on NavDestination.
                            it.route == item.route || // Direct object match
                                    (it.route as? String)?.startsWith(item.route::class.qualifiedName ?: "NO_MATCH") ?: false // Basic check for classes
                        } == true

                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    // Pop up to the start destination to avoid building up stack
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination
                                    launchSingleTop = true
                                    // Restore state when reselecting
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding -> // Padding provided by inner Scaffold (incl. bottom bar)
            AppNavigation(
                navController = navController,
                modifier = Modifier.padding(innerPadding) // Apply inner padding here
                // Pass actions down if needed
                // showBrowserPicker = { uri, host, source, ruleId ->
                //     viewModel.prepareAndShowPickerSheet(sheetState, coroutineScope, uri, host, source, ruleId)
                // }
            )
        }
    }
}
