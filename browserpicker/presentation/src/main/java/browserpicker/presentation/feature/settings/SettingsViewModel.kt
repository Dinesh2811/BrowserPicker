package browserpicker.presentation.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.results.DomainResult
import browserpicker.domain.di.SystemIntegrationUseCases
import browserpicker.domain.di.UriHandlingUseCases
import browserpicker.domain.di.UriHistoryUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val systemIntegrationUseCases: SystemIntegrationUseCases,
    private val uriHistoryUseCases: UriHistoryUseCases,
    private val uriHandlingUseCases: UriHandlingUseCases,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _isDefaultBrowser = MutableStateFlow<Boolean?>(null)
    val isDefaultBrowser: StateFlow<Boolean?> = _isDefaultBrowser.asStateFlow()

    private val _clipboardMonitoringEnabled = MutableStateFlow(false)
    val clipboardMonitoringEnabled: StateFlow<Boolean> = _clipboardMonitoringEnabled.asStateFlow()

    private val _clipboardUris = MutableStateFlow<List<String>>(emptyList())
    val clipboardUris: StateFlow<List<String>> = _clipboardUris.asStateFlow()

    private val _installedBrowsers = MutableStateFlow<List<String>>(emptyList())
    val installedBrowsers: StateFlow<List<String>> = _installedBrowsers.asStateFlow()

    init {
        checkDefaultBrowserStatus()
        monitorInstalledBrowsers()
    }

    private fun checkDefaultBrowserStatus() {
        viewModelScope.launch {
            systemIntegrationUseCases.checkDefaultBrowserStatusUseCase()
                .collect { result ->
                    when (result) {
                        is DomainResult.Success -> {
                            _isDefaultBrowser.value = result.data
                        }
                        is DomainResult.Failure -> {
                            _uiState.update { it.copy(error = result.error.message) }
                        }
                    }
                }
        }
    }

    private fun monitorInstalledBrowsers() {
        viewModelScope.launch {
            systemIntegrationUseCases.monitorSystemBrowserChangesUseCase()
                .collect { result ->
                    when (result) {
                        is DomainResult.Success -> {
                            _installedBrowsers.value = result.data
                        }
                        is DomainResult.Failure -> {
                            _uiState.update { it.copy(error = result.error.message) }
                        }
                    }
                }
        }
    }

    fun openBrowserPreferences() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = systemIntegrationUseCases.openBrowserPreferencesUseCase()) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        preferencesOpened = true
                    )}
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

    fun setAsDefaultBrowser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = systemIntegrationUseCases.setAsDefaultBrowserUseCase()) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        defaultBrowserSet = result.data
                    )}
                    // Refresh status
                    checkDefaultBrowserStatus()
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

    fun toggleClipboardMonitoring(enabled: Boolean) {
        _clipboardMonitoringEnabled.value = enabled
        
        if (enabled) {
            startClipboardMonitoring()
        }
    }

    private fun startClipboardMonitoring() {
        viewModelScope.launch {
            systemIntegrationUseCases.monitorUriClipboardUseCase()
                .collect { result ->
                    when (result) {
                        is DomainResult.Success -> {
                            val uri = result.data
                            _clipboardUris.update { list -> 
                                (list + uri).distinct().take(5) // Keep last 5 unique URIs
                            }
                        }
                        is DomainResult.Failure -> {
                            // Silently ignore failures, or log them if needed
                        }
                    }
                }
        }
    }

    fun handleClipboardUri(uri: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = systemIntegrationUseCases.handleUncaughtUriUseCase(uri)) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        clipboardUriHandled = true,
                        clipboardUriHandledSuccessfully = result.data
                    )}
                    // Remove from list if handled
                    if (result.data) {
                        _clipboardUris.update { list -> list.filter { it != uri } }
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

    fun shareUri(uri: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = systemIntegrationUseCases.shareUriUseCase(uri)) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        uriShared = true
                    )}
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

    fun backupData(filePath: String, includeHistory: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = systemIntegrationUseCases.backupDataUseCase(filePath, includeHistory)) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        backupCompleted = true
                    )}
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

    fun restoreData(filePath: String, clearExistingData: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = systemIntegrationUseCases.restoreDataUseCase(filePath, clearExistingData)) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        restoreCompleted = true
                    )}
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

    fun cleanupOldHistory(olderThanDays: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
//            when (val result = uriHistoryUseCases.cleanupUriHistoryUseCase(olderThanDays)) {
            when (val result = uriHandlingUseCases.cleanupUriHistoryUseCase(olderThanDays)) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        cleanupCompleted = true,
                        deletedRecordsCount = result.data
                    )}
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
            preferencesOpened = false,
            defaultBrowserSet = false,
            clipboardUriHandled = false,
            clipboardUriHandledSuccessfully = false,
            uriShared = false,
            backupCompleted = false,
            restoreCompleted = false,
            cleanupCompleted = false,
            deletedRecordsCount = null
        )}
    }
}

data class SettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val preferencesOpened: Boolean = false,
    val defaultBrowserSet: Boolean = false,
    val clipboardUriHandled: Boolean = false,
    val clipboardUriHandledSuccessfully: Boolean = false,
    val uriShared: Boolean = false,
    val backupCompleted: Boolean = false,
    val restoreCompleted: Boolean = false,
    val cleanupCompleted: Boolean = false,
    val deletedRecordsCount: Int? = null
) 