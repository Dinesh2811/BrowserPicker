package browserpicker.presentation.test.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Settings Screen - App configuration and preferences.
 * 
 * This screen displays:
 * - Default browser status and control
 * - Data backup and restore options
 * - History cleaning options
 * - App information and help
 * - General preferences
 * 
 * It allows users to configure the behavior of the app and
 * manage their data.
 * 
 * Uses: SettingsViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: androidx.navigation.NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {

}

/*
package browserpicker.presentation.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Settings Screen - App configuration and preferences.
 *
 * This screen displays:
 * - Default browser status and control
 * - Data backup and restore options
 * - History cleaning options
 * - App information and help
 * - General preferences
 *
 * It allows users to configure the behavior of the app and
 * manage their data.
 *
 * Uses: SettingsViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: androidx.navigation.NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showCleanupDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    // File pickers
    val backupFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            // Convert URI to file path and perform backup
            // viewModel.backupData(filePath, includeHistory = true)
        }
    }

    val restoreFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Convert URI to file path and perform restore
            // viewModel.restoreData(filePath, clearExistingData = false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Default browser section
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Default Browser",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        if (uiState.isDefaultBrowser) "This app is set as your default browser"
                        else "This app is not your default browser",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (uiState.isDefaultBrowser) {
                                viewModel.openBrowserPreferences()
                            } else {
                                viewModel.setAsDefaultBrowser()
                            }
                        }
                    ) {
                        Text(
                            if (uiState.isDefaultBrowser) "Change Default Browser"
                            else "Set as Default Browser"
                        )
                    }
                }
            }

            // Data management section
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Data Management",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Backup button
                        Button(
                            onClick = { showBackupDialog = true }
                        ) {
                            Text("Backup Data")
                        }

                        // Restore button
                        Button(
                            onClick = { showRestoreDialog = true }
                        ) {
                            Text("Restore Data")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Cleanup history button
                        Button(
                            onClick = { showCleanupDialog = true }
                        ) {
                            Text("Clean History")
                        }

                        // Clear history button
                        Button(
                            onClick = { showClearHistoryDialog = true }
                        ) {
                            Text("Clear All History")
                        }
                    }

                    // Status message for operations
                    if (uiState.lastOperation != null) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            uiState.lastOperation?: "No operation performed yet",
                            color = if (uiState.lastOperationSuccess)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // App information section
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "About",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Browser Picker v1.0",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    // Backup dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup Data") },
            text = { Text("Do you want to include URI history in the backup?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackupDialog = false
                        backupFilePicker.launch("browser_picker_backup.json")
                    }
                ) {
                    Text("Include History")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBackupDialog = false
                        // Backup without history
                    }
                ) {
                    Text("Exclude History")
                }
            }
        )
    }

    // Restore dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Data") },
            text = { Text("Do you want to clear existing data before restoring?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        restoreFilePicker.launch(arrayOf("application/json"))
                    }
                ) {
                    Text("Clear & Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        restoreFilePicker.launch(arrayOf("application/json"))
                    }
                ) {
                    Text("Merge")
                }
            }
        )
    }

    // Cleanup dialog
    if (showCleanupDialog) {
        var cleanupDays by remember { mutableStateOf("30") }

        AlertDialog(
            onDismissRequest = { showCleanupDialog = false },
            title = { Text("Clean Up History") },
            text = {
                Column {
                    Text("Remove URI records older than:")

                    TextField(
                        value = cleanupDays,
                        onValueChange = { cleanupDays = it },
                        label = { Text("Days") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cleanupHistory(cleanupDays.toIntOrNull() ?: 30)
                        showCleanupDialog = false
                    }
                ) {
                    Text("Clean")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clear history dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear All History") },
            text = { Text("Are you sure you want to delete all URI history? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearHistoryDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
 */