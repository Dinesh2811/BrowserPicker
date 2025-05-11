package browserpicker.presentation.blockedurls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import browserpicker.domain.model.Folder
import browserpicker.domain.model.HostRule
import browserpicker.presentation.common.components.EmptyStateView
import browserpicker.presentation.common.components.LoadingIndicator
import browserpicker.presentation.navigation.NavRoutes

/**
 * Blocked URLs Screen - Shows blocked hosts with folder organization.
 * 
 * This screen displays:
 * - Folder structure for blocked URLs
 * - List of blocked hosts in the current folder
 * - Options to create new folders
 * - Options to move blocked URLs between folders
 * 
 * It provides an organizational system for the user's blocked hosts,
 * allowing them to maintain a structured collection of sites they want to block.
 * 
 * Uses: BlockedUrlsViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUrlsScreen(
    navController: androidx.navigation.NavController,
    viewModel: BlockedUrlsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentFolder by viewModel.currentFolder.collectAsState(initial = null)
    val childFolders by viewModel.childFolders.collectAsState(initial = emptyList())
    val blockedUrls by viewModel.blockedUrlsInFolder.collectAsState(initial = emptyList())
    
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentFolder?.name ?: "Blocked URLs") },
                navigationIcon = {
                    if (currentFolder != null) {
                        IconButton(onClick = { 
                            // Navigate to parent folder or root
                            viewModel.selectFolder(currentFolder?.parentFolderId)
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateFolderDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Folder")
                    }
                }
            )
        },
        floatingActionButton = {
            // FAB for creating a new blocked URL entry could go here
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingIndicator(message = "Loading blocked URLs...")
        } else if (uiState.error != null) {
            // Error view
            Text("Error: ${uiState.error}")
        } else if (childFolders.isEmpty() && blockedUrls.isEmpty()) {
            // Empty state
            EmptyStateView(message = "No blocked URLs yet")
        } else {
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // Folders section
                if (childFolders.isNotEmpty()) {
                    Text(
                        "Folders",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Folder list
                    Column {
                        childFolders.forEach { folder ->
                            FolderItem(
                                folder = folder,
                                onClick = {
                                    // Navigate to folder
                                    viewModel.selectFolder(folder.id)
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Blocked URLs section
                if (blockedUrls.isNotEmpty()) {
                    Text(
                        "Blocked URLs",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Blocked URL list
                    Column {
                        blockedUrls.forEach { hostRule ->
                            BlockedUrlItem(
                                hostRule = hostRule,
                                onClick = {
                                    // Navigate to host rule details
                                    navController.navigate("${NavRoutes.HOST_RULE_DETAILS}/${hostRule.id}")
                                },
                                onRemove = {
                                    viewModel.removeBlockedUrl(hostRule.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Create folder dialog
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onCreateFolder = { folderName ->
                viewModel.createFolder(folderName)
                showCreateFolderDialog = false
            }
        )
    }
}

/**
 * Dialog to create a new folder
 */
@Composable
private fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreateFolder: (String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Folder") },
        text = {
            TextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Folder Name") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreateFolder(folderName) },
                enabled = folderName.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Display a folder item
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
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(folder.name)
    }
}

/**
 * Display a blocked URL item
 */
@Composable
private fun BlockedUrlItem(
    hostRule: HostRule,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(hostRule.host)
        IconButton(onClick = onRemove) {
            // Delete icon
        }
    }
} 