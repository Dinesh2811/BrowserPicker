package browserpicker.presentation.feature.browserpicker

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