package browserpicker.presentation.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import browserpicker.domain.model.HostRule
import browserpicker.domain.usecases.folder.GetChildFoldersUseCase
import browserpicker.domain.usecases.folder.GetFolderUseCase
import browserpicker.domain.usecases.folder.GetFolderHierarchyUseCase
import browserpicker.domain.usecases.uri.host.GetHostRulesByFolderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Folder Details screen.
 *
 * This ViewModel handles:
 * - Loading and displaying the details of a specific folder
 * - Displaying child folders within the current folder
 * - Displaying host rules within the current folder
 * - Navigating the folder hierarchy
 *
 * Used by: FolderDetailsScreen
 */
@HiltViewModel
class FolderDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFolderUseCase: GetFolderUseCase,
    private val getChildFoldersUseCase: GetChildFoldersUseCase,
    private val getHostRulesByFolderUseCase: GetHostRulesByFolderUseCase,
    private val getFolderHierarchyUseCase: GetFolderHierarchyUseCase
) : ViewModel() {

    private val folderId: Long = savedStateHandle.get<Long>("folderId") ?: -1L
    private val folderType: Int = savedStateHandle.get<Int>("folderType") ?: -1

    private val _uiState = MutableStateFlow(FolderDetailsUiState())
    val uiState: StateFlow<FolderDetailsUiState> = _uiState.asStateFlow()

    // Current folder details
    val currentFolder: Flow<Folder?> = getFolderUseCase(folderId)
        .map { result -> result.getOrNull() }
        .onEach { folder ->
            _uiState.value = _uiState.value.copy(currentFolder = folder)
        }

    // Child folders within the current folder
    val childFolders: Flow<List<Folder>> = getChildFoldersUseCase(folderId)
        .map { result -> result.getOrNull() ?: emptyList() }

    // Host rules within the current folder
    val hostRulesInFolder: Flow<List<HostRule>> = getHostRulesByFolderUseCase(folderId)
        .map { result -> result.getOrNull() ?: emptyList() }

    // Folder hierarchy (breadcrumb)
//    val folderHierarchy: List<Folder> = (getFolderHierarchyUseCase(folderId)).getOrNull()?: emptyList()

    init {
        if (folderId == -1L || FolderType.fromValue(folderType) == FolderType.UNKNOWN) {
            _uiState.value = _uiState.value.copy(
                error = "Invalid Folder ID or Type"
            )
        }
        // Data loading is handled by the flows above
    }

    /**
     * Navigate to a child folder.
     * This would typically trigger a navigation event in the UI layer.
     */
    fun navigateToFolder(childFolderId: Long, childFolderType: FolderType) {
        // This function is primarily for intent in the ViewModel
        // The actual navigation would be triggered by observing childFolders in the UI
    }

    /**
     * Navigate back up the folder hierarchy.
     * This would typically trigger a navigation event in the UI layer.
     */
    fun navigateUpFolder() {
        // This function is primarily for intent in the ViewModel
        // The actual navigation would be triggered by observing folderHierarchy in the UI
    }
}

/**
 * UI state for the Folder Details screen
 */
data class FolderDetailsUiState(
    val currentFolder: Folder? = null,
    val isLoading: Boolean = false, // Consider using loading state from flows
    val error: String? = null
)
