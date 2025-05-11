package browserpicker.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import browserpicker.domain.model.*
import browserpicker.domain.model.query.FilterOptions
import browserpicker.domain.model.query.SortOrder
import browserpicker.domain.model.query.UriHistoryQuery
import browserpicker.domain.model.query.UriRecordGroupField
import browserpicker.domain.model.query.UriRecordSortField
import browserpicker.domain.usecases.analytics.GetMostVisitedHostsUseCase
import browserpicker.domain.usecases.uri.history.DeleteAllUriHistoryUseCase
import browserpicker.domain.usecases.uri.history.DeleteUriRecordUseCase
import browserpicker.domain.usecases.uri.history.GetPagedUriHistoryUseCase
import browserpicker.domain.usecases.uri.history.GetUriFilterOptionsUseCase
import browserpicker.domain.usecases.uri.history.GetUriHistoryCountUseCase
import browserpicker.domain.usecases.uri.history.GetUriHistoryDateCountsUseCase
import browserpicker.domain.usecases.uri.history.GetUriHistoryGroupCountsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * ViewModel for the URI History screen.
 *
 * This ViewModel handles:
 * - Loading and displaying URI history with paging
 * - Filtering, sorting, and grouping URI history
 * - Deleting individual URI records
 * - Clearing all history
 * - Showing statistics on URI history
 *
 * Used by: UriHistoryScreen, UriHistoryFilterDialog
 */
@HiltViewModel
class UriHistoryViewModel @Inject constructor(
    private val getPagedUriHistoryUseCase: GetPagedUriHistoryUseCase,
    private val getUriHistoryCountUseCase: GetUriHistoryCountUseCase,
    private val getUriHistoryGroupCountsUseCase: GetUriHistoryGroupCountsUseCase,
    private val getUriHistoryDateCountsUseCase: GetUriHistoryDateCountsUseCase,
    private val getMostVisitedHostsUseCase: GetMostVisitedHostsUseCase,
    private val deleteUriRecordUseCase: DeleteUriRecordUseCase,
    private val deleteAllUriHistoryUseCase: DeleteAllUriHistoryUseCase,
    private val getUriFilterOptionsUseCase: GetUriFilterOptionsUseCase
) : ViewModel() {

    // Current filter/sort state
    private val _queryState = MutableStateFlow(UriHistoryQuery())
    val queryState: StateFlow<UriHistoryQuery> = _queryState.asStateFlow()
    
    // Filter options for UI
    private val _filterOptions = MutableStateFlow<FilterOptions?>(null)
    val filterOptions: StateFlow<FilterOptions?> = _filterOptions.asStateFlow()
    
    // Paged URI history data
    val pagedUriHistory: Flow<PagingData<UriRecord>> = _queryState
        .flatMapLatest { query ->
            getPagedUriHistoryUseCase(query)
        }
        .cachedIn(viewModelScope)
    
    // Total record count
    val totalCount: Flow<Long> = _queryState
        .flatMapLatest { query ->
            getUriHistoryCountUseCase(query)
                .map { result -> result.getOrNull() ?: 0L }
        }
    
    // Group counts for analytics
    val groupCounts: Flow<List<GroupCount>> = _queryState
        .flatMapLatest { query ->
            getUriHistoryGroupCountsUseCase(query)
                .map { result -> result.getOrNull() ?: emptyList() }
        }
    
    init {
        loadFilterOptions()
    }
    
    /**
     * Load available filter options
     */
    private fun loadFilterOptions() {
        viewModelScope.launch {
            getUriFilterOptionsUseCase()
                .collect { result ->
                    result.onSuccess { options ->
                        _filterOptions.value = options
                    }
                }
        }
    }
    
    /**
     * Update the current query
     */
    fun updateQuery(query: UriHistoryQuery) {
        _queryState.value = query
    }
    
    /**
     * Update search text
     */
    fun updateSearchText(searchText: String) {
        _queryState.value = _queryState.value.copy(searchQuery = searchText.takeIf { it.isNotBlank() })
    }
    
    /**
     * Update sort options
     */
    fun updateSort(sortBy: UriRecordSortField, sortOrder: SortOrder) {
        _queryState.value = _queryState.value.copy(sortBy = sortBy, sortOrder = sortOrder)
    }
    
    /**
     * Update grouping options
     */
    fun updateGrouping(groupBy: UriRecordGroupField, groupSortOrder: SortOrder = SortOrder.ASC) {
        _queryState.value = _queryState.value.copy(groupBy = groupBy, groupSortOrder = groupSortOrder)
    }
    
    /**
     * Delete a URI record
     */
    fun deleteRecord(id: Long) {
        viewModelScope.launch {
            deleteUriRecordUseCase(id)
        }
    }
    
    /**
     * Clear all history
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            deleteAllUriHistoryUseCase()
        }
    }
}

/**
 * UI state for URI history filtering
 */
data class UriHistoryFilterState(
    val searchQuery: String = "",
    val selectedUriSources: Set<UriSource> = emptySet(),
    val selectedActions: Set<InteractionAction> = emptySet(),
    val selectedBrowsers: Set<String?> = emptySet(),
    val selectedHosts: Set<String> = emptySet(),
    val dateRange: Pair<Instant, Instant>? = null,
    val sortBy: UriRecordSortField = UriRecordSortField.TIMESTAMP,
    val sortOrder: SortOrder = SortOrder.DESC,
    val groupBy: UriRecordGroupField = UriRecordGroupField.NONE,
    val groupSortOrder: SortOrder = SortOrder.ASC
) 