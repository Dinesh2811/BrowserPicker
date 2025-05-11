package browserpicker.presentation.main

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Home screen content.
 *
 * This screen shows:
 * - App status (default browser)
 * - Quick actions
 * - Recently handled URIs
 * - Browser stats summary
 */
@Composable
fun HomeScreen(
    navController: androidx.navigation.NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    // Home screen implementation will go here
    // This is the landing screen showing app status, quick actions, and recent activities
}
