package browserpicker.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.domain.model.DateCount
import browserpicker.domain.model.GroupCount
import browserpicker.domain.usecases.analytics.AnalyzeUriStatusChangesUseCase
import browserpicker.domain.usecases.analytics.AnalyzeUriTrendsUseCase
import browserpicker.domain.usecases.analytics.GenerateHistoryReportUseCase
import browserpicker.domain.usecases.analytics.GetMostVisitedHostsUseCase
import browserpicker.domain.usecases.analytics.GetTopActionsByHostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import javax.inject.Inject

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
@HiltViewModel
class UriAnalyticsViewModel @Inject constructor(
    private val getMostVisitedHostsUseCase: GetMostVisitedHostsUseCase,
    private val getTopActionsByHostUseCase: GetTopActionsByHostUseCase,
    private val analyzeUriTrendsUseCase: AnalyzeUriTrendsUseCase,
    private val analyzeUriStatusChangesUseCase: AnalyzeUriStatusChangesUseCase,
    private val generateHistoryReportUseCase: GenerateHistoryReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(UriAnalyticsUiState())
    val uiState: StateFlow<UriAnalyticsUiState> = _uiState.asStateFlow()
    
    init {
        loadMostVisitedHosts()
        loadUriTrends()
        loadStatusChanges()
    }
    
    /**
     * Load most visited hosts
     */
    private fun loadMostVisitedHosts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            getMostVisitedHostsUseCase(limit = 10)
                .collect { result ->
                    result.onSuccess { hosts ->
                        _uiState.value = _uiState.value.copy(
                            topHosts = hosts,
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
    }
    
    /**
     * Load URI usage trends
     */
    private fun loadUriTrends() {
        viewModelScope.launch {
            analyzeUriTrendsUseCase(timeRange = _uiState.value.selectedDateRange)
                .collect { result ->
                    result.onSuccess { trends ->
                        _uiState.value = _uiState.value.copy(
                            uriTrends = trends
                        )
                    }
                }
        }
    }
    
    /**
     * Load URI status changes (bookmarked/blocked)
     */
    private fun loadStatusChanges() {
        viewModelScope.launch {
            analyzeUriStatusChangesUseCase(timeRange = _uiState.value.selectedDateRange)
                .collect { result ->
                    result.onSuccess { changes ->
                        _uiState.value = _uiState.value.copy(
                            statusChanges = changes
                        )
                    }
                }
        }
    }
    
    /**
     * Load actions for a specific host
     */
    fun loadActionsForHost(host: String) {
        viewModelScope.launch {
            getTopActionsByHostUseCase(host)
                .collect { result ->
                    result.onSuccess { actions ->
                        _uiState.value = _uiState.value.copy(
                            selectedHostActions = actions,
                            selectedHost = host
                        )
                    }
                }
        }
    }
    
    /**
     * Generate a URI history report
     */
    fun generateReport(exportToFile: Boolean = false, filePath: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingReport = true)
            
            val result = generateHistoryReportUseCase(
                timeRange = _uiState.value.selectedDateRange,
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
        loadUriTrends()
        loadStatusChanges()
    }
    
    /**
     * Clear selected date range
     */
    fun clearDateRange() {
        _uiState.value = _uiState.value.copy(
            selectedDateRange = null
        )
        loadUriTrends()
        loadStatusChanges()
    }
}

/**
 * UI state for the URI Analytics screen
 */
data class UriAnalyticsUiState(
    val topHosts: List<GroupCount> = emptyList(),
    val uriTrends: Map<String, List<DateCount>> = emptyMap(),
    val statusChanges: Map<String, List<DateCount>> = emptyMap(),
    val selectedHost: String? = null,
    val selectedHostActions: List<GroupCount> = emptyList(),
    val selectedDateRange: Pair<Instant, Instant>? = null,
    val report: Any? = null,
    val reportGenerated: Boolean = false,
    val isGeneratingReport: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
) 