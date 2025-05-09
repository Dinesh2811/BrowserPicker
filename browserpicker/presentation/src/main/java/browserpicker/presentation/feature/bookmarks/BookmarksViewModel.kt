package browserpicker.presentation.feature.bookmarks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.results.DomainResult
import browserpicker.domain.di.FolderUseCases
import browserpicker.domain.di.HostRuleUseCases
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val hostRuleUseCases: HostRuleUseCases,
    private val folderUseCases: FolderUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookmarksUiState())
    val uiState: StateFlow<BookmarksUiState> = _uiState.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()

    private val _currentFolderPath = MutableStateFlow<List<Folder>>(emptyList())
    val currentFolderPath: StateFlow<List<Folder>> = _currentFolderPath.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<HostRule>>(emptyList())
    val bookmarks: StateFlow<List<HostRule>> = _bookmarks.asStateFlow()

    private val _subfolders = MutableStateFlow<List<Folder>>(emptyList())
    val subfolders: StateFlow<List<Folder>> = _subfolders.asStateFlow()

    init {
        // Start with root bookmarks folder
        ensureDefaultFoldersExist()
        loadRootFolder()
    }

    private fun ensureDefaultFoldersExist() {
        viewModelScope.launch {
            folderUseCases.ensureDefaultFoldersExistUseCase()
        }
    }

    private fun loadRootFolder() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            folderUseCases.getRootFoldersUseCase(FolderType.BOOKMARK)
                .collect { result ->
                    when (result) {
                        is DomainResult.Success -> {
                            val rootFolders = result.data
                            if (rootFolders.isNotEmpty()) {
                                val defaultFolder = rootFolders.find { it.id == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID }
                                    ?: rootFolders.first()
                                
                                _selectedFolderId.value = defaultFolder.id
                                _currentFolderPath.value = listOf(defaultFolder)
                                loadCurrentFolder(defaultFolder.id)
                            } else {
                                _uiState.update { it.copy(
                                    isLoading = false,
                                    error = "No bookmark root folders found"
                                )}
                            }
                        }
                        is DomainResult.Failure -> {
                            _uiState.update { it.copy(
                                isLoading = false,
                                error = result.error.message
                            )}
                        }
                    }
                }
        }
    }

    fun navigateToFolder(folderId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Get folder hierarchy
            when (val pathResult = folderUseCases.getFolderHierarchyUseCase(folderId)) {
                is DomainResult.Success -> {
                    _currentFolderPath.value = pathResult.data
                    _selectedFolderId.value = folderId
                    loadCurrentFolder(folderId)
                }
                is DomainResult.Failure -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = pathResult.error.message
                    )}
                }
            }
        }
    }

    fun navigateUp() {
        val currentPath = _currentFolderPath.value
        if (currentPath.size > 1) {
            val parentFolder = currentPath[currentPath.size - 2]
            navigateToFolder(parentFolder.id)
        }
    }

    private fun loadCurrentFolder(folderId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Load subfolders
            folderUseCases.getChildFoldersUseCase(folderId)
                .combine(hostRuleUseCases.getHostRulesByFolderUseCase(folderId)) { subfoldersResult, bookmarksResult ->
                    Pair(subfoldersResult, bookmarksResult)
                }
                .collect { (subfoldersResult, bookmarksResult) ->
                    when {
                        subfoldersResult is DomainResult.Success && bookmarksResult is DomainResult.Success -> {
                            _subfolders.value = subfoldersResult.data
                            _bookmarks.value = bookmarksResult.data.filter { it.uriStatus == UriStatus.BOOKMARKED }
                            _uiState.update { it.copy(isLoading = false) }
                        }
                        subfoldersResult is DomainResult.Failure -> {
                            _uiState.update { it.copy(
                                isLoading = false,
                                error = subfoldersResult.error.message
                            )}
                        }
                        bookmarksResult is DomainResult.Failure -> {
                            _uiState.update { it.copy(
                                isLoading = false,
                                error = bookmarksResult.error.message
                            )}
                        }
                    }
                }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val currentFolderId = _selectedFolderId.value
            
            when (val result = folderUseCases.createFolderUseCase(
                name = name,
                parentFolderId = currentFolderId,
                type = FolderType.BOOKMARK
            )) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        folderCreated = true,
                        newFolderId = result.data
                    )}
                    // Refresh current folder contents
                    currentFolderId?.let { loadCurrentFolder(it) }
                }
                is DomainResult.Failure -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = result.error.message
                    )}
                }
            }
        }
    }

    fun renameFolder(folderId: Long, newName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // First get the folder
            when (val folderResult = folderUseCases.getFolderUseCase(folderId).first()) {
                is DomainResult.Success -> {
                    val folder = folderResult.data
                    if (folder != null) {
                        // Update the folder
                        when (val updateResult = folderUseCases.updateFolderUseCase(folder.copy(name = newName))) {
                            is DomainResult.Success -> {
                                _uiState.update { it.copy(
                                    isLoading = false,
                                    folderRenamed = true
                                )}
                                // Refresh current folder contents and path
                                _selectedFolderId.value?.let {
                                    loadCurrentFolder(it)
                                    if (it == folderId) {
                                        navigateToFolder(it) // Refresh the path
                                    }
                                }
                            }
                            is DomainResult.Failure -> {
                                _uiState.update { it.copy(
                                    isLoading = false,
                                    error = updateResult.error.message
                                )}
                            }
                        }
                    } else {
                        _uiState.update { it.copy(
                            isLoading = false,
                            error = "Folder not found"
                        )}
                    }
                }
                is DomainResult.Failure -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = folderResult.error.message
                    )}
                }
            }
        }
    }

    fun deleteFolder(folderId: Long, forceCascade: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = folderUseCases.deleteFolderUseCase(folderId, forceCascade)) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        folderDeleted = true
                    )}
                    
                    // If we deleted the current folder, navigate up
                    if (_selectedFolderId.value == folderId) {
                        navigateUp()
                    } else {
                        // Otherwise just refresh the current folder
                        _selectedFolderId.value?.let { loadCurrentFolder(it) }
                    }
                }
                is DomainResult.Failure -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = result.error.message
                    )}
                }
            }
        }
    }

    fun moveHostRule(hostRuleId: Long, destinationFolderId: Long?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = folderUseCases.moveHostRuleToFolderUseCase(hostRuleId, destinationFolderId)) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        hostRuleMoved = true
                    )}
                    // Refresh current folder contents
                    _selectedFolderId.value?.let { loadCurrentFolder(it) }
                }
                is DomainResult.Failure -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = result.error.message
                    )}
                }
            }
        }
    }

    fun moveFolder(folderId: Long, newParentFolderId: Long?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = folderUseCases.moveFolderUseCase(folderId, newParentFolderId)) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        folderMoved = true
                    )}
                    // Refresh current folder contents
                    _selectedFolderId.value?.let { loadCurrentFolder(it) }
                }
                is DomainResult.Failure -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = result.error.message
                    )}
                }
            }
        }
    }

    fun deleteBookmark(hostRuleId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = hostRuleUseCases.deleteHostRuleUseCase(hostRuleId)) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        bookmarkDeleted = true
                    )}
                    // Refresh current folder contents
                    _selectedFolderId.value?.let { loadCurrentFolder(it) }
                }
                is DomainResult.Failure -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = result.error.message
                    )}
                }
            }
        }
    }

    fun clearBookmarkStatus(host: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = hostRuleUseCases.clearHostStatusUseCase(host)) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        bookmarkStatusCleared = true
                    )}
                    // Refresh current folder contents
                    _selectedFolderId.value?.let { loadCurrentFolder(it) }
                }
                is DomainResult.Failure -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = result.error.message
                    )}
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetActionStates() {
        _uiState.update { it.copy(
            folderCreated = false,
            folderRenamed = false,
            folderDeleted = false,
            folderMoved = false,
            hostRuleMoved = false,
            bookmarkDeleted = false,
            bookmarkStatusCleared = false,
            newFolderId = null
        )}
    }
}

data class BookmarksUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val folderCreated: Boolean = false,
    val folderRenamed: Boolean = false,
    val folderDeleted: Boolean = false,
    val folderMoved: Boolean = false,
    val hostRuleMoved: Boolean = false,
    val bookmarkDeleted: Boolean = false,
    val bookmarkStatusCleared: Boolean = false,
    val newFolderId: Long? = null
) 