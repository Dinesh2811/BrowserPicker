package browserpicker.presentation.feature.analytics

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.di.DefaultDispatcher
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.di.MainDispatcher
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

/**
 * Represents the analytics filter options for browsers
 */
@Immutable
data class BrowserAnalyticsFilterOptions(
    val timeRange: Pair<Instant, Instant>? = null,
    val sortField: BrowserStatSortField = BrowserStatSortField.USAGE_COUNT,
    val sortOrder: SortOrder = SortOrder.DESC,
    val selectedBrowsers: Set<String> = emptySet(),
)

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
    val filterOptions: BrowserAnalyticsFilterOptions = BrowserAnalyticsFilterOptions(),
    val fullReport: UiState<BrowserUsageReport> = UiState.Loading,
    val isGeneratingReport: Boolean = false,
)

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

    private val _filterOptions = MutableStateFlow(BrowserAnalyticsFilterOptions())

    init {
        loadInitialData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadInitialData() {
        // Load most frequently used browser
        viewModelScope.launch {
            getMostFrequentlyUsedBrowserUseCase()
                .toUiState()
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    UiState.Loading
                )
                .collect { uiState ->
                    _state.update { it.copy(mostFrequentBrowser = uiState) }
                }
        }

        // Load most recently used browser
        viewModelScope.launch {
            getMostRecentlyUsedBrowserUseCase()
                .toUiState()
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    UiState.Loading
                )
                .collect { uiState ->
                    _state.update { it.copy(mostRecentBrowser = uiState) }
                }
        }

        // Load available browsers
        viewModelScope.launch {
            getAvailableBrowsersUseCase()
                .toUiState()
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    UiState.Loading
                )
                .collect { uiState ->
                    _state.update { it.copy(availableBrowsers = uiState) }
                }
        }

        // Observe filter options and load filtered data
        viewModelScope.launch {
            _filterOptions
                .flatMapLatest { filterOptions ->
                    getBrowserUsageStatsUseCase(
                        sortBy = filterOptions.sortField,
                        sortOrder = filterOptions.sortOrder
                    ).toUiState()
                }
                .collect { uiState ->
                    // Apply filtering on Success state
                    val filteredState = when (uiState) {
                        is UiState.Success -> {
                            val selectedBrowsers = _filterOptions.value.selectedBrowsers
                            if (selectedBrowsers.isEmpty()) {
                                uiState
                            } else {
                                UiState.Success(
                                    uiState.data.filter { stat ->
                                        selectedBrowsers.contains(stat.browserPackageName)
                                    }
                                )
                            }
                        }

                        else -> uiState
                    }

                    _state.update { it.copy(usageStats = filteredState) }
                }
        }

        // Load trend data
        viewModelScope.launch {
            _filterOptions
                .flatMapLatest { filterOptions ->
                    analyzeBrowserUsageTrendsUseCase(
                        timeRange = filterOptions.timeRange
                    ).toUiState()
                }
                .collect { uiState ->
                    // Apply filtering on Success state
                    val filteredState = when (uiState) {
                        is UiState.Success -> {
                            val selectedBrowsers = _filterOptions.value.selectedBrowsers
                            if (selectedBrowsers.isEmpty()) {
                                uiState
                            } else {
                                UiState.Success(
                                    uiState.data.filterKeys { browserPackage ->
                                        selectedBrowsers.contains(browserPackage)
                                    }
                                )
                            }
                        }

                        else -> uiState
                    }

                    _state.update { it.copy(trendData = filteredState) }
                }
        }
    }

    /**
     * Updates the time range filter for analytics data
     */
    fun updateTimeRange(from: Instant?, to: Instant? = Clock.System.now()) {
        if (from == null && to == null) {
            _filterOptions.update { it.copy(timeRange = null) }
        } else if (from != null && to != null && from <= to) {
            _filterOptions.update { it.copy(timeRange = Pair(from, to)) }
        }
    }

    /**
     * Sets the sort field and order for browser statistics
     */
    fun setSortingOptions(field: BrowserStatSortField, order: SortOrder) {
        _filterOptions.update {
            it.copy(sortField = field, sortOrder = order)
        }
    }

    /**
     * Updates the selected browsers for filtering
     */
    fun updateSelectedBrowsers(selectedPackageNames: Set<String>) {
        _filterOptions.update {
            it.copy(selectedBrowsers = selectedPackageNames)
        }
    }

    /**
     * Generates a comprehensive browser usage report
     */
    fun generateReport(exportToFile: Boolean = false, filePath: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isGeneratingReport = true) }

            val result = generateBrowserUsageReportUseCase(
                timeRange = _filterOptions.value.timeRange,
                exportToFile = exportToFile,
                filePath = filePath
            )

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
        _filterOptions.update {
            BrowserAnalyticsFilterOptions()
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
//        viewModelScope.launch {
//            _uiState.update { it.copy(isLoading = true) }
//
//            when (val result = browserUseCases.getBrowserUsageStatUseCase(packageName).first()) {
//                is DomainResult.Success -> {
//                    // Browser stats found, delete them
//                    val deleteResult = browserUseCases.deleteBrowserStatUseCase(packageName)
//                    if (deleteResult is DomainResult.Success) {
//                        _uiState.update { it.copy(
//                            isLoading = false,
//                            statDeleted = true
//                        )}
//                        // Reload stats
//                        loadBrowserStats()
//                        loadMostFrequentlyUsedBrowser()
//                        loadMostRecentlyUsedBrowser()
//                        loadUsageTrends()
//                    } else {
//                        _uiState.update { it.copy(
//                            isLoading = false,
//                            error = (deleteResult as DomainResult.Failure).error.message
//                        )}
//                    }
//                }
//                is DomainResult.Failure -> {
//                    _uiState.update { it.copy(
//                        isLoading = false,
//                        error = result.error.message
//                    )}
//                }
//            }
//        }
    }

    fun clearAllBrowserStats() {
//        viewModelScope.launch {
//            _uiState.update { it.copy(isLoading = true, showClearConfirmation = false) }
//
//            when (val result = browserUseCases.deleteAllStatsUseCase()) {
//                is DomainResult.Success -> {
//                    _uiState.update { it.copy(
//                        isLoading = false,
//                        allStatsCleared = true
//                    )}
//                    // Reload stats
//                    loadBrowserStats()
//                    loadMostFrequentlyUsedBrowser()
//                    loadMostRecentlyUsedBrowser()
//                    loadUsageTrends()
//                }
//                is DomainResult.Failure -> {
//                    _uiState.update { it.copy(
//                        isLoading = false,
//                        error = result.error.message
//                    )}
//                }
//            }
//        }
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