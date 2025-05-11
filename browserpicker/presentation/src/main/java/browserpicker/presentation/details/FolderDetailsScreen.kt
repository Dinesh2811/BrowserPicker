package browserpicker.presentation.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriStatus
import browserpicker.presentation.common.components.EmptyStateView
import browserpicker.presentation.common.components.LoadingIndicator
import browserpicker.presentation.navigation.NavRoutes

/**
 * Folder Details Screen - Shows details and contents of a folder.
 *
 * This screen displays:
 * - Folder name and type
 * - Child folders within this folder
 * - Host rules contained in the folder
 * - Options to edit folder properties
 * - Options to delete the folder
 * - Options to move items to different folders
 *
 * Uses: FolderDetailsViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailsScreen(
    navController: androidx.navigation.NavController,
    folderId: Long,
    folderType: Int,
    viewModel: FolderDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val folder by viewModel.folder.collectAsState(initial = null)
    val childFolders by viewModel.childFolders.collectAsState(initial = emptyList())
    val hostRules by viewModel.hostRules.collectAsState(initial = emptyList())

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showDeleteFolderDialog by remember { mutableStateOf(false) }
    var showMoveItemDialog by remember { mutableStateOf<Long?>(null) } // Host rule ID or null

    // Effect to handle deletion
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            // Navigate back after deletion
            navController.navigateUp()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folder?.name ?: "Folder Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Edit action
                    IconButton(onClick = { showEditNameDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }

                    // Delete action
                    IconButton(onClick = { showDeleteFolderDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        },
        floatingActionButton = {
            if (folder != null) {
                FloatingActionButton(
                    onClick = {
                        // Create new item in folder logic would go here
                        // This could open a dialog to create a new folder or add a host rule
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingIndicator(message = "Loading folder details...")
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
        } else if (folder == null) {
            // Folder not found
            EmptyStateView(message = "Folder not found")
        } else {
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Folder info card
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Folder Information",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Name: ${folder?.name}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            "Type: ${formatFolderType(FolderType.fromValue(folderType))}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            "Items: ${childFolders.size + hostRules.size}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Child folders section
                if (childFolders.isNotEmpty()) {
                    Text(
                        "Sub-Folders",
                        style = MaterialTheme.typography.titleMedium
                    )

                    childFolders.forEach { childFolder ->
                        FolderItem(
                            folder = childFolder,
                            onClick = {
                                navController.navigate("${NavRoutes.FOLDER_DETAILS}/${childFolder.id}/${childFolder.type.value}")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Host rules section
                if (hostRules.isNotEmpty()) {
                    Text(
                        "Items in Folder",
                        style = MaterialTheme.typography.titleMedium
                    )

                    hostRules.forEach { hostRule ->
                        HostRuleItem(
                            hostRule = hostRule,
                            onClick = {
                                navController.navigate("${NavRoutes.HOST_RULE_DETAILS}/${hostRule.id}")
                            },
                            onMove = { showMoveItemDialog = hostRule.id },
                            onDelete = { viewModel.deleteHostRule(hostRule.id) }
                        )
                    }
                }

                // Empty state if no items
                if (childFolders.isEmpty() && hostRules.isEmpty()) {
                    EmptyStateView(message = "This folder is empty")
                }
            }
        }
    }

    // Edit folder name dialog
    if (showEditNameDialog) {
        var newName by remember { mutableStateOf(folder?.name ?: "") }

        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Folder Name") },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Folder Name") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateFolderName(newName)
                        showEditNameDialog = false
                    },
                    enabled = newName.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete folder dialog
    if (showDeleteFolderDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteFolderDialog = false },
            title = { Text("Delete Folder") },
            text = {
                Text("Are you sure you want to delete this folder? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFolder(forceCascade = true)
                        showDeleteFolderDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Move item dialog
    showMoveItemDialog?.let { hostRuleId ->
        AlertDialog(
            onDismissRequest = { showMoveItemDialog = null },
            title = { Text("Move Item") },
            text = {
                Text("Move item functionality would be implemented here")
                // This would include a folder picker component
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // TODO: Implement moving to selected folder
                        showMoveItemDialog = null
                    }
                ) {
                    Text("Move")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveItemDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Folder item component
 */
@Composable
private fun FolderItem(
    folder: Folder,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = "Folder",
            modifier = Modifier.padding(end = 16.dp)
        )

        Text(
            folder.name,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Host rule item component
 */
@Composable
private fun HostRuleItem(
    hostRule: HostRule,
    onClick: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon based on status
        val icon = when(hostRule.uriStatus) {
            UriStatus.BLOCKED -> Icons.Default.Block
            UriStatus.BOOKMARKED -> Icons.Default.Star
            else -> Icons.Default.ArrowForward
        }

        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 16.dp)
        )

        // Host info
        Text(
            hostRule.host,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        // Actions
        IconButton(onClick = onMove) {
            Icon(Icons.Default.FolderOpen, contentDescription = "Move")
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}

/**
 * Format folder type for display
 */
@Composable
private fun formatFolderType(type: FolderType): String {
    return when (type) {
        FolderType.BOOKMARK -> "Bookmarks"
        FolderType.BLOCK -> "Blocked URLs"
        else -> "Unknown"
    }
}
