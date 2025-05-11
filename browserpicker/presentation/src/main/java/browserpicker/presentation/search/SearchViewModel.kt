package browserpicker.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriStatus
import browserpicker.domain.usecases.analytics.SearchFoldersUseCase
import browserpicker.domain.usecases.analytics.SearchHostRulesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Search screen.
 * 
 * This ViewModel handles:
 * - Global search across host rules and folders
 * - Filtering search results by type
 * - Navigating to search results
 * 
 * Used by: SearchScreen
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchHostRulesUseCase: SearchHostRulesUseCase,
    private val searchFoldersUseCase: SearchFoldersUseCase
) : ViewModel() {

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Selected filters
    private val _selectedFilters = MutableStateFlow(SearchFilters())
    val selectedFilters: StateFlow<SearchFilters> = _selectedFilters.asStateFlow()
    
    // UI state
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    // Folders search results
    val folderResults: Flow<List<Folder>> = combine(
        searchQuery,
        selectedFilters
    ) { query, filters ->
        Pair(query, filters)
    }.filter { (query, filters) ->
        query.isNotBlank() && filters.includeFolders
    }.flatMapLatest { (query, filters) ->
        searchFoldersUseCase(
            query = query,
            folderType = when {
                filters.includeBookmarkFolders && !filters.includeBlockedFolders -> FolderType.BOOKMARK
                !filters.includeBookmarkFolders && filters.includeBlockedFolders -> FolderType.BLOCK
                else -> null
            }
        ).map { result -> result.getOrNull() ?: emptyList() }
    }
    
    // Host rules search results
    val hostRuleResults: Flow<PagingData<HostRule>> = combine(
        searchQuery,
        selectedFilters
    ) { query, filters ->
        Pair(query, filters)
    }.filter { (query, filters) ->
        query.isNotBlank() && filters.includeHostRules
    }.flatMapLatest { (query, filters) ->
        searchHostRulesUseCase(
            query = query,
            filterByStatus = when {
                filters.includeBookmarks && !filters.includeBlocked -> UriStatus.BOOKMARKED
                !filters.includeBookmarks && filters.includeBlocked -> UriStatus.BLOCKED
                else -> null
            }
        )
    }.cachedIn(viewModelScope)
    
    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Update search filters
     */
    fun updateFilters(filters: SearchFilters) {
        _selectedFilters.value = filters
    }
    
    /**
     * Toggle a specific filter
     */
    fun toggleFilter(filter: SearchFilterType) {
        val currentFilters = _selectedFilters.value
        
        _selectedFilters.value = when (filter) {
            SearchFilterType.HOST_RULES -> currentFilters.copy(
                includeHostRules = !currentFilters.includeHostRules
            )
            SearchFilterType.FOLDERS -> currentFilters.copy(
                includeFolders = !currentFilters.includeFolders
            )
            SearchFilterType.BOOKMARKS -> currentFilters.copy(
                includeBookmarks = !currentFilters.includeBookmarks
            )
            SearchFilterType.BLOCKED -> currentFilters.copy(
                includeBlocked = !currentFilters.includeBlocked
            )
            SearchFilterType.BOOKMARK_FOLDERS -> currentFilters.copy(
                includeBookmarkFolders = !currentFilters.includeBookmarkFolders
            )
            SearchFilterType.BLOCKED_FOLDERS -> currentFilters.copy(
                includeBlockedFolders = !currentFilters.includeBlockedFolders
            )
        }
    }
    
    /**
     * Clear all search filters
     */
    fun clearFilters() {
        _selectedFilters.value = SearchFilters()
    }
}

/**
 * Search filter types
 */
enum class SearchFilterType {
    HOST_RULES,
    FOLDERS,
    BOOKMARKS,
    BLOCKED,
    BOOKMARK_FOLDERS,
    BLOCKED_FOLDERS
}

/**
 * Search filters state
 */
data class SearchFilters(
    val includeHostRules: Boolean = true,
    val includeFolders: Boolean = true,
    val includeBookmarks: Boolean = true,
    val includeBlocked: Boolean = true,
    val includeBookmarkFolders: Boolean = true,
    val includeBlockedFolders: Boolean = true
)

/**
 * UI state for the Search screen
 */
data class SearchUiState(
    val isLoading: Boolean = false,
    val error: String? = null
) 