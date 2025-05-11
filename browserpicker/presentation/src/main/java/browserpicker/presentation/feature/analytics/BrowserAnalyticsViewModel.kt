package browserpicker.presentation.feature.analytics

import androidx.compose.runtime.Immutable
import androidx.lifecycle.*
import browserpicker.core.di.DefaultDispatcher
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.di.MainDispatcher
import browserpicker.core.results.DomainResult
import browserpicker.domain.di.BrowserUseCases
import browserpicker.domain.di.UriHandlingUseCases
import browserpicker.domain.model.BrowserAppInfo
import browserpicker.domain.model.BrowserUsageStat
import browserpicker.domain.model.DateCount
import browserpicker.domain.model.query.BrowserStatSortField
import browserpicker.domain.model.query.SortOrder
import browserpicker.domain.usecases.analytics.AnalyzeBrowserUsageTrendsUseCase
import browserpicker.domain.usecases.analytics.BrowserUsageReport
import browserpicker.domain.usecases.analytics.GenerateBrowserUsageReportUseCase
import browserpicker.domain.usecases.browser.GetAvailableBrowsersUseCase
import browserpicker.domain.usecases.browser.GetBrowserUsageStatsUseCase
import browserpicker.domain.usecases.browser.GetMostFrequentlyUsedBrowserUseCase
import browserpicker.domain.usecases.browser.GetMostRecentlyUsedBrowserUseCase
import browserpicker.presentation.UiState
import browserpicker.presentation.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Represents the analytics filter options for browsers
 */
@Immutable
data class BrowserAnalyticsFilterOptions(
    val timeRange: Pair<Instant, Instant>? = null,
    val sortField: BrowserStatSortField = BrowserStatSortField.USAGE_COUNT,
    val sortOrder: SortOrder = SortOrder.DESC,
    val selectedBrowsers: Set<String> = emptySet()
) {
    companion object {
        val DEFAULT = BrowserAnalyticsFilterOptions()
    }
}

/**
 * Represents the complete UI state for browser analytics
 */
@Immutable
data class BrowserAnalyticsUiState(
    val usageStats: UiState<List<BrowserUsageStat>> = UiState.Loading,
    val trendData: UiState<Map<String, List<DateCount>>> = UiState.Loading,
    val mostFrequentBrowser: UiState<BrowserAppInfo?> = UiState.Loading,
    val mostRecentBrowser: UiState<BrowserAppInfo?> = UiState.Loading,
    val availableBrowsers: UiState<List<BrowserAppInfo>> = UiState.Loading,
    val filterOptions: BrowserAnalyticsFilterOptions = BrowserAnalyticsFilterOptions.DEFAULT,
    val fullReport: UiState<BrowserUsageReport> = UiState.Loading,
    val isGeneratingReport: Boolean = false,
)

/**
 * ViewModel responsible for browser analytics data management and presentation.
 * Handles loading, filtering, and reporting of browser usage statistics.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BrowserAnalyticsViewModel @Inject constructor(
    private val instantProvider: InstantProvider,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    private val getBrowserUsageStatsUseCase: GetBrowserUsageStatsUseCase,
    private val analyzeBrowserUsageTrendsUseCase: AnalyzeBrowserUsageTrendsUseCase,
    private val getMostFrequentlyUsedBrowserUseCase: GetMostFrequentlyUsedBrowserUseCase,
    private val getMostRecentlyUsedBrowserUseCase: GetMostRecentlyUsedBrowserUseCase,
    private val getAvailableBrowsersUseCase: GetAvailableBrowsersUseCase,
    private val generateBrowserUsageReportUseCase: GenerateBrowserUsageReportUseCase,
): ViewModel() {

    private val _state = MutableStateFlow(BrowserAnalyticsUiState())
    val state: StateFlow<BrowserAnalyticsUiState> = _state.asStateFlow()

    private val _filterOptions = MutableStateFlow(BrowserAnalyticsFilterOptions.DEFAULT)
    private val _refreshTrigger = MutableStateFlow(0)

    init {
        loadInitialData()
        observeFilterAndRefreshTriggers()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadInitialData() {
        loadMostFrequentBrowser()
        loadMostRecentBrowser()
        loadAvailableBrowsers()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeFilterAndRefreshTriggers() {
        getBrowserUsageStats()
        analyzeBrowserUsageTrends()
    }

    /**
     * Updates the time range filter for analytics data
     * @param from Start date for time range filtering, null for no start constraint
     * @param to End date for time range filtering, defaults to current time
     */
    fun updateTimeRange(from: Instant?, to: Instant? = instantProvider.now()) {
        viewModelScope.launch {
            withContext(defaultDispatcher) {
                if (from == null && to == null) {
                    _filterOptions.update { it.copy(timeRange = null) }
                } else if (from != null && to != null && from <= to) {
                    _filterOptions.update { it.copy(timeRange = Pair(from, to)) }
                }
            }
        }
    }

    /**
     * Sets the sort field and order for browser statistics
     * @param field Sort field (usage count, last used timestamp)
     * @param order Sort direction (ascending, descending)
     */
    fun setSortingOptions(field: BrowserStatSortField, order: SortOrder) {
        viewModelScope.launch {
            withContext(defaultDispatcher) {
                _filterOptions.update {
                    it.copy(sortField = field, sortOrder = order)
                }
            }
        }
    }

    /**
     * Updates the selected browsers for filtering
     * @param selectedPackageNames Set of package names to filter by
     */
    fun updateSelectedBrowsers(selectedPackageNames: Set<String>) {
        viewModelScope.launch {
            withContext(defaultDispatcher) {
                _filterOptions.update {
                    it.copy(selectedBrowsers = selectedPackageNames)
                }
            }
        }
    }

    /**
     * Generates a comprehensive browser usage report
     * @param exportToFile Whether to export report to a file
     * @param filePath Optional file path for export
     */
    fun generateReport(exportToFile: Boolean = false, filePath: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isGeneratingReport = true, fullReport = UiState.Loading) }

            val result = withContext(ioDispatcher) {
                generateBrowserUsageReportUseCase(
                    timeRange = _filterOptions.value.timeRange,
                    exportToFile = exportToFile,
                    filePath = filePath
                )
            }

            _state.update {
                it.copy(
                    fullReport = result.toUiState(),
                    isGeneratingReport = false
                )
            }
        }
    }

    /**
     * Resets all filters to default values
     */
    fun resetFilters() {
        viewModelScope.launch {
            withContext(defaultDispatcher) {
                _filterOptions.update {
                    BrowserAnalyticsFilterOptions.DEFAULT
                }
            }
        }
    }

    /**
     * Refreshes all data sources
     */
    fun refreshData() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    usageStats = UiState.Loading,
                    trendData = UiState.Loading,
                    mostFrequentBrowser = UiState.Loading,
                    mostRecentBrowser = UiState.Loading,
                    availableBrowsers = UiState.Loading,
                    fullReport = if (it.fullReport !is UiState.Loading) UiState.Loading else it.fullReport
                )
            }
            loadInitialData()
            _refreshTrigger.update { it + 1 }
        }
    }

    private fun loadMostFrequentBrowser() {
        viewModelScope.launch {
            getMostFrequentlyUsedBrowserUseCase()
                .flowOn(ioDispatcher)
                .toUiState()
                .catch { unexpectedError ->
                    emit(UiState.Error("Failed to load most frequent browser due to an unexpected issue: ${unexpectedError.message}", unexpectedError))
                }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000.milliseconds),
                    UiState.Loading
                )
                .collect { uiState ->
                    _state.update { it.copy(mostFrequentBrowser = uiState) }
                }
        }
    }

    private fun loadMostRecentBrowser() {
        viewModelScope.launch {
            getMostRecentlyUsedBrowserUseCase()
                .flowOn(ioDispatcher)
                .toUiState()
                .catch { unexpectedError ->
                    emit(UiState.Error("Failed to load most recent browser due to an unexpected issue: ${unexpectedError.message}", unexpectedError))
                }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000.milliseconds),
                    UiState.Loading
                )
                .collect { uiState ->
                    _state.update { it.copy(mostRecentBrowser = uiState) }
                }
        }
    }

    private fun loadAvailableBrowsers() {
        viewModelScope.launch {
            getAvailableBrowsersUseCase()
                .flowOn(ioDispatcher)
                .toUiState()
                .catch { unexpectedError ->
                    emit(UiState.Error("Failed to load available browsers due to an unexpected issue: ${unexpectedError.message}", unexpectedError))
                }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000.milliseconds),
                    UiState.Loading
                )
                .collect { uiState ->
                    _state.update { it.copy(availableBrowsers = uiState) }
                }
        }
    }

    private fun getBrowserUsageStats() {
        viewModelScope.launch {
            combine(_filterOptions, _refreshTrigger) { options, _ -> options }
                .onStart { _state.update { it.copy(usageStats = UiState.Loading) } }
                .distinctUntilChanged()
                .flatMapLatest { filterOptions ->
                    getBrowserUsageStatsUseCase(
                        sortBy = filterOptions.sortField,
                        sortOrder = filterOptions.sortOrder
                    )
                        .flowOn(ioDispatcher)
                        .map { result ->
                            when (result) {
                                is DomainResult.Success -> {
                                    val selectedBrowsers = filterOptions.selectedBrowsers
                                    val filteredData = if (selectedBrowsers.isEmpty()) {
                                        result.data
                                    } else {
                                        result.data.filter { stat ->
                                            selectedBrowsers.contains(stat.browserPackageName)
                                        }
                                    }
                                    DomainResult.Success(filteredData)
                                }
                                is DomainResult.Failure -> result
                            }
                        }
                        .toUiState()
                        .catch { unexpectedError ->
                            emit(UiState.Error("Failed to load browser usage stats due to an unexpected issue: ${unexpectedError.message}", unexpectedError))
                        }
                }
                .flowOn(defaultDispatcher)
                .collect { uiState ->
                    _state.update { it.copy(usageStats = uiState, filterOptions = _filterOptions.value) }
                }
        }
    }

    private fun analyzeBrowserUsageTrends() {
        viewModelScope.launch {
            combine(_filterOptions, _refreshTrigger) { options, _ -> options }
                .onStart { _state.update { it.copy(trendData = UiState.Loading) } }
                .distinctUntilChanged() // Process only if actual filter options change
                .flatMapLatest { filterOptions ->
                    analyzeBrowserUsageTrendsUseCase(
                        timeRange = filterOptions.timeRange
                    )
                        .flowOn(ioDispatcher)
                        .map { result ->
                            when (result) {
                                is DomainResult.Success -> {
                                    val selectedBrowsers = filterOptions.selectedBrowsers
                                    val filteredData = if (selectedBrowsers.isEmpty()) {
                                        result.data
                                    } else {
                                        result.data.filterKeys { browserPackage ->
                                            selectedBrowsers.contains(browserPackage)
                                        }
                                    }
                                    DomainResult.Success(filteredData)
                                }
                                is DomainResult.Failure -> result
                            }
                        }
                        .toUiState()
                        .catch { unexpectedError ->
                            emit(UiState.Error("Failed to load browser trend data due to an unexpected issue: ${unexpectedError.message}", unexpectedError))
                        }
                }
                .flowOn(defaultDispatcher) // Run collection part on default dispatcher
                .collect { uiState ->
                    _state.update { it.copy(trendData = uiState) }
                }
        }
    }
}


/*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.results.DomainResult
import browserpicker.domain.di.BrowserUseCases
import browserpicker.domain.di.SearchAndAnalyticsUseCases
import browserpicker.domain.model.BrowserUsageStat
import browserpicker.domain.model.*
import browserpicker.domain.model.query.*
import browserpicker.domain.usecases.analytics.BrowserUsageReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import javax.inject.Inject

@HiltViewModel
class BrowserAnalyticsViewModel @Inject constructor(
    private val browserUseCases: BrowserUseCases,
    private val searchAndAnalyticsUseCases: SearchAndAnalyticsUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserAnalyticsUiState())
    val uiState: StateFlow<BrowserAnalyticsUiState> = _uiState.asStateFlow()

    private val _browserStats = MutableStateFlow<List<BrowserUsageStat>>(emptyList())
    val browserStats: StateFlow<List<BrowserUsageStat>> = _browserStats.asStateFlow()

    private val _mostFrequentlyUsedBrowser = MutableStateFlow<String?>(null)
    val mostFrequentlyUsedBrowser: StateFlow<String?> = _mostFrequentlyUsedBrowser.asStateFlow()

    private val _mostRecentlyUsedBrowser = MutableStateFlow<String?>(null)
    val mostRecentlyUsedBrowser: StateFlow<String?> = _mostRecentlyUsedBrowser.asStateFlow()

    private val _usageTrends = MutableStateFlow<Map<String, List<DateCount>>>(emptyMap())
    val usageTrends: StateFlow<Map<String, List<DateCount>>> = _usageTrends.asStateFlow()

    private val _timeRange = MutableStateFlow<Pair<Instant, Instant>?>(null)
    val timeRange: StateFlow<Pair<Instant, Instant>?> = _timeRange.asStateFlow()

    private val _sortConfig = MutableStateFlow(SortConfig(BrowserStatSortField.USAGE_COUNT, SortOrder.DESC))
    val sortConfig: StateFlow<SortConfig> = _sortConfig.asStateFlow()

    init {
        loadBrowserStats()
        loadMostFrequentlyUsedBrowser()
        loadMostRecentlyUsedBrowser()
        loadUsageTrends()
    }

    private fun loadBrowserStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            _sortConfig
                .flatMapLatest { sortConfig ->
                    browserUseCases.getBrowserUsageStatsUseCase(sortConfig.field, sortConfig.order)
                }
                .collect { result ->
                    when (result) {
                        is DomainResult.Success -> {
                            _browserStats.value = result.data
                            _uiState.update { it.copy(isLoading = false) }
                        }
                        is DomainResult.Failure -> {
                            _uiState.update { it.copy(
                                isLoading = false,
                                error = result.error.message
                            )}
                        }
                    }
                }
        }
    }

    private fun loadMostFrequentlyUsedBrowser() {
        viewModelScope.launch {
            browserUseCases.getMostFrequentlyUsedBrowserUseCase()
                .collect { result ->
                    if (result is DomainResult.Success) {
                        result.data?.let {
                            _mostFrequentlyUsedBrowser.value = it.packageName
                        }
                    }
                }
        }
    }

    private fun loadMostRecentlyUsedBrowser() {
        viewModelScope.launch {
            browserUseCases.getMostRecentlyUsedBrowserUseCase()
                .collect { result ->
                    if (result is DomainResult.Success) {
                        result.data?.let {
                            _mostRecentlyUsedBrowser.value = it.packageName
                        }
                    }
                }
        }
    }

    private fun loadUsageTrends() {
        viewModelScope.launch {
            _timeRange
                .flatMapLatest { range ->
                    searchAndAnalyticsUseCases.analyzeBrowserUsageTrendsUseCase(range)
                }
                .collect { result ->
                    when (result) {
                        is DomainResult.Success -> {
                            _usageTrends.value = result.data
                        }
                        is DomainResult.Failure -> {
                            _uiState.update { it.copy(error = result.error.message) }
                        }
                    }
                }
        }
    }

    fun updateTimeRange(range: Pair<Instant, Instant>?) {
        _timeRange.value = range
    }

    fun updateSortConfig(field: BrowserStatSortField, order: SortOrder) {
        _sortConfig.value = SortConfig(field, order)
    }

    fun generateBrowserUsageReport(exportToFile: Boolean = false, filePath: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, reportGenerating = true) }

            when (val result = searchAndAnalyticsUseCases.generateBrowserUsageReportUseCase(
                timeRange = _timeRange.value,
                exportToFile = exportToFile,
                filePath = filePath
            )) {
                is DomainResult.Success -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        reportGenerating = false,
                        reportGenerated = true,
                        report = result.data
                    )}
                }
                is DomainResult.Failure -> {
                    _uiState.update { it.copy(
                        isLoading = false,
                        reportGenerating = false,
                        error = result.error.message
                    )}
                }
            }
        }
    }

    fun clearBrowserStats(packageName: String) {
        TODO("Not yet implemented")
    }

    fun clearAllBrowserStats() {
        TODO("Not yet implemented")
    }

    fun showClearConfirmation() {
        _uiState.update { it.copy(showClearConfirmation = true) }
    }

    fun dismissClearConfirmation() {
        _uiState.update { it.copy(showClearConfirmation = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetActionStates() {
        _uiState.update { it.copy(
            statDeleted = false,
            allStatsCleared = false,
            reportGenerated = false,
            report = null
        )}
    }
}

data class SortConfig(
    val field: BrowserStatSortField,
    val order: SortOrder
)

data class BrowserAnalyticsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val statDeleted: Boolean = false,
    val allStatsCleared: Boolean = false,
    val showClearConfirmation: Boolean = false,
    val reportGenerating: Boolean = false,
    val reportGenerated: Boolean = false,
    val report: BrowserUsageReport? = null
)

 */