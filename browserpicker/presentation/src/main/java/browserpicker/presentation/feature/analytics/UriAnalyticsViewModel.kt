package browserpicker.presentation.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import browserpicker.core.results.DomainResult
import browserpicker.domain.di.SearchAndAnalyticsUseCases
import browserpicker.domain.model.DateCount
import browserpicker.domain.model.GroupCount
import browserpicker.domain.usecases.analytics.UriHistoryReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import javax.inject.Inject

@HiltViewModel
class UriAnalyticsViewModel @Inject constructor(
    private val searchAndAnalyticsUseCases: SearchAndAnalyticsUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(UriAnalyticsUiState())
    val uiState: StateFlow<UriAnalyticsUiState> = _uiState.asStateFlow()

    private val _mostVisitedHosts = MutableStateFlow<List<GroupCount>>(emptyList())
    val mostVisitedHosts: StateFlow<List<GroupCount>> = _mostVisitedHosts.asStateFlow()

    private val _uriTrends = MutableStateFlow<Map<String, List<DateCount>>>(emptyMap())
    val uriTrends: StateFlow<Map<String, List<DateCount>>> = _uriTrends.asStateFlow()

    private val _timeRange = MutableStateFlow<Pair<Instant, Instant>?>(null)
    val timeRange: StateFlow<Pair<Instant, Instant>?> = _timeRange.asStateFlow()

    private val _hostForTopActions = MutableStateFlow<String?>(null)
    val hostForTopActions: StateFlow<String?> = _hostForTopActions.asStateFlow()

    private val _topActionsByHost = MutableStateFlow<List<GroupCount>>(emptyList())
    val topActionsByHost: StateFlow<List<GroupCount>> = _topActionsByHost.asStateFlow()

    init {
        loadMostVisitedHosts()
        loadUriTrends()
    }

    private fun loadMostVisitedHosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            _timeRange
                .flatMapLatest { range ->
                    searchAndAnalyticsUseCases.getMostVisitedHostsUseCase(
                        limit = 10,
                        timeRange = range
                    )
                }
                .collect { result ->
                    when (result) {
                        is DomainResult.Success -> {
                            _mostVisitedHosts.value = result.data
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

    private fun loadUriTrends() {
        viewModelScope.launch {
            _timeRange
                .flatMapLatest { range ->
                    searchAndAnalyticsUseCases.analyzeUriTrendsUseCase(range)
                }
                .collect { result ->
                    when (result) {
                        is DomainResult.Success -> {
                            _uriTrends.value = result.data
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

    fun loadTopActionsForHost(host: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            _hostForTopActions.value = host
            
            searchAndAnalyticsUseCases.getTopActionsByHostUseCase(host)
                .collect { result ->
                    when (result) {
                        is DomainResult.Success -> {
                            _topActionsByHost.value = result.data
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

    fun generateUriHistoryReport(exportToFile: Boolean = false, filePath: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, reportGenerating = true) }
            
            when (val result = searchAndAnalyticsUseCases.generateHistoryReportUseCase(
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetReportState() {
        _uiState.update { it.copy(
            reportGenerated = false,
            report = null
        )}
    }
}

data class UriAnalyticsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val reportGenerating: Boolean = false,
    val reportGenerated: Boolean = false,
    val report: UriHistoryReport? = null
) 