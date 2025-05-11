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
import kotlinx.coroutines.flow.map
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