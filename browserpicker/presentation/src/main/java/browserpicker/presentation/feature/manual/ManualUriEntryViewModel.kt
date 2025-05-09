package browserpicker.presentation.feature.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.results.DomainResult
import browserpicker.domain.di.UriHandlingUseCases
import browserpicker.domain.model.*
import browserpicker.domain.model.query.*
import browserpicker.domain.model.UriSource
import browserpicker.domain.service.ParsedUri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManualUriEntryViewModel @Inject constructor(
    private val uriHandlingUseCases: UriHandlingUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManualUriEntryUiState())
    val uiState: StateFlow<ManualUriEntryUiState> = _uiState.asStateFlow()

    private val _inputUri = MutableStateFlow("")
    val inputUri: StateFlow<String> = _inputUri.asStateFlow()

    private val _validatedUri = MutableStateFlow<ParsedUri?>(null)
    val validatedUri: StateFlow<ParsedUri?> = _validatedUri.asStateFlow()

    fun updateInputUri(uri: String) {
        _inputUri.value = uri
        // Clear previous validation results when input changes
        _validatedUri.value = null
        _uiState.update { it.copy(
            validationError = null,
            processingError = null,
            isProcessed = false,
            isBlocked = false,
            shouldOpenDirectly = false,
            shouldShowPicker = false,
            selectedBrowserPackage = null,
            host = null
        )}
    }

    fun validateUri() {
        viewModelScope.launch {
            val uri = _inputUri.value
            if (uri.isBlank()) {
                _uiState.update { it.copy(validationError = "URI cannot be empty") }
                return@launch
            }

            _uiState.update { it.copy(isValidating = true, validationError = null) }
            
            when (val result = uriHandlingUseCases.validateUriUseCase(uri)) {
                is DomainResult.Success -> {
                    val parsedUri = result.data
                    if (parsedUri != null) {
                        _validatedUri.value = parsedUri
                        _uiState.update { it.copy(
                            isValidating = false,
                            isValid = true,
                            validationError = null
                        )}
                    } else {
                        _uiState.update { it.copy(
                            isValidating = false,
                            isValid = false,
                            validationError = "Invalid URI format"
                        )}
                    }
                }
                is DomainResult.Failure -> {
                    _uiState.update { it.copy(
                        isValidating = false,
                        isValid = false,
                        validationError = result.error.message
                    )}
                }
            }
        }
    }

    fun processUri() {
        viewModelScope.launch {
            val uri = _inputUri.value
            if (uri.isBlank()) {
                _uiState.update { it.copy(validationError = "URI cannot be empty") }
                return@launch
            }

            _uiState.update { it.copy(isProcessing = true, processingError = null) }
            
            when (val result = uriHandlingUseCases.handleUriUseCase(uri, UriSource.MANUAL)) {
                is DomainResult.Success -> {
                    when (val handleResult = result.data) {
                        is HandleUriResult.Blocked -> {
                            _uiState.update { it.copy(
                                isProcessing = false,
                                isProcessed = true,
                                isBlocked = true
                            )}
                        }
                        is HandleUriResult.OpenDirectly -> {
                            _uiState.update { it.copy(
                                isProcessing = false,
                                isProcessed = true,
                                shouldOpenDirectly = true,
                                selectedBrowserPackage = handleResult.browserPackageName
                            )}
                        }
                        is HandleUriResult.ShowPicker -> {
                            _uiState.update { it.copy(
                                isProcessing = false,
                                isProcessed = true,
                                shouldShowPicker = true,
                                host = handleResult.host
                            )}
                        }
                        is HandleUriResult.InvalidUri -> {
                            _uiState.update { it.copy(
                                isProcessing = false,
                                isProcessed = true,
                                processingError = handleResult.reason
                            )}
                        }
                    }
                }
                is DomainResult.Failure -> {
                    _uiState.update { it.copy(
                        isProcessing = false,
                        processingError = result.error.message
                    )}
                }
            }
        }
    }

    fun getRecentUris() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingRecent = true) }
            
            uriHandlingUseCases.getRecentUrisUseCase()
                .collect { result ->
                    when (result) {
                        is DomainResult.Success -> {
                            _uiState.update { it.copy(
                                isLoadingRecent = false,
                                recentUris = result.data.map { parsedUri -> parsedUri.originalString }
                            )}
                        }
                        is DomainResult.Failure -> {
                            _uiState.update { it.copy(
                                isLoadingRecent = false,
                                error = result.error.message
                            )}
                        }
                    }
                }
        }
    }

    fun selectRecentUri(uri: String) {
        _inputUri.value = uri
        validateUri()
    }

    fun clearErrors() {
        _uiState.update { it.copy(
            validationError = null,
            processingError = null,
            error = null
        )}
    }

    fun resetState() {
        _inputUri.value = ""
        _validatedUri.value = null
        _uiState.value = ManualUriEntryUiState()
    }
}

data class ManualUriEntryUiState(
    val isValidating: Boolean = false,
    val isValid: Boolean = false,
    val validationError: String? = null,
    val isProcessing: Boolean = false,
    val isProcessed: Boolean = false,
    val processingError: String? = null,
    val isBlocked: Boolean = false,
    val shouldOpenDirectly: Boolean = false,
    val shouldShowPicker: Boolean = false,
    val selectedBrowserPackage: String? = null,
    val host: String? = null,
    val isLoadingRecent: Boolean = false,
    val recentUris: List<String> = emptyList(),
    val error: String? = null
) 