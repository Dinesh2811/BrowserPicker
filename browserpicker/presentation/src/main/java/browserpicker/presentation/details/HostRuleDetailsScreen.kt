package browserpicker.presentation.details

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import browserpicker.domain.model.BrowserAppInfo
import browserpicker.domain.model.UriStatus
import browserpicker.presentation.common.components.LoadingIndicator

/**
 * Host Rule Details Screen - Shows details of a host rule.
 *
 * This screen displays:
 * - Host information
 * - Current status (bookmarked/blocked)
 * - Preferred browser setting
 * - Folder assignment
 * - Options to update these settings
 *
 * It provides detailed control over how a specific host is handled
 * in the app, including which browser it should open in.
 *
 * Uses: HostRuleDetailsViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostRuleDetailsScreen(
    navController: androidx.navigation.NavController,
    hostRuleId: Long,
    viewModel: HostRuleDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableBrowsers by viewModel.availableBrowsers.collectAsState(initial = emptyList())

    var showStatusDialog by remember { mutableStateOf(false) }
    var showBrowserDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.hostRule?.host ?: "Host Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingIndicator(message = "Loading host details...")
        } else if (uiState.error != null) {
            // Error view
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Error: ${uiState.error}")
                Button(onClick = {
                    navController.navigateUp()
                }) {
                    Text("Go Back")
                }
            }
        } else {
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Host info card
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Host Information",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Host: ${uiState.hostRule?.host ?: "Unknown"}",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            "Status: ${uiState.uriStatus.name}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (uiState.folderId != null) {
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                "Folder ID: ${uiState.folderId}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (uiState.preferredBrowser != null) {
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                "Preferred Browser: ${uiState.preferredBrowser?.appName ?: "Unknown"}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Text(
                                "Preference ${if (uiState.isPreferenceEnabled) "Enabled" else "Disabled"}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Status section
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Status",
                                style = MaterialTheme.typography.titleMedium
                            )

                            IconButton(onClick = { showStatusDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Status")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            when (uiState.uriStatus) {
                                UriStatus.BOOKMARKED -> "This host is bookmarked"
                                UriStatus.BLOCKED -> "This host is blocked"
                                else -> "No special status"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Preferred browser section
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Preferred Browser",
                                style = MaterialTheme.typography.titleMedium
                            )

                            IconButton(onClick = { showBrowserDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Browser")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (uiState.preferredBrowser != null) {
                            Text(
                                "URLs from this host will ${if (uiState.isPreferenceEnabled) "" else "not "}automatically open in ${uiState.preferredBrowser?.appName}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(
                                "No preferred browser set",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Folder section
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Folder",
                                style = MaterialTheme.typography.titleMedium
                            )

                            IconButton(onClick = { showFolderDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Change Folder")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (uiState.folderId != null) {
                            Text(
                                "This host is in folder ID: ${uiState.folderId}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(
                                "This host is not in any folder",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    // Status dialog
    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Update Status") },
            text = {
                Column {
                    Text("Select a status for this host:")

                    Spacer(modifier = Modifier.height(16.dp))

                    // Status options
                    UriStatus.entries.filter { it != UriStatus.UNKNOWN }.forEach { status ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.uriStatus == status,
                                onClick = {
                                    viewModel.updateStatus(status)
                                    showStatusDialog = false
                                }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(status.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusDialog = false }) {
                    Text("Cancel")
                }
            },
            dismissButton = null
        )
    }

    // Browser dialog
    if (showBrowserDialog) {
        AlertDialog(
            onDismissRequest = { showBrowserDialog = false },
            title = { Text("Set Preferred Browser") },
            text = {
                Column {
                    Text("Select a preferred browser for this host:")

                    Spacer(modifier = Modifier.height(16.dp))

                    // Browser options
                    availableBrowsers.forEach { browser ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.preferredBrowserPackage == browser.packageName,
                                onClick = {
                                    viewModel.setPreferredBrowser(browser.packageName, true)
                                    showBrowserDialog = false
                                }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(browser.appName)
                        }
                    }

                    // None option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.preferredBrowserPackage == null,
                            onClick = {
                                viewModel.setPreferredBrowser(null)
                                showBrowserDialog = false
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text("None (Always show picker)")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Enable/disable preference
                    if (uiState.preferredBrowserPackage != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = uiState.isPreferenceEnabled,
                                onCheckedChange = { isEnabled ->
                                    viewModel.setPreferredBrowser(
                                        uiState.preferredBrowserPackage,
                                        isEnabled
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text("Enable automatic opening")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBrowserDialog = false }) {
                    Text("Cancel")
                }
            },
            dismissButton = null
        )
    }

    // Folder dialog
    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Change Folder") },
            text = {
                Text("Folder selection would go here")
                // This would be implemented with a folder picker
            },
            confirmButton = {
                TextButton(onClick = {
                    // Move to selected folder
                    showFolderDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}