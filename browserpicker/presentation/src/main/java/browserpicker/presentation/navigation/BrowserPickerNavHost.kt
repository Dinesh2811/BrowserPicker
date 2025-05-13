package browserpicker.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun MainScreen(
    onNewIntent: (String) -> Unit = {}
) {}
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {}
@Composable
fun UriHistoryScreen(
    modifier: Modifier = Modifier,
    onNavigateToNested1: () -> Unit,
    onNavigateToNested2: () -> Unit,
    onNavigateToNested3: () -> Unit
) {}
@Composable
fun PreferencesScreen(
    modifier: Modifier = Modifier,
    onNavigateToNested1: () -> Unit,
    onNavigateToNested2: () -> Unit,
    onNavigateToNested3: () -> Unit
) {}
@Composable
fun FolderDetailsScreen(
    modifier: Modifier = Modifier,
    onNavigateToNested1: () -> Unit,
    onNavigateToNested2: () -> Unit,
    onNavigateToNested3: () -> Unit
) {}
@Composable
fun BrowserAnalyticsScreen(modifier: Modifier = Modifier) {}
@Composable
fun NestedUriHistoryScreen1(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {}
@Composable
fun NestedUriHistoryScreen2(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {}
@Composable
fun NestedUriHistoryScreen3(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {}
@Composable
fun NestedPreferencesScreen1(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {}
@Composable
fun NestedPreferencesScreen2(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {}
@Composable
fun NestedPreferencesScreen3(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {}
@Composable
fun NestedFolderDetailsScreen1(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {}
@Composable
fun NestedFolderDetailsScreen2(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {}
@Composable
fun NestedFolderDetailsScreen3(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {}
@Composable
private fun BrowserPickerNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: AppDestination = HomeRoute,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // --- Top Level Screens ---
        composable<HomeRoute> { HomeScreen() }
        composable<UriHistoryRoute> {
            UriHistoryScreen(
                onNavigateToNested1 = { navController.navigate(UriHistoryRoute.NestedRoute1) },
                onNavigateToNested2 = { navController.navigate(UriHistoryRoute.NestedRoute2) },
                onNavigateToNested3 = { navController.navigate(UriHistoryRoute.NestedRoute3) }
            )
        }
        composable<PreferencesRoute> {
            PreferencesScreen(
                onNavigateToNested1 = { navController.navigate(PreferencesRoute.NestedRoute1) },
                onNavigateToNested2 = { navController.navigate(PreferencesRoute.NestedRoute2) },
                onNavigateToNested3 = { navController.navigate(PreferencesRoute.NestedRoute3) }
            )
        }
        composable<FolderDetailsRoute> {
            FolderDetailsScreen(
                onNavigateToNested1 = { navController.navigate(FolderDetailsRoute.NestedRoute1) },
                onNavigateToNested2 = { navController.navigate(FolderDetailsRoute.NestedRoute2) },
                onNavigateToNested3 = { navController.navigate(FolderDetailsRoute.NestedRoute3) }
            )
        }
        composable<BrowserAnalyticsRoute> { BrowserAnalyticsScreen() }

        // --- Nested Screens ---
        composable<UriHistoryRoute.NestedRoute1> { NestedUriHistoryScreen1(onNavigateBack = { navController.popBackStack() }) }
        composable<UriHistoryRoute.NestedRoute2> { NestedUriHistoryScreen2(onNavigateBack = { navController.popBackStack() }) }
        composable<UriHistoryRoute.NestedRoute3> { NestedUriHistoryScreen3(onNavigateBack = { navController.popBackStack() }) }

        composable<PreferencesRoute.NestedRoute1> { NestedPreferencesScreen1(onNavigateBack = { navController.popBackStack() }) }
        composable<PreferencesRoute.NestedRoute2> { NestedPreferencesScreen2(onNavigateBack = { navController.popBackStack() }) }
        composable<PreferencesRoute.NestedRoute3> { NestedPreferencesScreen3(onNavigateBack = { navController.popBackStack() }) }

        composable<FolderDetailsRoute.NestedRoute1> { NestedFolderDetailsScreen1(onNavigateBack = { navController.popBackStack() }) }
        composable<FolderDetailsRoute.NestedRoute2> { NestedFolderDetailsScreen2(onNavigateBack = { navController.popBackStack() }) }
        composable<FolderDetailsRoute.NestedRoute3> { NestedFolderDetailsScreen3(onNavigateBack = { navController.popBackStack() }) }
    }
}
