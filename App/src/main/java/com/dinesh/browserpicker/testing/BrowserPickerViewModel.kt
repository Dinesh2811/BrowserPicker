package com.dinesh.browserpicker.testing

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.di.DefaultDispatcher
import browserpicker.core.di.IoDispatcher
import browserpicker.core.di.MainDispatcher
import browserpicker.core.results.DomainResult
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

@OptIn(FlowPreview::class)
@HiltViewModel
class BrowserPickerViewModel @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    private val browserPickerRepository: BrowserPickerRepository,
    private val uriHistoryRepository: UriHistoryRepository,
    private val hostRuleRepository: HostRuleRepository,
    private val folderRepository: FolderRepository,
    private val browserStatsRepository: BrowserStatsRepository,
    private val uriParser: UriParser
): ViewModel() {
    private val _browserPickerUiState: MutableStateFlow<BrowserPickerUiState> = MutableStateFlow(BrowserPickerUiState(uiResult = UiResult.Loading))
    val browserPickerUiState: StateFlow<BrowserPickerUiState> = _browserPickerUiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = _browserPickerUiState.value
    )

    init {
        loadInstalledBrowsers()
        viewModelScope.launch {
            folderRepository.ensureDefaultFoldersExist()
        }
    }

    private fun loadInstalledBrowsers() {
        viewModelScope.launch {
            browserPickerRepository.getInstalledBrowserApps()
                .flowOn(ioDispatcher)
                .collect { result ->
                    when (result) {
                        is DomainResult.Success -> {
                            if (result.data.isEmpty()) {
                                _browserPickerUiState.update { state ->
                                    state.copy(
                                        uiResult = UiResult.Error(PersistentError.InstalledBrowserApps.Empty())
                                    )
                                }
                            } else {
                                _browserPickerUiState.update { state ->
                                    state.copy(
                                        allAvailableBrowsers = result.data,
                                        uiResult = UiResult.Success(BrowserPickerUiEffect.BrowserAppsLoaded)
                                    )
                                }
                            }
                        }
                        is DomainResult.Failure -> {
                            _browserPickerUiState.update { state ->
                                state.copy(
                                    uiResult = UiResult.Error(
                                        PersistentError.InstalledBrowserApps.LoadFailed(
                                            message = result.error.message,
                                            cause = result.error.cause
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
        }
    }

    fun processUri(uri: Uri, source: UriSource) {
        viewModelScope.launch {
            _browserPickerUiState.update { it.copy(uri = uri, uiResult = UiResult.Loading) }

            val parseResult = uriParser.parseAndValidateWebUri(uri)
            when (parseResult) {
                is DomainResult.Success -> {
                    val parsedUri = parseResult.data
                    checkHostRuleAndProcess(parsedUri, source)
                }
                is DomainResult.Failure -> {
                    _browserPickerUiState.update { state ->
                        state.copy(uiResult = UiResult.Error(TransientError.INVALID_URL_FORMAT))
                    }
                }
            }
        }
    }

    private suspend fun checkHostRuleAndProcess(parsedUri: ParsedUri, source: UriSource) {
        when (val hostRuleResult = hostRuleRepository.getHostRuleByHost(parsedUri.host)) {
            is DomainResult.Success -> {
                val hostRule = hostRuleResult.data

                if (hostRule != null) {
                    when (hostRule.uriStatus) {
                        UriStatus.BLOCKED -> {
                            handleBlockedUri(parsedUri, source, hostRule)
                            return
                        }
                        UriStatus.BOOKMARKED -> {
                            if (hostRule.preferredBrowserPackage != null && hostRule.isPreferenceEnabled) {
                                handlePreferredBrowserUri(parsedUri, source, hostRule)
                                return
                            }
                        }
                        UriStatus.NONE -> {
                            if (hostRule.preferredBrowserPackage != null && hostRule.isPreferenceEnabled) {
                                handlePreferredBrowserUri(parsedUri, source, hostRule)
                                return
                            }
                        }
                        UriStatus.UNKNOWN -> { /* Treat as no rule */ }
                    }
                }

                // No blocking or auto-opening, show UI
                val uriProcessingResult = UriProcessingResult(
                    parsedUri = parsedUri,
                    uriSource = source,
                    isBlocked = false,
                    isBookmarked = hostRule?.uriStatus == UriStatus.BOOKMARKED,
                    hostRule = hostRule
                )

                _browserPickerUiState.update { state ->
                    state.copy(
                        uriProcessingResult = uriProcessingResult,
                        uiResult = UiResult.Idle
                    )
                }
            }
            is DomainResult.Failure -> {
                _browserPickerUiState.update { state ->
                    state.copy(uiResult = UiResult.Error(TransientError.HOST_RULE_ACCESS_FAILED))
                }
            }
        }
    }

    private suspend fun handleBlockedUri(parsedUri: ParsedUri, source: UriSource, hostRule: HostRule) {
        // Log the blocked URI interaction
        uriHistoryRepository.addUriRecord(
            uriString = parsedUri.originalString,
            host = parsedUri.host,
            source = source,
            action = InteractionAction.BLOCKED_URI_ENFORCED,
            chosenBrowser = null,
            associatedHostRuleId = hostRule.id
        )

        // Update UI state to show blocked status
        _browserPickerUiState.update { state ->
            state.copy(uiResult = UiResult.Blocked)
        }
    }

    private suspend fun handlePreferredBrowserUri(parsedUri: ParsedUri, source: UriSource, hostRule: HostRule) {
        // Null safety check for preferred browser
        val preferredBrowser = hostRule.preferredBrowserPackage
        if (preferredBrowser == null) {
            checkHostRuleAndProcess(parsedUri, source)
            return
        }

        // Log the preferred browser URI interaction
        uriHistoryRepository.addUriRecord(
            uriString = parsedUri.originalString,
            host = parsedUri.host,
            source = source,
            action = InteractionAction.OPENED_BY_PREFERENCE,
            chosenBrowser = preferredBrowser,
            associatedHostRuleId = hostRule.id
        )

        // Record browser usage
        browserStatsRepository.recordBrowserUsage(preferredBrowser)

        // Update UI state to auto-open browser
        val uriProcessingResult = UriProcessingResult(
            parsedUri = parsedUri,
            uriSource = source,
            isBlocked = false,
            isBookmarked = hostRule.uriStatus == UriStatus.BOOKMARKED,
            alwaysOpenBrowserPackage = preferredBrowser,
            hostRule = hostRule
        )

        _browserPickerUiState.update { state ->
            state.copy(
                uriProcessingResult = uriProcessingResult,
                uiResult = UiResult.Success(BrowserPickerUiEffect.AutoOpenBrowser)
            )
        }
    }

    fun selectBrowser(browserAppInfo: BrowserAppInfo) {
        _browserPickerUiState.update { state ->
            state.copy(selectedBrowserAppInfo = browserAppInfo)
        }
    }

    fun openUriWithSelectedBrowser(savePreference: Boolean = false) {
        val currentState = _browserPickerUiState.value
        val selectedBrowser = currentState.selectedBrowserAppInfo
        val uriProcessingResult = currentState.uriProcessingResult

        if (selectedBrowser == null) {
            _browserPickerUiState.update { state ->
                state.copy(uiResult = UiResult.Error(TransientError.NO_BROWSER_SELECTED))
            }
            return
        }

        if (uriProcessingResult == null) {
            _browserPickerUiState.update { state ->
                state.copy(uiResult = UiResult.Error(TransientError.UNEXPECTED_ERROR_PROCESSING_URI))
            }
            return
        }

        viewModelScope.launch {
            // Save preference if requested
            if (savePreference) {
                saveHostPreference(uriProcessingResult.parsedUri.host, selectedBrowser.packageName)
            }

            // Record URI history
            val interactionAction = if (savePreference) {
                InteractionAction.OPENED_BY_PREFERENCE
            } else {
                InteractionAction.OPENED_ONCE
            }

            uriHistoryRepository.addUriRecord(
                uriString = uriProcessingResult.parsedUri.originalString,
                host = uriProcessingResult.parsedUri.host,
                source = uriProcessingResult.uriSource,
                action = interactionAction,
                chosenBrowser = selectedBrowser.packageName,
                associatedHostRuleId = uriProcessingResult.hostRule?.id
            )

            // Record browser usage
            browserStatsRepository.recordBrowserUsage(selectedBrowser.packageName)

            // Update UI state
            _browserPickerUiState.update { state ->
                state.copy(
                    uiResult = UiResult.Success(
                        BrowserPickerUiEffect.UriOpenedOnce(selectedBrowser.packageName)
                    )
                )
            }
        }
    }

    private suspend fun saveHostPreference(host: String, browserPackage: String) {
        hostRuleRepository.getHostRuleByHost(host).let { result ->
            when (result) {
                is DomainResult.Success -> {
                    val existingRule = result.data
                    hostRuleRepository.saveHostRule(
                        host = host,
                        status = existingRule?.uriStatus ?: UriStatus.NONE,
                        folderId = existingRule?.folderId,
                        preferredBrowser = browserPackage,
                        isPreferenceEnabled = true
                    )
                }
                is DomainResult.Failure -> {
                    _browserPickerUiState.update { state ->
                        state.copy(uiResult = UiResult.Error(TransientError.HOST_RULE_ACCESS_FAILED))
                    }
                }
            }
        }
    }

    fun bookmarkCurrentUri() {
        val currentState = _browserPickerUiState.value
        val uriProcessingResult = currentState.uriProcessingResult ?: return
        val host = uriProcessingResult.parsedUri.host

        viewModelScope.launch {
            // Get default bookmark folder
            folderRepository.getRootFoldersByType(FolderType.BOOKMARK).first().let { foldersResult ->
                when (foldersResult) {
                    is DomainResult.Success -> {
                        val defaultFolder = foldersResult.data.firstOrNull { it.name == "Bookmarked" }
                        if (defaultFolder != null) {
                            // Save host rule with bookmark status
                            hostRuleRepository.saveHostRule(
                                host = host,
                                status = UriStatus.BOOKMARKED,
                                folderId = defaultFolder.id,
                                preferredBrowser = uriProcessingResult.hostRule?.preferredBrowserPackage,
                                isPreferenceEnabled = uriProcessingResult.hostRule?.isPreferenceEnabled ?: true
                            ).let { saveResult ->
                                when (saveResult) {
                                    is DomainResult.Success -> {
                                        _browserPickerUiState.update { state ->
                                            state.copy(
                                                uiResult = UiResult.Success(BrowserPickerUiEffect.UriBookmarked),
                                                uriProcessingResult = uriProcessingResult.copy(isBookmarked = true)
                                            )
                                        }
                                    }
                                    is DomainResult.Failure -> {
                                        _browserPickerUiState.update { state ->
                                            state.copy(uiResult = UiResult.Error(TransientError.HOST_RULE_ACCESS_FAILED))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is DomainResult.Failure -> {
                        _browserPickerUiState.update { state ->
                            state.copy(uiResult = UiResult.Error(
                                PersistentError.HostRuleAccessFailed(
                                    message = "Could not access folders",
                                    cause = foldersResult.error.cause
                                )
                            ))
                        }
                    }
                }
            }
        }
    }

    fun blockCurrentUri() {
        val currentState = _browserPickerUiState.value
        val uriProcessingResult = currentState.uriProcessingResult ?: return
        val host = uriProcessingResult.parsedUri.host

        viewModelScope.launch {
            // Get default block folder
            folderRepository.getRootFoldersByType(FolderType.BLOCK).first().let { foldersResult ->
                when (foldersResult) {
                    is DomainResult.Success -> {
                        val defaultFolder = foldersResult.data.firstOrNull { it.name == "Blocked" }
                        if (defaultFolder != null) {
                            // Save host rule with block status
                            hostRuleRepository.saveHostRule(
                                host = host,
                                status = UriStatus.BLOCKED,
                                folderId = defaultFolder.id,
                                preferredBrowser = null, // Clear any preferred browser when blocking
                                isPreferenceEnabled = false // Disable preferences when blocking
                            ).let { saveResult ->
                                when (saveResult) {
                                    is DomainResult.Success -> {
                                        _browserPickerUiState.update { state ->
                                            state.copy(
                                                uiResult = UiResult.Success(BrowserPickerUiEffect.UriBlocked)
                                            )
                                        }
                                    }
                                    is DomainResult.Failure -> {
                                        _browserPickerUiState.update { state ->
                                            state.copy(uiResult = UiResult.Error(TransientError.HOST_RULE_ACCESS_FAILED))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is DomainResult.Failure -> {
                        _browserPickerUiState.update { state ->
                            state.copy(uiResult = UiResult.Error(
                                PersistentError.HostRuleAccessFailed(
                                    message = "Could not access folders",
                                    cause = foldersResult.error.cause
                                )
                            ))
                        }
                    }
                }
            }
        }
    }

    fun consumeUiOutcome() {
        _browserPickerUiState.update {
            when(val currentUiState = it.uiResult) {
                is UiResult.Error -> {
                    when(currentUiState.error) {
                        is PersistentError -> it
                        is TransientError -> it.copy(uiResult = UiResult.Idle)
                    }
                }
                is UiResult.Success -> it.copy(uiResult = UiResult.Idle)
                UiResult.Blocked -> it.copy(uiResult = UiResult.Idle)
                else -> it
            }
        }
    }

    fun clearSelectedBrowser() {
        _browserPickerUiState.update { it.copy(selectedBrowserAppInfo = null) }
    }

    fun dismissBrowserPicker() {
        val currentState = _browserPickerUiState.value
        val uriProcessingResult = currentState.uriProcessingResult ?: return

        viewModelScope.launch {
            // Record dismissal in URI history
            uriHistoryRepository.addUriRecord(
                uriString = uriProcessingResult.parsedUri.originalString,
                host = uriProcessingResult.parsedUri.host,
                source = uriProcessingResult.uriSource,
                action = InteractionAction.DISMISSED,
                chosenBrowser = null,
                associatedHostRuleId = uriProcessingResult.hostRule?.id
            )

            // Reset UI state
            _browserPickerUiState.update { state ->
                state.copy(
                    selectedBrowserAppInfo = null,
                    uri = null,
                    uriProcessingResult = null,
                    uiResult = UiResult.Idle
                )
            }
        }
    }
}
