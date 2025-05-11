package browserpicker.presentation.main

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.di.DefaultDispatcher
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.di.MainDispatcher
import browserpicker.core.results.AppError
import browserpicker.domain.di.BrowserUseCases
import browserpicker.domain.di.FolderUseCases
import browserpicker.domain.di.HostRuleUseCases
import browserpicker.domain.di.SearchAndAnalyticsUseCases
import browserpicker.domain.di.SystemIntegrationUseCases
import browserpicker.domain.di.UriHandlingUseCases
import browserpicker.domain.di.UriHistoryUseCases
import browserpicker.domain.model.*
import browserpicker.domain.model.query.HandleUriResult
import browserpicker.presentation.UiState
import browserpicker.presentation.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Represents the UI state for the Main screen
 */
@Immutable
data class MainUiState(
    val isDefaultBrowser: UiState<Boolean> = UiState.Loading,
    val recentUris: UiState<List<UriRecord>> = UiState.Loading,
    val availableBrowsers: UiState<List<BrowserAppInfo>> = UiState.Loading,
    val mostFrequentBrowser: UiState<BrowserAppInfo?> = UiState.Loading,
    val clipboardUri: UiState<String?> = UiState.Success(null),
    val systemBrowserChanges: UiState<List<String>> = UiState.Success(emptyList()),
    val isProcessingUri: Boolean = false,
    val processingResult: HandleUriResultUi? = null
)

/**
 * UI representation of HandleUriResult for presentation layer
 */
@Immutable
sealed interface HandleUriResultUi {
    @Immutable data object Blocked: HandleUriResultUi
    @Immutable data class OpenDirectly(val browserName: String, val browserPackage: String): HandleUriResultUi
    @Immutable data class ShowPicker(val uriString: String, val host: String): HandleUriResultUi
    @Immutable data class InvalidUri(val reason: String): HandleUriResultUi
}

/**
 * Main ViewModel responsible for app-wide state and functionality.
 *
 * This ViewModel handles:
 * - Default browser status checking
 * - Clipboard URI monitoring
 * - System browser changes monitoring
 * - Incoming URI handling from intents
 *
 * Used by: MainActivity, MainScreen, HomeScreen
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val instantProvider: InstantProvider,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    private val uriHandlingUseCases: UriHandlingUseCases,
    private val browserUseCases: BrowserUseCases,
    private val hostRuleUseCases: HostRuleUseCases,
    private val uriHistoryUseCases: UriHistoryUseCases,
    private val folderUseCases: FolderUseCases,
    private val searchAndAnalyticsUseCases: SearchAndAnalyticsUseCases,
//    private val systemIntegrationUseCases: SystemIntegrationUseCases,
): ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state

    private val _refreshTrigger = MutableStateFlow(0)

    init {
        observeDefaultBrowserStatus()
        observeRecentUris()
        observeAvailableBrowsers()
        observeMostFrequentBrowser()
        observeClipboardUri()
        observeSystemBrowserChanges()
    }

    /**
     * Handles a URI from an external intent or other source
     */
    fun handleUri(uriString: String, source: UriSource = UriSource.INTENT) {
        viewModelScope.launch {
            _state.update { it.copy(isProcessingUri = true, processingResult = null) }

            val result = withContext(ioDispatcher) {
                uriHandlingUseCases.handleUriUseCase(uriString, source)
            }

            result.fold(
                onSuccess = { handleUriResult ->
                    val uiResult = when (handleUriResult) {
                        is HandleUriResult.Blocked -> HandleUriResultUi.Blocked
                        is HandleUriResult.OpenDirectly -> {
                            // Get browser name for better UI display
                            val browserInfo = getBrowserInfo(handleUriResult.browserPackageName)
                            HandleUriResultUi.OpenDirectly(
                                browserName = browserInfo?.appName ?: handleUriResult.browserPackageName,
                                browserPackage = handleUriResult.browserPackageName
                            )
                        }
                        is HandleUriResult.ShowPicker -> {
                            HandleUriResultUi.ShowPicker(
                                uriString = handleUriResult.uriString,
                                host = handleUriResult.host
                            )
                        }
                        is HandleUriResult.InvalidUri -> {
                            HandleUriResultUi.InvalidUri(reason = handleUriResult.reason)
                        }
                    }

                    _state.update { it.copy(isProcessingUri = false, processingResult = uiResult) }

                    // If it should open directly, perform the opening
                    if (uiResult is HandleUriResultUi.OpenDirectly) {
                        openUriInBrowser(uriString, uiResult.browserPackage)
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isProcessingUri = false,
                            processingResult = HandleUriResultUi.InvalidUri(error.message)
                        )
                    }
                }
            )
        }
    }

    /**
     * Opens a URI in the specified browser
     */
    fun openUriInBrowser(uriString: String, browserPackageName: String) {
//        viewModelScope.launch {
//            withContext(ioDispatcher) {
//                systemIntegrationUseCases.openUriInBrowserUseCase(uriString, browserPackageName)
//                // Record browser usage regardless of success/failure of opening
//                browserUseCases.recordBrowserUsageUseCase(browserPackageName)
//
//                // Refresh recent URIs after opening
//                _refreshTrigger.update { it + 1 }
//            }
//        }
    }

    /**
     * Sets the app as default browser
     */
    fun setAsDefaultBrowser() {
//        viewModelScope.launch {
//            withContext(ioDispatcher) {
//                systemIntegrationUseCases.setAsDefaultBrowserUseCase()
//            }
//        }
    }

    /**
     * Opens system settings to change default browser
     */
    fun openBrowserSettings() {
//        viewModelScope.launch {
//            withContext(ioDispatcher) {
//                systemIntegrationUseCases.openBrowserPreferencesUseCase()
//            }
//        }
    }

    /**
     * Shares a URI with another app
     */
    fun shareUri(uriString: String) {
//        viewModelScope.launch {
//            withContext(ioDispatcher) {
//                systemIntegrationUseCases.shareUriUseCase(uriString)
//            }
//        }
    }

    /**
     * Refreshes all data
     */
    fun refreshData() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isDefaultBrowser = UiState.Loading,
                    recentUris = UiState.Loading,
                    availableBrowsers = UiState.Loading,
                    mostFrequentBrowser = UiState.Loading
                )
            }
            _refreshTrigger.update { it + 1 }
        }
    }

    /**
     * Clears the current processing result
     */
    fun clearProcessingResult() {
        _state.update { it.copy(processingResult = null) }
    }

    private fun observeDefaultBrowserStatus() {
//        viewModelScope.launch {
//            systemIntegrationUseCases.checkDefaultBrowserStatusUseCase()
//                .flowOn(ioDispatcher)
//                .distinctUntilChanged()
//                .toUiState()
//                .catch { error ->
//                    emit(UiState.Error("Failed to check default browser status: ${error.message}", error))
//                }
//                .collectLatest { uiState ->
//                    _state.update { it.copy(isDefaultBrowser = uiState) }
//                }
//        }
    }

    private fun observeRecentUris() {
        viewModelScope.launch {
            uriHandlingUseCases.getRecentUrisUseCase()
                .flowOn(ioDispatcher)
                .distinctUntilChanged()
                .toUiState()
                .catch { error ->
                    emit(UiState.Error("Failed to load recent URIs: ${error.message}", error))
                }
                .collectLatest { uiState ->
                    _state.update { it.copy(recentUris = uiState) }
                }
        }
    }

    private fun observeAvailableBrowsers() {
        viewModelScope.launch {
            browserUseCases.getAvailableBrowsersUseCase()
                .flowOn(ioDispatcher)
                .distinctUntilChanged()
                .toUiState()
                .catch { error ->
                    emit(UiState.Error("Failed to load available browsers: ${error.message}", error))
                }
                .collectLatest { uiState ->
                    _state.update { it.copy(availableBrowsers = uiState) }
                }
        }
    }

    private fun observeMostFrequentBrowser() {
        viewModelScope.launch {
            browserUseCases.getMostFrequentlyUsedBrowserUseCase()
                .flowOn(ioDispatcher)
                .distinctUntilChanged()
                .toUiState()
                .catch { error ->
                    emit(UiState.Error("Failed to load most frequent browser: ${error.message}", error))
                }
                .collectLatest { uiState ->
                    _state.update { it.copy(mostFrequentBrowser = uiState) }
                }
        }
    }

    private fun observeClipboardUri() {
//        viewModelScope.launch {
//            systemIntegrationUseCases.monitorUriClipboardUseCase()
//                .flowOn(ioDispatcher)
//                .distinctUntilChanged()
//                .toUiState()
//                .catch { error ->
//                    emit(UiState.Error("Failed to monitor clipboard", error))
//                }
//                .collectLatest { uiState ->
//                    _state.update { it.copy(clipboardUri = uiState) }
//                }
//        }
    }

    private fun observeSystemBrowserChanges() {
//        viewModelScope.launch {
//            systemIntegrationUseCases.monitorSystemBrowserChangesUseCase()
//                .flowOn(ioDispatcher)
//                .distinctUntilChanged()
//                .toUiState()
//                .catch { error ->
//                    emit(UiState.Success(emptyList()))  // Silently fail on system browser monitoring errors
//                }
//                .collectLatest { uiState ->
//                    _state.update { it.copy(systemBrowserChanges = uiState) }
//                    // Refresh available browsers when changes are detected
//                    if (uiState is UiState.Success && uiState.data.isNotEmpty()) {
//                        observeAvailableBrowsers()
//                    }
//                }
//        }
    }

    private suspend fun getBrowserInfo(packageName: String): BrowserAppInfo? {
        val availableBrowsers = _state.value.availableBrowsers.getOrNull() ?: return null
        return availableBrowsers.find { it.packageName == packageName }
    }
}
