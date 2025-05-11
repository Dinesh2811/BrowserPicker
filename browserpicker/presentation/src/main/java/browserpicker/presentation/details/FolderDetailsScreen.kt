package browserpicker.presentation.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import browserpicker.domain.model.HostRule
import browserpicker.presentation.common.components.EmptyStateView
import browserpicker.presentation.common.components.LoadingIndicator
import browserpicker.presentation.navigation.NavRoutes

/**
 * Folder Details Screen - Displays the contents of a specific folder.
 *
 * This screen displays:
 * - The name of the current folder in the top bar.
 * - A list of child folders within the current folder.
 * - A list of host rules (bookmarks or blocked URLs) within the current folder.
 * - Navigation back to the parent folder or the root list.
 *
 * Used by: BookmarksScreen, BlockedUrlsScreen, SearchScreen
 * Uses: FolderDetailsViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailsScreen(
    navController: androidx.navigation.NavController,
    folderId: Long,
    folderType: Int, // Pass as Int and convert in ViewModel if necessary, or pass FolderType if feasible
    viewModel: FolderDetailsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentFolder by viewModel.currentFolder.collectAsState(initial = null)
    val childFolders by viewModel.childFolders.collectAsState(initial = emptyList())
    val hostRules by viewModel.hostRulesInFolder.collectAsState(initial = emptyList())
    val folderHierarchy: List<Folder> = emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentFolder?.name ?: "Folder") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
                // Actions like Add Folder/Host Rule could go here
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LoadingIndicator(message = "Loading folder contents...")
            }
            uiState.error != null -> {
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
                        // Retry loading
                        viewModel.loadFolderContents() // Assuming a load function exists or trigger via state change
                    }) {
                        Text("Retry")
                    }
                }
            }
            childFolders.isEmpty() && hostRules.isEmpty() -> {
                // Empty state
                EmptyStateView(message = "This folder is empty")
            }
            else -> {
                // Main content: List of subfolders and host rules
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                ) {
                    // Folder hierarchy breadcrumb (optional but good UX)
                    item {
                        FolderHierarchyBreadcrumb(
                            hierarchy = folderHierarchy,
                            onFolderClick = { clickedFolderId ->
                                // Navigate to the clicked folder
                                // navController.navigate("${NavRoutes.FOLDER_DETAILS}/${clickedFolderId}/${folderType}")
                                // Alternatively, viewModel.selectFolder(clickedFolderId) if state-driven navigation
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Child Folders section
                    if (childFolders.isNotEmpty()) {
                        item {
                            Text("Folders", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(childFolders) { folder ->
                            FolderItem(
                                folder = folder,
                                onClick = {
                                    // Navigate to child folder details
                                    navController.navigate("${NavRoutes.FOLDER_DETAILS}/${folder.id}/${folder.type.value}")
                                }
                            )
                            Divider()
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    // Host Rules section
                    if (hostRules.isNotEmpty()) {
                        item {
                            Text("Items", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(hostRules) { hostRule ->
                            // Reuse BookmarkItem/BlockedUrlItem or create a generic HostRuleItem
                            HostRuleItem(
                                hostRule = hostRule,
                                onClick = {
                                    // Navigate to host rule details
                                    navController.navigate("${NavRoutes.HOST_RULE_DETAILS}/${hostRule.id}")
                                }
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Displays a clickable folder item in a list.
 *
 * @param folder The folder to display.
 * @param onClick Callback when the folder is clicked.
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
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Folder, contentDescription = "Folder", modifier = Modifier.padding(end = 16.dp))
        Text(folder.name, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Displays a clickable host rule item in a list.
 *
 * @param hostRule The host rule to display.
 * @param onClick Callback when the host rule is clicked.
 */
@Composable
private fun HostRuleItem(
    hostRule: HostRule,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // TODO: Add icon based on uriStatus (bookmark/blocked)
        Text(hostRule.host, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Displays the folder hierarchy as a breadcrumb trail.
 *
 * @param hierarchy The list of folders representing the path from root to the current folder.
 * @param onFolderClick Callback when a folder in the hierarchy is clicked.
 */
@Composable
private fun FolderHierarchyBreadcrumb(
    hierarchy: List<Folder>,
    onFolderClick: (folderId: Long) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        hierarchy.forEachIndexed { index, folder ->
            Text(
                folder.name,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable { onFolderClick(folder.id) },
                color = if (index == hierarchy.lastIndex) MaterialTheme.colorScheme.primary else LocalContentColor.current.copy(alpha = ContentAlpha.medium)
            )
            if (index < hierarchy.lastIndex) {
                Text(">")
            }
        }
    }
}

// Dummy function to simulate loading, replace with actual ViewModel logic call
private fun FolderDetailsViewModel.loadFolderContents() {
    // This would trigger the flows in the ViewModel
}
