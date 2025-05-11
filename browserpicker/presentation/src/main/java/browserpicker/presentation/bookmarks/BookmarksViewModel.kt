package browserpicker.presentation.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriStatus
import browserpicker.domain.usecases.folder.CreateFolderUseCase
import browserpicker.domain.usecases.folder.DeleteFolderUseCase
import browserpicker.domain.usecases.folder.GetChildFoldersUseCase
import browserpicker.domain.usecases.folder.GetFolderUseCase
import browserpicker.domain.usecases.folder.GetRootFoldersUseCase
import browserpicker.domain.usecases.folder.MoveHostRuleToFolderUseCase
import browserpicker.domain.usecases.folder.UpdateFolderUseCase
import browserpicker.domain.usecases.uri.host.DeleteHostRuleUseCase
import browserpicker.domain.usecases.uri.host.GetHostRulesByFolderUseCase
import browserpicker.domain.usecases.uri.host.GetHostRulesByStatusUseCase
import browserpicker.domain.usecases.uri.host.SaveHostRuleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Bookmarks screen.
 * 
 * This ViewModel handles:
 * - Displaying bookmarked URIs in a folder structure
 * - Creating, updating, and deleting folders
 * - Moving bookmarks between folders
 * - Adding and removing bookmarks
 * 
 * Used by: BookmarksScreen, FolderDetailsScreen
 */
@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val getRootFoldersUseCase: GetRootFoldersUseCase,
    private val getChildFoldersUseCase: GetChildFoldersUseCase,
    private val getFolderUseCase: GetFolderUseCase,
    private val createFolderUseCase: CreateFolderUseCase,
    private val updateFolderUseCase: UpdateFolderUseCase,
    private val deleteFolderUseCase: DeleteFolderUseCase,
    private val getHostRulesByStatusUseCase: GetHostRulesByStatusUseCase,
    private val getHostRulesByFolderUseCase: GetHostRulesByFolderUseCase,
    private val saveHostRuleUseCase: SaveHostRuleUseCase,
    private val deleteHostRuleUseCase: DeleteHostRuleUseCase,
    private val moveHostRuleToFolderUseCase: MoveHostRuleToFolderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookmarksUiState())
    val uiState: StateFlow<BookmarksUiState> = _uiState.asStateFlow()
    
    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    
    // Current folder details
    val currentFolder: Flow<Folder?> = _selectedFolderId
        .flatMapLatest { folderId ->
            if (folderId == null) {
                flowOf(null)
            } else {
                getFolderUseCase(folderId)
                    .map { result -> result.getOrNull() }
            }
        }
    
    // Root folders
    val rootFolders: Flow<List<Folder>> = getRootFoldersUseCase(FolderType.BOOKMARK)
        .map { result -> result.getOrNull() ?: emptyList() }
    
    // Child folders for the selected folder
    val childFolders: Flow<List<Folder>> = _selectedFolderId
        .flatMapLatest { folderId ->
            if (folderId == null) {
                rootFolders
            } else {
                getChildFoldersUseCase(folderId)
                    .map { result -> result.getOrNull() ?: emptyList() }
            }
        }
    
    // Bookmarks in the selected folder
    val bookmarksInFolder: Flow<List<HostRule>> = _selectedFolderId
        .flatMapLatest { folderId ->
            if (folderId == null) {
                getHostRulesByStatusUseCase(UriStatus.BOOKMARKED)
                    .map { result -> result.getOrNull() ?: emptyList() }
            } else {
                getHostRulesByFolderUseCase(folderId)
                    .map { result -> result.getOrNull() ?: emptyList() }
            }
        }
    
    /**
     * Select a folder to view
     */
    fun selectFolder(folderId: Long?) {
        _selectedFolderId.value = folderId
    }
    
    /**
     * Create a new folder
     */
    fun createFolder(name: String, parentFolderId: Long? = _selectedFolderId.value) {
        viewModelScope.launch {
            createFolderUseCase(name, parentFolderId, FolderType.BOOKMARK)
        }
    }
    
    /**
     * Update a folder
     */
    fun updateFolder(folder: Folder) {
        viewModelScope.launch {
            updateFolderUseCase(folder)
        }
    }
    
    /**
     * Delete a folder
     */
    fun deleteFolder(folderId: Long, forceCascade: Boolean = false) {
        viewModelScope.launch {
            deleteFolderUseCase(folderId, forceCascade)
        }
    }
    
    /**
     * Move a bookmark to a different folder
     */
    fun moveBookmark(hostRuleId: Long, destinationFolderId: Long?) {
        viewModelScope.launch {
            moveHostRuleToFolderUseCase(hostRuleId, destinationFolderId)
        }
    }
    
    /**
     * Remove a bookmark
     */
    fun removeBookmark(hostRuleId: Long) {
        viewModelScope.launch {
            deleteHostRuleUseCase(hostRuleId)
        }
    }
}

/**
 * UI state for the Bookmarks screen
 */
data class BookmarksUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFolderId: Long? = null,
    val isInSelectionMode: Boolean = false,
    val selectedHostRuleIds: Set<Long> = emptySet()
) 