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


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
    // launchBrowserIntent: (uri: String, packageName: String) -> Unit // Keep if needed for launching
) {
    val navController = rememberNavController()
    val mainState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val sheetState = rememberStandardBottomSheetState(
        skipHiddenState = false,
        initialValue = if (mainState.pickerContext != null) SheetValue.Expanded else SheetValue.Hidden
    )

    LaunchedEffect(mainState.pickerContext) {
        if (mainState.pickerContext != null) {
            launch {
                try { sheetState.expand() } catch (e: Exception) { Timber.e(e, "Error expanding sheet") }
            }
        } else if (sheetState.isVisible && sheetState.currentValue == SheetValue.Expanded) {
            // Optional: Hide if context cleared while sheet is expanded
            // launch { sheetState.hide() }
        }
    }

    val bottomNavItems = listOf(
        BottomNavItem("History", Icons.Default.History, History),
        BottomNavItem("Bookmarks", Icons.Default.Bookmarks, Rules(UriStatus.BOOKMARKED.value)),
        BottomNavItem("Blocked", Icons.Default.Block, Rules(UriStatus.BLOCKED.value)),
        BottomNavItem("Folders", Icons.Default.Folder, Folders(FolderType.BOOKMARK.value)),
        BottomNavItem("Stats", Icons.Default.QueryStats, Stats),
    )

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(mainState.userMessages) {
        mainState.userMessages.firstOrNull()?.let { message ->
            val result = snackbarHostState.showSnackbar(
                message = message.message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessage(message.id)
        }
    }


    BottomSheetScaffold(
        scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState),
        sheetContent = {
            BrowserPickerSheetContent(
                browsers = mainState.availableBrowsers, // Pass available browsers
                context = mainState.pickerContext,
                onBrowserSelected = { browser, context ->
                    viewModel.handleBrowserSelected(browser, context, sheetState, coroutineScope)
                    // launchBrowserIntent(context.uriString, browser.packageName) // Call actual launcher
                },
                onSetPreferenceSelected = { browser, context ->
                    viewModel.handleSetPreferenceSelected(browser, context, sheetState, coroutineScope)
                    // launchBrowserIntent(context.uriString, browser.packageName) // Call actual launcher
                },
                onBookmarkSelected = { browser, folderId, context ->
                    viewModel.handleBookmarkSelected(browser, folderId, context, sheetState, coroutineScope)
                    // launchBrowserIntent(context.uriString, browser.packageName) // Call actual launcher
                },
                onBlockSelected = { context ->
                    viewModel.handleBlockSelected(context, sheetState, coroutineScope)
                }
            )
        },
        sheetPeekHeight = 0.dp,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { scaffoldPadding ->

        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.route ||
                                    (it.route as? String)?.startsWith(item.route::class.qualifiedName ?: "NO_MATCH") ?: false
                        } == true

                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            AppNavigation(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                availableBrowsers = mainState.availableBrowsers // Pass browsers down
            )
        }
    }
}
