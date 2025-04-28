package browserpicker.presentation.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.domain.model.*
import browserpicker.domain.service.DomainError
import browserpicker.domain.usecase.*
import browserpicker.presentation.common.MessageType
import browserpicker.presentation.common.UserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val getFoldersUseCase: GetFoldersUseCase, // Use case to get folders
    private val createFolderUseCase: CreateFolderUseCase,
    private val updateFolderUseCase: UpdateFolderUseCase,
    private val deleteFolderUseCase: DeleteFolderUseCase,
    // TODO: Need Get *ALL* Folders use case if building tree from flat list
//    private val getAllFoldersByTypeUseCase: GetAllFoldersByTypeUseCase
    // Let's assume for now GetFoldersUseCase(null, type) gets all roots,
    // and we recursively fetch children. This is inefficient.
    // ---->> REVISION: Need a use case to get ALL folders of a type.
) : ViewModel() {

    // --- Temporary revision --- Add this dependency
    // private val getAllFoldersByTypeUseCase: GetAllFoldersByTypeUseCase

    private val _currentType = MutableStateFlow(FolderType.BOOKMARK)

    // Flow that fetches ALL folders of the current type
    @OptIn(ExperimentalCoroutinesApi::class)
    private val allFoldersFlow: Flow<List<Folder>> = _currentType
        .flatMapLatest { type ->
            // Replace with actual use case fetching *all* folders by type
            // For now, simulate with root fetching - THIS NEEDS FIXING
            getFoldersUseCase(parentFolderId = null, type = type)
            // .catch { emit(emptyList()) } // Add error handling
        }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)

    private val _uiState = MutableStateFlow(FoldersScreenState())
    val uiState: StateFlow<FoldersScreenState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                allFoldersFlow, // Fetch flat list
                _currentType
            ) { flatList, type ->
                // Build tree structure from flat list
                val tree = buildFolderTree(flatList)
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false, // Manage loading state more granularly if needed
                        folderTree = tree,
                        currentType = type,
                        // Preserve messages & dialog state
                        userMessages = currentState.userMessages,
                        dialogState = currentState.dialogState
                    )
                }
            }.collect()
        }
    }

    // Helper to build tree (can be moved to a separate utility)
    private fun buildFolderTree(folders: List<Folder>): List<FolderTreeNode> {
        val nodes = folders.associateBy { it.id }
        val treeNodes = mutableMapOf<Long, FolderTreeNode>()
        val rootNodes = mutableListOf<FolderTreeNode>()

        // Create all nodes first
        folders.forEach { folder ->
            treeNodes[folder.id] = FolderTreeNode(folder = folder)
        }

        // Build hierarchy
        folders.forEach { folder ->
            val node = treeNodes[folder.id]!!
            if (folder.parentFolderId == null) {
                rootNodes.add(node)
            } else {
                val parentNode = treeNodes[folder.parentFolderId]
                if (parentNode != null) {
                    // This approach creates duplicates if not careful.
                    // Need a better way: build children lists directly.
                }
            }
        }
        // --> Revision: Simpler recursive build needed
        val nodesById = folders.associateBy { it.id }
        return folders
            .filter { it.parentFolderId == null } // Start with roots
            .map { buildNodeRecursive(it, nodesById, 0) }
            .sortedBy { it.folder.name } // Sort roots
    }

    private fun buildNodeRecursive(
        folder: Folder,
        allNodes: Map<Long, Folder>,
        level: Int
    ): FolderTreeNode {
        val children = allNodes.values
            .filter { it.parentFolderId == folder.id }
            .map { buildNodeRecursive(it, allNodes, level + 1) }
            .sortedBy { it.folder.name } // Sort children
        return FolderTreeNode(folder = folder, children = children, level = level)
    }
    // --- End Tree Building ---

    fun setFolderType(type: FolderType) {
        _currentType.value = type
    }

    // --- Dialog Actions ---
    fun showAddFolderDialog(parentFolderId: Long?) {
        val type = _currentType.value
        _uiState.update { it.copy(dialogState = FolderDialogState.Add(parentFolderId, type)) }
    }

    fun showEditFolderDialog(folder: Folder) {
        _uiState.update { it.copy(dialogState = FolderDialogState.Edit(folder)) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialogState = FolderDialogState.Hidden) }
    }

    // --- CRUD Actions ---
    fun createFolder(name: String, parentFolderId: Long?) {
        val type = _currentType.value // Get type from current state
        viewModelScope.launch {
            createFolderUseCase(
                name = name,
                parentFolderId = parentFolderId,
                type = type,
                onSuccess = {
                    addMessage("Folder '$name' created.")
                    dismissDialog()
                    // Tree will rebuild via flow
                },
                onError = { error ->
                    handleDomainError("Failed to create folder", error)
                }
            )
        }
    }

    fun updateFolder(folder: Folder, newName: String, newParentId: Long?) {
        val folderToUpdate = folder.copy(name = newName, parentFolderId = newParentId)
        viewModelScope.launch {
            updateFolderUseCase(
                folder = folderToUpdate,
                onSuccess = {
                    addMessage("Folder '${folderToUpdate.name}' updated.")
                    dismissDialog()
                    // Tree rebuilds via flow
                },
                onError = { error ->
                    handleDomainError("Failed to update folder", error)
                }
            )
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            deleteFolderUseCase(
                folderId = folderId,
                onSuccess = {
                    addMessage("Folder deleted.")
                    // Tree rebuilds
                },
                onError = { error ->
                    handleDomainError("Failed to delete folder", error)
                }
            )
        }
    }

    // --- Message Handling --- (Same as HistoryViewModel)
    fun clearMessage(id: Long) {
        _uiState.update { state ->
            state.copy(userMessages = state.userMessages.filterNot { it.id == id })
        }
    }

    private fun addMessage(text: String, type: MessageType = MessageType.SUCCESS) {
        _uiState.update {
            it.copy(userMessages = it.userMessages + UserMessage(message = text, type = type))
        }
    }

    private fun handleDomainError(prefix: String, error: DomainError) {
        Timber.e("$prefix: $error")
        val message = when (error) {
            is DomainError.Validation -> "$prefix: ${error.message}"
            is DomainError.NotFound -> "$prefix: ${error.entityType} not found."
            is DomainError.Conflict -> "$prefix: ${error.message}"
            is DomainError.Database -> "$prefix: Database error."
            is DomainError.Unexpected -> "$prefix: Unexpected error."
            is DomainError.Custom -> "$prefix: ${error.message}"
        }
        addMessage(message, MessageType.ERROR)
    }
}
