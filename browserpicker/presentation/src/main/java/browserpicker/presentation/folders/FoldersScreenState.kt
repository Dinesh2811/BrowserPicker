package browserpicker.presentation.folders

import androidx.compose.runtime.Immutable
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import browserpicker.presentation.common.LoadingStatus
import browserpicker.presentation.common.UserMessage

@Immutable
data class FoldersScreenState(
    val isLoading: Boolean = false,
    val folderTree: List<FolderTreeNode> = emptyList(), // Root nodes of the tree
    val currentType: FolderType = FolderType.BOOKMARK,
    val userMessages: List<UserMessage> = emptyList(),
    val dialogState: FolderDialogState = FolderDialogState.Hidden
)

// Represents a node in the folder hierarchy for UI display
@Immutable
data class FolderTreeNode(
    val folder: Folder,
    val children: List<FolderTreeNode> = emptyList(),
    val level: Int = 0 // Indentation level
)

// Represents the state of the Add/Edit Folder dialog
@Immutable
sealed interface FolderDialogState {
    data object Hidden : FolderDialogState
    // Pass parent ID and current type for context
    data class Add(val parentFolderId: Long?, val type: FolderType) : FolderDialogState
    data class Edit(val folder: Folder) : FolderDialogState
    // Add Move dialog state later if needed
}
