package browserpicker.presentation.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.*
import browserpicker.domain.service.DomainError
import browserpicker.domain.usecase.folders.GetFoldersUseCase
import browserpicker.domain.usecase.rules.DeleteHostRuleUseCase
import browserpicker.domain.usecase.rules.GetHostRulesUseCase
import browserpicker.domain.usecase.rules.SaveHostRuleUseCase
import browserpicker.presentation.common.MessageType
import browserpicker.presentation.common.UserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val getHostRulesUseCase: GetHostRulesUseCase,
    private val getFoldersUseCase: GetFoldersUseCase, // To list folders for assignment
    private val saveHostRuleUseCase: SaveHostRuleUseCase,
    private val deleteHostRuleUseCase: DeleteHostRuleUseCase
) : ViewModel() {

    private val _currentType = MutableStateFlow(UriStatus.BOOKMARKED) // Start with bookmarks
    private val _selectedFolderId = MutableStateFlow<Long?>(null)

    // Combine filters to drive data fetching
    @OptIn(ExperimentalCoroutinesApi::class)
    private val rulesFlow: Flow<DomainResult<List<HostRule>, AppError>> = combine(
        _currentType,
        _selectedFolderId
    ) { type, folderId -> Pair(type, folderId) }
        .flatMapLatest { (type, folderId) ->
            getHostRulesUseCase(
                statusFilter = type,
                folderFilter = folderId // Null means root/all for that type
            )
        }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1) // Share the flow

    // Folders matching the current rule type
    @OptIn(ExperimentalCoroutinesApi::class)
    private val foldersFlow: Flow<List<Folder>> = _currentType
        .flatMapLatest { type ->
            val folderType = if (type == UriStatus.BOOKMARKED) FolderType.BOOKMARK else FolderType.BLOCK
            getFoldersUseCase(parentFolderId = null, type = folderType) // Get root folders first
            // TODO: Enhance later to fetch full hierarchy if needed for dropdowns
        }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)


    private val _uiState = MutableStateFlow(RulesScreenState())
    val uiState: StateFlow<RulesScreenState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                rulesFlow,
                foldersFlow,
                _currentType,
                _selectedFolderId
            ) { rules, folders, type, folderId ->
                // Update main state when data or filters change
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false, // Assuming loading handled internally by flows for now
                        rules = rules.getOrNull()!!,
                        folders = folders,
                        currentType = type,
                        selectedFolderId = folderId,
                        // Keep dialog state and messages unless explicitly changed
                        dialogState = currentState.dialogState,
                        userMessages = currentState.userMessages
                    )
                }
            }.collect() // Start collecting the combined flow
        }
    }

    fun setFilterType(type: UriStatus) {
        if (type == UriStatus.BOOKMARKED || type == UriStatus.BLOCKED) {
            _currentType.value = type
            _selectedFolderId.value = null // Reset folder filter when type changes
        }
    }

    fun setSelectedFolder(folderId: Long?) {
        _selectedFolderId.value = folderId
    }

    // --- Dialog Actions ---

    fun showAddRuleDialog(prefillHost: String = "") {
        val currentFolders = _uiState.value.folders
        val type = _currentType.value
        _uiState.update {
            it.copy(dialogState = RuleDialogState.Add(prefillHost, currentFolders, type))
        }
    }

    fun showEditRuleDialog(rule: HostRule) {
        val currentFolders = _uiState.value.folders
        _uiState.update {
            it.copy(dialogState = RuleDialogState.Edit(rule, currentFolders))
        }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialogState = RuleDialogState.Hidden) }
    }

    // --- CRUD Actions ---

    fun saveRule(
        host: String,
        status: UriStatus, // Should match currentType or be derived from dialog
        folderId: Long?,
        preferredBrowser: String?,
        isPreferenceEnabled: Boolean
    ) {
        viewModelScope.launch {
            saveHostRuleUseCase(
                host = host,
                status = status,
                folderId = folderId,
                preferredBrowser = preferredBrowser,
                isPreferenceEnabled = isPreferenceEnabled,
                onSuccess = {
                    addMessage("Rule for '$host' saved.")
                    dismissDialog()
                    // Rules list will update via flow
                },
                onError = { error ->
                    handleDomainError("Failed to save rule", error)
                    // Keep dialog open? Or close and show error? Let's keep it open for correction.
                }
            )
        }
    }

    fun deleteRule(host: String) {
        viewModelScope.launch {
            deleteHostRuleUseCase(
                host = host,
                onSuccess = {
                    addMessage("Rule for '$host' deleted.")
                    // List updates via flow
                },
                onError = { error ->
                    handleDomainError("Failed to delete rule", error)
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
