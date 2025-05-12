package browserpicker.presentation.history

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import browserpicker.core.di.DefaultDispatcher
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.di.MainDispatcher
import browserpicker.core.results.DomainResult
import browserpicker.domain.di.UriHistoryUseCases
import browserpicker.domain.model.*
import browserpicker.domain.model.query.FilterOptions
import browserpicker.domain.model.query.SortOrder
import browserpicker.domain.model.query.UriHistoryQuery
import browserpicker.domain.model.query.UriRecordAdvancedFilterDomain
import browserpicker.domain.model.query.UriRecordGroupField
import browserpicker.domain.model.query.UriRecordSortField
import browserpicker.domain.service.PagingDefaults
import browserpicker.presentation.UiState
import browserpicker.presentation.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * Represents the complete UI state for the URI History screen
 */
@Immutable
data class UriHistoryUiState(
    val totalCount: UiState<Long> = UiState.Loading,
    val groupCounts: UiState<List<GroupCount>> = UiState.Success(emptyList()), // Default to empty, loaded only when grouped
    val dateCounts: UiState<List<DateCount>> = UiState.Success(emptyList()), // Default to empty, loaded only when grouped by date
    val filterOptions: UiState<FilterOptions> = UiState.Loading,
    val currentQuery: UriHistoryQuery = UriHistoryQuery.DEFAULT,
    val isLoading: Boolean = false, // General loading indicator for actions
    val userMessage: UiState<String?> = UiState.Success(null), // For snackbar messages
    val deleteConfirmationTargetId: Long? = null // ID for confirming single deletion
)

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
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UriHistoryViewModel @Inject constructor(
    private val instantProvider: InstantProvider,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    // Removed unused UseCases for now, can be added back if needed
    // private val uriHandlingUseCases: UriHandlingUseCases,
    // private val browserUseCases: BrowserUseCases,
    // private val hostRuleUseCases: HostRuleUseCases,
    private val uriHistoryUseCases: UriHistoryUseCases,
    // private val folderUseCases: FolderUseCases,
    // private val searchAndAnalyticsUseCases: SearchAndAnalyticsUseCases,
    // private val systemIntegrationUseCases: SystemIntegrationUseCases,
): ViewModel() {

    private val _state = MutableStateFlow(UriHistoryUiState())
    val state: StateFlow<UriHistoryUiState> = _state.asStateFlow()

    private val _queryOptions = MutableStateFlow(UriHistoryQuery.DEFAULT)

    // Flow for paged data, cached in ViewModel scope
    val pagedHistoryFlow: Flow<PagingData<UriRecord>> = _queryOptions
        .flatMapLatest { query ->
            uriHistoryUseCases.getPagedUriHistoryUseCase(
                query = query,
                pagingConfig = PagingDefaults.DEFAULT_PAGING_CONFIG // Or allow customization
            )
        }
        .cachedIn(viewModelScope) // Cache the PagingData

    init {
        loadFilterOptions()
        observeQueryChanges()
    }

    private fun observeQueryChanges() {
        viewModelScope.launch {
            _queryOptions
                .onEach { query -> _state.update { it.copy(currentQuery = query) } }
                .distinctUntilChanged()
                .collectLatest { query ->
                    // Update non-paged data based on the query
                    updateTotalCount(query)
                    updateGroupCounts(query) // Will only fetch if groupBy is active
                    updateDateCounts(query) // Will only fetch if groupBy DATE is active
                }
        }
    }

    private fun loadFilterOptions() {
        viewModelScope.launch {
            uriHistoryUseCases.getUriFilterOptionsUseCase()
                .flowOn(ioDispatcher)
                .toUiState()
                .catch { unexpectedError ->
                    emit(UiState.Error("Failed to load filter options: ${unexpectedError.message}", unexpectedError))
                }
                .collectLatest { uiState ->
                    _state.update { it.copy(filterOptions = uiState) }
                }
        }
    }

    private fun updateTotalCount(query: UriHistoryQuery) {
        viewModelScope.launch {
            _state.update { it.copy(totalCount = UiState.Loading) }
            uriHistoryUseCases.getUriHistoryCountUseCase(query)
                .flowOn(ioDispatcher)
                .toUiState()
                .catch { unexpectedError ->
                    emit(UiState.Error("Failed to load total count: ${unexpectedError.message}", unexpectedError))
                }
                .collectLatest { uiState ->
                    _state.update { it.copy(totalCount = uiState) }
                }
        }
    }

    private fun updateGroupCounts(query: UriHistoryQuery) {
        // Only fetch group counts if a grouping field (other than NONE or DATE) is selected
        if (query.groupBy != UriRecordGroupField.NONE && query.groupBy != UriRecordGroupField.DATE) {
            viewModelScope.launch {
                _state.update { it.copy(groupCounts = UiState.Loading) }
                uriHistoryUseCases.getUriHistoryGroupCountsUseCase(query)
                    .flowOn(ioDispatcher)
                    .toUiState()
                    .catch { unexpectedError ->
                        emit(UiState.Error("Failed to load group counts: ${unexpectedError.message}", unexpectedError))
                    }
                    .collectLatest { uiState ->
                        _state.update { it.copy(groupCounts = uiState) }
                    }
            }
        } else {
            // Clear group counts if grouping is NONE or DATE
            _state.update { it.copy(groupCounts = UiState.Success(emptyList())) }
        }
    }

    private fun updateDateCounts(query: UriHistoryQuery) {
        // Only fetch date counts if grouping by DATE is selected
        if (query.groupBy == UriRecordGroupField.DATE) {
            viewModelScope.launch {
                _state.update { it.copy(dateCounts = UiState.Loading) }
                uriHistoryUseCases.getUriHistoryDateCountsUseCase(query)
                    .flowOn(ioDispatcher)
                    .toUiState()
                    .catch { unexpectedError ->
                        emit(UiState.Error("Failed to load date counts: ${unexpectedError.message}", unexpectedError))
                    }
                    .collectLatest { uiState ->
                        _state.update { it.copy(dateCounts = uiState) }
                    }
            }
        } else {
            // Clear date counts if grouping is not DATE
            _state.update { it.copy(dateCounts = UiState.Success(emptyList())) }
        }
    }

    // --- Query Update Functions ---

    fun updateSearchQuery(query: String?) {
        _queryOptions.update { it.copy(searchQuery = query?.trim()) }
    }

    fun updateFilterBySource(sources: Set<UriSource>?) {
        _queryOptions.update { it.copy(filterByUriSource = sources?.ifEmpty { null }) }
    }

    fun updateFilterByAction(actions: Set<InteractionAction>?) {
        _queryOptions.update { it.copy(filterByInteractionAction = actions?.ifEmpty { null }) }
    }

    fun updateFilterByBrowser(browsers: Set<String?>?) {
        // Ensure null browser is included if the set contains null
        val processedBrowsers = browsers?.mapNotNull { it?.ifBlank { null } }?.toSet()
        val includesNull = browsers?.contains(null) == true
        val finalSet = if (includesNull) processedBrowsers?.plus(null as String?) else processedBrowsers
        _queryOptions.update { it.copy(filterByChosenBrowser = finalSet?.ifEmpty { null }) }
    }

    fun updateFilterByHost(hosts: Set<String>?) {
        _queryOptions.update { it.copy(filterByHost = hosts?.mapNotNull { it.ifBlank { null } }?.toSet()?.ifEmpty { null }) }
    }

    fun updateDateRange(from: Instant?, to: Instant?) {
        viewModelScope.launch(defaultDispatcher) {
            val range = if (from != null && to != null && from <= to) Pair(from, to) else null
            _queryOptions.update { it.copy(filterByDateRange = range) }
        }
    }

    fun setSorting(field: UriRecordSortField, order: SortOrder) {
        _queryOptions.update { it.copy(sortBy = field, sortOrder = order) }
    }

    fun setGrouping(field: UriRecordGroupField, order: SortOrder = SortOrder.ASC) {
        _queryOptions.update { it.copy(groupBy = field, groupSortOrder = order) }
    }

    fun addAdvancedFilter(filter: UriRecordAdvancedFilterDomain) {
        _queryOptions.update {
            val currentFilters = it.advancedFilters.toMutableList()
            // Avoid adding duplicate filters if necessary (simple equality check here)
            if (!currentFilters.contains(filter)) {
                currentFilters.add(filter)
            }
            it.copy(advancedFilters = currentFilters)
        }
    }

    fun removeAdvancedFilter(filter: UriRecordAdvancedFilterDomain) {
        _queryOptions.update {
            it.copy(advancedFilters = it.advancedFilters.filterNot { existing -> existing == filter })
        }
    }

    fun resetFilters() {
        _queryOptions.update { UriHistoryQuery.DEFAULT }
    }

    // --- Actions ---

    fun requestDeleteConfirmation(id: Long) {
        _state.update { it.copy(deleteConfirmationTargetId = id) }
    }

    fun cancelDeleteConfirmation() {
        _state.update { it.copy(deleteConfirmationTargetId = null) }
    }

    fun confirmDeleteUriRecord() {
        val idToDelete = state.value.deleteConfirmationTargetId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, deleteConfirmationTargetId = null) } // Clear target ID
            val result = withContext(ioDispatcher) {
                uriHistoryUseCases.deleteUriRecordUseCase(idToDelete)
            }
            _state.update { it.copy(isLoading = false) }
            handleActionResult(result, successMessage = "History record deleted", failurePrefix = "Failed to delete record")
            // Paging library might automatically update, but a manual refresh call might be needed
            // depending on Pager behavior and data source invalidation.
            // Consider adding a refresh mechanism if needed.
        }
    }

    fun deleteAllHistory() {
        // Consider adding a confirmation dialog trigger here similar to single delete
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = withContext(ioDispatcher) {
                uriHistoryUseCases.deleteAllUriHistoryUseCase()
            }
            _state.update { it.copy(isLoading = false) }
            result.onSuccess { count ->
                handleActionResult(DomainResult.Success(Unit), successMessage = "Deleted $count history records", failurePrefix = "")
            }.onFailure {
                handleActionResult(DomainResult.Failure(it), successMessage = "", failurePrefix = "Failed to clear history")
            }
            // Refresh needed after clearing all
            // TODO: Trigger Paging refresh if necessary
        }
    }

    fun exportHistory(filePath: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = withContext(ioDispatcher) {
                uriHistoryUseCases.exportUriHistoryUseCase(filePath, state.value.currentQuery)
            }
            _state.update { it.copy(isLoading = false) }
            result.onSuccess { count ->
                handleActionResult(DomainResult.Success(Unit), successMessage = "Exported $count records to $filePath", failurePrefix = "")
            }.onFailure {
                handleActionResult(DomainResult.Failure(it), successMessage = "", failurePrefix = "Failed to export history")
            }
        }
    }

    fun importHistory(filePath: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val result = withContext(ioDispatcher) {
                uriHistoryUseCases.importUriHistoryUseCase(filePath)
            }
            _state.update { it.copy(isLoading = false) }
            result.onSuccess { count ->
                handleActionResult(DomainResult.Success(Unit), successMessage = "Imported $count records from $filePath", failurePrefix = "")
                // Refresh needed after import
                // TODO: Trigger Paging refresh if necessary
            }.onFailure {
                handleActionResult(DomainResult.Failure(it), successMessage = "", failurePrefix = "Failed to import history")
            }
        }
    }

    /**
     * Dismisses the current user message (e.g., snackbar)
     */
    fun dismissUserMessage() {
        _state.update { it.copy(userMessage = UiState.Success(null)) }
    }

    private fun handleActionResult(result: DomainResult<Unit, browserpicker.core.results.AppError>, successMessage: String, failurePrefix: String) {
        val message = result.fold(
            onSuccess = { successMessage },
            onFailure = { "$failurePrefix: ${it.message}" }
        )
        _state.update { it.copy(userMessage = UiState.Success(message)) }
    }
}
