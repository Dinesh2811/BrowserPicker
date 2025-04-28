package browserpicker.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import browserpicker.presentation.*
import browserpicker.presentation.folders.FoldersScreen
import browserpicker.presentation.history.HistoryScreen
import browserpicker.presentation.picker.BrowserAppInfo
import browserpicker.presentation.rules.RulesScreen
import browserpicker.presentation.settings.SettingsScreen
import browserpicker.presentation.stats.StatsScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    availableBrowsers: List<BrowserAppInfo>,
    modifier: Modifier = Modifier
    // Pass actions needed by screens if they trigger sheet, etc.
    // showBrowserPicker: (uri: String, host: String, source: UriSource, ruleId: Long?) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = History, // Start with History screen
        modifier = modifier
    ) {
        addHistoryDestination(navController)
        addRulesDestination(navController, availableBrowsers)
        addFoldersDestination(navController)
        addStatsDestination(navController)
        addSettingsDestination(navController)
        // Add other destinations...
    }
}

// Extension functions for cleaner NavGraphBuilder syntax

private fun NavGraphBuilder.addHistoryDestination(navController: NavHostController) {
    composable<History> { // Type-safe destination
        HistoryScreen(
            viewModel = hiltViewModel()
            // Pass navigation lambdas if needed, e.g.:
            // onRuleClick = { host -> navController.navigate(/* specific rule screen */) }
        )
    }
}

private fun NavGraphBuilder.addRulesDestination(
    navController: NavHostController,
    availableBrowsers: List<BrowserAppInfo> // Added parameter
) {
    composable<Rules> { backStackEntry ->
        val args: Rules = backStackEntry.toRoute()
        RulesScreen(
            viewModel = hiltViewModel(),
            initialType = args.type,
            availableBrowsers = availableBrowsers // Pass browsers
            // onFolderClick = { folderId -> navController.navigate(Folders(type = args.type, selectedFolder = folderId)) }
        )
    }
}

private fun NavGraphBuilder.addFoldersDestination(navController: NavHostController) {
    composable<Folders> { backStackEntry ->
        val args: Folders = backStackEntry.toRoute()
        FoldersScreen(
            viewModel = hiltViewModel(),
            initialType = args.type
            // onRuleClick = { ... }
        )
    }
}

private fun NavGraphBuilder.addStatsDestination(navController: NavHostController) {
    composable<Stats> {
        StatsScreen(
            viewModel = hiltViewModel()
        )
    }
}

private fun NavGraphBuilder.addSettingsDestination(navController: NavHostController) {
    composable<Settings> {
        SettingsScreen(
            // viewModel = hiltViewModel() // If settings has a VM
        )
    }
}
