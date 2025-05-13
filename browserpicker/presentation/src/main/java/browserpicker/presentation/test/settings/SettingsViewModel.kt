package browserpicker.presentation.test.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.di.DefaultDispatcher
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.di.MainDispatcher
import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.di.BrowserUseCases
import browserpicker.domain.di.FolderUseCases
import browserpicker.domain.di.HostRuleUseCases
import browserpicker.domain.di.SearchAndAnalyticsUseCases
import browserpicker.domain.di.SystemIntegrationUseCases
import browserpicker.domain.di.UriHandlingUseCases
import browserpicker.domain.di.UriHistoryUseCases
import browserpicker.presentation.UiState
import browserpicker.presentation.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@Immutable
data class SettingsUiState(
    val isDefaultBrowser: UiState<Boolean> = UiState.Loading,
    val backupState: UiState<Unit> = UiState.Success(Unit), // Represents the state of the last backup operation
    val restoreState: UiState<Unit> = UiState.Success(Unit), // Represents the state of the last restore operation
    val cleanupState: UiState<Int> = UiState.Success(0), // Represents the state of the last cleanup operation (number of deleted records)
    val isPerformingBackup: Boolean = false,
    val isPerformingRestore: Boolean = false,
    val isPerformingCleanup: Boolean = false,
    val snackbarMessage: String? = null // For showing transient messages
)

/**
 * ViewModel for the Settings screen.
 *
 * This ViewModel handles:
 * - App preferences management (implicitly via settings actions)
 * - Default browser status
 * - Data backup and restore
 * - History cleanup
 * - System integrations (like opening settings)
 *
 * Used by: SettingsScreen
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
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
    private val systemIntegrationUseCases: SystemIntegrationUseCases,
): ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state

    init {
        observeDefaultBrowserStatus()
    }

    private fun observeDefaultBrowserStatus() {
        viewModelScope.launch {
            systemIntegrationUseCases.checkDefaultBrowserStatusUseCase()
                .flowOn(ioDispatcher)
                .distinctUntilChanged()
                .toUiState()
                .catch { error ->
                    emit(UiState.Error("Failed to check default browser status: ${error.message}", error))
                }
                .collectLatest { uiState ->
                    _state.update { it.copy(isDefaultBrowser = uiState) }
                }
        }
    }

    /**
     * Attempts to set this app as the default browser.
     */
    fun setAsDefaultBrowser() {
        viewModelScope.launch {
            // No state update needed here, the observer will pick up the change
            val result = withContext(ioDispatcher) {
                systemIntegrationUseCases.setAsDefaultBrowserUseCase()
            }
            result.onFailure { error ->
                showSnackbar("Failed to set default browser: ${error.message}")
            }
        }
    }

    /**
     * Opens the system settings screen where the user can change the default browser.
     */
    fun openBrowserSettings() {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                systemIntegrationUseCases.openBrowserPreferencesUseCase()
            }
            result.onFailure { error ->
                showSnackbar("Failed to open browser settings: ${error.message}")
            }
        }
    }

    /**
     * Initiates a data backup process.
     * @param filePath The path where the backup file should be saved.
     * @param includeHistory Whether to include URI history in the backup.
     */
    fun backupData(filePath: String, includeHistory: Boolean = true) {
        viewModelScope.launch {
            _state.update { it.copy(isPerformingBackup = true, backupState = UiState.Loading) }
            val result = withContext(ioDispatcher) {
                systemIntegrationUseCases.backupDataUseCase(filePath, includeHistory)
            }
            _state.update {
                it.copy(
                    isPerformingBackup = false,
                    backupState = result.toUiState(),
                    snackbarMessage = result.fold(
                        onSuccess = { "Backup successful" },
                        onFailure = { err -> "Backup failed: ${err.message}" }
                    )
                )
            }
        }
    }

    /**
     * Initiates a data restore process.
     * @param filePath The path from which to restore the backup file.
     * @param clearExistingData Whether to clear existing data before restoring.
     */
    fun restoreData(filePath: String, clearExistingData: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isPerformingRestore = true, restoreState = UiState.Loading) }
            val result = withContext(ioDispatcher) {
                systemIntegrationUseCases.restoreDataUseCase(filePath, clearExistingData)
            }
            _state.update {
                it.copy(
                    isPerformingRestore = false,
                    restoreState = result.toUiState(),
                    snackbarMessage = result.fold(
                        onSuccess = { "Restore successful" },
                        onFailure = { err -> "Restore failed: ${err.message}" }
                    )
                )
            }
            // Optionally trigger a refresh of other data if restore was successful
            result.onSuccess { refreshAllData() }
        }
    }

    /**
     * Cleans up URI history older than the specified number of days.
     * @param olderThanDays Threshold for deleting old records.
     */
    fun cleanupHistory(olderThanDays: Int) {
        if (olderThanDays <= 0) {
            showSnackbar("Please provide a positive number of days for cleanup.")
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isPerformingCleanup = true, cleanupState = UiState.Loading) }
            val result = withContext(ioDispatcher) {
                uriHandlingUseCases.cleanupUriHistoryUseCase(olderThanDays)
            }
            _state.update {
                it.copy(
                    isPerformingCleanup = false,
                    cleanupState = result.toUiState(),
                    snackbarMessage = result.fold(
                        onSuccess = { count -> "Cleanup successful: $count records deleted" },
                        onFailure = { err -> "Cleanup failed: ${err.message}" }
                    )
                )
            }
        }
    }

    /**
     * Clears a displayed snackbar message.
     */
    fun clearSnackbarMessage() {
        _state.update { it.copy(snackbarMessage = null) }
    }

    /**
     * Refreshes the default browser status check.
     */
    fun refreshAllData() {
        _state.update { it.copy(isDefaultBrowser = UiState.Loading) }
        observeDefaultBrowserStatus()
        // Add calls to refresh other potentially affected data if needed
    }

    private fun showSnackbar(message: String) {
        _state.update { it.copy(snackbarMessage = message) }
    }
}
