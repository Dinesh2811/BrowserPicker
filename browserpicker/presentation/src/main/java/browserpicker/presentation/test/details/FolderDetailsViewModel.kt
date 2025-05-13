package browserpicker.presentation.test.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import browserpicker.domain.model.HostRule
import browserpicker.domain.usecases.folder.DeleteFolderUseCase
import browserpicker.domain.usecases.folder.GetChildFoldersUseCase
import browserpicker.domain.usecases.folder.GetFolderUseCase
import browserpicker.domain.usecases.folder.MoveHostRuleToFolderUseCase
import browserpicker.domain.usecases.folder.UpdateFolderUseCase
import browserpicker.domain.usecases.uri.host.DeleteHostRuleUseCase
import browserpicker.domain.usecases.uri.host.GetHostRulesByFolderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Folder Details screen.
 *
 * This ViewModel handles:
 * - Displaying folder information
 * - Showing child folders
 * - Showing host rules in the folder
 * - Updating folder properties
 * - Deleting the folder
 * - Moving host rules to different folders
 *
 * Used by: FolderDetailsScreen
 */
@HiltViewModel
class FolderDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getFolderUseCase: GetFolderUseCase,
    private val getChildFoldersUseCase: GetChildFoldersUseCase,
    private val getHostRulesByFolderUseCase: GetHostRulesByFolderUseCase,
    private val updateFolderUseCase: UpdateFolderUseCase,
    private val deleteFolderUseCase: DeleteFolderUseCase,
    private val deleteHostRuleUseCase: DeleteHostRuleUseCase,
    private val moveHostRuleToFolderUseCase: MoveHostRuleToFolderUseCase
) : ViewModel() {

    private val folderId: Long = savedStateHandle.get<Long>("folderId") ?: -1L
    private val folderType: Int = savedStateHandle.get<Int>("folderType") ?: FolderType.BOOKMARK.value

    private val _uiState = MutableStateFlow(FolderDetailsUiState())
    val uiState: StateFlow<FolderDetailsUiState> = _uiState.asStateFlow()

    // Current folder details
    val folder: Flow<Folder?> = flow {
        if (folderId != -1L) {
            getFolderUseCase(folderId)
                .collect { result ->
                    emit(result.getOrNull())
                }
        } else {
            emit(null)
        }
    }

    // Child folders
    val childFolders: Flow<List<Folder>> = flow {
        if (folderId != -1L) {
            getChildFoldersUseCase(folderId)
                .collect { result ->
                    emit(result.getOrNull() ?: emptyList())
                }
        } else {
            emit(emptyList())
        }
    }

    // Host rules in folder
    val hostRules: Flow<List<HostRule>> = flow {
        if (folderId != -1L) {
            getHostRulesByFolderUseCase(folderId)
                .collect { result ->
                    emit(result.getOrNull() ?: emptyList())
                }
        } else {
            emit(emptyList())
        }
    }

    init {
        if (folderId != -1L) {
            loadFolderInfo()
        } else {
            _uiState.value = _uiState.value.copy(
                error = "Invalid Folder ID"
            )
        }
    }

    /**
     * Load folder info
     */
    private fun loadFolderInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            folder.collect { folder ->
                if (folder != null) {
                    _uiState.value = _uiState.value.copy(
                        folder = folder,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Folder not found",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Update folder name
     */
    fun updateFolderName(name: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true)

            val folder = _uiState.value.folder ?: return@launch
            val updatedFolder = folder.copy(name = name)

            val result = updateFolderUseCase(updatedFolder)

            _uiState.value = _uiState.value.copy(
                isUpdating = false,
                updateSuccess = result.isSuccess,
                error = if (result.isFailure) "Failed to update folder" else null
            )
        }
    }

    /**
     * Delete the folder
     */
    fun deleteFolder(forceCascade: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)

            val result = deleteFolderUseCase(folderId, forceCascade)

            _uiState.value = _uiState.value.copy(
                isDeleting = false,
                isDeleted = result.isSuccess,
                error = if (result.isFailure) "Failed to delete folder" else null
            )
        }
    }

    /**
     * Delete a host rule
     */
    fun deleteHostRule(hostRuleId: Long) {
        viewModelScope.launch {
            val result = deleteHostRuleUseCase(hostRuleId)

            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete host rule"
                )
            }
        }
    }

    /**
     * Move a host rule to another folder
     */
    fun moveHostRule(hostRuleId: Long, targetFolderId: Long?) {
        viewModelScope.launch {
            val result = moveHostRuleToFolderUseCase(hostRuleId, targetFolderId)

            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to move host rule"
                )
            }
        }
    }
}

/**
 * UI state for the Folder Details screen
 */
data class FolderDetailsUiState(
    val folder: Folder? = null,
    val hasChildFolders: Boolean = false,
    val isUpdating: Boolean = false,
    val updateSuccess: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
