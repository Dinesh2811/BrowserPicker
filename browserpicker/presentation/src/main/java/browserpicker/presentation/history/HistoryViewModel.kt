package browserpicker.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriRecord
import browserpicker.domain.model.UriSource
import browserpicker.domain.model.query.*
import browserpicker.domain.service.DomainError
import browserpicker.domain.usecase.*
import browserpicker.presentation.common.LoadingStatus
import browserpicker.presentation.common.MessageType
import browserpicker.presentation.common.UserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getPagedUriHistoryUseCase: GetPagedUriHistoryUseCase,
    private val getHistoryOverviewUseCase: GetHistoryOverviewUseCase,
    private val getHistoryFilterOptionsUseCase: GetHistoryFilterOptionsUseCase,
    private val deleteUriRecordUseCase: DeleteUriRecordUseCase,
    private val clearUriHistoryUseCase: ClearUriHistoryUseCase
) : ViewModel() {

    private val _queryState = MutableStateFlow(UriHistoryQuery.DEFAULT)

    // Main UI State Flow
    private val _uiState = MutableStateFlow(HistoryScreenState())
    val uiState: StateFlow<HistoryScreenState> = _uiState.asStateFlow()

    // Flow for PagingData, driven by queryState and cached
    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedHistory: Flow<PagingData<UriRecord>> = _queryState
        .flatMapLatest { query ->
            getPagedUriHistoryUseCase(query)
        }
        .cachedIn(viewModelScope) // Cache the PagingData

    init {
        // Observe query changes to update overview
        viewModelScope.launch {
            _queryState.collectLatest { query ->
                _uiState.update { it.copy(query = query) } // Update query in state
                fetchOverview(query)
            }
        }

        // Observe the cached paged data flow and expose it within the main UI state
        // This makes it easy for the UI to collect just one state object
        viewModelScope.launch {
            pagedHistory.collectLatest { pagingData ->
                // Note: We expose the *cached* flow directly, not via _uiState.
                // Update: Let's keep the flow separate for simpler Compose collection via .collectAsLazyPagingItems()
                // _uiState.update { it.copy(historyPagingDataFlow = flowOf(pagingData)) } // Don't do this
            }
        }

        fetchFilterOptions()
    }

    private fun fetchOverview(query: UriHistoryQuery) {
        viewModelScope.launch {
            getHistoryOverviewUseCase(query)
                .onStart {
                    _uiState.update {
                        it.copy(overview = it.overview.copy(loadingStatus = LoadingStatus.LOADING))
                    }
                }
                .catch { e ->
                    Timber.e(e, "Error fetching history overview")
                    _uiState.update {
                        it.copy(
                            overview = it.overview.copy(loadingStatus = LoadingStatus.ERROR),
                            userMessages = it.userMessages + UserMessage(message = "Error loading overview", type = MessageType.ERROR)
                        )
                    }
                }
                .collect { overviewData ->
                    _uiState.update {
                        it.copy(
                            overview = HistoryOverviewState(
                                loadingStatus = LoadingStatus.SUCCESS,
                                totalCount = overviewData.totalCount,
                                groupCounts = overviewData.groupCounts,
                                dateCounts = overviewData.dateCounts,
                                activeGrouping = query.groupBy // Reflect current grouping
                            )
                        )
                    }
                }
        }
    }

    private fun fetchFilterOptions() {
        viewModelScope.launch {
            getHistoryFilterOptionsUseCase()
                .onStart { _uiState.update { it.copy(filterOptionsLoading = LoadingStatus.LOADING)} }
                .catch { e ->
                    Timber.e(e, "Error fetching filter options")
                    _uiState.update {
                        it.copy(
                            filterOptionsLoading = LoadingStatus.ERROR,
                            userMessages = it.userMessages + UserMessage(message = "Error loading filters", type = MessageType.ERROR)
                        )
                    }
                }
                .collect { options ->
                    _uiState.update {
                        it.copy(
                            filterOptions = options,
                            filterOptionsLoading = LoadingStatus.SUCCESS
                        )
                    }
                }
        }
    }


    // --- Actions to Modify Query ---

    fun setSearchQuery(query: String?) {
        _queryState.update { it.copy(searchQuery = query?.trim()) }
    }

    fun setSort(field: UriRecordSortField, order: SortOrder) {
        _queryState.update { it.copy(sortBy = field, sortOrder = order) }
    }

    fun setGroup(field: UriRecordGroupField, order: SortOrder = SortOrder.ASC) {
        // Reset specific filters if grouping changes? Maybe not necessary.
        _queryState.update { it.copy(groupBy = field, groupSortOrder = order) }
    }

    fun setDateRangeFilter(range: Pair<Instant, Instant>?) {
        _queryState.update { it.copy(filterByDateRange = range) }
    }

    fun setSourceFilter(sources: Set<UriSource>?) {
        _queryState.update { it.copy(filterByUriSource = sources) }
    }

    fun setInteractionFilter(actions: Set<InteractionAction>?) {
        _queryState.update { it.copy(filterByInteractionAction = actions) }
    }

    fun setBrowserFilter(browsers: Set<String?>?) {
        _queryState.update { it.copy(filterByChosenBrowser = browsers) }
    }

    fun setHostFilter(hosts: Set<String>?) {
        _queryState.update { it.copy(filterByHost = hosts) }
    }

    // --- Actions for Deletion ---

    fun deleteHistoryRecord(id: Long) {
        viewModelScope.launch {
            deleteUriRecordUseCase(
                id = id,
                onSuccess = {
                    // Paging list will update automatically via Room observation
                    // Show confirmation message
                    addMessage("History record deleted.")
                },
                onError = { error ->
                    handleDomainError("Failed to delete record", error)
                }
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            clearUriHistoryUseCase(
                onSuccess = {
                    addMessage("History cleared successfully.")
                    // PagingSource should become empty automatically
                },
                onError = { error ->
                    handleDomainError("Failed to clear history", error)
                }
            )
        }
    }

    // --- Message Handling ---
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
