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
import browserpicker.domain.model.DateCount
import browserpicker.domain.model.GroupCount
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import browserpicker.domain.usecases.analytics.UriHistoryReport
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
 * Represents the analytics filter options for URIs
 */
@Immutable
data class UriAnalyticsFilterOptions(
    val timeRange: Pair<Instant, Instant>? = null,
    val selectedHosts: Set<String> = emptySet(),
    val selectedActions: Set<InteractionAction> = emptySet(),
    val selectedSources: Set<UriSource> = emptySet()
) {
    companion object {
        val DEFAULT = UriAnalyticsFilterOptions()
    }
}

/**
 * Represents the complete UI state for URI analytics
 */
@Immutable
data class UriAnalyticsUiState(
    val trendData: UiState<Map<String, List<DateCount>>> = UiState.Loading,
    val statusChangeData: UiState<Map<String, List<DateCount>>> = UiState.Loading,
    val mostVisitedHosts: UiState<List<GroupCount>> = UiState.Loading,
    val topActionsByHost: UiState<Map<String, List<GroupCount>>> = UiState.Loading,
    val hostList: UiState<List<String>> = UiState.Loading,
    val filterOptions: UriAnalyticsFilterOptions = UriAnalyticsFilterOptions.DEFAULT,
    val fullReport: UiState<UriHistoryReport> = UiState.Loading,
    val isGeneratingReport: Boolean = false
)

/**
 * ViewModel for the URI Analytics screen.
 *
 * This ViewModel handles:
 * - Displaying URI usage trends
 * - Showing most visited hosts
 * - Analyzing user actions for URIs
 * - Generating URI history reports
 * - Monitoring status changes (bookmarked/blocked)
 *
 * Used by: UriAnalyticsScreen
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UriAnalyticsViewModel @Inject constructor(
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

    private val _state = MutableStateFlow(UriAnalyticsUiState())
    val state: StateFlow<UriAnalyticsUiState> = _state.asStateFlow()

    private val _filterOptions = MutableStateFlow(UriAnalyticsFilterOptions.DEFAULT)
    private val _refreshTrigger = MutableStateFlow(0)

    init {
        loadInitialData()
        observeFilterAndRefreshTriggers()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadInitialData() {
        loadHostList()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeFilterAndRefreshTriggers() {
        loadUriTrends()
        loadMostVisitedHosts()
        loadTopActionsByHost()
        loadStatusChanges()
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
     * Updates the selected hosts for filtering
     * @param hosts Set of hosts to filter by
     */
    fun updateSelectedHosts(hosts: Set<String>) {
        viewModelScope.launch {
            withContext(defaultDispatcher) {
                _filterOptions.update {
                    it.copy(selectedHosts = hosts)
                }
            }
        }
    }

    /**
     * Updates the selected interaction actions for filtering
     * @param actions Set of interaction actions to filter by
     */
    fun updateSelectedActions(actions: Set<InteractionAction>) {
        viewModelScope.launch {
            withContext(defaultDispatcher) {
                _filterOptions.update {
                    it.copy(selectedActions = actions)
                }
            }
        }
    }

    /**
     * Updates the selected URI sources for filtering
     * @param sources Set of URI sources to filter by
     */
    fun updateSelectedSources(sources: Set<UriSource>) {
        viewModelScope.launch {
            withContext(defaultDispatcher) {
                _filterOptions.update {
                    it.copy(selectedSources = sources)
                }
            }
        }
    }

    /**
     * Generates a comprehensive URI history report
     * @param exportToFile Whether to export report to a file
     * @param filePath Optional file path for export
     */
    fun generateReport(exportToFile: Boolean = false, filePath: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isGeneratingReport = true, fullReport = UiState.Loading) }

            val result = withContext(ioDispatcher) {
                searchAndAnalyticsUseCases.generateHistoryReportUseCase(
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
                    UriAnalyticsFilterOptions.DEFAULT
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
                    trendData = UiState.Loading,
                    statusChangeData = UiState.Loading,
                    mostVisitedHosts = UiState.Loading,
                    topActionsByHost = UiState.Loading,
                    hostList = UiState.Loading,
                    fullReport = if (it.fullReport !is UiState.Loading) UiState.Loading else it.fullReport
                )
            }
            loadInitialData()
            _refreshTrigger.update { it + 1 }
        }
    }

    /**
     * Gets details about top actions for a specific host
     * @param host The host to get details for
     */
    fun getHostDetails(host: String) {
        viewModelScope.launch {
            _state.update {
                val currentTopActions = it.topActionsByHost.getOrNull() ?: emptyMap()
                if (currentTopActions.containsKey(host)) {
                    // Already have this host's data
                    return@launch
                }

                it.copy(
                    topActionsByHost = UiState.Loading
                )
            }

            loadTopActionsForHost(host)
        }
    }

    private fun loadHostList() {
        viewModelScope.launch {
            uriHistoryUseCases.getUriFilterOptionsUseCase()
                .flowOn(ioDispatcher)
                .map { result ->
                    result.mapSuccess { options ->
                        // Combine both history hosts and rule hosts
                        (options.distinctHistoryHosts + options.distinctRuleHosts).distinct()
                    }
                }
                .toUiState()
                .catch { unexpectedError ->
                    emit(UiState.Error("Failed to load hosts: ${unexpectedError.message}", unexpectedError))
                }
                .collect { uiState ->
                    _state.update { it.copy(hostList = uiState) }
                }
        }
    }

    private fun loadUriTrends() {
        viewModelScope.launch {
            combine(_filterOptions, _refreshTrigger) { options, _ -> options }
                .onStart { _state.update { it.copy(trendData = UiState.Loading) } }
                .distinctUntilChanged()
                .flatMapLatest { filterOptions ->
                    searchAndAnalyticsUseCases.analyzeUriTrendsUseCase(
                        timeRange = filterOptions.timeRange
                    )
                        .flowOn(ioDispatcher)
                        .map { result ->
                            when (result) {
                                is DomainResult.Success -> {
                                    val selectedHosts = filterOptions.selectedHosts
                                    val filteredData = if (selectedHosts.isEmpty()) {
                                        result.data
                                    } else {
                                        result.data.filterKeys { host ->
                                            selectedHosts.contains(host)
                                        }
                                    }
                                    DomainResult.Success(filteredData)
                                }
                                is DomainResult.Failure -> result
                            }
                        }
                        .toUiState()
                        .catch { unexpectedError ->
                            emit(UiState.Error("Failed to load URI trends: ${unexpectedError.message}", unexpectedError))
                        }
                }
                .flowOn(defaultDispatcher)
                .collect { uiState ->
                    _state.update { it.copy(trendData = uiState) }
                }
        }
    }

    private fun loadMostVisitedHosts() {
        viewModelScope.launch {
            combine(_filterOptions, _refreshTrigger) { options, _ -> options }
                .onStart { _state.update { it.copy(mostVisitedHosts = UiState.Loading) } }
                .distinctUntilChanged()
                .flatMapLatest { filterOptions ->
                    searchAndAnalyticsUseCases.getMostVisitedHostsUseCase(
                        limit = 10,
                        timeRange = filterOptions.timeRange
                    )
                        .flowOn(ioDispatcher)
                        .toUiState()
                        .catch { unexpectedError ->
                            emit(UiState.Error("Failed to load most visited hosts: ${unexpectedError.message}", unexpectedError))
                        }
                }
                .flowOn(defaultDispatcher)
                .collect { uiState ->
                    _state.update { it.copy(mostVisitedHosts = uiState) }
                }
        }
    }

    private fun loadTopActionsByHost() {
        viewModelScope.launch {
            val hostList = when (val hostListState = _state.value.hostList) {
                is UiState.Success -> {
                    val selectedHosts = _filterOptions.value.selectedHosts
                    if (selectedHosts.isEmpty()) {
                        // If no selection, use top 5 hosts from the most visited
                        when (val mostVisited = _state.value.mostVisitedHosts) {
                            is UiState.Success -> {
                                mostVisited.data.take(5).mapNotNull { it.groupValue }
                            }
                            else -> emptyList()
                        }
                    } else {
                        // Use the selected hosts
                        selectedHosts.toList()
                    }
                }
                else -> emptyList()
            }

            if (hostList.isEmpty()) {
                _state.update { it.copy(topActionsByHost = UiState.Success(emptyMap())) }
                return@launch
            }

            _state.update { it.copy(topActionsByHost = UiState.Loading) }

            val actionsMap = mutableMapOf<String, List<GroupCount>>()

            for (host in hostList) {
                val result = withContext(ioDispatcher) {
                    searchAndAnalyticsUseCases.getTopActionsByHostUseCase(host)
                        .stateIn(
                            viewModelScope,
                            SharingStarted.WhileSubscribed(5000.milliseconds),
                            DomainResult.Success(emptyList())
                        )
                        .value
                }

                when (result) {
                    is DomainResult.Success -> {
                        actionsMap[host] = result.data
                    }
                    is DomainResult.Failure -> {
                        // Skip failed hosts
                    }
                }
            }

            _state.update { it.copy(topActionsByHost = UiState.Success(actionsMap)) }
        }
    }

    private fun loadTopActionsForHost(host: String) {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                searchAndAnalyticsUseCases.getTopActionsByHostUseCase(host)
                    .stateIn(
                        viewModelScope,
                        SharingStarted.WhileSubscribed(5000.milliseconds),
                        DomainResult.Success(emptyList())
                    )
                    .value
            }

            when (result) {
                is DomainResult.Success -> {
                    val currentMap = (_state.value.topActionsByHost.getOrNull() ?: emptyMap()).toMutableMap()
                    currentMap[host] = result.data
                    _state.update { it.copy(topActionsByHost = UiState.Success(currentMap)) }
                }
                is DomainResult.Failure -> {
                    // Preserve current state, don't update on failure
                }
            }
        }
    }

    private fun loadStatusChanges() {
        viewModelScope.launch {
            combine(_filterOptions, _refreshTrigger) { options, _ -> options }
                .onStart { _state.update { it.copy(statusChangeData = UiState.Loading) } }
                .distinctUntilChanged()
                .flatMapLatest { filterOptions ->
                    searchAndAnalyticsUseCases.analyzeUriStatusChangesUseCase(
                        timeRange = filterOptions.timeRange
                    )
                        .flowOn(ioDispatcher)
                        .map { result ->
                            when (result) {
                                is DomainResult.Success -> {
                                    val selectedHosts = filterOptions.selectedHosts
                                    val filteredData = if (selectedHosts.isEmpty()) {
                                        result.data
                                    } else {
                                        result.data.filterKeys { host ->
                                            selectedHosts.contains(host)
                                        }
                                    }
                                    DomainResult.Success(filteredData)
                                }
                                is DomainResult.Failure -> result
                            }
                        }
                        .toUiState()
                        .catch { unexpectedError ->
                            emit(UiState.Error("Failed to load status changes: ${unexpectedError.message}", unexpectedError))
                        }
                }
                .flowOn(defaultDispatcher)
                .collect { uiState ->
                    _state.update { it.copy(statusChangeData = uiState) }
                }
        }
    }
}
