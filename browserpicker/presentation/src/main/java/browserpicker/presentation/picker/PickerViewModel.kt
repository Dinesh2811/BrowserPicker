package browserpicker.presentation.picker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.di.IoDispatcher
import browserpicker.domain.model.*
import browserpicker.domain.service.DomainError
import browserpicker.domain.usecase.folders.GetFoldersUseCase
import browserpicker.domain.usecase.history.RecordUriInteractionUseCase
import browserpicker.domain.usecase.rules.DeleteHostRuleUseCase
import browserpicker.domain.usecase.rules.GetHostRuleUseCase
import browserpicker.domain.usecase.rules.SaveHostRuleUseCase
import browserpicker.presentation.common.MessageType
import browserpicker.presentation.common.UserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PickerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle, // To receive arguments
    private val recordUriInteractionUseCase: RecordUriInteractionUseCase,
    private val saveHostRuleUseCase: SaveHostRuleUseCase,
    private val getHostRuleUseCase: GetHostRuleUseCase,
    private val getFoldersUseCase: GetFoldersUseCase,
    private val deleteHostRuleUseCase: DeleteHostRuleUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    // Extract initial data passed to the ViewModel (e.g., via Navigation Compose arguments)
    private val initialUri: String = savedStateHandle["uriString"] ?: ""
    private val initialHost: String = savedStateHandle["host"] ?: ""
    private val initialSource: UriSource = UriSource.fromValueOrNull(savedStateHandle["sourceValue"] ?: UriSource.INTENT.value) ?: UriSource.INTENT
    private val initialRuleId: Long? = savedStateHandle.get<Long?>("ruleId")?.takeIf { it > 0 } // Handle potential 0 or null

    private val _uiState = MutableStateFlow(
        PickerScreenState(
            uriString = initialUri,
            host = initialHost,
            source = initialSource,
            associatedHostRuleId = initialRuleId
        )
    )
    val uiState: StateFlow<PickerScreenState> = _uiState.asStateFlow()

    // SharedFlow for one-off events like closing the screen
    // private val _eventFlow = MutableSharedFlow<PickerEvent>()
    // val eventFlow = _eventFlow.asSharedFlow()


    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // 1. Load dummy browser list (replace with actual implementation later)
            val dummyBrowsers = listOf(
                BrowserAppInfo("Browser A", "com.browser.a"),
                BrowserAppInfo("Browser B", "com.browser.b"),
                BrowserAppInfo("Browser C", "com.browser.c")
            )

            // 2. Load current rule for the host
            val ruleFlow = if (initialHost.isNotBlank()) {
                getHostRuleUseCase(initialHost)
            } else {
                flowOf(null) // No host, no rule
            }

            // 3. Load available bookmark folders
            val foldersFlow = getFoldersUseCase(parentFolderId = null, type = FolderType.BOOKMARK)
                // TODO: Load full hierarchy if needed for a nested dropdown
//                .catch { emit(emptyList()) } // Handle error fetching folders

            // Combine results
            combine(ruleFlow, foldersFlow) { rule, folders ->
                Pair(rule, folders)
            }.collect { (rule, folders) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        browsers = dummyBrowsers,
                        currentRule = rule?.getOrNull(),
                        availableBookmarkFolders = folders.getOrNull()!!,
                        // Update rule ID from fetched rule if initial was null but rule exists
                        associatedHostRuleId = it.associatedHostRuleId ?: rule?.getOrNull()?.id
                    )
                }
            }
        }
    }

    // --- User Actions ---

    fun openOnce(browser: BrowserAppInfo) {
        viewModelScope.launch {
            recordUriInteractionUseCase(
                uriString = initialUri,
                host = initialHost,
                source = initialSource,
                action = InteractionAction.OPENED_ONCE,
                chosenBrowser = browser.packageName,
                associatedHostRuleId = uiState.value.associatedHostRuleId,
                onSuccess = { signalClose() },
                onError = { error -> handleDomainError("Failed recording 'Open Once'", error) }
            )
            // Note: Actual launching happens in the UI layer after this completes
        }
    }

    fun setPreferenceAndOpen(browser: BrowserAppInfo) {
        viewModelScope.launch {
            saveHostRuleUseCase(
                host = initialHost,
                status = UriStatus.NONE, // Setting preference doesn't inherently bookmark/block
                folderId = null,
                preferredBrowser = browser.packageName,
                isPreferenceEnabled = true,
                onSuccess = { savedRuleId ->
                    viewModelScope.launch(ioDispatcher) {
                        // Now record the interaction using the new/updated rule ID
                        recordUriInteractionUseCase(
                            uriString = initialUri,
                            host = initialHost,
                            source = initialSource,
                            action = InteractionAction.PREFERENCE_SET, // Changed action
                            chosenBrowser = browser.packageName,
                            associatedHostRuleId = savedRuleId,
                            onSuccess = { signalClose() },
                            onError = { error -> handleDomainError("Failed recording 'Preference Set'", error) }
                        )
                    }
                },
                onError = { error -> handleDomainError("Failed setting preference", error) }
            )
            // Actual launching happens in UI
        }
    }

    fun bookmarkAndOpen(browser: BrowserAppInfo, selectedFolderId: Long?) {
        viewModelScope.launch {
            saveHostRuleUseCase(
                host = initialHost,
                status = UriStatus.BOOKMARKED,
                folderId = selectedFolderId, // Assign to selected folder
                preferredBrowser = uiState.value.currentRule?.preferredBrowserPackage, // Keep existing pref if any
                isPreferenceEnabled = uiState.value.currentRule?.isPreferenceEnabled ?: true, // Keep existing pref if any
                onSuccess = { savedRuleId ->
                    viewModelScope.launch(ioDispatcher) {
                        recordUriInteractionUseCase(
                            uriString = initialUri,
                            host = initialHost,
                            source = initialSource,
                            action = InteractionAction.OPENED_ONCE, // Opened, bookmarking is separate state change
                            chosenBrowser = browser.packageName,
                            associatedHostRuleId = savedRuleId,
                            onSuccess = { signalClose() },
                            onError = { error -> handleDomainError("Failed recording 'Open Once' after bookmark", error) }
                        )
                    }
                },
                onError = { error -> handleDomainError("Failed saving bookmark", error) }
            )
            // Actual launching happens in UI
        }
    }

    fun blockHost() {
        viewModelScope.launch {
            saveHostRuleUseCase(
                host = initialHost,
                status = UriStatus.BLOCKED,
                folderId = null, // Default block folder assigned by repo logic? Or prompt user? Default for now.
                preferredBrowser = null,
                isPreferenceEnabled = false,
                onSuccess = { savedRuleId ->
                    viewModelScope.launch(ioDispatcher) {
                        recordUriInteractionUseCase(
                            uriString = initialUri,
                            host = initialHost,
                            source = initialSource,
                            action = InteractionAction.BLOCKED_URI_ENFORCED, // Correct action? Or different? Let's assume enforced by picker action.
                            chosenBrowser = null,
                            associatedHostRuleId = savedRuleId,
                            onSuccess = { signalClose() },
                            onError = { error -> handleDomainError("Failed recording 'Block enforced'", error) }
                        )
                    }
                },
                onError = { error -> handleDomainError("Failed saving block rule", error) }
            )
        }
    }

    fun dismiss() {
        viewModelScope.launch {
            recordUriInteractionUseCase(
                uriString = initialUri,
                host = initialHost,
                source = initialSource,
                action = InteractionAction.DISMISSED,
                chosenBrowser = null,
                associatedHostRuleId = uiState.value.associatedHostRuleId,
                onSuccess = { signalClose() },
                onError = {
//                    handleDomainError("Failed recording 'Dismissed'", it)

                    if (it != Unit) signalClose()
                }
//                        error -> handleDomainError("Failed recording 'Dismissed'", error) }
//                    // Still signal close even if recording fails? Yes.
//                    .also { if (it != Unit) signalClose()
//                    }
            )
        }
    }

    // Optional: Action to clear preference from picker
    fun clearPreference() {
        val rule = uiState.value.currentRule ?: return
        viewModelScope.launch {
            // Option 1: Just disable preference
            saveHostRuleUseCase(
                host = rule.host,
                status = rule.uriStatus, // Keep bookmark/block status
                folderId = rule.folderId,
                preferredBrowser = rule.preferredBrowserPackage, // Keep browser, just disable flag
                isPreferenceEnabled = false,
                onSuccess = { addMessage("Preference disabled.") /* Don't close */ },
                onError = { error -> handleDomainError("Failed disabling preference", error) }
            )
            // Option 2: Remove preference browser entirely (depends on desired UX)
            // saveHostRuleUseCase(..., preferredBrowser = null, isPreferenceEnabled = false, ...)
        }
    }

    // --- Signal Handling ---
    private fun signalClose() {
        _uiState.update { it.copy(closeSignal = true) }
        // Or: viewModelScope.launch { _eventFlow.emit(PickerEvent.ClosePicker) }
    }

    fun consumeCloseSignal() {
        _uiState.update { it.copy(closeSignal = false) }
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
