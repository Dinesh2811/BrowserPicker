package browserpicker.presentation.settings

// package browserpicker.presentation.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import browserpicker.presentation.common.MessageType
import browserpicker.presentation.common.UserMessage
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter") // Content padding applied to Column
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
    // snackbarHostState and coroutineScope might be passed from MainScreen Scaffold if using global Snackbar
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() } // Local Snackbar Host State

    LaunchedEffect(state.userMessages) {
        state.userMessages.firstOrNull()?.let { message ->
            snackbarHostState.showSnackbar(
                message = message.message,
                duration = SnackbarDuration.Short // Or Long, Indefinite
            )
            viewModel.clearMessage(message.id) // Consume the message
        }
    }

    // We need a SnackbarHost state. It can be local to this screen, or managed by MainScreen.
    // Let's use MainScreen's global one via the ViewModel's message list.

    // Observe state.userMessages in MainScreen and show Snackbar there.

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
            .padding(16.dp) // Additional padding for content
        ) {
            // --- General Settings Section ---
            Text("General", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
            Divider()

            SettingsItem(
                title = "Set Default Browser",
                description = "Open system settings to choose the default browser app."
            ) {
                Toast.makeText(context, "ACTION_MANAGE_DEFAULT_APPS", Toast.LENGTH_SHORT).show()
                // Action: Open system default apps settings
//                val intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS)
//                // Or Settings.ACTION_HOME_SETTINGS for home app, need correct action for browser
//                // The correct action is often found via documentation or trial/error on different Android versions
//                // A common way is ACTION_MANAGE_APPLICATIONS_SETTINGS or ACTION_APPLICATION_DETAILS_SETTINGS
//                // For default browser specifically: ACTION_MANAGE_DEFAULT_APPS might lead there, or user navigates.
//                // Let's use a generic one that might be close or requires user navigation.
//                // val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS) // Lists all apps
//                // ACTION_MANAGE_DEFAULT_APPS is better if available and leads to default apps section.
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required if launching from non-Activity context
//                runCatching { context.startActivity(intent) }
//                    .onFailure { viewModel.addMessage("Could not open settings.", MessageType.ERROR) }
            }
            Divider()

            // --- Data Management Section ---
            Spacer(modifier = Modifier.height(16.dp))
            Text("Data Management", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
            Divider()

            SettingsItem(
                title = "Clear URI History",
                description = "Delete all records of intercepted URIs and interactions."
            ) {
                viewModel.onClearHistoryClick()
            }
            Divider()

            SettingsItem(
                title = "Clear Browser Usage Stats",
                description = "Reset all recorded browser usage counts and last used times."
            ) {
                viewModel.onClearStatsClick()
            }
            Divider()

            // Add more settings sections/items here
            // Spacer(modifier = Modifier.height(16.dp))
            // Text("About", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
            // Divider()
            // SettingsItem(title = "App Version", description = "1.0.0") {} // Read from build config
            // Divider()
        }
    }

    // --- Confirmation Dialogs ---
    when (state.dialogState) {
        SettingsDialogState.ShowClearHistoryConfirmation -> {
            ConfirmActionDialog(
                title = "Confirm Clear History",
                text = "Are you sure you want to permanently delete all URI history records? This action cannot be undone.",
                confirmButtonText = "Clear History",
                dismissButtonText = "Cancel",
                onConfirm = { viewModel.onConfirmClearHistory() },
                onDismiss = { viewModel.onCancelDialog() }
            )
        }
        SettingsDialogState.ShowClearStatsConfirmation -> {
            ConfirmActionDialog(
                title = "Confirm Clear Stats",
                text = "Are you sure you want to permanently delete all browser usage statistics? This action cannot be undone.",
                confirmButtonText = "Clear Stats",
                dismissButtonText = "Cancel",
                onConfirm = { viewModel.onConfirmClearStats() },
                onDismiss = { viewModel.onCancelDialog() }
            )
        }
        SettingsDialogState.Hidden -> {
            // No dialog shown
        }
    }

    // Note: Loading indicator could be shown overlaying the screen based on state.isLoading
    if (state.isLoading) {
        // Dialog covering content or a progress indicator
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    description: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 0.dp), // Padding handled by parent column
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier
            .weight(1f)
            .padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null // Decorative
        )
    }
}

@Composable
fun ConfirmActionDialog(
    title: String,
    text: String,
    confirmButtonText: String,
    dismissButtonText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissButtonText)
            }
        }
    )
}

// Placeholder for Settings destination in AppNavigation (already added in Segment 6)
/*
private fun NavGraphBuilder.addSettingsDestination(navController: NavHostController) {
     composable<Settings> {
         SettingsScreen(
             viewModel = hiltViewModel()
         )
     }
}
*/
