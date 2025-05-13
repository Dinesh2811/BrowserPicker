package browserpicker.presentation.test.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.domain.model.BrowserAppInfo
import browserpicker.domain.model.UriRecord
import browserpicker.domain.usecases.uri.history.DeleteUriRecordUseCase
import browserpicker.domain.usecases.uri.history.GetUriRecordByIdUseCase
import browserpicker.domain.usecases.uri.host.SaveHostRuleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the URI Details screen.
 *
 * This ViewModel handles:
 * - Displaying details of a specific URI record
 * - Showing information about the browser used for the URI
 * - Providing options to bookmark or block the host
 * - Allowing reprocessing of the URI with a different browser
 * - Deleting the URI record
 *
 * Used by: UriDetailsScreen
 */
@HiltViewModel
class UriDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getUriRecordByIdUseCase: GetUriRecordByIdUseCase,
    private val saveHostRuleUseCase: SaveHostRuleUseCase,
//    private val getBrowserInfoUseCase: GetBrowserInfoUseCase,
//    private val getHostRuleByHostUseCase: GetHostRuleByHostUseCase,
//    private val reprocessUriUseCase: ReprocessUriUseCase,
    private val deleteUriRecordUseCase: DeleteUriRecordUseCase
) : ViewModel() {

    private val uriRecordId: Long = savedStateHandle.get<Long>("uriRecordId") ?: -1L

    private val _uiState = MutableStateFlow(UriDetailsUiState())
    val uiState: StateFlow<UriDetailsUiState> = _uiState.asStateFlow()

    init {
        if (uriRecordId != -1L) {
            loadUriRecordDetails()
        } else {
            _uiState.value = _uiState.value.copy(
                error = "Invalid URI Record ID"
            )
        }
    }

    /**
     * Load URI record details
     */
    private fun loadUriRecordDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val result = getUriRecordByIdUseCase(uriRecordId)

            result.onSuccess { uriRecord ->
                if (uriRecord != null) {
                    _uiState.value = _uiState.value.copy(
                        uriRecord = uriRecord,
                        isLoading = false
                    )

                    // Load browser info if available
                    uriRecord.chosenBrowserPackage?.let { packageName ->
                        loadBrowserInfo(packageName)
                    }

                    // Load host rule info
                    loadHostRuleInfo(uriRecord.host)
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "URI record not found",
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
     * Load browser info
     */
    private fun loadBrowserInfo(packageName: String) {
//        viewModelScope.launch {
//            val result = getBrowserInfoUseCase(packageName)
//
//            result.onSuccess { browserInfo ->
//                _uiState.value = _uiState.value.copy(
//                    browserInfo = browserInfo
//                )
//            }
//        }
    }

    /**
     * Load host rule info
     */
    private fun loadHostRuleInfo(host: String) {
//        viewModelScope.launch {
//            val result = getHostRuleByHostUseCase(host)
//
//            result.onSuccess { hostRule ->
//                _uiState.value = _uiState.value.copy(
//                    isBookmarked = hostRule?.isBookmarked ?: false,
//                    isBlocked = hostRule?.isBlocked ?: false,
//                    hostRuleId = hostRule?.id
//                )
//            }
//        }
    }

    /**
     * Toggle bookmark status for the host
     */
    fun toggleBookmark() {
        viewModelScope.launch {
            val uriRecord = _uiState.value.uriRecord ?: return@launch

            // Implementation would create or update a host rule
            // with bookmarked status

            // After updating, refresh the host rule info
            loadHostRuleInfo(uriRecord.host)
        }
    }

    /**
     * Toggle block status for the host
     */
    fun toggleBlock() {
        viewModelScope.launch {
            val uriRecord = _uiState.value.uriRecord ?: return@launch

            // Implementation would create or update a host rule
            // with blocked status

            // After updating, refresh the host rule info
            loadHostRuleInfo(uriRecord.host)
        }
    }

    /**
     * Reprocess the URI with a different browser
     */
    fun reprocessUri() {
//        viewModelScope.launch {
//            val uriRecord = _uiState.value.uriRecord ?: return@launch
//
//            _uiState.value = _uiState.value.copy(isReprocessing = true)
//
//            val result = reprocessUriUseCase(uriRecord.uri)
//
//            _uiState.value = _uiState.value.copy(
//                isReprocessing = false,
//                reprocessingComplete = result.isSuccess
//            )
//        }
    }

    /**
     * Delete the URI record
     */
    fun deleteRecord() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)

            val result = deleteUriRecordUseCase(uriRecordId)

            _uiState.value = _uiState.value.copy(
                isDeleting = false,
                isDeleted = result.isSuccess,
                error = if (result.isFailure) "Failed to delete record" else null
            )
        }
    }
}

/**
 * UI state for the URI Details screen
 */
data class UriDetailsUiState(
    val uriRecord: UriRecord? = null,
    val browserInfo: BrowserAppInfo? = null,
    val isBookmarked: Boolean = false,
    val isBlocked: Boolean = false,
    val hostRuleId: Long? = null,
    val isReprocessing: Boolean = false,
    val reprocessingComplete: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
