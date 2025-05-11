package browserpicker.presentation.analytics

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.di.DefaultDispatcher
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.di.MainDispatcher
import browserpicker.core.results.DomainResult
import browserpicker.domain.di.BrowserUseCases
import browserpicker.domain.di.FolderUseCases
import browserpicker.domain.di.HostRuleUseCases
import browserpicker.domain.di.SearchAndAnalyticsUseCases
import browserpicker.domain.di.SystemIntegrationUseCases
import browserpicker.domain.di.UriHandlingUseCases
import browserpicker.domain.di.UriHistoryUseCases
import browserpicker.domain.model.BrowserAppInfo
import browserpicker.domain.model.BrowserUsageStat
import browserpicker.domain.model.DateCount
import browserpicker.domain.model.query.BrowserStatSortField
import browserpicker.domain.model.query.SortOrder
import browserpicker.domain.usecases.analytics.BrowserUsageReport
import browserpicker.presentation.UiState
import browserpicker.presentation.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val selectedBrowserDetails: UiState<BrowserUsageStat?> = UiState.Loading
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
    private val uriHandlingUseCases: UriHandlingUseCases,
    private val browserUseCases: BrowserUseCases,
    private val hostRuleUseCases: HostRuleUseCases,
    private val uriHistoryUseCases: UriHistoryUseCases,
    private val folderUseCases: FolderUseCases,
    private val searchAndAnalyticsUseCases: SearchAndAnalyticsUseCases,
    private val systemIntegrationUseCases: SystemIntegrationUseCases,
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

        // Automatically load details for the most recent browser
        viewModelScope.launch {
            // Wait for most recent browser to load first
            val mostRecentBrowser = browserUseCases.getMostRecentlyUsedBrowserUseCase()
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000.milliseconds),
                    DomainResult.Success(null)
                )
                .value

            when (mostRecentBrowser) {
                is DomainResult.Success -> {
                    mostRecentBrowser.data?.let { browser ->
                        getBrowserDetails(browser.packageName)
                    }
                }
                else -> { /* Do nothing */ }
            }
        }
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
                searchAndAnalyticsUseCases.generateBrowserUsageReportUseCase(
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

            // If this is a file export, show feedback message or handle file path
            if (exportToFile && result is DomainResult.Success) {
                // In a real app, you might want to show a toast or some UI feedback
                // or provide a way to share/view the exported file
            }
        }
    }

    /**
     * Gets details for a specific browser to display more information
     * @param packageName The package name of the browser to get details for
     */
    fun getBrowserDetails(packageName: String) {
        viewModelScope.launch {
            _state.update { it.copy(selectedBrowserDetails = UiState.Loading) }

            val browserStat = withContext(ioDispatcher) {
                browserUseCases.getBrowserUsageStatUseCase(packageName)
                    .stateIn(
                        viewModelScope,
                        SharingStarted.WhileSubscribed(5000.milliseconds),
                        DomainResult.Success(null)
                    )
                    .value
            }

            _state.update {
                it.copy(selectedBrowserDetails = browserStat.toUiState())
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
            browserUseCases.getMostFrequentlyUsedBrowserUseCase()
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
            browserUseCases.getMostRecentlyUsedBrowserUseCase()
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
            browserUseCases.getAvailableBrowsersUseCase()
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
                    browserUseCases.getBrowserUsageStatsUseCase(
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
                .distinctUntilChanged()
                .flatMapLatest { filterOptions ->
                    searchAndAnalyticsUseCases.analyzeBrowserUsageTrendsUseCase(
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
                .flowOn(defaultDispatcher)
                .collect { uiState ->
                    _state.update { it.copy(trendData = uiState) }
                }
        }
    }
}
