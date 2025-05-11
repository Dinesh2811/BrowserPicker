package browserpicker.presentation.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.domain.model.BrowserAppInfo
import browserpicker.domain.model.Folder
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriStatus
import browserpicker.domain.usecases.browser.GetAvailableBrowsersUseCase
import browserpicker.domain.usecases.browser.GetPreferredBrowserForHostUseCase
import browserpicker.domain.usecases.browser.SetPreferredBrowserForHostUseCase
import browserpicker.domain.usecases.folder.GetChildFoldersUseCase
import browserpicker.domain.usecases.folder.MoveHostRuleToFolderUseCase
import browserpicker.domain.usecases.uri.host.GetHostRuleByIdUseCase
import browserpicker.domain.usecases.uri.host.SaveHostRuleUseCase
import browserpicker.domain.usecases.uri.host.UpdateHostRuleStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Host Rule Details screen.
 *
 * This ViewModel handles:
 * - Displaying details of a host rule
 * - Setting and changing preferred browser for a host
 * - Changing status (bookmarked/blocked)
 * - Moving a host rule to a different folder
 *
 * Used by: HostRuleDetailsScreen
 */
@HiltViewModel
class HostRuleDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHostRuleByIdUseCase: GetHostRuleByIdUseCase,
    private val saveHostRuleUseCase: SaveHostRuleUseCase,
    private val getAvailableBrowsersUseCase: GetAvailableBrowsersUseCase,
    private val getPreferredBrowserForHostUseCase: GetPreferredBrowserForHostUseCase,
    private val setPreferredBrowserForHostUseCase: SetPreferredBrowserForHostUseCase,
    private val getChildFoldersUseCase: GetChildFoldersUseCase,
    private val moveHostRuleToFolderUseCase: MoveHostRuleToFolderUseCase,
    private val updateHostRuleStatusUseCase: UpdateHostRuleStatusUseCase
) : ViewModel() {

    private val hostRuleId: Long = savedStateHandle.get<Long>("hostRuleId") ?: -1L

    private val _uiState = MutableStateFlow(HostRuleDetailsUiState())
    val uiState: StateFlow<HostRuleDetailsUiState> = _uiState.asStateFlow()

    // Available browsers for selection
    val availableBrowsers: Flow<List<BrowserAppInfo>> = getAvailableBrowsersUseCase()
        .map { result -> result.getOrNull() ?: emptyList() }

    init {
        if (hostRuleId != -1L) {
            loadHostRuleDetails()
        } else {
            _uiState.value = _uiState.value.copy(
                error = "Invalid Host Rule ID"
            )
        }
    }

    /**
     * Load host rule details
     */
    private fun loadHostRuleDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = getHostRuleByIdUseCase(hostRuleId)

            result.onSuccess { hostRule ->
                if (hostRule != null) {
                    _uiState.value = _uiState.value.copy(
                        hostRule = hostRule,
                        uriStatus = hostRule.uriStatus,
                        folderId = hostRule.folderId,
                        preferredBrowserPackage = hostRule.preferredBrowserPackage,
                        isPreferenceEnabled = hostRule.isPreferenceEnabled,
                        isLoading = false
                    )

                    // Load preferred browser info
                    loadPreferredBrowser(hostRule.host)
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Host rule not found",
                        isLoading = false
                    )
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    error = error.message,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Load preferred browser info
     */
    private fun loadPreferredBrowser(host: String) {
        viewModelScope.launch {
            getPreferredBrowserForHostUseCase(host)
                .collect { result ->
                    result.onSuccess { browserInfo ->
                        _uiState.value = _uiState.value.copy(
                            preferredBrowser = browserInfo
                        )
                    }
                }
        }
    }

    /**
     * Update host rule status
     */
    fun updateStatus(status: UriStatus) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val hostRule = _uiState.value.hostRule ?: return@launch

            val result = updateHostRuleStatusUseCase(
                hostRuleId = hostRule.id,
                newStatus = status,
                folderId = _uiState.value.folderId,
                preferredBrowserPackage = _uiState.value.preferredBrowserPackage,
                isPreferenceEnabled = _uiState.value.isPreferenceEnabled
            )

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    uriStatus = status,
                    isLoading = false,
                    statusUpdateSuccess = true
                )

                // Reload the host rule to get updated data
                loadHostRuleDetails()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update status: ${error.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Set preferred browser
     */
    fun setPreferredBrowser(packageName: String?, isEnabled: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val hostRule = _uiState.value.hostRule ?: return@launch

            if (packageName != null) {
                val result = setPreferredBrowserForHostUseCase(hostRule.host, packageName, isEnabled)

                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        preferredBrowserPackage = packageName,
                        isPreferenceEnabled = isEnabled,
                        isLoading = false,
                        browserUpdateSuccess = true
                    )

                    // Reload the host rule to get updated data
                    loadHostRuleDetails()
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to set preferred browser: ${error.message}",
                        isLoading = false
                    )
                }
            } else {
                // Clear preferred browser
                val result = saveHostRuleUseCase(
                    host = hostRule.host,
                    status = hostRule.uriStatus,
                    folderId = hostRule.folderId,
                    preferredBrowserPackage = null,
                    isPreferenceEnabled = false
                )

                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        preferredBrowserPackage = null,
                        isPreferenceEnabled = false,
                        isLoading = false,
                        browserUpdateSuccess = true
                    )

                    // Reload the host rule to get updated data
                    loadHostRuleDetails()
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to clear preferred browser: ${error.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Move to a different folder
     */
    fun moveToFolder(folderId: Long?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = moveHostRuleToFolderUseCase(hostRuleId, folderId)

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    folderId = folderId,
                    isLoading = false,
                    folderUpdateSuccess = true
                )

                // Reload the host rule to get updated data
                loadHostRuleDetails()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    error = "Failed to move to folder: ${error.message}",
                    isLoading = false
                )
            }
        }
    }
}

/**
 * UI state for the Host Rule Details screen
 */
data class HostRuleDetailsUiState(
    val hostRule: HostRule? = null,
    val uriStatus: UriStatus = UriStatus.NONE,
    val folderId: Long? = null,
    val preferredBrowserPackage: String? = null,
    val preferredBrowser: BrowserAppInfo? = null,
    val isPreferenceEnabled: Boolean = false,
    val statusUpdateSuccess: Boolean = false,
    val browserUpdateSuccess: Boolean = false,
    val folderUpdateSuccess: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)