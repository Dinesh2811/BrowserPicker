package browserpicker.presentation.history

// package browserpicker.presentation.history

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import browserpicker.domain.model.UriRecord // Import domain model

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
    // Add navigation callbacks: onRuleClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Collect PagingData using collectAsLazyPagingItems
    val historyItems: LazyPagingItems<UriRecord> = viewModel.pagedHistory.collectAsLazyPagingItems()

    // Snackbar Host State for screen-specific messages (if needed, or rely on MainScreen's)
    // val snackbarHostState = remember { SnackbarHostState() }
    // LaunchedEffect(state.userMessages) { ... }

    Scaffold(
        // Can have a TopAppBar specific to this screen
        topBar = {
            TopAppBar(title = { Text("History (${state.overview.totalCount})") } /* Add filter/sort actions */)
        }
        // Use scaffold's snackbar host if not using MainScreen's global one
        // snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            // TODO: Add Filter/Sort/Group controls here, interacting with viewModel

            // Display Paging List
            LazyColumn(modifier = Modifier.weight(1f)) {
                // Handle Paging LoadStates (Loading, Error, Empty)
                when (val refreshState = historyItems.loadState.refresh) {
                    is LoadState.Loading -> {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is LoadState.Error -> {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("Error loading history: ${refreshState.error.localizedMessage}")
                                // TODO: Add retry button
                            }
                        }
                    }
                    else -> { // NotLoading
                        if (historyItems.itemCount == 0 && historyItems.loadState.append.endOfPaginationReached) {
                            item {
                                Box(modifier = Modifier.fillParentMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("No history records found.")
                                }
                            }
                        }
                    }
                }

                // Display actual items
                items(
                    count = historyItems.itemCount,
                    key = historyItems.itemKey { it.id },
                    contentType = historyItems.itemContentType { "UriRecord" }
                ) { index ->
                    val item = historyItems[index]
                    if (item != null) {
                        HistoryItem(
                            record = item,
                            onDeleteClick = { viewModel.deleteHistoryRecord(item.id) }
                            // Add onClick, onLongClick, etc.
                        )
                        Divider()
                    } else {
                        // Placeholder for loading item
                        Spacer(modifier = Modifier.height(50.dp).fillMaxWidth()) // Basic placeholder
                    }
                }

                // Handle Append LoadState (Loading spinner at bottom, error indicator)
                when (val appendState = historyItems.loadState.append) {
                    is LoadState.Loading -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    is LoadState.Error -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("Error loading more: ${appendState.error.localizedMessage}")
                                // TODO: Add retry button
                            }
                        }
                    }
                    else -> { /* NotLoading */ }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    record: UriRecord,
    onDeleteClick: () -> Unit
    // onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(record.host, style = MaterialTheme.typography.titleMedium)
            Text(record.uriString, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text("Browser: ${record.chosenBrowserPackage ?: "N/A"} | Action: ${record.interactionAction.name}", style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Record")
        }
    }
}

// --- Create similar stub screens for RulesScreen, FoldersScreen, StatsScreen, SettingsScreen ---