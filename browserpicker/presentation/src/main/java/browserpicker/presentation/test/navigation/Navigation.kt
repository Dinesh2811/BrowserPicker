package browserpicker.presentation.test.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import browserpicker.presentation.test.analytics.BrowserAnalyticsScreen
import browserpicker.presentation.test.analytics.UriAnalyticsScreen
import browserpicker.presentation.test.blockedurls.BlockedUrlsScreen
import browserpicker.presentation.test.bookmarks.BookmarksScreen
import browserpicker.presentation.test.browserpicker.BrowserPickerScreen
import browserpicker.presentation.test.details.FolderDetailsScreen
import browserpicker.presentation.test.details.HostRuleDetailsScreen
import browserpicker.presentation.test.details.UriDetailsScreen
import browserpicker.presentation.test.history.UriHistoryScreen
import browserpicker.presentation.test.main.HomeScreen
import browserpicker.presentation.test.search.SearchScreen
import browserpicker.presentation.test.settings.SettingsScreen

@Composable
fun BrowserPickerNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: Any = HomeRoute,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Home screen
        composable<HomeRoute> {
            HomeScreen(navController)
        }

        // Browser Picker screen - Main functionality to pick browser for intercepted URIs
        composable<BrowserPickerRoute> {
            BrowserPickerScreen(navController)
        }

        // URI History screen
        composable<UriHistoryRoute> {
            UriHistoryScreen(navController)
        }

        // Bookmarks screen
        composable<BookmarksRoute> {
            BookmarksScreen(navController)
        }

        // Blocked URLs screen
        composable<BlockedUrlsRoute> {
            BlockedUrlsScreen(navController)
        }

        // Folder details screen
        composable<FolderDetailsRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<FolderDetailsRoute>()
            FolderDetailsScreen(navController, args.folderId, args.folderType)
        }

        // URI details screen
        composable<UriDetailsRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<UriDetailsRoute>()
            UriDetailsScreen(navController, args.uriRecordId)
        }

        // Browser Analytics screen
        composable<BrowserAnalyticsRoute> {
            BrowserAnalyticsScreen(navController)
        }

        // Host rule details screen
        composable<HostRuleDetailsRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<HostRuleDetailsRoute>()
            HostRuleDetailsScreen(navController, args.hostRuleId)
        }

        // Search screen
        composable<SearchRoute> {
            SearchScreen(navController)
        }

        // Settings screen
        composable<SettingsRoute> {
            SettingsScreen(navController)
        }

        // URI analytics screen
        composable<UriAnalyticsRoute> {
            UriAnalyticsScreen(navController)
        }
    }
}
