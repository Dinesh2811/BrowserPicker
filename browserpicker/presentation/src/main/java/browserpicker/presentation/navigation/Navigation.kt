package browserpicker.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import browserpicker.presentation.analytics.UriAnalyticsViewModel

/**
 * Main navigation routes for the Browser Picker app
 */
object NavRoutes {
    const val HOME = "home"
    const val BROWSER_PICKER = "browser_picker"
    const val URI_HISTORY = "uri_history"
    const val BOOKMARKS = "bookmarks"
    const val BLOCKED_URLS = "blocked_urls"
    const val FOLDER_DETAILS = "folder_details"
    const val ANALYTICS = "analytics"
    const val BROWSER_STATS = "browser_stats"
    const val HOST_RULE_DETAILS = "host_rule_details"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val URI_ANALYTICS = "uriAnalytics"

    // Routes with parameters
    const val FOLDER_DETAILS_WITH_ID = "folder_details/{folderId}/{folderType}"
    const val HOST_RULE_DETAILS_WITH_ID = "host_rule_details/{hostRuleId}"
    const val URI_DETAILS = "uri_details/{uriRecordId}"
}

/**
 * NavHost for the Browser Picker app
 * Sets up the navigation graph and connects all screens
 *
 * @param navController The NavHostController to control navigation
 * @param startDestination The start destination route
 * @param modifier Modifier to be applied to the NavHost
 */
@Composable
fun BrowserPickerNavHost(
    navController: NavHostController,
    startDestination: String = NavRoutes.HOME,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Home screen
        composable(NavRoutes.HOME) {
            // HomeScreen(navController)
        }
        
        // Browser Picker screen - Main functionality to pick browser for intercepted URIs
        composable(
            route = NavRoutes.BROWSER_PICKER,
            arguments = listOf(
                navArgument("uriString") { 
                    type = NavType.StringType 
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            // BrowserPickerScreen(navController)
        }
        
        // URI History screen
        composable(NavRoutes.URI_HISTORY) {
            // UriHistoryScreen(navController)
        }
        
        // Bookmarks screen
        composable(NavRoutes.BOOKMARKS) {
            // BookmarksScreen(navController)
        }
        
        // Blocked URLs screen
        composable(NavRoutes.BLOCKED_URLS) {
            // BlockedUrlsScreen(navController)
        }
        
        // Folder details screen
        composable(
            route = NavRoutes.FOLDER_DETAILS_WITH_ID,
            arguments = listOf(
                navArgument("folderId") { type = NavType.LongType },
                navArgument("folderType") { type = NavType.IntType }
            )
        ) {
            // val folderId = it.arguments?.getLong("folderId") ?: return@composable
            // val folderType = it.arguments?.getInt("folderType") ?: return@composable
            // FolderDetailsScreen(navController, folderId, folderType)
        }
        
        // Analytics screen
        composable(NavRoutes.ANALYTICS) {
            // AnalyticsScreen(navController)
        }
        
        // Browser stats screen
        composable(NavRoutes.BROWSER_STATS) {
            // BrowserStatsScreen(navController)
        }
        
        // Host rule details screen
        composable(
            route = NavRoutes.HOST_RULE_DETAILS_WITH_ID,
            arguments = listOf(
                navArgument("hostRuleId") { type = NavType.LongType }
            )
        ) {
            // val hostRuleId = it.arguments?.getLong("hostRuleId") ?: return@composable
            // HostRuleDetailsScreen(navController, hostRuleId)
        }
        
        // URI details screen
        composable(
            route = NavRoutes.URI_DETAILS,
            arguments = listOf(
                navArgument("uriRecordId") { type = NavType.LongType }
            )
        ) {
            // val uriRecordId = it.arguments?.getLong("uriRecordId") ?: return@composable
            // UriDetailsScreen(navController, uriRecordId)
        }
        
        // Search screen
        composable(NavRoutes.SEARCH) {
            // SearchScreen(navController)
        }
        
        // Settings screen
        composable(NavRoutes.SETTINGS) {
            // SettingsScreen(navController)
        }

        // URI analytics screen
        composable(NavRoutes.URI_ANALYTICS) {
            // UriAnalyticsScreen(navController)
        }
    }
}
