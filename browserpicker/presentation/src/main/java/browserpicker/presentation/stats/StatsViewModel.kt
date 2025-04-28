package browserpicker.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.domain.model.BrowserUsageStat
import browserpicker.domain.model.query.BrowserStatSortField
import browserpicker.domain.service.DomainError
import browserpicker.domain.usecase.ClearBrowserStatsUseCase
import browserpicker.domain.usecase.GetBrowserStatsUseCase
import browserpicker.presentation.common.MessageType
import browserpicker.presentation.common.UserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
open class StatsViewModel @Inject constructor(
    private val getBrowserStatsUseCase: GetBrowserStatsUseCase,
    private val clearBrowserStatsUseCase: ClearBrowserStatsUseCase
) : ViewModel() {

    private val _sortField = MutableStateFlow(BrowserStatSortField.USAGE_COUNT)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val statsFlow: Flow<List<BrowserUsageStat>> = _sortField
        .flatMapLatest { sortField ->
            getBrowserStatsUseCase(sortField)
        }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)

    private val _uiState = MutableStateFlow(StatsScreenState())
    open val uiState: StateFlow<StatsScreenState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                statsFlow,
                _sortField
            ) { stats, sort ->
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false, // Manage loading state more granularly if needed
                        stats = stats,
                        sortField = sort,
                        userMessages = currentState.userMessages // Preserve messages
                    )
                }
            }.collect()
        }
    }

    fun setSortField(field: BrowserStatSortField) {
        _sortField.value = field
    }

    fun clearStats() {
        viewModelScope.launch {
            clearBrowserStatsUseCase(
                onSuccess = { addMessage("Browser stats cleared.") },
                onError = { error -> handleDomainError("Failed to clear stats", error) }
            )
        }
    }

    // --- Message Handling --- (Same as HistoryViewModel)
    fun clearMessage(id: Long) {
        _uiState.update { state ->
            state.copy(userMessages = state.userMessages.filterNot { it.id == id })
        }
    }

    private fun addMessage(text: String, type: MessageType = MessageType.SUCCESS) {
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
