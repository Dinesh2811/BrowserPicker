package browserpicker.presentation.feature.browserpicker

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.di.DefaultDispatcher
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.di.MainDispatcher
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.*
import browserpicker.domain.model.query.BrowserStatSortField
import browserpicker.domain.model.query.HandleUriResult
import browserpicker.domain.model.query.SortOrder
import browserpicker.domain.service.ParsedUri
import browserpicker.domain.usecases.browser.*
import browserpicker.domain.usecases.system.OpenUriInBrowserUseCase
import browserpicker.domain.usecases.uri.host.*
import browserpicker.domain.usecases.uri.shared.*
import browserpicker.presentation.UiState
import browserpicker.presentation.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Represents the possible states of the browser picker interface
 */
@Immutable
sealed interface BrowserPickerStep {
    @Immutable data object Loading : BrowserPickerStep
    @Immutable data class ValidatingUri(val uriString: String) : BrowserPickerStep
    @Immutable data class ChoosingBrowser(val uriString: String, val host: String, val parsedUri: ParsedUri) : BrowserPickerStep
    @Immutable data class OpeningUri(val browserPackage: String, val uriString: String) : BrowserPickerStep
    @Immutable data class Error(val message: String, val uriString: String?) : BrowserPickerStep
    @Immutable data class Blocked(val host: String, val hostRuleId: Long?) : BrowserPickerStep
}

/**
 * Represents the current state of preference for a URI being handled
 */
@Immutable
data class UriPreferenceStatus(
    val hasPreference: Boolean = false,
    val hostRule: HostRule? = null,
    val preferredBrowserPackage: String? = null,
    val isBookmarked: Boolean = false,
    val isBlocked: Boolean = false,
    val isSavingPreference: Boolean = false,
    val preferenceResult: UiState<Unit>? = null
)

/**
 * Complete UI state for the browser picker screen
 */
@Immutable
data class BrowserPickerUiState(
    val source: UriSource = UriSource.INTENT,
    val currentStep: BrowserPickerStep = BrowserPickerStep.Loading,
    val availableBrowsers: UiState<List<BrowserAppInfo>> = UiState.Loading,
    val mostUsedBrowsers: UiState<List<BrowserUsageStat>> = UiState.Loading,
    val uriPreference: UriPreferenceStatus = UriPreferenceStatus(),
    val recentUris: UiState<List<UriRecord>> = UiState.Loading,
)

/**
 * ViewModel for browser picker functionality
 * Handles URI processing, browser selection, and preference management
 */
@HiltViewModel
class BrowserPickerViewModel @Inject constructor(
    private val instantProvider: InstantProvider,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    private val validateUriUseCase: ValidateUriUseCase,
    private val handleUriUseCase: HandleUriUseCase,
    private val recordUriInteractionUseCase: RecordUriInteractionUseCase,
    private val getRecentUrisUseCase: GetRecentUrisUseCase,
    private val getAvailableBrowsersUseCase: GetAvailableBrowsersUseCase,
    private val getBrowserUsageStatsUseCase: GetBrowserUsageStatsUseCase,
    private val getHostRuleUseCase: GetHostRuleUseCase,
    private val saveHostRuleUseCase: SaveHostRuleUseCase,
    private val recordBrowserUsageUseCase: RecordBrowserUsageUseCase,
    private val openUriInBrowserUseCase: OpenUriInBrowserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(BrowserPickerUiState())
    val state: StateFlow<BrowserPickerUiState> = _state.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        // Load available browsers
        viewModelScope.launch {
            getAvailableBrowsersUseCase()
                .flowOn(ioDispatcher)
                .toUiState()
                .catch { error ->
                    _state.update {
                        it.copy(availableBrowsers = UiState.Error("Failed to load browsers: ${error.message}", error))
                    }
                }
                .collect { uiState ->
                    _state.update { it.copy(availableBrowsers = uiState) }
                }
        }

        // Load most used browsers
        viewModelScope.launch {
            getBrowserUsageStatsUseCase(
                sortBy = BrowserStatSortField.USAGE_COUNT,
                sortOrder = SortOrder.DESC
            )
                .flowOn(ioDispatcher)
                .toUiState()
                .catch { error ->
                    _state.update {
                        it.copy(mostUsedBrowsers = UiState.Error("Failed to load browser stats: ${error.message}", error))
                    }
                }
                .collect { uiState ->
                    _state.update { it.copy(mostUsedBrowsers = uiState) }
                }
        }

        // Load recent URIs
        viewModelScope.launch {
            getRecentUrisUseCase()
                .flowOn(ioDispatcher)
                .toUiState()
                .catch { error ->
                    _state.update {
                        it.copy(recentUris = UiState.Error("Failed to load recent URIs: ${error.message}", error))
                    }
                }
                .collect { uiState ->
                    _state.update { it.copy(recentUris = uiState) }
                }
        }
    }

    /**
     * Process a new URI from any source
     */
    fun processUri(uriString: String, source: UriSource = UriSource.INTENT) {
        if (uriString.isBlank()) {
            _state.update {
                it.copy(
                    source = source,
                    currentStep = BrowserPickerStep.Error("URI cannot be empty", null)
                )
            }
            return
        }

        _state.update {
            it.copy(
                source = source,
                currentStep = BrowserPickerStep.ValidatingUri(uriString)
            )
        }

        viewModelScope.launch {
            // Handle the URI through domain logic
            val result = withContext(ioDispatcher) {
                handleUriUseCase(uriString, source)
            }

            // Update state based on result
            when (result) {
                is DomainResult.Success -> {
                    when (val handleResult = result.data) {
                        is HandleUriResult.Blocked -> {
//                            _state.update {
//                                it.copy(
//                                    currentStep = BrowserPickerStep.Blocked(
//                                        host = (handleResult as HandleUriResult.Blocked).host,
//                                        hostRuleId = handleResult.hostRuleId
//                                    ),
//                                    uriPreference = UriPreferenceStatus(
//                                        isBlocked = true
//                                    )
//                                )
//                            }
//                            // Record the blocked interaction
//                            recordInteraction(
//                                uriString,
//                                (handleResult as HandleUriResult.Blocked).host,
//                                source,
//                                InteractionAction.BLOCKED_URI_ENFORCED,
//                                null,
//                                handleResult.hostRuleId
//                            )
                        }
                        is HandleUriResult.OpenDirectly -> {
                            val openResult = handleResult as HandleUriResult.OpenDirectly
                            _state.update {
                                it.copy(currentStep = BrowserPickerStep.OpeningUri(
                                    browserPackage = openResult.browserPackageName,
                                    uriString = uriString
                                ))
                            }

                            // Open in browser directly
//                            openInBrowserAndRecord(
//                                uriString,
//                                openResult.browserPackageName,
//                                openResult.host,
//                                source,
//                                InteractionAction.OPENED_BY_PREFERENCE,
//                                openResult.hostRuleId
//                            )
                        }
                        is HandleUriResult.ShowPicker -> {
                            val pickerResult = handleResult as HandleUriResult.ShowPicker

                            // Get host rule for this URI if it exists
                            val hostRuleFlow = getHostRuleUseCase(pickerResult.host)
                                .flowOn(ioDispatcher)
                                .catch { /* Ignore errors */ }
                                .take(1)

                            // Load URI and validation info
                            val parsedUriResult = withContext(ioDispatcher) {
                                validateUriUseCase(uriString)
                            }

                            // Combine parsed URI and host rule
                            val parsedUri = parsedUriResult.getOrNull()
                            if (parsedUri == null) {
                                _state.update {
                                    it.copy(currentStep = BrowserPickerStep.Error(
                                        message = "Invalid URI: $uriString",
                                        uriString = uriString
                                    ))
                                }
                                return@launch
                            }

                            // Get host rule and update state
                            val hostRule = hostRuleFlow.firstOrNull()?.getOrNull()
                            val isBookmarked = hostRule?.uriStatus == UriStatus.BOOKMARKED
                            val isBlocked = hostRule?.uriStatus == UriStatus.BLOCKED

                            _state.update {
                                it.copy(
                                    currentStep = BrowserPickerStep.ChoosingBrowser(
                                        uriString = uriString,
                                        host = pickerResult.host,
                                        parsedUri = parsedUri
                                    ),
                                    uriPreference = UriPreferenceStatus(
                                        hasPreference = hostRule?.preferredBrowserPackage != null && hostRule.isPreferenceEnabled,
                                        hostRule = hostRule,
                                        preferredBrowserPackage = hostRule?.preferredBrowserPackage,
                                        isBookmarked = isBookmarked,
                                        isBlocked = isBlocked
                                    )
                                )
                            }
                        }
                        is HandleUriResult.InvalidUri -> {
                            _state.update {
                                it.copy(currentStep = BrowserPickerStep.Error(
                                    message = "Invalid URI: ${(handleResult as HandleUriResult.InvalidUri).reason}",
                                    uriString = uriString
                                ))
                            }
                        }
                    }
                }
                is DomainResult.Failure -> {
                    _state.update {
                        it.copy(currentStep = BrowserPickerStep.Error(
                            message = "Error processing URI: ${result.error.message}",
                            uriString = uriString
                        ))
                    }
                }
            }
        }
    }

    /**
     * Choose a browser for the current URI
     */
    fun chooseBrowser(browserPackage: String, remember: Boolean = false) {
        val currentStep = _state.value.currentStep
        if (currentStep !is BrowserPickerStep.ChoosingBrowser) {
            return
        }

        val uriString = currentStep.uriString
        val host = currentStep.host

        _state.update {
            it.copy(
                currentStep = BrowserPickerStep.OpeningUri(
                    browserPackage = browserPackage,
                    uriString = uriString
                )
            )
        }

        viewModelScope.launch {
            // Save preference if requested
            if (remember) {
                _state.update {
                    it.copy(
                        uriPreference = it.uriPreference.copy(
                            isSavingPreference = true,
                            preferenceResult = null
                        )
                    )
                }

                val hostRuleResult = withContext(ioDispatcher) {
                    saveHostRuleUseCase(
                        host = host,
                        status = UriStatus.NONE,
                        folderId = null,
                        preferredBrowserPackage = browserPackage,
                        isPreferenceEnabled = true
                    )
                }

                _state.update {
                    it.copy(
                        uriPreference = it.uriPreference.copy(
                            isSavingPreference = false,
                            preferenceResult = hostRuleResult.toUiState().map { Unit }
                        )
                    )
                }
            }

            // Open in the browser
            openInBrowserAndRecord(
                uriString,
                browserPackage,
                host,
                _state.value.source,
                InteractionAction.OPENED_ONCE,
                _state.value.uriPreference.hostRule?.id
            )
        }
    }

    /**
     * Update a URI's status (bookmark, block)
     */
    fun updateUriStatus(status: UriStatus, folderId: Long? = null) {
        val currentStep = _state.value.currentStep
        if (currentStep !is BrowserPickerStep.ChoosingBrowser) {
            return
        }

        val host = currentStep.host

        viewModelScope.launch {
            _state.update {
                it.copy(
                    uriPreference = it.uriPreference.copy(
                        isSavingPreference = true,
                        preferenceResult = null
                    )
                )
            }

            val preferredBrowser = if (status != UriStatus.BLOCKED) {
                _state.value.uriPreference.preferredBrowserPackage
            } else {
                null // Remove preferred browser if blocking
            }

            val isPreferenceEnabled = status != UriStatus.BLOCKED

            val hostRuleResult = withContext(ioDispatcher) {
                saveHostRuleUseCase(
                    host = host,
                    status = status,
                    folderId = folderId,
                    preferredBrowserPackage = preferredBrowser,
                    isPreferenceEnabled = isPreferenceEnabled
                )
            }

            _state.update {
                it.copy(
                    uriPreference = UriPreferenceStatus(
                        hasPreference = preferredBrowser != null && isPreferenceEnabled,
                        hostRule = hostRuleResult.getOrNull()?.let {
                            // If we got an ID back, construct a host rule to update the UI
                            // In a real app, we'd query the repository to get the full host rule
                            HostRule(
                                id = it,
                                host = host,
                                uriStatus = status,
                                folderId = folderId,
                                preferredBrowserPackage = preferredBrowser,
                                isPreferenceEnabled = isPreferenceEnabled,
                                createdAt = instantProvider.now(),
                                updatedAt = instantProvider.now()
                            )
                        },
                        preferredBrowserPackage = preferredBrowser,
                        isBookmarked = status == UriStatus.BOOKMARKED,
                        isBlocked = status == UriStatus.BLOCKED,
                        isSavingPreference = false,
                        preferenceResult = hostRuleResult.toUiState().map { Unit }
                    )
                )
            }

            // If blocked, we need to update the UI state
            if (status == UriStatus.BLOCKED) {
                val hostRuleId = hostRuleResult.getOrNull()
                _state.update {
                    it.copy(
                        currentStep = BrowserPickerStep.Blocked(host, hostRuleId)
                    )
                }

                // Record the interaction
                val uriString = (currentStep as BrowserPickerStep.ChoosingBrowser).uriString
                recordInteraction(
                    uriString,
                    host,
                    _state.value.source,
                    InteractionAction.BLOCKED_URI_ENFORCED,
                    null,
                    hostRuleId
                )
            }
        }
    }

    /**
     * Cancel browser selection
     */
    fun dismissPicker() {
        val currentStep = _state.value.currentStep
        if (currentStep !is BrowserPickerStep.ChoosingBrowser) {
            return
        }

        viewModelScope.launch {
            // Record dismissal
            val uriString = currentStep.uriString
            val host = currentStep.host
            recordInteraction(
                uriString,
                host,
                _state.value.source,
                InteractionAction.DISMISSED,
                null,
                _state.value.uriPreference.hostRule?.id
            )

            // Reset to initial state
            _state.update {
                BrowserPickerUiState(
                    availableBrowsers = it.availableBrowsers,
                    mostUsedBrowsers = it.mostUsedBrowsers,
                    recentUris = it.recentUris
                )
            }
        }
    }

    /**
     * Retry after an error
     */
    fun retryAfterError() {
        val currentStep = _state.value.currentStep
        if (currentStep !is BrowserPickerStep.Error || currentStep.uriString == null) {
            return
        }

        processUri(currentStep.uriString, _state.value.source)
    }

    /**
     * Open a URI from clipboard or manual entry
     */
    fun openFromAlternateSource(uriString: String, source: UriSource) {
        processUri(uriString, source)
    }

    /**
     * Record browser usage and URI interaction, and open the URI
     */
    private suspend fun openInBrowserAndRecord(
        uriString: String,
        browserPackage: String,
        host: String,
        source: UriSource,
        action: InteractionAction,
        hostRuleId: Long?
    ) {
        // Record the interaction first
        recordInteraction(uriString, host, source, action, browserPackage, hostRuleId)

        // Record browser usage
        withContext(ioDispatcher) {
            recordBrowserUsageUseCase(browserPackage)
        }

        // Open the URI in the browser
        val openResult = withContext(ioDispatcher) {
            openUriInBrowserUseCase(uriString, browserPackage)
        }

        if (openResult is DomainResult.Failure) {
            _state.update {
                it.copy(
                    currentStep = BrowserPickerStep.Error(
                        message = "Failed to open URI: ${openResult.error.message}",
                        uriString = uriString
                    )
                )
            }
        } else {
            // Successfully opened, reset to initial state after a short delay
            // to show the "Opening..." state briefly
            withContext(mainDispatcher) {
                kotlinx.coroutines.delay(500.milliseconds)
                _state.update {
                    BrowserPickerUiState(
                        availableBrowsers = it.availableBrowsers,
                        mostUsedBrowsers = it.mostUsedBrowsers,
                        recentUris = it.recentUris
                    )
                }
            }
        }
    }

    /**
     * Record a URI interaction
     */
    private suspend fun recordInteraction(
        uriString: String,
        host: String,
        source: UriSource,
        action: InteractionAction,
        chosenBrowser: String?,
        hostRuleId: Long?
    ) {
        withContext(ioDispatcher) {
            recordUriInteractionUseCase(
                uriString = uriString,
                host = host,
                source = source,
                action = action,
                chosenBrowser = chosenBrowser,
                associatedHostRuleId = hostRuleId
            )
        }
    }
}

/*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.results.DomainResult
import browserpicker.domain.di.BrowserUseCases
import browserpicker.domain.di.UriHandlingUseCases
import browserpicker.domain.model.BrowserAppInfo
import browserpicker.domain.model.*
import browserpicker.domain.model.query.*
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowserPickerViewModel @Inject constructor(
    private val uriHandlingUseCases: UriHandlingUseCases,
    private val browserUseCases: BrowserUseCases,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserPickerUiState())
    val uiState: StateFlow<BrowserPickerUiState> = _uiState.asStateFlow()

    init {
        loadBrowsers()
        // Restore URI from saved state if available
        savedStateHandle.get<String>("currentUri")?.let { uri ->
            handleUri(uri, UriSource.INTENT)
        }
    }

    fun handleUri(uriString: String, source: UriSource) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // First validate URI
            val validationResult = uriHandlingUseCases.validateUriUseCase(uriString)
            if (validationResult is DomainResult.Failure) {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = validationResult.error.message
                    )
                }
                return@launch
            }
            
            // Handle URI
            when (val result = uriHandlingUseCases.handleUriUseCase(uriString, source)) {
                is DomainResult.Success -> {
                    when (val handleResult = result.data) {
                        is HandleUriResult.Blocked -> {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    uriBlocked = true,
                                    currentUri = uriString
                                )
                            }
                            savedStateHandle["currentUri"] = uriString
                        }
                        is HandleUriResult.OpenDirectly -> {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    openDirectly = true,
                                    selectedBrowser = handleResult.browserPackageName,
                                    currentUri = uriString
                                )
                            }
                            savedStateHandle["currentUri"] = uriString
                            
                            // Record the interaction
                            val parsedUri = validationResult.getOrNull()
                            if (parsedUri != null) {
                                uriHandlingUseCases.recordUriInteractionUseCase(
                                    uriString = uriString,
                                    host = parsedUri.host,
                                    source = source,
                                    action = InteractionAction.OPENED_BY_PREFERENCE,
                                    chosenBrowser = handleResult.browserPackageName,
                                    associatedHostRuleId = handleResult.hostRuleId
                                )
                            }
                            
                            // Record browser usage
                            browserUseCases.recordBrowserUsageUseCase(handleResult.browserPackageName)
                        }
                        is HandleUriResult.ShowPicker -> {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    currentUri = uriString,
                                    currentHost = handleResult.host,
                                    associatedHostRuleId = handleResult.hostRuleId
                                )
                            }
                            savedStateHandle["currentUri"] = uriString
                        }
                        is HandleUriResult.InvalidUri -> {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false, 
                                    error = handleResult.reason
                                )
                            }
                        }
                    }
                }
                is DomainResult.Failure -> {
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            error = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun selectBrowser(browserPackage: String, setAsPreferred: Boolean = false) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val currentUri = currentState.currentUri
            val currentHost = currentState.currentHost
            
            if (currentUri != null && currentHost != null) {
                // Record the interaction
                uriHandlingUseCases.recordUriInteractionUseCase(
                    uriString = currentUri,
                    host = currentHost,
                    source = UriSource.INTENT, // Assuming default, should be passed from UI
                    action = if (setAsPreferred) InteractionAction.PREFERENCE_SET else InteractionAction.OPENED_ONCE,
                    chosenBrowser = browserPackage,
                    associatedHostRuleId = currentState.associatedHostRuleId
                )
                
                // Record browser usage
                browserUseCases.recordBrowserUsageUseCase(browserPackage)
                
                // Set as preferred if requested
                if (setAsPreferred) {
                    browserUseCases.setPreferredBrowserForHostUseCase(
                        host = currentHost,
                        packageName = browserPackage
                    )
                }
                
                _uiState.update { 
                    it.copy(
                        selectedBrowser = browserPackage,
                        browserPreferenceSet = setAsPreferred
                    )
                }
            }
        }
    }
    
    fun dismissPicker() {
        viewModelScope.launch {
            val currentState = _uiState.value
            val currentUri = currentState.currentUri
            val currentHost = currentState.currentHost
            
            if (currentUri != null && currentHost != null) {
                // Record the dismissal
                uriHandlingUseCases.recordUriInteractionUseCase(
                    uriString = currentUri,
                    host = currentHost,
                    source = UriSource.INTENT, // Assuming default
                    action = InteractionAction.DISMISSED,
                    chosenBrowser = null,
                    associatedHostRuleId = currentState.associatedHostRuleId
                )
                
                _uiState.update { 
                    it.copy(
                        pickerDismissed = true,
                        currentUri = null,
                        currentHost = null,
                        associatedHostRuleId = null
                    )
                }
                savedStateHandle.remove<String>("currentUri")
            }
        }
    }

    private fun loadBrowsers() {
        viewModelScope.launch {
            browserUseCases.getAvailableBrowsersUseCase()
                .collect { result ->
                    when (result) {
                        is DomainResult.Success -> {
                            _uiState.update { 
                                it.copy(
                                    availableBrowsers = result.data,
                                    isLoading = false
                                )
                            }
                        }
                        is DomainResult.Failure -> {
                            _uiState.update { 
                                it.copy(
                                    error = result.error.message,
                                    isLoading = false
                                )
                            }
                        }
                    }
                }
        }
    }

    fun resetState() {
        _uiState.update {
            BrowserPickerUiState()
        }
        savedStateHandle.remove<String>("currentUri")
    }
}

data class BrowserPickerUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val availableBrowsers: List<BrowserAppInfo> = emptyList(),
    val currentUri: String? = null,
    val currentHost: String? = null,
    val associatedHostRuleId: Long? = null,
    val selectedBrowser: String? = null,
    val browserPreferenceSet: Boolean = false,
    val uriBlocked: Boolean = false,
    val openDirectly: Boolean = false,
    val pickerDismissed: Boolean = false
)

 */