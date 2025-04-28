package browserpicker.presentation.folders

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import browserpicker.presentation.folders.*
import browserpicker.presentation.navigation.Rules // Example navigation
import timber.log.Timber

// Initial Type is passed as a navigation argument
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter") // Scaffold handles padding
@Composable
fun FoldersScreen(
    viewModel: FoldersViewModel = hiltViewModel(),
    initialType: FolderType // Parameter from navigation
    // Add navigation callbacks if needed, e.g.:
    // onNavigateToRules: (UriStatus) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current // Needed for potentially launching intents or accessing resources

    // Set initial type from navigation argument
    LaunchedEffect(initialType) {
        viewModel.setFolderType(initialType)
    }

    // Show user messages from ViewModel
    LaunchedEffect(state.userMessages) {
        state.userMessages.firstOrNull()?.let { userMessage ->
            snackbarHostState.showSnackbar(
                message = userMessage.message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessage(userMessage.id) // Consume the message
        }
    }

    // Manage Delete Confirmation Dialog State
    var folderToDelete: Folder? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${state.currentType.name} Folders") }
                // TODO: Add actions like switching folder type if desired
                // actions = {
                //     IconButton(onClick = { viewModel.setFolderType(FolderType.BOOKMARK) }) { /* Icon */ }
                //     IconButton(onClick = { viewModel.setFolderType(FolderType.BLOCK) }) { /* Icon */ }
                // }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddFolderDialog(parentFolderId = null) } // Add root folder via FAB
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Folder")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (state.isLoading) {
                // TODO: Show a full screen loading indicator if initial load is slow
                // Currently, flows handle updates, isLoading state might be redundant
                // Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                //     CircularProgressIndicator()
                // }
            } else if (state.folderTree.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No folders found for ${state.currentType.name}", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                // Flatten the tree for LazyColumn
                val flattenedTree = remember(state.folderTree) {
                    state.folderTree.flatMap { rootNode -> flattenTreeNode(rootNode) }
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = flattenedTree,
                        key = { it.folder.id } // Use folder ID as key
                    ) { node ->
                        FolderTreeItem(
                            node = node,
                            // onFolderClick = { clickedFolder -> /* Handle navigation or state change */ },
                            onAddChildClick = { parentId -> viewModel.showAddFolderDialog(parentId) },
                            onEditClick = { folder -> viewModel.showEditFolderDialog(folder) },
                            onDeleteClick = { folder -> folderToDelete = folder } // Trigger confirmation dialog
                        )
                        Divider(Modifier.padding(start = (node.level * 16 + 16).dp)) // Indented divider
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    when (val dialogState = state.dialogState) {
        is FolderDialogState.Add -> {
            AddFolderDialog(
                parentFolderId = dialogState.parentFolderId,
                type = dialogState.type,
                onConfirm = { name, parentId ->
                    viewModel.createFolder(name, parentId)
                    // Dialog dismissed by VM after successful action
                },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is FolderDialogState.Edit -> {
            EditFolderDialog(
                folder = dialogState.folder,
                onConfirm = { folder, newName, newParentId ->
                    viewModel.updateFolder(folder, newName, newParentId)
                    // Dialog dismissed by VM after successful action
                },
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        FolderDialogState.Hidden -> { /* No dialog shown */ }
    }

    // --- Delete Confirmation Dialog ---
    folderToDelete?.let { folder ->
        DeleteConfirmationDialog(
            folderName = folder.name,
            onConfirm = {
                viewModel.deleteFolder(folder.id)
                folderToDelete = null // Dismiss dialog
            },
            onDismiss = { folderToDelete = null } // Dismiss dialog
        )
    }
}

// Helper function to flatten the FolderTreeNode hierarchy for LazyColumn
// Returns a flat list of nodes, including their depth
private fun flattenTreeNode(node: FolderTreeNode): List<FolderTreeNode> {
    val list = mutableListOf<FolderTreeNode>()
    list.add(node)
    node.children.forEach { child ->
        list.addAll(flattenTreeNode(child)) // Recursively add children and their descendants
    }
    return list
}

// Need a Use Case to get ALL folders of a specific type for tree building!
// This would require adding GetAllFoldersByTypeUseCase in the domain layer.
// For now, the ViewModel uses getFoldersUseCase(null, type) which only gets roots.
// The `buildFolderTree` helper in the ViewModel is also based on that limited data.
// CORRECT APPROACH:
// 1. Add `GetAllFoldersByTypeUseCase` in Domain (calls Repository.getAllFoldersByType)
// 2. Update `FoldersViewModel` to use `getAllFoldersByTypeUseCase`.
// 3. The `buildFolderTree` helper in the ViewModel should receive the FULL flat list from this new UseCase.
// 4. The `flattenTreeNode` helper here is correct for rendering the resulting tree structure.

// REVISED FoldersViewModel (Snippet)
/*
@HiltViewModel
class FoldersViewModel @Inject constructor(
    // ... other dependencies ...
    private val getAllFoldersByTypeUseCase: GetAllFoldersByTypeUseCase // <<-- NEW USE CASE
) : ViewModel() {
    // ... other state/flows ...
     @OptIn(ExperimentalCoroutinesApi::class)
     private val allFoldersFlow: Flow<List<Folder>> = _currentType
         .flatMapLatest { type ->
              getAllFoldersByTypeUseCase(type) // Use the new Use Case
         }
         .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)

     // init { ... observe combined flows ... }

     // Helper remains the same, but now receives the full list
     private fun buildFolderTree(folders: List<Folder>): List<FolderTreeNode> {
         val nodesById = folders.associateBy { it.id }
         return folders
             .filter { it.parentFolderId == null } // Start with roots
             .map { buildNodeRecursive(it, nodesById, 0) }
             .sortedBy { it.folder.name } // Sort roots
     }
     // ... buildNodeRecursive helper ...
}
*/
