package browserpicker.presentation.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import browserpicker.core.results.DomainResult
import browserpicker.domain.di.UriHistoryUseCases
import browserpicker.domain.model.*
import browserpicker.domain.model.query.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import javax.inject.Inject

@HiltViewModel
class UriHistoryViewModel @Inject constructor(
    private val uriHistoryUseCases: UriHistoryUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(UriHistoryUiState())
    val uiState: StateFlow<UriHistoryUiState> = _uiState.asStateFlow()

    private val _queryState = MutableStateFlow(UriHistoryQuery.DEFAULT)
    val queryState: StateFlow<UriHistoryQuery> = _queryState.asStateFlow()

    private val _filterOptions = MutableStateFlow<FilterOptions?>(null)
    val filterOptions: StateFlow<FilterOptions?> = _filterOptions.asStateFlow()

    private val _totalCount = MutableStateFlow<Long?>(null)
    val totalCount: StateFlow<Long?> = _totalCount.asStateFlow()

    private val _groupCounts = MutableStateFlow<List<GroupCount>>(emptyList())
    val groupCounts: StateFlow<List<GroupCount>> = _groupCounts.asStateFlow()

    private val _dateCounts = MutableStateFlow<List<DateCount>>(emptyList())
    val dateCounts: StateFlow<List<DateCount>> = _dateCounts.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedUriHistory: Flow<PagingData<UriRecord>> = _queryState
        .flatMapLatest { query ->
            uriHistoryUseCases.getPagedUriHistoryUseCase(query)
        }
        .cachedIn(viewModelScope)

    init {
        loadFilterOptions()
        updateTotalCount()
        updateGroupCounts()
        updateDateCounts()
    }

    private fun loadFilterOptions() {
        viewModelScope.launch {
            uriHistoryUseCases.getUriFilterOptionsUseCase()
                .collect { result ->
                    if (result is DomainResult.Success) {
                        _filterOptions.value = result.data
                    } else {
                        _uiState.update { it.copy(error = (result as DomainResult.Failure).error.message) }
                    }
                }
        }
    }

    private fun updateTotalCount() {
        viewModelScope.launch {
            _queryState
                .flatMapLatest { query ->
                    uriHistoryUseCases.getUriHistoryCountUseCase(query)
                }
                .collect { result ->
                    if (result is DomainResult.Success) {
                        _totalCount.value = result.data
                    } else {
                        _uiState.update { it.copy(error = (result as DomainResult.Failure).error.message) }
                    }
                }
        }
    }

    private fun updateGroupCounts() {
        viewModelScope.launch {
            _queryState
                .flatMapLatest { query ->
                    uriHistoryUseCases.getUriHistoryGroupCountsUseCase(query)
                }
                .collect { result ->
                    if (result is DomainResult.Success) {
                        _groupCounts.value = result.data
                    } else {
                        _uiState.update { it.copy(error = (result as DomainResult.Failure).error.message) }
                    }
                }
        }
    }

    private fun updateDateCounts() {
        viewModelScope.launch {
            _queryState
                .flatMapLatest { query ->
                    uriHistoryUseCases.getUriHistoryDateCountsUseCase(query)
                }
                .collect { result ->
                    if (result is DomainResult.Success) {
                        _dateCounts.value = result.data
                    } else {
                        _uiState.update { it.copy(error = (result as DomainResult.Failure).error.message) }
                    }
                }
        }
    }

    fun updateSearchQuery(searchQuery: String?) {
        _queryState.update { it.copy(searchQuery = searchQuery) }
    }

    fun updateUriSourceFilter(sources: Set<UriSource>?) {
        _queryState.update { it.copy(filterByUriSource = sources) }
    }

    fun updateInteractionActionFilter(actions: Set<InteractionAction>?) {
        _queryState.update { it.copy(filterByInteractionAction = actions) }
    }

    fun updateBrowserFilter(browsers: Set<String?>?) {
        _queryState.update { it.copy(filterByChosenBrowser = browsers) }
    }

    fun updateHostFilter(hosts: Set<String>?) {
        _queryState.update { it.copy(filterByHost = hosts) }
    }

    fun updateDateRangeFilter(dateRange: Pair<Instant, Instant>?) {
        _queryState.update { it.copy(filterByDateRange = dateRange) }
    }

    fun updateSorting(sortBy: UriRecordSortField, sortOrder: SortOrder) {
        _queryState.update { it.copy(sortBy = sortBy, sortOrder = sortOrder) }
    }

    fun updateGrouping(groupBy: UriRecordGroupField, sortOrder: SortOrder = SortOrder.ASC) {
        _queryState.update { it.copy(groupBy = groupBy, groupSortOrder = sortOrder) }
    }

    fun updateAdvancedFilters(filters: List<UriRecordAdvancedFilterDomain>) {
        _queryState.update { it.copy(advancedFilters = filters) }
    }

    fun resetFilters() {
        _queryState.value = UriHistoryQuery.DEFAULT
    }

    fun deleteUriRecord(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = uriHistoryUseCases.deleteUriRecordUseCase(id)) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, lastDeletedId = id) }
                    // Refresh counts
                    updateTotalCount()
                    updateGroupCounts()
                    updateDateCounts()
                }
                is DomainResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                }
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showClearConfirmation = false) }
            
            when (val result = uriHistoryUseCases.deleteAllUriHistoryUseCase()) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        allHistoryCleared = true,
                        deletedCount = result.data
                    )}
                    // Reset counts
                    updateTotalCount()
                    updateGroupCounts()
                    updateDateCounts()
                }
                is DomainResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                }
            }
        }
    }

    fun showClearConfirmation() {
        _uiState.update { it.copy(showClearConfirmation = true) }
    }

    fun dismissClearConfirmation() {
        _uiState.update { it.copy(showClearConfirmation = false) }
    }

    fun exportHistory(filePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = uriHistoryUseCases.exportUriHistoryUseCase(filePath, _queryState.value)) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        exportSuccess = true,
                        exportedCount = result.data
                    )}
                }
                is DomainResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                }
            }
        }
    }

    fun importHistory(filePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            when (val result = uriHistoryUseCases.importUriHistoryUseCase(filePath)) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        importSuccess = true,
                        importedCount = result.data
                    )}
                    // Refresh counts
                    updateTotalCount()
                    updateGroupCounts()
                    updateDateCounts()
                    // Refresh filter options as new hosts/browsers may be available
                    loadFilterOptions()
                }
                is DomainResult.Failure -> {
                    _uiState.update { it.copy(isLoading = false, error = result.error.message) }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetExportImportState() {
        _uiState.update { it.copy(
            exportSuccess = false,
            importSuccess = false,
            exportedCount = null,
            importedCount = null
        )}
    }
}

data class UriHistoryUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastDeletedId: Long? = null,
    val allHistoryCleared: Boolean = false,
    val deletedCount: Int? = null,
    val showClearConfirmation: Boolean = false,
    val exportSuccess: Boolean = false,
    val importSuccess: Boolean = false,
    val exportedCount: Int? = null,
    val importedCount: Int? = null
) 