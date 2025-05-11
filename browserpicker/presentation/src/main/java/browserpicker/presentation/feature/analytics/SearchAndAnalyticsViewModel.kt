package browserpicker.presentation.feature.analytics

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import browserpicker.core.di.DefaultDispatcher
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.di.MainDispatcher
import browserpicker.core.results.DomainResult
import browserpicker.core.results.AppError
import browserpicker.domain.model.*
import browserpicker.domain.model.query.*
import browserpicker.domain.service.PagingDefaults
import browserpicker.domain.usecases.analytics.*
import browserpicker.presentation.UiState
import browserpicker.presentation.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.hours

/**
 * Search query options for analytics
 */
@Immutable
data class AnalyticsSearchQuery(
    val searchText: String = "",
    val timeRange: Pair<Instant, Instant>? = null,
    val hostFilter: String? = null,
    val statusFilter: UriStatus? = null,
    val folderFilter: Long? = null,
    val limit: Int = 10,
)

/**
 * UI state for trend data
 */
@Immutable
data class TrendDataUiState(
    val hostTrends: UiState<Map<String, List<DateCount>>> = UiState.Loading,
    val actionTrends: UiState<Map<String, List<DateCount>>> = UiState.Loading,
    val browserTrends: UiState<Map<String, List<DateCount>>> = UiState.Loading,
)

/**
 * UI state for search and analytics
 */
@Immutable
data class SearchAnalyticsUiState(
    val isSearchActive: Boolean = false,
    val searchQuery: AnalyticsSearchQuery = AnalyticsSearchQuery(),
    val trendData: TrendDataUiState = TrendDataUiState(),
    val topHosts: UiState<List<GroupCount>> = UiState.Loading,
    val searchResults: Flow<PagingData<HostRule>>? = null,
    val hostDetail: UiState<Pair<String, List<GroupCount>>> = UiState.Loading,
    val historyReport: UiState<UriHistoryReport> = UiState.Loading,
    val isGeneratingReport: Boolean = false,
)

/**
 * ViewModel for search and analytics functionality
 * Provides capabilities to search through host rules and analyze URI trends
 */
@HiltViewModel
class SearchAndAnalyticsViewModel @Inject constructor(
    private val instantProvider: InstantProvider,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    private val searchHostRulesUseCase: SearchHostRulesUseCase,
    private val analyzeUriTrendsUseCase: AnalyzeUriTrendsUseCase,
    private val analyzeBrowserUsageTrendsUseCase: AnalyzeBrowserUsageTrendsUseCase,
    private val analyzeUriStatusChangesUseCase: AnalyzeUriStatusChangesUseCase,
    private val getMostVisitedHostsUseCase: GetMostVisitedHostsUseCase,
    private val getTopActionsByHostUseCase: GetTopActionsByHostUseCase,
    private val generateHistoryReportUseCase: GenerateHistoryReportUseCase,
): ViewModel() {

    private val _state = MutableStateFlow(SearchAnalyticsUiState())
    val state: StateFlow<SearchAnalyticsUiState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow(AnalyticsSearchQuery())

    init {
        setupSearchObserver()
        loadInitialData()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun setupSearchObserver() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300.milliseconds)
                .distinctUntilChanged()
                .onStart { _state.update { it.copy(isSearchActive = false) } }
                .collectLatest { query ->
                    if (query.searchText.isNotBlank() || query.hostFilter != null || query.statusFilter != null || query.folderFilter != null) {
                        val searchResults = searchHostRulesUseCase(
                            query = query.searchText,
                            filterByStatus = query.statusFilter,
                            includeFolderId = query.folderFilter,
                            pagingConfig = PagingDefaults.DEFAULT_PAGING_CONFIG
                        ).cachedIn(viewModelScope)

                        _state.update {
                            it.copy(
                                isSearchActive = true,
                                searchQuery = query,
                                searchResults = searchResults
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isSearchActive = false,
                                searchQuery = query,
                                searchResults = null
                            )
                        }
                    }
                }
        }
    }

    private fun loadInitialData() {
        loadTrendData()
        loadTopHosts()
    }

    private fun loadTrendData() {
        // Get default time range (last 30 days)
        val now = instantProvider.now()
        val thirtyDaysAgo = now.minus(30.days)
        val timeRange = Pair(thirtyDaysAgo, now)

        // Load URI trends
        viewModelScope.launch {
            analyzeUriTrendsUseCase(timeRange)
                .flowOn(ioDispatcher)
                .toUiState()
                .catch { error ->
                    _state.update {
                        it.copy(
                            trendData = it.trendData.copy(
                                hostTrends = UiState.Error("Failed to load URI trends: ${error.message}", error)
                            )
                        )
                    }
                }
                .collect { uiState ->
                    _state.update {
                        it.copy(
                            trendData = it.trendData.copy(hostTrends = uiState)
                        )
                    }
                }
        }

        // Load browser trends
        viewModelScope.launch {
            analyzeBrowserUsageTrendsUseCase(timeRange)
                .flowOn(ioDispatcher)
                .toUiState()
                .catch { error ->
                    _state.update {
                        it.copy(
                            trendData = it.trendData.copy(
                                browserTrends = UiState.Error("Failed to load browser trends: ${error.message}", error)
                            )
                        )
                    }
                }
                .collect { uiState ->
                    _state.update {
                        it.copy(
                            trendData = it.trendData.copy(browserTrends = uiState)
                        )
                    }
                }
        }

        // Load status change trends
        viewModelScope.launch {
            analyzeUriStatusChangesUseCase(timeRange)
                .flowOn(ioDispatcher)
                .toUiState()
                .catch { error ->
                    _state.update {
                        it.copy(
                            trendData = it.trendData.copy(
                                actionTrends = UiState.Error("Failed to load status change trends: ${error.message}", error)
                            )
                        )
                    }
                }
                .collect { uiState ->
                    _state.update {
                        it.copy(
                            trendData = it.trendData.copy(actionTrends = uiState)
                        )
                    }
                }
        }
    }

    private fun loadTopHosts() {
        viewModelScope.launch {
            val now = instantProvider.now()
            val thirtyDaysAgo = now.minus(30.days)
            val timeRange = Pair(thirtyDaysAgo, now)

            getMostVisitedHostsUseCase(limit = 10, timeRange = timeRange)
                .flowOn(ioDispatcher)
                .toUiState()
                .catch { error ->
                    _state.update {
                        it.copy(topHosts = UiState.Error("Failed to load top hosts: ${error.message}", error))
                    }
                }
                .collect { uiState ->
                    _state.update { it.copy(topHosts = uiState) }
                }
        }
    }

    /**
     * Updates the search query text
     */
    fun updateSearchText(text: String) {
        _searchQuery.update { it.copy(searchText = text) }
    }

    /**
     * Updates filters for the search
     */
    fun updateSearchFilters(
        hostFilter: String? = _searchQuery.value.hostFilter,
        statusFilter: UriStatus? = _searchQuery.value.statusFilter,
        folderFilter: Long? = _searchQuery.value.folderFilter,
        timeRange: Pair<Instant, Instant>? = _searchQuery.value.timeRange,
    ) {
        _searchQuery.update {
            it.copy(
                hostFilter = hostFilter,
                statusFilter = statusFilter,
                folderFilter = folderFilter,
                timeRange = timeRange
            )
        }
    }

    /**
     * Reset all search filters
     */
    fun resetSearchFilters() {
        _searchQuery.update {
            AnalyticsSearchQuery(searchText = it.searchText)
        }
    }

    /**
     * Load details for a specific host
     */
    fun loadHostDetails(host: String) {
        if (host.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(hostDetail = UiState.Loading) }

            getTopActionsByHostUseCase(host)
                .flowOn(ioDispatcher)
                .toUiState()
                .catch { error ->
                    _state.update {
                        it.copy(hostDetail = UiState.Error("Failed to load host details: ${error.message}", error))
                    }
                }
                .map { result ->
                    result.map { actions -> Pair(host, actions) }
                }
                .collect { uiState ->
                    _state.update { it.copy(hostDetail = uiState) }
                }
        }
    }

    /**
     * Generate a comprehensive history report
     */
    fun generateHistoryReport(exportToFile: Boolean = false, filePath: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isGeneratingReport = true) }

            val timeRange = _searchQuery.value.timeRange ?: run {
                val now = instantProvider.now()
                Pair(now.minus(30.days), now)
            }

            val result = withContext(ioDispatcher) {
                generateHistoryReportUseCase(
                    timeRange = timeRange,
                    exportToFile = exportToFile,
                    filePath = filePath
                )
            }

            _state.update {
                it.copy(
                    historyReport = result.toUiState(),
                    isGeneratingReport = false
                )
            }
        }
    }

    /**
     * Refresh all analytics data
     */
    fun refreshAnalytics() {
        _state.update {
            it.copy(
                trendData = TrendDataUiState(),
                topHosts = UiState.Loading
            )
        }
        loadInitialData()
    }

    /**
     * Update the time range for analytics
     */
    fun updateTimeRange(period: AnalyticsTimePeriod) {
        val now = instantProvider.now()
        val startTime = when (period) {
            AnalyticsTimePeriod.LAST_24_HOURS -> now.minus(24.hours)
            AnalyticsTimePeriod.LAST_7_DAYS -> now.minus(7.days)
            AnalyticsTimePeriod.LAST_30_DAYS -> now.minus(30.days)
            AnalyticsTimePeriod.LAST_90_DAYS -> now.minus(90.days)
            AnalyticsTimePeriod.ALL_TIME -> null
        }

        val timeRange = if (startTime != null) Pair(startTime, now) else null
        _searchQuery.update { it.copy(timeRange = timeRange) }

        // Reload data with new time range
        loadTrendData()
        loadTopHosts()
    }
}

/**
 * Enum class representing different time periods for analytics
 */
enum class AnalyticsTimePeriod {
    LAST_24_HOURS,
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_90_DAYS,
    ALL_TIME
}
