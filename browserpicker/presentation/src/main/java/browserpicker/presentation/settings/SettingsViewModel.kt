package browserpicker.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.domain.service.DomainError
import browserpicker.domain.usecase.*
import browserpicker.presentation.common.MessageType
import browserpicker.presentation.common.UserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val clearUriHistoryUseCase: ClearUriHistoryUseCase,
    private val clearBrowserStatsUseCase: ClearBrowserStatsUseCase
    // Inject Use Cases for other settings actions here
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsScreenState())
    val uiState: StateFlow<SettingsScreenState> = _uiState.asStateFlow()

    // --- User Actions Triggered from UI ---

    fun onClearHistoryClick() {
        _uiState.update { it.copy(dialogState = SettingsDialogState.ShowClearHistoryConfirmation) }
    }

    fun onClearStatsClick() {
        _uiState.update { it.copy(dialogState = SettingsDialogState.ShowClearStatsConfirmation) }
    }

    // Add other settings actions here (e.g., onToggleClipboardSource, onExportDataClick)

    // --- Dialog Actions ---

    fun onConfirmClearHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, dialogState = SettingsDialogState.Hidden) } // Hide dialog immediately
            clearUriHistoryUseCase(
                onSuccess = {
                    addMessage("URI history cleared successfully.")
                    _uiState.update { it.copy(isLoading = false) }
                },
                onError = { error ->
                    handleDomainError("Failed to clear history", error)
                    _uiState.update { it.copy(isLoading = false) }
                }
            )
        }
    }

    fun onConfirmClearStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, dialogState = SettingsDialogState.Hidden) } // Hide dialog immediately
            clearBrowserStatsUseCase(
                onSuccess = {
                    addMessage("Browser stats cleared successfully.")
                    _uiState.update { it.copy(isLoading = false) }
                },
                onError = { error ->
                    handleDomainError("Failed to clear browser stats", error)
                    _uiState.update { it.copy(isLoading = false) }
                }
            )
        }
    }

    fun onCancelDialog() {
        _uiState.update { it.copy(dialogState = SettingsDialogState.Hidden) }
    }


    // --- Message Handling ---
    fun clearMessage(id: Long) {
        _uiState.update { state ->
            state.copy(userMessages = state.userMessages.filterNot { it.id == id })
        }
    }

    private fun addMessage(text: String, type: MessageType = MessageType.INFO) {
        _uiState.update {
            it.copy(userMessages = it.userMessages + UserMessage(message = text, type = type))
        }
    }

    private fun handleDomainError(prefix: String, error: DomainError) {
        Timber.e("$prefix: $error")
        val message = when (error) {
            is DomainError.Validation -> "$prefix: ${error.message}"
            is DomainError.NotFound -> "$prefix: ${error.entityType} not found."
            is DomainError.Conflict -> "$prefix: ${error.message}"
            is DomainError.Database -> "$prefix: Database error."
            is DomainError.Unexpected -> "$prefix: Unexpected error."
            is DomainError.Custom -> "$prefix: ${error.message}"
        }
        addMessage(message, MessageType.ERROR)
    }
}
