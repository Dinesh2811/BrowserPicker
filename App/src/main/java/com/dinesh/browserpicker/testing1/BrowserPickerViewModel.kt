package com.dinesh.browserpicker.testing1

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.di.DefaultDispatcher
import browserpicker.core.di.IoDispatcher
import browserpicker.core.di.MainDispatcher
import browserpicker.core.results.DomainResult
import browserpicker.core.results.UriValidationError
import browserpicker.domain.model.BrowserAppInfo
import browserpicker.domain.model.FolderType
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import browserpicker.domain.model.UriStatus
import browserpicker.domain.repository.BrowserStatsRepository
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.repository.HostRuleRepository
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.service.ParsedUri
import browserpicker.domain.service.UriParser
import browserpicker.presentation.features.browserpicker.*
import browserpicker.presentation.util.BrowserDefault
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.random.Random

/*
@OptIn(ExperimentalCoroutinesApi::class) // For flatMapLatest
@HiltViewModel
class BrowserPickerViewModel @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val uriParser: UriParser, // Added dependency
    private val browserPickerRepository: BrowserPickerRepository,
    private val uriHistoryRepository: UriHistoryRepository,
    private val hostRuleRepository: HostRuleRepository,
    private val browserStatsRepository: BrowserStatsRepository,
    // private val folderRepository: FolderRepository, // Not used in this immediate flow
) : ViewModel() {

    private val _browserPickerUiState = MutableStateFlow(BrowserPickerUiState(uiResult = UiResult.Idle))
    val browserPickerUiState: StateFlow<BrowserPickerUiState> = _browserPickerUiState.asStateFlow()

    // Input channel for URI processing requests
    private val _uriProcessingRequest = MutableSharedFlow<Pair<Uri, UriSource>>(
        replay = 0, // No replay needed, process as they come
        extraBufferCapacity = 1, // Buffer one item
        onBufferOverflow = BufferOverflow.DROP_OLDEST // Drop oldest if processing can't keep up
    )

    init {
        viewModelScope.launch {
            _uriProcessingRequest
                .distinctUntilChanged() // Avoid reprocessing identical consecutive requests
                .flatMapLatest { (uri, source) ->
                    // This flow builder handles the processing of a single URI request
                    buildProcessUriFlow(uri, source)
                }
                .collect { newState ->
                    _browserPickerUiState.value = newState
                }
        }
    }

    private fun buildProcessUriFlow(uri: Uri, source: UriSource): Flow<BrowserPickerUiState> = flow {
        // Reset to loading state for the current URI, keeping existing URI if it's the same
        // or updating if it's a new one. Clear previous results.
        emit(
            _browserPickerUiState.value.copy(
                uri = uri, // Update URI for current processing
                uiResult = UiResult.Loading,
                uriProcessingResult = null,
                allAvailableBrowsers = emptyList(),
                selectedBrowserAppInfo = null
            )
        )

        // 1. Validate UriSource
        if (source == UriSource.UNKNOWN) {
            emit(
                _browserPickerUiState.value.copy(
                    uiResult = UiResult.Error(TransientError.UNEXPECTED_ERROR_PROCESSING_URI)
                )
            )
            return@flow
        }

        // 2. Parse URI
        val parsedUri = when (val parsedUriResult = uriParser.parseAndValidateWebUri(uri)) {
            is DomainResult.Success -> parsedUriResult.data
            is DomainResult.Failure -> {
                val error = when (parsedUriResult.error) {
                    is UriValidationError.BlankOrEmpty -> TransientError.NULL_OR_EMPTY_URL
                    is UriValidationError.Invalid -> TransientError.INVALID_URL_FORMAT
                }
                emit(_browserPickerUiState.value.copy(uiResult = UiResult.Error(error)))
                return@flow
            }
        }

        // 3. Fetch HostRule (on IO dispatcher)
        val hostRuleResult = withContext(ioDispatcher) {
            hostRuleRepository.getHostRuleByHost(parsedUri.host)
        }

        val hostRule: HostRule? = when (hostRuleResult) {
            is DomainResult.Success -> hostRuleResult.data
            is DomainResult.Failure -> {
                emit(
                    _browserPickerUiState.value.copy(
                        uiResult = UiResult.Error(
                            PersistentError.HostRuleAccessFailed(
                                "Failed to access host rule for ${parsedUri.host}",
                                hostRuleResult.error.cause
                            )
                        )
                    )
                )
                return@flow // Stop processing if host rule fetch fails
            }
        }

        // Prepare initial UriProcessingResult
        var currentProcessingResult = UriProcessingResult(
            parsedUri = parsedUri,
            uriSource = source,
            hostRule = hostRule,
            isBookmarked = hostRule?.uriStatus == UriStatus.BOOKMARKED
            // effectivePreference is not set here as per earlier discussion
        )

        // 4. Decision Logic
        // 4a. Check if Blocked
        if (hostRule != null && hostRule.uriStatus == UriStatus.BLOCKED) {
            withContext(ioDispatcher) {
                uriHistoryRepository.addUriRecord(
                    uriString = parsedUri.originalString, host = parsedUri.host,
                    source = source, action = InteractionAction.BLOCKED_URI_ENFORCED,
                    chosenBrowser = null, associatedHostRuleId = hostRule.id
                ) // Consider handling DomainResult for logging/errors
            }
            currentProcessingResult = currentProcessingResult.copy(isBlocked = true)
            emit(
                _browserPickerUiState.value.copy(
                    uriProcessingResult = currentProcessingResult,
                    uiResult = UiResult.Blocked // Specific state for blocked URI
                )
            )
            return@flow
        }

        // 4b. Check for Preferred Browser (if not blocked)
        if (hostRule != null && hostRule.preferredBrowserPackage != null && hostRule.isPreferenceEnabled) {
            withContext(ioDispatcher) {
                uriHistoryRepository.addUriRecord(
                    uriString = parsedUri.originalString, host = parsedUri.host,
                    source = source, action = InteractionAction.OPENED_BY_PREFERENCE,
                    chosenBrowser = hostRule.preferredBrowserPackage, associatedHostRuleId = hostRule.id
                ) // Consider handling DomainResult
                hostRule.preferredBrowserPackage?.let { browserStatsRepository.recordBrowserUsage(it) }
            }
            currentProcessingResult = currentProcessingResult.copy(alwaysOpenBrowserPackage = hostRule.preferredBrowserPackage)
            emit(
                _browserPickerUiState.value.copy(
                    uriProcessingResult = currentProcessingResult,
                    uiResult = UiResult.Success(BrowserPickerUiEffect.AutoOpenBrowser)
                )
            )
            return@flow
        }

        // 4c. Show Picker: Load installed browsers
        val browsersDomainResult = withContext(ioDispatcher) {
            // Assuming getInstalledBrowserApps() gives a snapshot or a flow that completes.
            // .first() is suitable if it's a flow that emits the current list then completes,
            // or if it's a BehaviorFlow/StateFlow like source where first emission is current state.
            browserPickerRepository.getInstalledBrowserApps().first()
        }

        when (browsersDomainResult) {
            is DomainResult.Success -> {
                val browsers = browsersDomainResult.data
                if (browsers.isEmpty()) {
                    emit(
                        _browserPickerUiState.value.copy(
                            uriProcessingResult = currentProcessingResult,
                            uiResult = UiResult.Error(PersistentError.InstalledBrowserApps.Empty())
                        )
                    )
                } else {
                    emit(
                        _browserPickerUiState.value.copy(
                            uriProcessingResult = currentProcessingResult,
                            allAvailableBrowsers = browsers,
                            uiResult = UiResult.Success(BrowserPickerUiEffect.BrowserAppsLoaded)
                        )
                    )
                }
            }
            is DomainResult.Failure -> {
                val appError = browsersDomainResult.error
                // Map the specific AppError to a more specific UiError if possible,
                // otherwise use a general LoadFailed.
                val uiError = if (appError is PersistentError.InstalledBrowserApps) {
                    appError // If repository already returns a suitable UiError subtype
                } else {
                    PersistentError.InstalledBrowserApps.LoadFailed(
                        message = appError.message,
                        cause = appError.cause
                    )
                }
                emit(
                    _browserPickerUiState.value.copy(
                        uriProcessingResult = currentProcessingResult,
                        uiResult = UiResult.Error(uiError)
                    )
                )
            }
        }
    }
        .flowOn(defaultDispatcher) // Perform CPU-bound logic and flow operators on default dispatcher
        .catch { e -> // Catch unexpected exceptions from the flow itself or operators
            // Log.e("BrowserPickerViewModel", "Unhandled error in processUriFlow", e)
            emit(
                _browserPickerUiState.value.copy(
                    uiResult = UiResult.Error(
                        TransientError.UNEXPECTED_ERROR_PROCESSING_URI
                    )
                )
            )
        }

    /**
     * Called by the UI to signal that an incoming URI needs to be processed.
     */
    fun processIncomingUri(uri: Uri, source: UriSource) {
        viewModelScope.launch {
            _uriProcessingRequest.emit(uri to source)
        }
    }

    /**
     * Called by the UI after an event (like a Toast for a TransientError or an Effect)
     * has been handled, to reset the UiResult state to Idle.
     */
    fun consumeUiOutcome() {
        _browserPickerUiState.update { currentState ->
            when (val currentUiResult = currentState.uiResult) {
                is UiResult.Error -> {
                    // Only reset transient errors. Persistent errors should remain
                    // until the underlying issue is resolved or a new action is taken.
                    if (currentUiResult.error is TransientError) {
                        currentState.copy(uiResult = UiResult.Idle)
                    } else {
                        currentState // Persistent errors remain
                    }
                }
                is UiResult.Success -> {
                    // Success states often carry one-time effects. Reset to Idle after consumption.
                    currentState.copy(uiResult = UiResult.Idle)
                }
                UiResult.Blocked -> {
                    // If 'Blocked' is an event (like triggering a notification and dismissing),
                    // then reset to Idle. If it's a persistent state UI, this might differ.
                    // Based on description, it's an event.
                    currentState.copy(uiResult = UiResult.Idle)
                }
                UiResult.Loading, UiResult.Idle -> currentState // No change needed
            }
        }
    }

    // TODO: Implement other ViewModel logic:
    // - fun onBrowserSelected(browserAppInfo: BrowserAppInfo)
    // - fun openOnce()
    // - fun openAlways()
    // - fun toggleBookmark()
    // - fun blockHost()
    // - fun onSearchQueryChanged(query: String)


    private suspend fun getDefaultFolderId(folderType: FolderType, hostForErrorMessage: String): Long? {
        // Ensure default folders exist
        val ensureResult = folderRepository.ensureDefaultFoldersExist()
        if (ensureResult is DomainResult.Failure) {
            _browserPickerUiState.update {
                it.copy(uiResult = UiResult.Error(
                    PersistentError.FolderAccessFailed(
                        "Failed to ensure default folders for $hostForErrorMessage: ${ensureResult.error.message}",
                        ensureResult.error.cause
                    )
                ))
            }
            return null
        }

        val folderName = if (folderType == FolderType.BOOKMARK) "Bookmarked" else "Blocked"
        return when (val result = folderRepository.findFolderByNameAndParent(folderName, null, folderType)) {
            is DomainResult.Success -> {
                result.data?.id ?: run {
                    _browserPickerUiState.update {
                        it.copy(uiResult = UiResult.Error(
                            PersistentError.FolderAccessFailed(
                                "Default '$folderName' folder not found for $hostForErrorMessage."
                            )
                        ))
                    }
                    null
                }
            }
            is DomainResult.Failure -> {
                _browserPickerUiState.update {
                    it.copy(uiResult = UiResult.Error(
                        PersistentError.FolderAccessFailed(
                            "Error finding default '$folderName' folder for $hostForErrorMessage: ${result.error.message}",
                            result.error.cause
                        )
                    ))
                }
                null
            }
        }
    }


    fun onBrowserSelected(browserAppInfo: BrowserAppInfo) {
        _browserPickerUiState.update {
            it.copy(selectedBrowserAppInfo = browserAppInfo)
        }
    }

    fun openOnce() {
        viewModelScope.launch {
            val currentState = _browserPickerUiState.value
            val selectedBrowser = currentState.selectedBrowserAppInfo
            val parsedUri = currentState.uriProcessingResult?.parsedUri

            if (selectedBrowser == null) {
                _browserPickerUiState.update { it.copy(uiResult = UiResult.Error(TransientError.NO_BROWSER_SELECTED)) }
                return@launch
            }
            if (parsedUri == null) {
                _browserPickerUiState.update { it.copy(uiResult = UiResult.Error(TransientError.UNEXPECTED_ERROR_PROCESSING_URI.copy(message = "URI not available for opening."))) }
                return@launch
            }

            _browserPickerUiState.update { it.copy(uiResult = UiResult.Loading) } // Indicate work

            val hostRuleId = currentState.uriProcessingResult.hostRule?.id

            // Record URI History
            val historyResult = withContext(ioDispatcher) {
                uriHistoryRepository.addUriRecord(
                    uriString = parsedUri.originalString, host = parsedUri.host,
                    source = currentState.uriProcessingResult.uriSource,
                    action = InteractionAction.OPENED_ONCE,
                    chosenBrowser = selectedBrowser.packageName,
                    associatedHostRuleId = hostRuleId
                )
            }
            if (historyResult is DomainResult.Failure) {
                _browserPickerUiState.update {
                    it.copy(uiResult = UiResult.Error(
                        PersistentError.UriHistoryRecordFailed(
                            "Failed to record URI history: ${historyResult.error.message}",
                            historyResult.error.cause
                        ))
                    )
                }
                return@launch
            }

            // Record Browser Stats
            val statsResult = withContext(ioDispatcher) {
                browserStatsRepository.recordBrowserUsage(selectedBrowser.packageName)
            }
            if (statsResult is DomainResult.Failure) {
                // Non-critical, perhaps log and proceed or show transient error
                _browserPickerUiState.update {
                    it.copy(uiResult = UiResult.Error(
                        PersistentError.BrowserStatsRecordFailed(
                            "Failed to record browser stats: ${statsResult.error.message}",
                            statsResult.error.cause
                        ))
                    )
                }
                // Decide if this error should halt the process or just be a warning
                // For now, let's proceed to emit the success effect for opening
            }

            _browserPickerUiState.update {
                it.copy(uiResult = UiResult.Success(
                    BrowserPickerUiEffect.UriOpenedOnce(selectedBrowser.packageName, parsedUri.host))
                )
            }
        }
    }

    fun openAlways() {
        viewModelScope.launch {
            val currentState = _browserPickerUiState.value
            val selectedBrowser = currentState.selectedBrowserAppInfo
            val parsedUri = currentState.uriProcessingResult?.parsedUri
            val currentHostRule = currentState.uriProcessingResult?.hostRule

            if (selectedBrowser == null) {
                _browserPickerUiState.update { it.copy(uiResult = UiResult.Error(TransientError.NO_BROWSER_SELECTED)) }
                return@launch
            }
            if (parsedUri == null) {
                _browserPickerUiState.update { it.copy(uiResult = UiResult.Error(TransientError.UNEXPECTED_ERROR_PROCESSING_URI.copy(message = "URI not available for saving preference."))) }
                return@launch
            }

            _browserPickerUiState.update { it.copy(uiResult = UiResult.Loading) }

            // Save HostRule: Status remains same unless it was NONE, then no specific status change here.
            // If currentHostRule is null, status is NONE. If bookmarked, status is BOOKMARKED.
            // The preference is independent of bookmark/block status in terms of what this function does.
            // It just sets the preferred browser.
            val statusToSave = currentHostRule?.uriStatus ?: UriStatus.NONE
            val folderIdToSave = if (statusToSave == UriStatus.BOOKMARKED) {
                currentHostRule?.folderId ?: getDefaultFolderId(FolderType.BOOKMARK, parsedUri.host)
            } else if (statusToSave == UriStatus.BLOCKED) {
                currentHostRule?.folderId ?: getDefaultFolderId(FolderType.BLOCK, parsedUri.host)
            } else {
                null
            }
            // If getDefaultFolderId returned null due to error, the UI state is already updated,
            // but we should not proceed with saving if folderId is required and null.
            if ((statusToSave == UriStatus.BOOKMARKED || statusToSave == UriStatus.BLOCKED) && folderIdToSave == null) {
                // Error already set by getDefaultFolderId
                return@launch
            }


            val saveRuleResult = withContext(ioDispatcher) {
                hostRuleRepository.saveHostRule(
                    host = parsedUri.host,
                    status = statusToSave,
                    folderId = folderIdToSave,
                    preferredBrowser = selectedBrowser.packageName,
                    isPreferenceEnabled = true // Explicitly enabling
                )
            }

            val newHostRuleId: Long?
            when (saveRuleResult) {
                is DomainResult.Success -> {
                    newHostRuleId = saveRuleResult.data
                    val updatedHostRule = HostRule( // Construct a representation of the saved rule
                        id = newHostRuleId, host = parsedUri.host, uriStatus = statusToSave,
                        folderId = folderIdToSave, preferredBrowserPackage = selectedBrowser.packageName,
                        isPreferenceEnabled = true,
                        createdAt = currentHostRule?.createdAt ?: java.time.Instant.now(), // Approx
                        updatedAt = java.time.Instant.now() // Approx
                    )
                    _browserPickerUiState.update {
                        it.copy(uriProcessingResult = it.uriProcessingResult?.copy(
                            hostRule = updatedHostRule,
                            alwaysOpenBrowserPackage = selectedBrowser.packageName
                        ))
                    }
                }
                is DomainResult.Failure -> {
                    _browserPickerUiState.update {
                        it.copy(uiResult = UiResult.Error(
                            PersistentError.HostRuleAccessFailed(
                                "Failed to save browser preference: ${saveRuleResult.error.message}",
                                saveRuleResult.error.cause
                            ))
                        )
                    }
                    return@launch
                }
            }

            // Record URI History (as opened by preference since this action establishes it for future)
            val historyResult = withContext(ioDispatcher) {
                uriHistoryRepository.addUriRecord(
                    uriString = parsedUri.originalString, host = parsedUri.host,
                    source = currentState.uriProcessingResult.uriSource,
                    action = InteractionAction.OPENED_BY_PREFERENCE, // User chose to always open with this
                    chosenBrowser = selectedBrowser.packageName,
                    associatedHostRuleId = newHostRuleId
                )
            }
            if (historyResult is DomainResult.Failure) {
                _browserPickerUiState.update {
                    it.copy(uiResult = UiResult.Error(
                        PersistentError.UriHistoryRecordFailed(
                            "Failed to record URI history after preference save: ${historyResult.error.message}",
                            historyResult.error.cause
                        ))
                    )
                }
                return@launch
            }

            // Record Browser Stats
            val statsResult = withContext(ioDispatcher) {
                browserStatsRepository.recordBrowserUsage(selectedBrowser.packageName)
            }
            if (statsResult is DomainResult.Failure) {
                _browserPickerUiState.update {
                    it.copy(uiResult = UiResult.Error(
                        PersistentError.BrowserStatsRecordFailed(
                            "Failed to record browser stats after preference save: ${statsResult.error.message}",
                            statsResult.error.cause
                        ))
                    )
                }
                // Decide if this error should halt the process or just be a warning
            }

            _browserPickerUiState.update {
                it.copy(uiResult = UiResult.Success(
                    BrowserPickerUiEffect.OpenAndSavePreference(selectedBrowser.packageName, parsedUri.host))
                )
            }
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val currentState = _browserPickerUiState.value
            val parsedUri = currentState.uriProcessingResult?.parsedUri
            val currentHostRule = currentState.uriProcessingResult?.hostRule

            if (parsedUri == null) {
                _browserPickerUiState.update { it.copy(uiResult = UiResult.Error(TransientError.UNEXPECTED_ERROR_PROCESSING_URI.copy(message = "URI not available for bookmarking."))) }
                return@launch
            }

            _browserPickerUiState.update { it.copy(uiResult = UiResult.Loading) }

            val currentStatus = currentHostRule?.uriStatus ?: UriStatus.NONE
            val isCurrentlyBookmarked = currentStatus == UriStatus.BOOKMARKED

            val newStatus: UriStatus
            val newFolderId: Long?
            val effectMessageHost = parsedUri.host

            if (isCurrentlyBookmarked) { // Unbookmark
                newStatus = UriStatus.NONE
                newFolderId = null // Remove from any folder
            } else { // Bookmark (or change from Blocked to Bookmarked)
                newStatus = UriStatus.BOOKMARKED
                newFolderId = getDefaultFolderId(FolderType.BOOKMARK, parsedUri.host)
                if (newFolderId == null) { // Error already handled by getDefaultFolderId
                    _browserPickerUiState.update { prev -> prev.copy(uiResult = prev.uiResult) } // No change if already error
                    return@launch
                }
            }

            val saveRuleResult = withContext(ioDispatcher) {
                hostRuleRepository.saveHostRule(
                    host = parsedUri.host,
                    status = newStatus,
                    folderId = newFolderId,
                    // Preserve existing preference if any, unless this action should clear it
                    preferredBrowser = currentHostRule?.preferredBrowserPackage,
                    isPreferenceEnabled = currentHostRule?.isPreferenceEnabled ?: true
                )
            }

            when (saveRuleResult) {
                is DomainResult.Success -> {
                    val newHostRule = HostRule(
                        id = saveRuleResult.data, host = parsedUri.host, uriStatus = newStatus,
                        folderId = newFolderId, preferredBrowserPackage = currentHostRule?.preferredBrowserPackage,
                        isPreferenceEnabled = currentHostRule?.isPreferenceEnabled ?: true,
                        createdAt = currentHostRule?.createdAt ?: java.time.Instant.now(),
                        updatedAt = java.time.Instant.now()
                    )
                    _browserPickerUiState.update {
                        it.copy(
                            uriProcessingResult = it.uriProcessingResult?.copy(
                                hostRule = newHostRule,
                                isBookmarked = (newStatus == UriStatus.BOOKMARKED)
                            ),
                            uiResult = UiResult.Success(BrowserPickerUiEffect.UriBookmarkChanged(effectMessageHost, newStatus == UriStatus.BOOKMARKED))
                        )
                    }
                }
                is DomainResult.Failure -> {
                    _browserPickerUiState.update {
                        it.copy(uiResult = UiResult.Error(
                            PersistentError.HostRuleAccessFailed(
                                "Failed to update bookmark status: ${saveRuleResult.error.message}",
                                saveRuleResult.error.cause
                            ))
                        )
                    }
                }
            }
        }
    }

    fun blockHost() {
        viewModelScope.launch {
            val currentState = _browserPickerUiState.value
            val parsedUri = currentState.uriProcessingResult?.parsedUri
            val currentHostRule = currentState.uriProcessingResult?.hostRule

            if (parsedUri == null) {
                _browserPickerUiState.update { it.copy(uiResult = UiResult.Error(TransientError.UNEXPECTED_ERROR_PROCESSING_URI.copy(message = "URI not available for blocking."))) }
                return@launch
            }

            _browserPickerUiState.update { it.copy(uiResult = UiResult.Loading) }

            val newStatus = UriStatus.BLOCKED
            val effectMessageHost = parsedUri.host
            val newFolderId = getDefaultFolderId(FolderType.BLOCK, parsedUri.host)

            if (newFolderId == null) { // Error already handled
                _browserPickerUiState.update { prev -> prev.copy(uiResult = prev.uiResult) } // No change if already error
                return@launch
            }

            val saveRuleResult = withContext(ioDispatcher) {
                hostRuleRepository.saveHostRule(
                    host = parsedUri.host,
                    status = newStatus,
                    folderId = newFolderId,
                    // Blocking might also clear browser preference for this host, or keep it disabled
                    preferredBrowser = currentHostRule?.preferredBrowserPackage, // Keep for potential unblock
                    isPreferenceEnabled = false // Disable preference when blocking
                )
            }

            val newHostRuleId: Long?
            when (saveRuleResult) {
                is DomainResult.Success -> {
                    newHostRuleId = saveRuleResult.data
                    val newHostRule = HostRule(
                        id = newHostRuleId, host = parsedUri.host, uriStatus = newStatus,
                        folderId = newFolderId, preferredBrowserPackage = currentHostRule?.preferredBrowserPackage,
                        isPreferenceEnabled = false, // Explicitly false
                        createdAt = currentHostRule?.createdAt ?: java.time.Instant.now(),
                        updatedAt = java.time.Instant.now()
                    )
                    _browserPickerUiState.update {
                        it.copy(
                            uriProcessingResult = it.uriProcessingResult?.copy(
                                hostRule = newHostRule,
                                isBookmarked = false, // Blocking overrides bookmark
                                isBlocked = true
                            )
                            // The UI will dismiss on UriBlocked effect.
                            // No need to also set UiResult.Blocked here, as that's for initial processing.
                        )
                    }
                }
                is DomainResult.Failure -> {
                    _browserPickerUiState.update {
                        it.copy(uiResult = UiResult.Error(
                            PersistentError.HostRuleAccessFailed(
                                "Failed to block host: ${saveRuleResult.error.message}",
                                saveRuleResult.error.cause
                            ))
                        )
                    }
                    return@launch
                }
            }

            // Record URI History - user manually blocked this host
            // TODO: Consider adding a more specific InteractionAction like HOST_MANUALLY_BLOCKED
            val historyResult = withContext(ioDispatcher) {
                uriHistoryRepository.addUriRecord(
                    uriString = parsedUri.originalString, host = parsedUri.host,
                    source = currentState.uriProcessingResult.uriSource,
                    action = InteractionAction.BLOCKED_URI_ENFORCED, // Re-using, but might need dedicated action
                    chosenBrowser = null,
                    associatedHostRuleId = newHostRuleId
                )
            }
            if (historyResult is DomainResult.Failure) {
                // Log or show non-critical error, main action (blocking) succeeded
                // For now, we'll update the UI to reflect the block success regardless
                println("Warning: Failed to record history for block action: ${historyResult.error.message}")
            }

            _browserPickerUiState.update {
                it.copy(uiResult = UiResult.Success(BrowserPickerUiEffect.UriBlockStatusChanged(effectMessageHost, true)))
            }
        }
    }

}

 */


// Inside BrowserPickerBottomSheetScreen's LaunchedEffect(browserPickerUiState.uiResult)


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BrowserPickerViewModel @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val uriParser: UriParser,
    private val browserPickerRepository: BrowserPickerRepository,
    private val uriHistoryRepository: UriHistoryRepository,
    private val hostRuleRepository: HostRuleRepository,
    private val folderRepository: FolderRepository, // Added FolderRepository
    private val browserStatsRepository: BrowserStatsRepository,
) : ViewModel() {

    private val _browserPickerUiState = MutableStateFlow(BrowserPickerUiState(uiResult = UiResult.Idle))
    val browserPickerUiState: StateFlow<BrowserPickerUiState> = _browserPickerUiState.asStateFlow()

    private val _uriProcessingRequest = MutableSharedFlow<Pair<Uri, UriSource>>(
        replay = 0, extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        processIncomingUri()
        viewModelScope.launch {
            _uriProcessingRequest
                .distinctUntilChanged()
                .flatMapLatest { (uri, source) -> buildProcessUriFlow(uri, source) }
                .collect { newState -> _browserPickerUiState.value = newState }
        }

        // Ensure default folders exist on ViewModel initialization (or at app startup)
        // This is a good place to call it, or alternatively, in an Application class.
        viewModelScope.launch(ioDispatcher) {
            folderRepository.ensureDefaultFoldersExist()
            // We can log errors here if needed, but for this context,
            // we assume it succeeds or subsequent operations will handle missing folders gracefully.
        }
    }

    fun generateRandomBrowserApps(count: Int = 10): List<BrowserAppInfo> {
        val appNames = listOf("Chrome", "1DM+", "Safari", "Soul Browser", "Edge", "Brave", "Samsung Internet Browser")
        val packageNames = listOf(
            "com.android.chrome",
            "idm.internet.download.manager.plus",
            "com.apple.safari",
            "com.mycompany.app.soulbrowser",
            "com.microsoft.emmx",
            "com.brave.browser",
            "com.sec.android.app.sbrowser",
        )

        return List(count) {
            val randomIndex = Random.Default.nextInt(appNames.size)
            BrowserAppInfo(
                appName = appNames[randomIndex],
                packageName = "${packageNames[randomIndex]}_$it",
//                    versionName = "${Random.Default.nextInt(1, 10)}.${Random.Default.nextInt(0, 10)}.${Random.Default.nextInt(0, 100)}",
//                    versionCode = Random.Default.nextLong(1, 1000),
//                isDefaultBrowser = Random.Default.nextBoolean(),
//                appIcon = null,
            )
        }
    }
    private fun buildProcessUriFlow(uri: Uri, source: UriSource): Flow<BrowserPickerUiState> = flow {
        emit(
            _browserPickerUiState.value.copy(
                uri = uri, uiResult = UiResult.Loading, uriProcessingResult = null,
                allAvailableBrowsers = emptyList(), selectedBrowserAppInfo = null
            )
        )

        if (source == UriSource.UNKNOWN) {
            emit(_browserPickerUiState.value.copy(uiResult = UiResult.Error(TransientError.UNEXPECTED_ERROR_PROCESSING_URI)))
            return@flow
        }

        val parsedUriResult = uriParser.parseAndValidateWebUri(uri)
        val parsedUri: ParsedUri = when (parsedUriResult) {
            is DomainResult.Success -> parsedUriResult.data
            is DomainResult.Failure -> {
                val error = when (parsedUriResult.error) {
                    is UriValidationError.BlankOrEmpty -> TransientError.NULL_OR_EMPTY_URL
                    is UriValidationError.Invalid -> TransientError.INVALID_URL_FORMAT
                }
                emit(_browserPickerUiState.value.copy(uiResult = UiResult.Error(error)))
                return@flow
            }
        }

        val hostRuleResult = withContext(ioDispatcher) { hostRuleRepository.getHostRuleByHost(parsedUri.host) }
        val hostRule: HostRule? = when (hostRuleResult) {
            is DomainResult.Success -> hostRuleResult.data
            is DomainResult.Failure -> {
                emit(_browserPickerUiState.value.copy(uiResult = UiResult.Error(PersistentError.HostRuleAccessFailed("Failed to access host rule for ${parsedUri.host}", hostRuleResult.error.cause))))
                return@flow
            }
        }

        var currentProcessingResult = UriProcessingResult(
            parsedUri = parsedUri, uriSource = source, hostRule = hostRule,
            isBookmarked = hostRule?.uriStatus == UriStatus.BOOKMARKED
        )

        if (hostRule?.uriStatus == UriStatus.BLOCKED) {
            withContext(ioDispatcher) {
                uriHistoryRepository.addUriRecord(
                    uriString = parsedUri.originalString, host = parsedUri.host, source = source,
                    action = InteractionAction.BLOCKED_URI_ENFORCED, chosenBrowser = null, associatedHostRuleId = hostRule.id
                )
            }
            currentProcessingResult = currentProcessingResult.copy(isBlocked = true)
            emit(_browserPickerUiState.value.copy(uriProcessingResult = currentProcessingResult, uiResult = UiResult.Blocked))
            return@flow
        }

        if (hostRule?.preferredBrowserPackage != null && hostRule.isPreferenceEnabled) {
            withContext(ioDispatcher) {
                uriHistoryRepository.addUriRecord(
                    uriString = parsedUri.originalString, host = parsedUri.host, source = source,
                    action = InteractionAction.OPENED_BY_PREFERENCE, chosenBrowser = hostRule.preferredBrowserPackage, associatedHostRuleId = hostRule.id
                )
                hostRule.preferredBrowserPackage?.let { browserStatsRepository.recordBrowserUsage(it) }
            }
            currentProcessingResult = currentProcessingResult.copy(alwaysOpenBrowserPackage = hostRule.preferredBrowserPackage)
            emit(_browserPickerUiState.value.copy(uriProcessingResult = currentProcessingResult, uiResult = UiResult.Success(BrowserPickerUiEffect.AutoOpenBrowser)))
            return@flow
        }

        val browsers = generateRandomBrowserApps()
        _browserPickerUiState.value.copy(uriProcessingResult = currentProcessingResult, allAvailableBrowsers = browsers, uiResult = UiResult.Success(BrowserPickerUiEffect.BrowserAppsLoaded))
        emit(_browserPickerUiState.value.copy(uriProcessingResult = currentProcessingResult, allAvailableBrowsers = browsers, uiResult = UiResult.Success(BrowserPickerUiEffect.BrowserAppsLoaded)))

//        val browsersDomainResult = withContext(ioDispatcher) { browserPickerRepository.getInstalledBrowserApps().first() }
//        when (browsersDomainResult) {
//            is DomainResult.Success -> {
////                val browsers = browsersDomainResult.data
//                val browsers = generateRandomBrowserApps()
//                if (browsers.isEmpty()) {
//                    emit(_browserPickerUiState.value.copy(uriProcessingResult = currentProcessingResult, uiResult = UiResult.Error(PersistentError.InstalledBrowserApps.Empty())))
//                } else {
//                    emit(_browserPickerUiState.value.copy(uriProcessingResult = currentProcessingResult, allAvailableBrowsers = browsers, uiResult = UiResult.Success(BrowserPickerUiEffect.BrowserAppsLoaded)))
//                }
//            }
//            is DomainResult.Failure -> {
//                val appError = browsersDomainResult.error
//                val uiError = if (appError is PersistentError.InstalledBrowserApps) appError
//                else PersistentError.InstalledBrowserApps.LoadFailed(appError.message, appError.cause)
//                emit(_browserPickerUiState.value.copy(uriProcessingResult = currentProcessingResult, uiResult = UiResult.Error(uiError)))
//            }
//        }
    }
        .flowOn(defaultDispatcher)
        .catch { e ->
            emit(_browserPickerUiState.value.copy(uiResult = UiResult.Error(TransientError.UNEXPECTED_ERROR_PROCESSING_URI)))
        }

    fun processIncomingUri(uri: Uri = BrowserDefault.URL.toUri(), source: UriSource = UriSource.INTENT) {
        viewModelScope.launch { _uriProcessingRequest.emit(uri to source) }
    }

    fun consumeUiOutcome() {
        _browserPickerUiState.update { currentState ->
            when (val currentUiResult = currentState.uiResult) {
                is UiResult.Error -> if (currentUiResult.error is TransientError) currentState.copy(uiResult = UiResult.Idle) else currentState
                is UiResult.Success, UiResult.Blocked -> currentState.copy(uiResult = UiResult.Idle)
                else -> currentState
            }
        }
    }

    // --- New Action Handlers ---

    fun onBrowserSelected(browserAppInfo: BrowserAppInfo) {
        _browserPickerUiState.update { it.copy(selectedBrowserAppInfo = browserAppInfo) }
    }

    fun openOnce() {
        viewModelScope.launch {
            val currentState = _browserPickerUiState.value
            val selectedBrowser = currentState.selectedBrowserAppInfo
            val processingResult = currentState.uriProcessingResult

            if (selectedBrowser == null) {
                _browserPickerUiState.update { it.copy(uiResult = UiResult.Error(TransientError.NO_BROWSER_SELECTED)) }
                return@launch
            }
            if (processingResult == null) {
                _browserPickerUiState.update { it.copy(uiResult = UiResult.Error(TransientError.UNEXPECTED_ERROR_PROCESSING_URI)) }
                return@launch
            }

            withContext(ioDispatcher) {
                uriHistoryRepository.addUriRecord(
                    uriString = processingResult.parsedUri.originalString,
                    host = processingResult.parsedUri.host,
                    source = processingResult.uriSource,
                    action = InteractionAction.OPENED_ONCE,
                    chosenBrowser = selectedBrowser.packageName,
                    associatedHostRuleId = processingResult.hostRule?.id
                )
                browserStatsRepository.recordBrowserUsage(selectedBrowser.packageName)
            }
            // Update UI state with effect to trigger launch
            _browserPickerUiState.update {
//                it.copy(uiResult = UiResult.Success(BrowserPickerUiEffect.UriOpenedOnce(selectedBrowser.packageName, processingResult.parsedUri.host)))
                it.copy(uiResult = UiResult.Success(BrowserPickerUiEffect.UriOpenedOnce(selectedBrowser.packageName)))
            }
        }
    }

    fun openAlways() {
        viewModelScope.launch {
            val currentState = _browserPickerUiState.value
            val selectedBrowser = currentState.selectedBrowserAppInfo
            val processingResult = currentState.uriProcessingResult

            if (selectedBrowser == null) {
                _browserPickerUiState.update { it.copy(uiResult = UiResult.Error(TransientError.NO_BROWSER_SELECTED)) }
                return@launch
            }
            if (processingResult == null || processingResult.parsedUri.host.isBlank()) {
                _browserPickerUiState.update { it.copy(uiResult = UiResult.Error(TransientError.UNEXPECTED_ERROR_PROCESSING_URI)) }
                return@launch
            }

            val host = processingResult.parsedUri.host
            val currentHostRule = processingResult.hostRule

            // Save preference
            val saveResult = withContext(ioDispatcher) {
                hostRuleRepository.saveHostRule(
                    host = host,
                    status = currentHostRule?.uriStatus ?: UriStatus.NONE, // Keep existing status or NONE
                    folderId = currentHostRule?.folderId, // Keep existing folderId
                    preferredBrowser = selectedBrowser.packageName,
                    isPreferenceEnabled = true
                )
            }

            when (saveResult) {
                is DomainResult.Success -> {
                    val newHostRuleId = saveResult.data
                    // Fetch the updated host rule to reflect in UI
                    val updatedHostRule = (hostRuleRepository.getHostRuleById(newHostRuleId) as? DomainResult.Success)?.data

                    // Record history for this specific action (opened once then set preference)
                    withContext(ioDispatcher) {
                        uriHistoryRepository.addUriRecord(
                            uriString = processingResult.parsedUri.originalString,
                            host = host,
                            source = processingResult.uriSource,
                            action = InteractionAction.OPENED_ONCE, // User chose to open and set preference
                            chosenBrowser = selectedBrowser.packageName,
                            associatedHostRuleId = newHostRuleId
                        )
                        browserStatsRepository.recordBrowserUsage(selectedBrowser.packageName)
                    }
                    _browserPickerUiState.update {
                        it.copy(
                            uriProcessingResult = it.uriProcessingResult?.copy(
                                hostRule = updatedHostRule,
                                alwaysOpenBrowserPackage = selectedBrowser.packageName
                            ),
                            uiResult = UiResult.Success(BrowserPickerUiEffect.SettingsSaved) // This tells UI preference is saved
                            // The UI will then likely also trigger the launch via AutoOpenBrowser or UriOpenedOnce
                            // For consistency with `openOnce`, let's use UriOpenedOnce for immediate launch
                            // The next time this URI is intercepted, AutoOpenBrowser will apply.
                            // We could chain effects, but simpler to let UI dismiss on SettingsSaved,
                            // and the host activity/fragment can handle the launch separately.
                            // Or, if SettingsSaved implies immediate launch, then also send AutoOpenBrowser.
                            // Let's make SettingsSaved also imply the launch for this time.
                        )
                    }
                    // After settings are saved, we also want to trigger the browser launch for this instance.
                    _browserPickerUiState.update {
                        it.copy(uiResult = UiResult.Success(BrowserPickerUiEffect.AutoOpenBrowser))
                    }

                }
                is DomainResult.Failure -> {
                    _browserPickerUiState.update {
                        it.copy(uiResult = UiResult.Error(PersistentError.HostRuleAccessFailed("Failed to save preference.", saveResult.error.cause)))
                    }
                }
            }
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val currentState = _browserPickerUiState.value
            val processingResult = currentState.uriProcessingResult

            if (processingResult == null || processingResult.parsedUri.host.isBlank()) {
                _browserPickerUiState.update { it.copy(uiResult = UiResult.Error(TransientError.UNEXPECTED_ERROR_PROCESSING_URI)) }
                return@launch
            }

            val host = processingResult.parsedUri.host
            val currentHostRule = processingResult.hostRule
            val isCurrentlyBookmarked = currentHostRule?.uriStatus == UriStatus.BOOKMARKED

            val newStatus = if (isCurrentlyBookmarked) UriStatus.NONE else UriStatus.BOOKMARKED
            var newFolderId: Long? = currentHostRule?.folderId

            if (newStatus == UriStatus.BOOKMARKED) {
                // Get default bookmark folder
                val defaultBookmarkFolderResult = withContext(ioDispatcher) {
                    folderRepository.getRootFoldersByType(FolderType.BOOKMARK).first()
                }
                when (defaultBookmarkFolderResult) {
                    is DomainResult.Success -> {
                        newFolderId = defaultBookmarkFolderResult.data.firstOrNull()?.id
                        if (newFolderId == null) {
                            _browserPickerUiState.update { it.copy(uiResult = UiResult.Error(PersistentError.FolderAccessFailed("Default bookmark folder not found."))) }
                            return@launch
                        }
                    }
                    is DomainResult.Failure -> {
                        _browserPickerUiState.update { it.copy(uiResult = UiResult.Error(PersistentError.FolderAccessFailed("Failed to get bookmark folder.", defaultBookmarkFolderResult.error.cause))) }
                        return@launch
                    }
                }
            } else { // Unbookmarking
                // If it was in a bookmark folder, clear the folderId
                currentHostRule?.folderId?.let { folderId ->
                    val folderFlow = folderRepository.getFolder(folderId)
                    val folderResult = withContext(ioDispatcher) { folderFlow.first() } // Assuming it emits once or is a StateFlow
                    if (folderResult is DomainResult.Success && folderResult.data?.type == FolderType.BOOKMARK) {
                        newFolderId = null
                    }
                }
            }


            val saveResult = withContext(ioDispatcher) {
                hostRuleRepository.saveHostRule(
                    host = host,
                    status = newStatus,
                    folderId = newFolderId,
                    preferredBrowser = currentHostRule?.preferredBrowserPackage, // Keep preference
                    isPreferenceEnabled = currentHostRule?.isPreferenceEnabled ?: true // Keep preference state
                )
            }

            when (saveResult) {
                is DomainResult.Success -> {
                    val newHostRuleId = saveResult.data
                    val updatedHostRule = (hostRuleRepository.getHostRuleById(newHostRuleId) as? DomainResult.Success)?.data
                    _browserPickerUiState.update {
                        it.copy(
                            uriProcessingResult = it.uriProcessingResult?.copy(
                                hostRule = updatedHostRule,
                                isBookmarked = (newStatus == UriStatus.BOOKMARKED)
                            ),
                            uiResult = UiResult.Success(BrowserPickerUiEffect.UriBookmarked) // Single effect for simplicity
                        )
                    }
                }
                is DomainResult.Failure -> {
                    _browserPickerUiState.update {
                        it.copy(uiResult = UiResult.Error(PersistentError.HostRuleAccessFailed("Failed to update bookmark.", saveResult.error.cause)))
                    }
                }
            }
        }
    }

    fun blockHost() {
        viewModelScope.launch {
            val currentState = _browserPickerUiState.value
            val processingResult = currentState.uriProcessingResult

            if (processingResult == null || processingResult.parsedUri.host.isBlank()) {
                _browserPickerUiState.update { it.copy(uiResult = UiResult.Error(TransientError.UNEXPECTED_ERROR_PROCESSING_URI)) }
                return@launch
            }

            val host = processingResult.parsedUri.host
            val currentHostRule = processingResult.hostRule

            // Get default block folder
            var newFolderId: Long? = null
            val defaultBlockFolderResult = withContext(ioDispatcher) {
                folderRepository.getRootFoldersByType(FolderType.BLOCK).first()
            }
            when (defaultBlockFolderResult) {
                is DomainResult.Success -> {
                    newFolderId = defaultBlockFolderResult.data.firstOrNull()?.id
                    if (newFolderId == null) {
                        _browserPickerUiState.update { it.copy(uiResult = UiResult.Error(PersistentError.FolderAccessFailed("Default block folder not found."))) }
                        return@launch
                    }
                }
                is DomainResult.Failure -> {
                    _browserPickerUiState.update { it.copy(uiResult = UiResult.Error(PersistentError.FolderAccessFailed("Failed to get block folder.", defaultBlockFolderResult.error.cause))) }
                    return@launch
                }
            }


            val saveResult = withContext(ioDispatcher) {
                hostRuleRepository.saveHostRule(
                    host = host,
                    status = UriStatus.BLOCKED,
                    folderId = newFolderId, // Associate with default "Blocked" folder
                    preferredBrowser = null, // Clear preference when blocking
                    isPreferenceEnabled = false // Disable preference when blocking
                )
            }

            when (saveResult) {
                is DomainResult.Success -> {
                    val newHostRuleId = saveResult.data
                    val updatedHostRule = (hostRuleRepository.getHostRuleById(newHostRuleId) as? DomainResult.Success)?.data

                    // Record this action as blocked
                    withContext(ioDispatcher) {
                        uriHistoryRepository.addUriRecord(
                            uriString = processingResult.parsedUri.originalString,
                            host = host,
                            source = processingResult.uriSource,
                            action = InteractionAction.BLOCKED_URI_ENFORCED, // Explicitly blocked by user
                            chosenBrowser = null,
                            associatedHostRuleId = newHostRuleId
                        )
                    }

                    _browserPickerUiState.update {
                        it.copy(
                            uriProcessingResult = it.uriProcessingResult?.copy(
                                hostRule = updatedHostRule,
                                isBookmarked = false, // Cannot be bookmarked if blocked
                                isBlocked = true,
                                alwaysOpenBrowserPackage = null
                            ),
                            uiResult = UiResult.Success(BrowserPickerUiEffect.UriBlocked)
                        )
                    }
                }
                is DomainResult.Failure -> {
                    _browserPickerUiState.update {
                        it.copy(uiResult = UiResult.Error(PersistentError.HostRuleAccessFailed("Failed to block host.", saveResult.error.cause)))
                    }
                }
            }
        }
    }

    // Helper for PersistentError for folders (if not already defined)

    // Add to PersistentError sealed interface
    // data class FolderAccessFailed(override val message: String, override val cause: Throwable? = null): PersistentError
    // Example of how to update PersistentError:
    // sealed interface PersistentError: UiError {
    //     // ... existing errors
    //     data class FolderAccessFailed(override val message: String, override val cause: Throwable? = null): PersistentError
    // }
    // Make sure your PersistentError class is updated with FolderAccessFailed
}

// Ensure your PersistentError sealed interface is updated:
// Add this inside your existing `PersistentError` sealed interface
// data class FolderAccessFailed(override val message: String, override val cause: Throwable? = null): PersistentError