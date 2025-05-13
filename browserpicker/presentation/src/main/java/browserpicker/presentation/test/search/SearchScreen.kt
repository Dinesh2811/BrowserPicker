package browserpicker.presentation.test.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import browserpicker.domain.model.Folder
import browserpicker.domain.model.HostRule
import browserpicker.presentation.test.common.components.EmptyStateView
import browserpicker.presentation.test.navigation.FolderDetailsRoute

/**
 * Search Screen - Global search across the app.
 * 
 * This screen provides:
 * - A search bar for global search
 * - Filter options to narrow down results
 * - Display of search results from different data types
 * - Navigation to detailed views of search results
 * 
 * It allows users to quickly find specific hosts, bookmarks,
 * blocked sites, or folders across the app.
 * 
 * Uses: SearchViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: androidx.navigation.NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilters by viewModel.selectedFilters.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    val folderResults by viewModel.folderResults.collectAsState(initial = emptyList())
    val hostRuleResults = viewModel.hostRuleResults.collectAsLazyPagingItems()
    
    var showFilterDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Filter")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search bar
            SearchBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onSearch = { /* Nothing to do, search is automatic */ },
                active = false,
                onActiveChange = { /* Nothing to do */ },
                placeholder = { Text("Search for hosts, folders, bookmarks...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            ) {
                // This content only shows when the search bar is active/expanded
                // We don't need this for our implementation
            }
            
            // Filter chips display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!selectedFilters.includeHostRules) {
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.toggleFilter(SearchFilterType.HOST_RULES) },
                        label = { Text("No Hosts") }
                    )
                }
                
                if (!selectedFilters.includeFolders) {
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.toggleFilter(SearchFilterType.FOLDERS) },
                        label = { Text("No Folders") }
                    )
                }
                
                if (!selectedFilters.includeBookmarks) {
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.toggleFilter(SearchFilterType.BOOKMARKS) },
                        label = { Text("No Bookmarks") }
                    )
                }
                
                if (!selectedFilters.includeBlocked) {
                    FilterChip(
                        selected = true,
                        onClick = { viewModel.toggleFilter(SearchFilterType.BLOCKED) },
                        label = { Text("No Blocked") }
                    )
                }
                
                if (selectedFilters != SearchFilters()) {
                    Button(
                        onClick = { viewModel.clearFilters() },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Clear Filters")
                    }
                }
            }
            
            // Search results
            if (searchQuery.isBlank()) {
                // Instructions when no search query
                EmptyStateView(message = "Enter a search term to find hosts, folders, and more")
            } else if (folderResults.isEmpty() && hostRuleResults.itemCount == 0) {
                // No results found
                EmptyStateView(message = "No results found for '$searchQuery'")
            } else {
                // Results display
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Folder results section
                    if (folderResults.isNotEmpty()) {
                        item {
                            Text(
                                "Folders",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        items(folderResults) { folder ->
                            FolderResultItem(
                                folder = folder,
                                onClick = {
                                    // Navigate to folder details using type-safe route
                                    navController.navigate(FolderDetailsRoute(folderId = folder.id, folderType = folder.type.value))
                                }
                            )
                        }
                        
                        item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
                    }
                    
                    // Host rule results
                    if (hostRuleResults.itemCount > 0) {
                        item {
                            Text(
                                "Host Rules",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        
                        // This would be implemented with a proper paging items display
                        // For now, we show a placeholder message
                        item {
                            Text(
                                "Host rule results would be displayed here",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Filter dialog
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filter Results") },
            text = {
                Column {
                    // Host rules filter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedFilters.includeHostRules,
                            onCheckedChange = { 
                                viewModel.toggleFilter(SearchFilterType.HOST_RULES)
                            }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text("Include Host Rules")
                    }
                    
                    // Folders filter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedFilters.includeFolders,
                            onCheckedChange = {
                                viewModel.toggleFilter(SearchFilterType.FOLDERS)
                            }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text("Include Folders")
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Host rule types
                    Text(
                        "Host Rule Types",
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    // Bookmarks filter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedFilters.includeBookmarks,
                            onCheckedChange = { 
                                viewModel.toggleFilter(SearchFilterType.BOOKMARKS)
                            }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text("Include Bookmarks")
                    }
                    
                    // Blocked filter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedFilters.includeBlocked,
                            onCheckedChange = { 
                                viewModel.toggleFilter(SearchFilterType.BLOCKED)
                            }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text("Include Blocked")
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Folder types
                    Text(
                        "Folder Types",
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    // Bookmark folders filter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedFilters.includeBookmarkFolders,
                            onCheckedChange = { 
                                viewModel.toggleFilter(SearchFilterType.BOOKMARK_FOLDERS)
                            }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text("Include Bookmark Folders")
                    }
                    
                    // Blocked folders filter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedFilters.includeBlockedFolders,
                            onCheckedChange = { 
                                viewModel.toggleFilter(SearchFilterType.BLOCKED_FOLDERS)
                            }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text("Include Blocked URL Folders")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showFilterDialog = false }
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        viewModel.clearFilters()
                        showFilterDialog = false
                    }
                ) {
                    Text("Reset")
                }
            }
        )
    }
}

/**
 * Folder result item in search results
 */
@Composable
private fun FolderResultItem(
    folder: Folder,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = "Folder",
            modifier = Modifier.padding(end = 16.dp)
        )
        
        Column {
            Text(
                folder.name,
                style = MaterialTheme.typography.bodyLarge
            )
            
            Text(
                "Folder Type: ${folder.type.name}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Host rule result item in search results
 */
@Composable
private fun HostRuleResultItem(
    hostRule: HostRule,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon based on status would go here
        
        Column {
            Text(
                hostRule.host,
                style = MaterialTheme.typography.bodyLarge
            )
            
            Text(
                "Status: ${hostRule.uriStatus.name}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
} 