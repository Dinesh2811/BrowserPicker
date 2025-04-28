package browserpicker.presentation.main

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.domain.model.*
import browserpicker.domain.service.DomainError
import browserpicker.domain.usecase.*
import browserpicker.presentation.common.MessageType
import browserpicker.presentation.common.UserMessage
import browserpicker.presentation.picker.BrowserAppInfo
import browserpicker.presentation.picker.PickerSheetContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Immutable
data class MainScreenState(
    val pickerContext: PickerSheetContext? = null, // Context for the bottom sheet picker
    val availableBrowsers: List<BrowserAppInfo> = emptyList(), // Replace with actual list
    val userMessages: List<UserMessage> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    // Use cases needed for actions triggered *from the bottom sheet*
    private val recordUriInteractionUseCase: RecordUriInteractionUseCase,
    private val saveHostRuleUseCase: SaveHostRuleUseCase,
    private val getHostRuleUseCase: GetHostRuleUseCase,
    private val getFoldersUseCase: GetFoldersUseCase
    // Inject service/utility to get installed browsers later
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainScreenState())
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()

    init {
        loadAvailableBrowsers() // Load browser list once
    }

    private fun loadAvailableBrowsers() {
        viewModelScope.launch {
            // --- Replace with actual browser fetching logic ---
            val dummyBrowsers = listOf(
                BrowserAppInfo("Browser A", "com.browser.a"),
                BrowserAppInfo("Browser B", "com.browser.b"),
                BrowserAppInfo("Browser C", "com.browser.c")
            )
            // -------------------------------------------------
            _uiState.update { it.copy(availableBrowsers = dummyBrowsers) }
        }
    }

    // --- Bottom Sheet Management ---

    /**
     * Call this function when the app determines the picker needs to be shown
     * (e.g., after HandleInterceptedUriUseCase returns ShowPicker).
     * This would typically happen *before* navigating to the main screen or passed
     * as arguments/results to it.
     *
     * @param sheetState The state of the BottomSheetScaffold's sheet.
     * @param coroutineScope The scope to launch suspend functions like sheetState.expand().
     * @param uri The URI to be opened.
     * @param host The extracted host.
     * @param source The source of the URI interception.
     * @param ruleId Optional existing rule ID associated with the host.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    fun prepareAndShowPickerSheet(
        sheetState: SheetState,
        coroutineScope: CoroutineScope, // Scope from the Composable caller
        uri: String,
        host: String,
        source: UriSource,
        ruleId: Long?
    ) {
        viewModelScope.launch { // Use viewModelScope for fetching data
            // Fetch necessary context data (rule, folders)
            val currentRule = if (host.isNotBlank()) getHostRuleUseCase(host).firstOrNull() else null
            val folders = getFoldersUseCase(null, FolderType.BOOKMARK).firstOrNull() ?: emptyList()

            val context = PickerSheetContext(
                uriString = uri,
                host = host,
                sourceValue = source.value,
                associatedHostRuleId = ruleId ?: currentRule?.id,
                currentRule = currentRule,
                availableBookmarkFolders = folders
            )

            _uiState.update { it.copy(pickerContext = context) }

            // Expand the sheet using the provided scope
            coroutineScope.launch {
                try {
                    sheetState.expand()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to expand sheet")
                    // Handle error? Maybe show a message.
                    addMessage("Could not show browser picker.", MessageType.ERROR)
                    _uiState.update { it.copy(pickerContext = null) } // Clear context on error
                }
            }
        }
    }

    /** Hides the sheet and clears the context. */
    @OptIn(ExperimentalMaterial3Api::class)
    fun hidePickerSheet(sheetState: SheetState, coroutineScope: CoroutineScope) {
        _uiState.update { it.copy(pickerContext = null) }
        coroutineScope.launch {
            try {
                sheetState.hide()
            } catch (e: Exception) {
                Timber.e(e, "Failed to hide sheet smoothly")
                // Potentially stuck? Try skipHiddenState
                try { sheetState.hide() } catch (_: Exception) {}
            }
        }
    }

    // --- Actions Called from BrowserPickerSheetContent ---

    fun handleBrowserSelected(
        browser: BrowserAppInfo,
        context: PickerSheetContext,
        sheetState: SheetState, // Need state and scope to hide the sheet after action
        coroutineScope: CoroutineScope
    ) {
        viewModelScope.launch {
            recordUriInteractionUseCase(
                uriString = context.uriString,
                host = context.host,
                source = UriSource.fromValue(context.sourceValue),
                action = InteractionAction.OPENED_ONCE,
                chosenBrowser = browser.packageName,
                associatedHostRuleId = context.associatedHostRuleId,
                onSuccess = {
                    // LAUNCH BROWSER INTENT (Responsibility of the UI caller/Activity)
                    addMessage("Opening with ${browser.appName}...", MessageType.INFO) // Indicate action
                    hidePickerSheet(sheetState, coroutineScope)
                    // The caller (Activity/Composable) needs to observe this state or
                    // receive an event to actually launch the Intent.
                },
                onError = { error -> handleDomainError("Failed recording 'Open Once'", error) }
            )
        }
    }

    fun handleSetPreferenceSelected(
        browser: BrowserAppInfo,
        context: PickerSheetContext,
        sheetState: SheetState,
        coroutineScope: CoroutineScope
    ) {
        viewModelScope.launch {
            saveHostRuleUseCase(
                host = context.host,
                status = context.currentRule?.uriStatus ?: UriStatus.NONE, // Keep status if exists
                folderId = context.currentRule?.folderId, // Keep folder if exists
                preferredBrowser = browser.packageName,
                isPreferenceEnabled = true,
                onSuccess = { savedRuleId ->
                    viewModelScope.launch {
                        recordUriInteractionUseCase(
                            uriString = context.uriString,
                            host = context.host,
                            source = UriSource.fromValue(context.sourceValue),
                            action = InteractionAction.PREFERENCE_SET,
                            chosenBrowser = browser.packageName,
                            associatedHostRuleId = savedRuleId,
                            onSuccess = {
                                // LAUNCH BROWSER INTENT
                                addMessage("Set preference and opening with ${browser.appName}...", MessageType.INFO)
                                hidePickerSheet(sheetState, coroutineScope)
                            },
                            onError = { error -> handleDomainError("Failed recording 'Preference Set'", error) }
                        )
                    }
                },
                onError = { error -> handleDomainError("Failed setting preference", error) }
            )
        }
    }

    fun handleBookmarkSelected(
        browser: BrowserAppInfo,
        folderId: Long?,
        context: PickerSheetContext,
        sheetState: SheetState,
        coroutineScope: CoroutineScope
    ) {
        // Implementation similar to set preference, but sets status to BOOKMARKED
        // ... (Combine saveHostRuleUseCase and recordUriInteractionUseCase) ...
        addMessage("Bookmark action needs implementation", MessageType.ERROR) // Placeholder
    }

    fun handleBlockSelected(
        context: PickerSheetContext,
        sheetState: SheetState,
        coroutineScope: CoroutineScope
    ) {
        viewModelScope.launch {
            saveHostRuleUseCase(
                host = context.host,
                status = UriStatus.BLOCKED,
                folderId = null, // TODO: Allow selecting block folder?
                preferredBrowser = null,
                isPreferenceEnabled = false,
                onSuccess = { savedRuleId ->
                    viewModelScope.launch {
                        recordUriInteractionUseCase(
                            uriString = context.uriString,
                            host = context.host,
                            source = UriSource.fromValue(context.sourceValue),
                            action = InteractionAction.BLOCKED_URI_ENFORCED,
                            chosenBrowser = null,
                            associatedHostRuleId = savedRuleId,
                            onSuccess = {
                                // DO NOT LAUNCH - Host is blocked
                                addMessage("Host '${context.host}' blocked.")
                                hidePickerSheet(sheetState, coroutineScope)
                                // Signal closure or navigation away from picker context?
                            },
                            onError = { error -> handleDomainError("Failed recording 'Block enforced'", error) }
                        )
                    }
                },
                onError = { error -> handleDomainError("Failed saving block rule", error) }
            )
        }
    }


    // --- Message Handling ---
    fun clearMessage(id: Long) {
        _uiState.update { state ->
            state.copy(userMessages = state.userMessages.filterNot { it.id == id })
        }
    }

    private fun addMessage(text: String, type: MessageType = MessageType.INFO) {
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
