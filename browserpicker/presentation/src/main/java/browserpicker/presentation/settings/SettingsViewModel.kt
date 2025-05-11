package browserpicker.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.domain.usecases.system.BackupDataUseCase
import browserpicker.domain.usecases.system.CheckDefaultBrowserStatusUseCase
import browserpicker.domain.usecases.system.OpenBrowserPreferencesUseCase
import browserpicker.domain.usecases.system.RestoreDataUseCase
import browserpicker.domain.usecases.system.SetAsDefaultBrowserUseCase
import browserpicker.domain.usecases.uri.history.DeleteAllUriHistoryUseCase
import browserpicker.domain.usecases.uri.shared.CleanupUriHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 * 
 * This ViewModel handles:
 * - App preferences management
 * - Default browser status
 * - Data backup and restore
 * - History cleanup
 * - System integrations
 * 
 * Used by: SettingsScreen
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val checkDefaultBrowserStatusUseCase: CheckDefaultBrowserStatusUseCase,
    private val setAsDefaultBrowserUseCase: SetAsDefaultBrowserUseCase,
    private val openBrowserPreferencesUseCase: OpenBrowserPreferencesUseCase,
    private val backupDataUseCase: BackupDataUseCase,
    private val restoreDataUseCase: RestoreDataUseCase,
    private val cleanupUriHistoryUseCase: CleanupUriHistoryUseCase,
    private val deleteAllUriHistoryUseCase: DeleteAllUriHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        checkDefaultBrowserStatus()
    }
    
    /**
     * Check if this app is set as the default browser
     */
    private fun checkDefaultBrowserStatus() {
        viewModelScope.launch {
            checkDefaultBrowserStatusUseCase()
                .collect { result ->
                    result.onSuccess { isDefault ->
                        _uiState.value = _uiState.value.copy(
                            isDefaultBrowser = isDefault
                        )
                    }
                }
        }
    }
    
    /**
     * Set this app as the default browser
     */
    fun setAsDefaultBrowser() {
        viewModelScope.launch {
            val result = setAsDefaultBrowserUseCase()
            
            result.onSuccess { isSet ->
                _uiState.value = _uiState.value.copy(
                    isDefaultBrowser = isSet
                )
            }
        }
    }
    
    /**
     * Open system browser preferences
     */
    fun openBrowserPreferences() {
        viewModelScope.launch {
            openBrowserPreferencesUseCase()
        }
    }
    
    /**
     * Backup app data
     */
    fun backupData(filePath: String, includeHistory: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            
            val result = backupDataUseCase(filePath, includeHistory)
            
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                lastOperation = if (result.isSuccess) "Backup successful" else "Backup failed}",
                lastOperationSuccess = result.isSuccess
            )
        }
    }
    
    /**
     * Restore app data
     */
    fun restoreData(filePath: String, clearExistingData: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            
            val result = restoreDataUseCase(filePath, clearExistingData)
            
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                lastOperation = if (result.isSuccess) "Restore successful" else "Restore failed}",
                lastOperationSuccess = result.isSuccess
            )
        }
    }
    
    /**
     * Clean up old URI history
     */
    fun cleanupHistory(olderThanDays: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            
            val result = cleanupUriHistoryUseCase(olderThanDays)
            
            result.onSuccess { count ->
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    lastOperation = "Cleaned up $count records",
                    lastOperationSuccess = true
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    lastOperation = "Cleanup failed: ${error.message}",
                    lastOperationSuccess = false
                )
            }
        }
    }
    
    /**
     * Clear all URI history
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            
            val result = deleteAllUriHistoryUseCase()
            
            result.onSuccess { count ->
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    lastOperation = "Deleted $count records",
                    lastOperationSuccess = true
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    lastOperation = "Delete failed: ${error.message}",
                    lastOperationSuccess = false
                )
            }
        }
    }
}

/**
 * UI state for the Settings screen
 */
data class SettingsUiState(
    val isDefaultBrowser: Boolean = false,
    val isProcessing: Boolean = false,
    val lastOperation: String? = null,
    val lastOperationSuccess: Boolean = false
) 