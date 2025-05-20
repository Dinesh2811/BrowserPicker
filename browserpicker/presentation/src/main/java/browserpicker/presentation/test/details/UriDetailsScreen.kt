package browserpicker.presentation.test.details

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import browserpicker.presentation.test.common.components.LoadingIndicator
import browserpicker.presentation.test.navigation.BrowserPickerRoute
import browserpicker.presentation.test.navigation.HostRuleDetailsRoute

/**
 * URI Details Screen - Shows details of a specific URI record.
 *
 * This screen displays:
 * - The full URI
 * - Timestamp of when it was processed
 * - Source of the URI (intent, clipboard, etc.)
 * - Action taken (opened, blocked, etc.)
 * - Browser that was used (if any)
 * - Options to bookmark/block the host
 * - Option to reprocess the URI
 * - Option to delete the record
 *
 * Uses: UriDetailsViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UriDetailsScreen(
    navController: androidx.navigation.NavController,
    uriRecordId: Long,
    viewModel: UriDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showReprocessConfirmDialog by remember { mutableStateOf(false) }

    // Effect to handle deletion
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            // Navigate back after deletion
            navController.navigateUp()
        }
    }

    LaunchedEffect(uiState.reprocessingComplete) {
        if (uiState.reprocessingComplete) {
            // Navigate to browser picker with this URI using type-safe navigation
            uiState.uriRecord?.uriString?.let { uri ->
                navController.navigate(BrowserPickerRoute(uriString = uri)) // Use the serializable route object
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("URI Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Delete action
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }

                    // Reprocess action
                    IconButton(onClick = { showReprocessConfirmDialog = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reprocess")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingIndicator(message = "Loading URI details...")
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
                Button(onClick = { navController.navigateUp() }) {
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
                // URI info card
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "URI Information",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        uiState.uriRecord?.let { record ->
                            // URI
                            Text(
                                "URI: ${record.uriString}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Host
                            Text(
                                "Host: ${record.host}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Timestamp
                            Text(
                                "Time: ${record.timestamp}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Source
                            Text(
                                "Source: ${formatUriSource(record.uriSource)}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Action
                            Text(
                                "Action: ${formatAction(record.interactionAction)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Browser info card
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Browser Information",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (uiState.browserInfo != null) {
                            // Browser name
                            Text(
                                "Browser: ${uiState.browserInfo?.appName}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Package name
                            Text(
                                "Package: ${uiState.browserInfo?.packageName}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(
                                "No browser was used for this URI",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Host rule actions card
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Host Actions",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Bookmark button
                            Button(
                                onClick = { viewModel.toggleBookmark() }
                            ) {
                                Text(if (uiState.isBookmarked) "Unbookmark" else "Bookmark")
                            }

                            // Block button
                            Button(
                                onClick = { viewModel.toggleBlock() }
                            ) {
                                Text(if (uiState.isBlocked) "Unblock" else "Block")
                            }

                            // View host details button
                            uiState.hostRuleId?.let { hostRuleId ->
                                Button(
                                    onClick = {
                                        // Navigate to host rule details using type-safe navigation
                                        navController.navigate(HostRuleDetailsRoute(hostRuleId = hostRuleId)) // Use the serializable route object
                                    }
                                ) {
                                    Text("Host Details")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete URI Record") },
            text = { Text("Are you sure you want to delete this URI record? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecord()
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reprocess confirmation dialog
    if (showReprocessConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showReprocessConfirmDialog = false },
            title = { Text("Reprocess URI") },
            text = { Text("Do you want to reprocess this URI? This will open the browser picker again.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.reprocessUri()
                        showReprocessConfirmDialog = false
                    }
                ) {
                    Text("Reprocess")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReprocessConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Format URI source for display
 */
@Composable
private fun formatUriSource(source: UriSource): String {
    return when (source) {
        UriSource.INTENT -> "App Intent"
        UriSource.CLIPBOARD -> "Clipboard"
        UriSource.MANUAL -> "Manual Entry"
        else -> "Unknown"
    }
}

/**
 * Format interaction action for display
 */
@Composable
private fun formatAction(action: InteractionAction): String {
    return when (action) {
        InteractionAction.OPENED_BY_PREFERENCE -> "OPENED_BY_PREFERENCE"
        InteractionAction.OPENED_ONCE -> "OPENED_ONCE"
//        InteractionAction.PREFERENCE_SET -> "PREFERENCE_SET"
        InteractionAction.BLOCKED_URI_ENFORCED -> "BLOCKED_URI_ENFORCED"
        InteractionAction.DISMISSED -> "DISMISSED"
        else -> "Unknown"
    }
}
