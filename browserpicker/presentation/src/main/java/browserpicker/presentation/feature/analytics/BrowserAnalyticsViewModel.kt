package browserpicker.presentation.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.results.DomainResult
import browserpicker.domain.di.BrowserUseCases
import browserpicker.domain.di.SearchAndAnalyticsUseCases
import browserpicker.domain.model.BrowserUsageStat
import browserpicker.domain.model.*
import browserpicker.domain.model.query.*
import browserpicker.domain.usecases.BrowserUsageReport
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