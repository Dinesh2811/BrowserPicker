package browserpicker.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.domain.model.BrowserUsageStat
import browserpicker.domain.model.DateCount
import browserpicker.domain.usecases.analytics.AnalyzeBrowserUsageTrendsUseCase
import browserpicker.domain.usecases.analytics.GenerateBrowserUsageReportUseCase
import browserpicker.domain.usecases.browser.GetBrowserUsageStatsUseCase
import browserpicker.domain.usecases.browser.GetMostFrequentlyUsedBrowserUseCase
import browserpicker.domain.usecases.browser.GetMostRecentlyUsedBrowserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * ViewModel for the Browser Analytics screen.
 *
 * This ViewModel handles:
 * - Displaying browser usage statistics
 * - Analyzing browser usage trends over time
 * - Generating browser usage reports
 * - Providing insights on most used browsers
 *
 * Used by: BrowserAnalyticsScreen
 */
@HiltViewModel
class BrowserAnalyticsViewModel @Inject constructor(
    private val getBrowserUsageStatsUseCase: GetBrowserUsageStatsUseCase,
    private val getMostFrequentlyUsedBrowserUseCase: GetMostFrequentlyUsedBrowserUseCase,
    private val getMostRecentlyUsedBrowserUseCase: GetMostRecentlyUsedBrowserUseCase,
    private val analyzeBrowserUsageTrendsUseCase: AnalyzeBrowserUsageTrendsUseCase,
    private val generateBrowserUsageReportUseCase: GenerateBrowserUsageReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserAnalyticsUiState())
    val uiState: StateFlow<BrowserAnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadBrowserStats()
        loadUsageTrends()
    }

    /**
     * Load browser usage statistics
     */
    private fun loadBrowserStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            getBrowserUsageStatsUseCase()
                .collect { result ->
                    result.onSuccess { stats ->
                        _uiState.value = _uiState.value.copy(
                            browserStats = stats,
                            isLoading = false
                        )
                    }.onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message,
                            isLoading = false
                        )
                    }
                }
        }

        viewModelScope.launch {
            getMostFrequentlyUsedBrowserUseCase()
                .collect { result ->
                    result.onSuccess { browserInfo ->
                        _uiState.value = _uiState.value.copy(
                            mostUsedBrowser = browserInfo?.appName
                        )
                    }
                }
        }

        viewModelScope.launch {
            getMostRecentlyUsedBrowserUseCase()
                .collect { result ->
                    result.onSuccess { browserInfo ->
                        _uiState.value = _uiState.value.copy(
                            mostRecentBrowser = browserInfo?.appName
                        )
                    }
                }
        }
    }

    /**
     * Load browser usage trends over time
     */
    private fun loadUsageTrends() {
        viewModelScope.launch {
            analyzeBrowserUsageTrendsUseCase()
                .collect { result ->
                    result.onSuccess { trends ->
                        _uiState.value = _uiState.value.copy(
                            usageTrends = trends
                        )
                    }
                }
        }
    }

    /**
     * Generate a browser usage report
     */
    fun generateReport(exportToFile: Boolean = false, filePath: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingReport = true)

            val result = generateBrowserUsageReportUseCase(
                exportToFile = exportToFile,
                filePath = filePath
            )

            result.onSuccess { report ->
                _uiState.value = _uiState.value.copy(
                    report = report,
                    isGeneratingReport = false,
                    reportGenerated = true
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    error = "Failed to generate report: ${error.message}",
                    isGeneratingReport = false
                )
            }
        }
    }

    /**
     * Set date range for analytics
     */
    fun setDateRange(start: Instant, end: Instant) {
        _uiState.value = _uiState.value.copy(
            selectedDateRange = start to end
        )
        loadUsageTrends()
    }

    /**
     * Clear selected date range
     */
    fun clearDateRange() {
        _uiState.value = _uiState.value.copy(
            selectedDateRange = null
        )
        loadUsageTrends()
    }
}

/**
 * UI state for the Browser Analytics screen
 */
data class BrowserAnalyticsUiState(
    val browserStats: List<BrowserUsageStat> = emptyList(),
    val usageTrends: Map<String, List<DateCount>> = emptyMap(),
    val mostUsedBrowser: String? = null,
    val mostRecentBrowser: String? = null,
    val selectedDateRange: Pair<Instant, Instant>? = null,
    val report: Any? = null,
    val reportGenerated: Boolean = false,
    val isGeneratingReport: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)



/*
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
 * ViewModel for the Browser Analytics screen.
 *
 * This ViewModel handles:
 * - Displaying browser usage statistics
 * - Handles loading, filtering, and reporting of browser usage statistics.
 * - Analyzing browser usage trends over time
 * - Generating browser usage reports
 * - Providing insights on most used browsers
 *
 * Used by: BrowserAnalyticsScreen
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

 */

/*

import androidx.lifecycle.*
import browserpicker.core.di.DefaultDispatcher
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.di.MainDispatcher
import browserpicker.domain.model.BrowserUsageStat
import browserpicker.domain.model.DateCount
import browserpicker.domain.usecases.analytics.AnalyzeBrowserUsageTrendsUseCase
import browserpicker.domain.usecases.analytics.GenerateBrowserUsageReportUseCase
import browserpicker.domain.usecases.browser.GetAvailableBrowsersUseCase
import browserpicker.domain.usecases.browser.GetBrowserUsageStatsUseCase
import browserpicker.domain.usecases.browser.GetMostFrequentlyUsedBrowserUseCase
import browserpicker.domain.usecases.browser.GetMostRecentlyUsedBrowserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * ViewModel for the Browser Analytics screen.
 *
 * This ViewModel handles:
 * - Displaying browser usage statistics
 * - Analyzing browser usage trends over time
 * - Generating browser usage reports
 * - Providing insights on most used browsers
 *
 * Used by: BrowserAnalyticsScreen
 */
@HiltViewModel
class BrowserAnalyticsViewModel @Inject constructor(
    private val getBrowserUsageStatsUseCase: GetBrowserUsageStatsUseCase,
    private val getMostFrequentlyUsedBrowserUseCase: GetMostFrequentlyUsedBrowserUseCase,
    private val getMostRecentlyUsedBrowserUseCase: GetMostRecentlyUsedBrowserUseCase,
    private val analyzeBrowserUsageTrendsUseCase: AnalyzeBrowserUsageTrendsUseCase,
    private val generateBrowserUsageReportUseCase: GenerateBrowserUsageReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowserAnalyticsUiState())
    val uiState: StateFlow<BrowserAnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadBrowserStats()
        loadUsageTrends()
    }

    /**
     * Load browser usage statistics
     */
    private fun loadBrowserStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            getBrowserUsageStatsUseCase()
                .collect { result ->
                    result.onSuccess { stats ->
                        _uiState.value = _uiState.value.copy(
                            browserStats = stats,
                            isLoading = false
                        )
                    }.onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message,
                            isLoading = false
                        )
                    }
                }
        }

        viewModelScope.launch {
            getMostFrequentlyUsedBrowserUseCase()
                .collect { result ->
                    result.onSuccess { browserInfo ->
                        _uiState.value = _uiState.value.copy(
                            mostUsedBrowser = browserInfo?.appName
                        )
                    }
                }
        }

        viewModelScope.launch {
            getMostRecentlyUsedBrowserUseCase()
                .collect { result ->
                    result.onSuccess { browserInfo ->
                        _uiState.value = _uiState.value.copy(
                            mostRecentBrowser = browserInfo?.appName
                        )
                    }
                }
        }
    }

    /**
     * Load browser usage trends over time
     */
    private fun loadUsageTrends() {
        viewModelScope.launch {
            analyzeBrowserUsageTrendsUseCase()
                .collect { result ->
                    result.onSuccess { trends ->
                        _uiState.value = _uiState.value.copy(
                            usageTrends = trends
                        )
                    }
                }
        }
    }

    /**
     * Generate a browser usage report
     */
    fun generateReport(exportToFile: Boolean = false, filePath: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingReport = true)

            val result = generateBrowserUsageReportUseCase(
                exportToFile = exportToFile,
                filePath = filePath
            )

            result.onSuccess { report ->
                _uiState.value = _uiState.value.copy(
                    report = report,
                    isGeneratingReport = false,
                    reportGenerated = true
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    error = "Failed to generate report: ${error.message}",
                    isGeneratingReport = false
                )
            }
        }
    }

    /**
     * Set date range for analytics
     */
    fun setDateRange(start: Instant, end: Instant) {
        _uiState.value = _uiState.value.copy(
            selectedDateRange = start to end
        )
        loadUsageTrends()
    }

    /**
     * Clear selected date range
     */
    fun clearDateRange() {
        _uiState.value = _uiState.value.copy(
            selectedDateRange = null
        )
        loadUsageTrends()
    }
}

/**
 * UI state for the Browser Analytics screen
 */
data class BrowserAnalyticsUiState(
    val browserStats: List<BrowserUsageStat> = emptyList(),
    val usageTrends: Map<String, List<DateCount>> = emptyMap(),
    val mostUsedBrowser: String? = null,
    val mostRecentBrowser: String? = null,
    val selectedDateRange: Pair<Instant, Instant>? = null,
    val report: Any? = null,
    val reportGenerated: Boolean = false,
    val isGeneratingReport: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

 */