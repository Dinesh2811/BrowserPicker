package browserpicker.presentation.test.blockedurls

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.di.DefaultDispatcher
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.domain.di.FolderUseCases
import browserpicker.domain.di.HostRuleUseCases
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriStatus
import browserpicker.presentation.UiState
import browserpicker.presentation.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Represents the UI state for the Blocked URLs screen
 */
@Immutable
data class BlockedUrlsUiState(
    val currentFolder: UiState<Folder?> = UiState.Loading, // Null for the root 'Blocked' folder
    val childFolders: UiState<List<Folder>> = UiState.Loading,
    val blockedHosts: UiState<List<HostRule>> = UiState.Loading,
    val isLoading: Boolean = false, // Overall loading state for actions
    val error: String? = null, // Generic error message for actions
    val folderActionState: FolderActionState = FolderActionState.Idle // Reusing the same state as Bookmarks for simplicity
)

/**
 * Represents the state of asynchronous folder actions (create, rename, delete)
 * Re-defined here or imported from a common place if shared heavily. Keeping it separate for now.
 */
@Immutable
sealed interface FolderActionState {
    @Immutable data object Idle : FolderActionState
    @Immutable data object Loading : FolderActionState
    @Immutable data class Success(val message: String) : FolderActionState
    @Immutable data class Error(val message: String) : FolderActionState
}


/**
 * ViewModel for the Blocked URLs screen.
 *
 * This ViewModel handles:
 * - Displaying blocked URIs in a folder structure
 * - Creating, updating, and deleting folders for blocked URLs
 * - Moving blocked URLs between folders
 * - Adding and removing blocked URL entries
 *
 * Used by: BlockedUrlsScreen, FolderDetailsScreen (when used for blocked URL folders)
 */
@HiltViewModel
class BlockedUrlsViewModel @Inject constructor(
    private val instantProvider: InstantProvider,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val folderUseCases: FolderUseCases,
    private val hostRuleUseCases: HostRuleUseCases,
): ViewModel() {

    private val _state = MutableStateFlow(BlockedUrlsUiState())
    val state: StateFlow<BlockedUrlsUiState> = _state.asStateFlow()

    private val _currentFolderId = MutableStateFlow<Long?>(null) // Null for the root folder
    private val _refreshTrigger = MutableStateFlow(0) // Used to manually trigger data refresh

    init {
        loadBlockedUrlsData()
    }

    /**
     * Loads the child folders and blocked hosts for the current folder.
     * Triggered initially and when the current folder changes or on refresh.
     */
    private fun loadBlockedUrlsData() {
        viewModelScope.launch {
            combine(_currentFolderId, _refreshTrigger) { folderId, refresh -> folderId }
                .distinctUntilChanged()
                .onStart { _state.update { it.copy(childFolders = UiState.Loading, blockedHosts = UiState.Loading) } }
                .flatMapLatest { folderId ->
                    val currentFolderFlow = if (folderId == null) {
                        // For root, emit null as the current folder
                        flowOf(UiState.Success(null))
                    } else {
                        // For subfolders, fetch the folder details
                        folderUseCases.getFolderUseCase(folderId).toUiState()
                    }

                    // Combine fetching child folders and host rules
                    combine(
                        currentFolderFlow,
                        folderUseCases.getChildFoldersUseCase(folderId ?: Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID).toUiState(),
                        hostRuleUseCases.getHostRulesByFolderUseCase(folderId ?: Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID).toUiState()
                    ) { currentFolderState, childFoldersState, hostRulesState ->
                        _state.value.copy(
                            currentFolder = currentFolderState,
                            childFolders = childFoldersState,
                            blockedHosts = hostRulesState
                        )
                    }
                }
                .flowOn(ioDispatcher)
                .catch { e ->
                    _state.update {
                        it.copy(
                            childFolders = UiState.Error("Failed to load data: ${e.message}"),
                            blockedHosts = UiState.Error("Failed to load data: ${e.message}"),
                            error = "An unexpected error occurred while loading: ${e.message}"
                        )
                    }
                }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    _state.value // Use the initial state until data loads
                )
                .collect { combinedState ->
                    _state.update { combinedState }
                }
        }
    }


    /**
     * Navigates into a specified folder.
     * @param folderId The ID of the folder to navigate into. Null to navigate to the root.
     */
    fun navigateToFolder(folderId: Long?) {
        viewModelScope.launch {
            _currentFolderId.emit(folderId)
        }
    }

    /**
     * Adds a URI as a blocked URL.
     * @param host The host of the URI to block.
     * @param uriString The full URI string. (Note: SaveHostRuleUseCase only takes host, not uriString for blocking status)
     * @param folderId Optional folder ID to add the blocked URL to. Defaults to the current folder.
     */
    fun addBlockedUrl(host: String, uriString: String, folderId: Long? = _currentFolderId.value) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, folderActionState = FolderActionState.Loading) }
            val result = withContext(ioDispatcher) {
                hostRuleUseCases.saveHostRuleUseCase(
                    host = host,
                    status = UriStatus.BLOCKED,
                    folderId = folderId
                )
            }
            _state.update {
                result.fold(
                    onSuccess = { newRuleId ->
                        // Refresh the data after adding
                        refreshData()
                        it.copy(
                            isLoading = false,
                            folderActionState = FolderActionState.Success("Blocked URL '$host' added successfully.")
                        )
                    },
                    onFailure = { error ->
                        it.copy(
                            isLoading = false,
                            error = error.message,
                            folderActionState = FolderActionState.Error("Failed to add blocked URL: ${error.message}")
                        )
                    }
                )
            }
        }
    }

    /**
     * Removes a blocked URL.
     * @param hostRuleId The ID of the host rule (blocked URL) to remove.
     */
    fun removeBlockedUrl(hostRuleId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = withContext(ioDispatcher) {
                hostRuleUseCases.deleteHostRuleUseCase(hostRuleId)
            }
            _state.update { blockedUrlsUiState ->
                result.fold(
                    onSuccess = {
                        // Refresh the data after removing
                        refreshData()
                        blockedUrlsUiState.copy(isLoading = false)
                    },
                    onFailure = { error ->
                        blockedUrlsUiState.copy(isLoading = false, error = "Failed to remove blocked URL: ${error.message}")
                    }
                )
            }
        }
    }

    /**
     * Creates a new folder within the current folder for blocked URLs.
     * @param name The name of the new folder.
     */
    fun createFolder(name: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, folderActionState = FolderActionState.Loading) }
            val result = withContext(ioDispatcher) {
                folderUseCases.createFolderUseCase(
                    name = name,
                    parentFolderId = _currentFolderId.value,
                    type = FolderType.BLOCK
                )
            }
            _state.update {
                result.fold(
                    onSuccess = { newFolderId ->
                        // Refresh the data after creating
                        refreshData()
                        it.copy(
                            isLoading = false,
                            folderActionState = FolderActionState.Success("Folder '$name' created successfully.")
                        )
                    },
                    onFailure = { error ->
                        it.copy(
                            isLoading = false,
                            error = error.message,
                            folderActionState = FolderActionState.Error("Failed to create folder: ${error.message}")
                        )
                    }
                )
            }
        }
    }

    /**
     * Renames an existing folder.
     * @param folderId The ID of the folder to rename.
     * @param newName The new name for the folder.
     */
    fun renameFolder(folderId: Long, newName: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, folderActionState = FolderActionState.Loading) }
            // First, get the existing folder to modify it
            val folderResult = withContext(ioDispatcher) { folderUseCases.getFolderUseCase(folderId).toUiState().stateIn(viewModelScope).value }

            when (folderResult) {
                is UiState.Success -> {
                    val folder = folderResult.data
                    if (folder != null) {
                        val updatedFolder = folder.copy(name = newName, updatedAt = instantProvider.now())
                        val updateResult = withContext(ioDispatcher) {
                            folderUseCases.updateFolderUseCase(updatedFolder)
                        }
                        _state.update { blockedUrlsUiState ->
                            updateResult.fold(
                                onSuccess = {
                                    refreshData()
                                    blockedUrlsUiState.copy(isLoading = false, folderActionState = FolderActionState.Success("Folder renamed to '$newName'."))
                                },
                                onFailure = { error ->
                                    blockedUrlsUiState.copy(isLoading = false, error = error.message, folderActionState = FolderActionState.Error("Failed to rename folder: ${error.message}"))
                                }
                            )
                        }
                    } else {
                        _state.update { it.copy(isLoading = false, error = "Folder not found.", folderActionState = FolderActionState.Error("Folder not found.")) }
                    }
                }
                is UiState.Error -> {
                    _state.update { it.copy(isLoading = false, error = folderResult.message, folderActionState = FolderActionState.Error("Failed to get folder details: ${folderResult.message}")) }
                }
                UiState.Loading -> { /* Should not happen here, handled by state update above */ }
            }
        }
    }

    /**
     * Deletes a folder.
     * @param folderId The ID of the folder to delete.
     * @param forceCascade Whether to delete contents if the folder is not empty.
     */
    fun deleteFolder(folderId: Long, forceCascade: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, folderActionState = FolderActionState.Loading) }
            val result = withContext(ioDispatcher) {
                folderUseCases.deleteFolderUseCase(folderId, forceCascade)
            }
            _state.update { blockedUrlsUiState ->
                result.fold(
                    onSuccess = {
                        // If the current folder was deleted, navigate up
                        if (_currentFolderId.value == folderId) {
                            // This logic might need refinement if navigating multiple levels up
                            navigateToFolder(null) // Navigate to root for now
                        } else {
                            refreshData()
                        }
                        blockedUrlsUiState.copy(isLoading = false, folderActionState = FolderActionState.Success("Folder deleted."))
                    },
                    onFailure = { error ->
                        blockedUrlsUiState.copy(isLoading = false, error = error.message, folderActionState = FolderActionState.Error("Failed to delete folder: ${error.message}"))
                    }
                )
            }
        }
    }

    /**
     * Moves a blocked host rule to a different folder.
     * @param hostRuleId The ID of the host rule to move.
     * @param destinationFolderId The ID of the destination folder. Null for the root folder.
     */
    fun moveBlockedUrlToFolder(hostRuleId: Long, destinationFolderId: Long?) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = withContext(ioDispatcher) {
                folderUseCases.moveHostRuleToFolderUseCase(hostRuleId, destinationFolderId)
            }
            _state.update { blockedUrlsUiState ->
                result.fold(
                    onSuccess = {
                        refreshData() // Refresh both source and destination folder views if needed
                        blockedUrlsUiState.copy(isLoading = false)
                    },
                    onFailure = { error ->
                        blockedUrlsUiState.copy(isLoading = false, error = "Failed to move blocked URL: ${error.message}")
                    }
                )
            }
        }
    }

    /**
     * Refreshes the data for the current view.
     */
    fun refreshData() {
        _refreshTrigger.update { it + 1 }
    }

    /**
     * Clears the action state message.
     */
    fun clearActionState() {
        _state.update { it.copy(folderActionState = FolderActionState.Idle) }
    }
}
