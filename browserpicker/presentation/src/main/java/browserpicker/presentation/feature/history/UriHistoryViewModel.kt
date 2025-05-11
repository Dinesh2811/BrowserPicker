package browserpicker.presentation.feature.history
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import browserpicker.core.di.DefaultDispatcher
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.di.MainDispatcher
import browserpicker.domain.model.*
import browserpicker.domain.model.query.*
import browserpicker.domain.service.PagingDefaults
import browserpicker.domain.usecases.uri.history.*
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
import kotlin.time.Duration.Companion.milliseconds

/**
 * UI state for URI history view
 */
@Immutable
data class UriHistoryUiState(
    val uriRecords: Flow<PagingData<UriRecord>>? = null,
    val totalCount: UiState<Long> = UiState.Loading,
    val groupCounts: UiState<List<GroupCount>> = UiState.Loading,
    val dateCounts: UiState<List<DateCount>> = UiState.Loading,
    val filterOptions: UiState<FilterOptions> = UiState.Loading,
    val currentQuery: UriHistoryQuery = UriHistoryQuery.DEFAULT,
    val selectedUriRecord: UiState<UriRecord?> = UiState.Success(null),
    val isExporting: Boolean = false,
    val exportResult: UiState<Int>? = null,
    val importResult: UiState<Int>? = null,
    val deletionInProgress: Boolean = false,
    val deleteResult: UiState<Unit>? = null
)

/**
 * ViewModel for URI history management
 * Handles loading, filtering, and operations on URI history records
 */
@HiltViewModel
class UriHistoryViewModel @Inject constructor(
    private val instantProvider: InstantProvider,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    private val getPagedUriHistoryUseCase: GetPagedUriHistoryUseCase,
    private val getUriHistoryCountUseCase: GetUriHistoryCountUseCase,
    private val getUriHistoryGroupCountsUseCase: GetUriHistoryGroupCountsUseCase,
    private val getUriHistoryDateCountsUseCase: GetUriHistoryDateCountsUseCase,
    private val getUriRecordByIdUseCase: GetUriRecordByIdUseCase,
    private val deleteUriRecordUseCase: DeleteUriRecordUseCase,
    private val deleteAllUriHistoryUseCase: DeleteAllUriHistoryUseCase,
    private val getUriFilterOptionsUseCase: GetUriFilterOptionsUseCase,
    private val exportUriHistoryUseCase: ExportUriHistoryUseCase,
    private val importUriHistoryUseCase: ImportUriHistoryUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(UriHistoryUiState())
    val state: StateFlow<UriHistoryUiState> = _state.asStateFlow()

    private val _historyQuery = MutableStateFlow(UriHistoryQuery.DEFAULT)

    init {
        loadInitialData()
        setupFilterObserver()
    }

    private fun loadInitialData() {
        // Load filter options
        viewModelScope.launch {
            getUriFilterOptionsUseCase()
                .flowOn(ioDispatcher)
                .toUiState()
                .catch { error ->
                    _state.update {
                        it.copy(filterOptions = UiState.Error("Failed to load filter options: ${error.message}", error))
                    }
                }
                .collect { uiState ->
                    _state.update { it.copy(filterOptions = uiState) }
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun setupFilterObserver() {
        // Observe query changes and update paged data
        viewModelScope.launch {
            _historyQuery
                .debounce(300.milliseconds)
                .distinctUntilChanged()
                .onStart { _state.update { it.copy(uriRecords = null) } }
                .flatMapLatest { query ->
                    val pagedData = getPagedUriHistoryUseCase(
                        query = query,
                        pagingConfig = PagingDefaults.DEFAULT_PAGING_CONFIG
                    ).cachedIn(viewModelScope)

                    // Wrap in a flow to combine with query
                    flowOf(pagedData)
                }
                .collect { pagedData ->
                    _state.update {
                        it.copy(
                            uriRecords = pagedData,
                            currentQuery = _historyQuery.value
                        )
                    }
                }
        }

        // Track total count based on query
        viewModelScope.launch {
            _historyQuery
                .debounce(300.milliseconds)
                .distinctUntilChanged()
                .onStart { _state.update { it.copy(totalCount = UiState.Loading) } }
                .flatMapLatest { query ->
                    getUriHistoryCountUseCase(query)
                        .flowOn(ioDispatcher)
                        .toUiState()
                }
                .catch { error ->
                    _state.update {
                        it.copy(totalCount = UiState.Error("Failed to load total count: ${error.message}", error))
                    }
                }
                .collect { uiState ->
                    _state.update { it.copy(totalCount = uiState) }
                }
        }

        // Track group counts based on query
        viewModelScope.launch {
            _historyQuery
                .debounce(300.milliseconds)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.groupBy == UriRecordGroupField.NONE) {
                        flowOf(UiState.Success(emptyList<GroupCount>()))
                    } else {
                        getUriHistoryGroupCountsUseCase(query)
                            .flowOn(ioDispatcher)
                            .toUiState()
                            .catch { error ->
                                emit(UiState.Error("Failed to load group counts: ${error.message}", error))
                            }
                    }
                }
                .collect { uiState ->
                    _state.update { it.copy(groupCounts = uiState) }
                }
        }

        // Track date counts based on query
        viewModelScope.launch {
            _historyQuery
                .debounce(300.milliseconds)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    getUriHistoryDateCountsUseCase(query)
                        .flowOn(ioDispatcher)
                        .toUiState()
                }
                .catch { error ->
                    _state.update {
                        it.copy(dateCounts = UiState.Error("Failed to load date counts: ${error.message}", error))
                    }
                }
                .collect { uiState ->
                    _state.update { it.copy(dateCounts = uiState) }
                }
        }
    }

    /**
     * Updates search query for URI history
     */
    fun updateSearchQuery(searchQuery: String?) {
        _historyQuery.update { it.copy(searchQuery = searchQuery) }
    }

    /**
     * Updates filters for URI sources
     */
    fun updateSourceFilters(sources: Set<UriSource>?) {
        _historyQuery.update { it.copy(filterByUriSource = sources) }
    }

    /**
     * Updates filters for interaction actions
     */
    fun updateActionFilters(actions: Set<InteractionAction>?) {
        _historyQuery.update { it.copy(filterByInteractionAction = actions) }
    }

    /**
     * Updates filters for chosen browsers
     */
    fun updateBrowserFilters(browsers: Set<String?>?) {
        _historyQuery.update { it.copy(filterByChosenBrowser = browsers) }
    }

    /**
     * Updates filters for hosts
     */
    fun updateHostFilters(hosts: Set<String>?) {
        _historyQuery.update { it.copy(filterByHost = hosts) }
    }

    /**
     * Updates date range filter
     */
    fun updateDateRangeFilter(from: Instant?, to: Instant?) {
        if (from == null || to == null) {
            _historyQuery.update { it.copy(filterByDateRange = null) }
            return
        }

        if (from <= to) {
            _historyQuery.update { it.copy(filterByDateRange = Pair(from, to)) }
        }
    }

    /**
     * Updates advanced filters (has rule, status)
     */
    fun updateAdvancedFilters(filters: List<UriRecordAdvancedFilterDomain>) {
        _historyQuery.update { it.copy(advancedFilters = filters) }
    }

    /**
     * Updates sort options
     */
    fun updateSorting(field: UriRecordSortField, order: SortOrder) {
        _historyQuery.update { it.copy(sortBy = field, sortOrder = order) }
    }

    /**
     * Updates grouping options
     */
    fun updateGrouping(field: UriRecordGroupField, order: SortOrder = SortOrder.ASC) {
        _historyQuery.update { it.copy(groupBy = field, groupSortOrder = order) }
    }

    /**
     * Resets all filters to default
     */
    fun resetFilters() {
        _historyQuery.value = UriHistoryQuery.DEFAULT
    }

    /**
     * Loads details for a specific URI record
     */
    fun loadUriRecord(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(selectedUriRecord = UiState.Loading) }

            val result = withContext(ioDispatcher) {
                getUriRecordByIdUseCase(id)
            }

            _state.update { it.copy(selectedUriRecord = result.toUiState()) }
        }
    }

    /**
     * Deletes a specific URI record
     */
    fun deleteUriRecord(id: Long) {
        viewModelScope.launch {
            _state.update { it.copy(deletionInProgress = true, deleteResult = null) }

            val result = withContext(ioDispatcher) {
                deleteUriRecordUseCase(id)
            }

            _state.update {
                it.copy(
                    deletionInProgress = false,
                    deleteResult = result.toUiState()
                )
            }
        }
    }

    /**
     * Deletes all URI history records
     */
    fun deleteAllHistory() {
        viewModelScope.launch {
            _state.update { it.copy(deletionInProgress = true, deleteResult = null) }

            withContext(ioDispatcher) {
                deleteAllUriHistoryUseCase()
            }

            // Force refresh UI after deletion
            resetFilters()

            _state.update { it.copy(deletionInProgress = false) }
        }
    }

    /**
     * Exports URI history to a file
     */
    fun exportHistory(filePath: String) {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true, exportResult = null) }

            val result = withContext(ioDispatcher) {
                exportUriHistoryUseCase(filePath, _historyQuery.value)
            }

            _state.update {
                it.copy(
                    isExporting = false,
                    exportResult = result.toUiState()
                )
            }
        }
    }

    /**
     * Imports URI history from a file
     */
    fun importHistory(filePath: String) {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true, importResult = null) }

            val result = withContext(ioDispatcher) {
                importUriHistoryUseCase(filePath)
            }

            // Force refresh after import
            resetFilters()

            _state.update {
                it.copy(
                    isExporting = false,
                    importResult = result.toUiState()
                )
            }
        }
    }

    /**
     * Clears recent operation results
     */
    fun clearOperationResults() {
        _state.update {
            it.copy(
                exportResult = null,
                importResult = null,
                deleteResult = null
            )
        }
    }
}

/*
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

 */