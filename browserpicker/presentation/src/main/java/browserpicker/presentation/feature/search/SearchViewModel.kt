package browserpicker.presentation.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import browserpicker.core.results.DomainResult
import browserpicker.domain.di.SearchAndAnalyticsUseCases
import browserpicker.domain.di.UriHandlingUseCases
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriStatus
import browserpicker.domain.service.ParsedUri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import browserpicker.domain.model.*
import browserpicker.domain.model.query.*

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchAndAnalyticsUseCases: SearchAndAnalyticsUseCases,
    private val uriHandlingUseCases: UriHandlingUseCases,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeSearchType = MutableStateFlow(SearchType.ALL)
    val activeSearchType: StateFlow<SearchType> = _activeSearchType.asStateFlow()

    private val _searchResults = MutableStateFlow<SearchResults>(SearchResults())
    val searchResults: StateFlow<SearchResults> = _searchResults.asStateFlow()

    private val _hostRuleStatusFilter = MutableStateFlow<UriStatus?>(null)
    val hostRuleStatusFilter: StateFlow<UriStatus?> = _hostRuleStatusFilter.asStateFlow()

    private val _folderTypeFilter = MutableStateFlow<FolderType?>(null)
    val folderTypeFilter: StateFlow<FolderType?> = _folderTypeFilter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedHostRules: Flow<PagingData<HostRule>> = 
        combine(_searchQuery, _hostRuleStatusFilter) { query, status ->
            Pair(query, status)
        }
        .flatMapLatest { (query, status) ->
            searchAndAnalyticsUseCases.searchHostRulesUseCase(
                query = query,
                filterByStatus = status,
                includeFolderId = null
            )
        }
        .cachedIn(viewModelScope)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotEmpty()) {
            performSearch()
        } else {
            clearSearchResults()
        }
    }

    fun setSearchType(type: SearchType) {
        _activeSearchType.value = type
        if (_searchQuery.value.isNotEmpty()) {
            performSearch()
        }
    }

    fun setHostRuleStatusFilter(status: UriStatus?) {
        _hostRuleStatusFilter.value = status
    }

    fun setFolderTypeFilter(type: FolderType?) {
        _folderTypeFilter.value = type
    }

    private fun performSearch() {
        viewModelScope.launch {
            val query = _searchQuery.value
            if (query.isEmpty()) {
                clearSearchResults()
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            when (_activeSearchType.value) {
                SearchType.ALL -> {
                    searchUris(query)
                    searchFolders(query)
                    // Host rules are handled by the paged flow
                }
                SearchType.URIS -> searchUris(query)
                SearchType.HOST_RULES -> {} // Handled by the paged flow
                SearchType.FOLDERS -> searchFolders(query)
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun searchUris(query: String) {
//        searchAndAnalyticsUseCases.searchUrisUseCase(query)
        uriHandlingUseCases.searchUrisUseCase(query)
            .collect { result ->
                when (result) {
                    is DomainResult.Success -> {
                        _searchResults.update { it.copy(uris = result.data) }
                    }
                    is DomainResult.Failure -> {
                        _uiState.update { it.copy(error = result.error.message) }
                    }
                }
            }
    }

    private suspend fun searchFolders(query: String) {
        searchAndAnalyticsUseCases.searchFoldersUseCase(query, _folderTypeFilter.value)
            .collect { result ->
                when (result) {
                    is DomainResult.Success -> {
                        _searchResults.update { it.copy(folders = result.data) }
                    }
                    is DomainResult.Failure -> {
                        _uiState.update { it.copy(error = result.error.message) }
                    }
                }
            }
    }

    private fun clearSearchResults() {
        _searchResults.value = SearchResults()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

enum class SearchType {
    ALL, URIS, HOST_RULES, FOLDERS
}

data class SearchResults(
    val uris: List<ParsedUri> = emptyList(),
    val folders: List<Folder> = emptyList()
)

data class SearchUiState(
    val isLoading: Boolean = false,
    val error: String? = null
) 