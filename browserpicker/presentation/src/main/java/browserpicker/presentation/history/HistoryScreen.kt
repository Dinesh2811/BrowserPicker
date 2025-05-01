package browserpicker.presentation.history

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import browserpicker.domain.model.query.*
import browserpicker.domain.model.GroupCount
import browserpicker.domain.model.* // Import all needed domain models
import browserpicker.presentation.common.* // Import common UI types
import kotlinx.datetime.Instant

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter") // Padding applied to content Column
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
    // Add navigation callbacks if needed, e.g., onRuleClick: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val historyItems: LazyPagingItems<UriRecord> = viewModel.pagedHistory.collectAsLazyPagingItems()

    // State to control Bottom Sheet visibility
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showGroupSheet by remember { mutableStateOf(false) }

    // State for Search input field
    var searchInput by remember { mutableStateOf(state.query.searchQuery ?: "") }
    LaunchedEffect(state.query.searchQuery) {
        // Keep internal search input in sync with ViewModel state when it changes externally
        searchInput = state.query.searchQuery ?: ""
    }

    // Snackbars are handled by the MainScreen Scaffold using the MainViewModel's messages.
    // We collect messages from HistoryViewModel's state and rely on MainScreen
    // observing this state to show them.

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History (${state.overview.totalCount})") },
                actions = {
                    // Search Action
                    IconButton(onClick = { /* Toggle search field visibility or open search dialog */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search History")
                    }
                    // Filter Action
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter History")
                    }
                    // Sort Action
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort History")
                    }
                    // Group Action
                    IconButton(onClick = { showGroupSheet = true }) {
                        Icon(Icons.Default.Group, contentDescription = "Group History")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .padding(paddingValues) // Apply Scaffold padding
            .fillMaxSize()
        ) {
            // Search Input Field (optional, could be a dialog)
            OutlinedTextField(
                value = searchInput,
                onValueChange = {
                    searchInput = it
                    // Apply search filter as user types (debounced in VM if needed)
                    viewModel.setSearchQuery(it)
                },
                label = { Text("Search") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchInput.isNotEmpty()) {
                        IconButton(onClick = {
                            searchInput = ""
                            viewModel.setSearchQuery(null)
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Display Overview stats if needed (can be optional)
            HistoryOverviewSection(state.overview)


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
                                Button(onClick = { historyItems.retry() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                    else -> { // NotLoading
                        if (historyItems.itemCount == 0 && historyItems.loadState.append.endOfPaginationReached) {
                            item {
                                Box(modifier = Modifier.fillParentMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("No history records found matching the current filters.")
                                }
                            }
                        }
                    }
                }

                // Display actual items or group headers/items
                if (state.query.groupBy != UriRecordGroupField.NONE) {
                    // If grouping is active, we might need a different structure
                    // Paging3 doesn't natively support grouping headers with `items()`.
                    // A custom PagingSource or transforming the data might be needed for true grouping.
                    // For a simple display, we could just list items sorted by group field.
                    // TODO: Implement proper grouping UI (headers + items)
                    items(
                        count = historyItems.itemCount,
                        key = historyItems.itemKey { it.id },
                        contentType = historyItems.itemContentType { "UriRecord" }
                    ) { index ->
                        val item = historyItems[index]
                        if (item != null) {
                            // Display group header logic here if needed, based on item's group key
                            // This is complex without a custom PagingSource that emits headers
                            HistoryItem(
                                record = item,
                                onDeleteClick = { viewModel.deleteHistoryRecord(item.id) }
                            )
                            Divider()
                        }
                    }
                } else {
                    // Simple list when no grouping
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
                            )
                            Divider()
                        } else {
                            // Placeholder for loading item
                            Spacer(modifier = Modifier.height(50.dp).fillMaxWidth()) // Basic placeholder
                        }
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
                                Button(onClick = { historyItems.retry() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                    else -> { /* NotLoading */ }
                }
            }
        }
    }

    // --- Filter/Sort/Group Bottom Sheets ---

    if (showFilterSheet) {
        HistoryFilterSheet(
            currentQuery = state.query,
            filterOptions = state.filterOptions,
            filterOptionsLoading = state.filterOptionsLoading,
            onApplyFilters = { sources, actions, browsers, hosts, dateRange ->
                viewModel.setSourceFilter(sources.takeIf { it.isNotEmpty() })
                viewModel.setInteractionFilter(actions.takeIf { it.isNotEmpty() })
                viewModel.setBrowserFilter(browsers.takeIf { it.isNotEmpty() })
                viewModel.setHostFilter(hosts.takeIf { it.isNotEmpty() })
                viewModel.setDateRangeFilter(dateRange)
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showSortSheet) {
        HistorySortSheet(
            currentSortField = state.query.sortBy,
            currentSortOrder = state.query.sortOrder,
            onApplySort = { field, order ->
                viewModel.setSort(field, order)
                showSortSheet = false
            },
            onDismiss = { showSortSheet = false }
        )
    }

    if (showGroupSheet) {
        HistoryGroupSheet(
            currentGroupField = state.query.groupBy,
            currentGroupSortOrder = state.query.groupSortOrder,
            groupCounts = state.overview.groupCounts, // Show counts in the sheet
            onApplyGroup = { field, order ->
                viewModel.setGroup(field, order)
                showGroupSheet = false
            },
            onDismiss = { showGroupSheet = false }
        )
    }

    // Messages from HistoryViewModel collected in MainScreen and shown in global Snackbar
    // LaunchedEffect(state.userMessages) {
    //     state.userMessages.firstOrNull()?.let { message ->
    //         snackbarHostState.showSnackbar(message.message, duration = SnackbarDuration.Short)
    //         viewModel.clearMessage(message.id)
    //     }
    // }
}

// --- Individual Composable for a History Item ---

@Composable
fun HistoryItem(
    record: UriRecord,
    onDeleteClick: () -> Unit,
    // onClick: (UriRecord) -> Unit = {}, // Optional: Open detail or re-open URI
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // .clickable { onClick(record) } // Enable click action
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(record.host, style = MaterialTheme.typography.titleMedium)
            Text(record.uriString, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text("Browser: ${record.chosenBrowserPackage ?: "N/A"} | Action: ${record.interactionAction.name} | Source: ${record.uriSource.name}", style = MaterialTheme.typography.bodySmall)
            // Format timestamp nicely
            Text(record.timestamp.toString(), style = MaterialTheme.typography.bodySmall) // TODO: Format timestamp
        }
        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Record")
        }
    }
}

// --- Composable for displaying Overview Stats (Optional section in the UI) ---

@Composable
fun HistoryOverviewSection(
    overview: HistoryOverviewState,
    modifier: Modifier = Modifier
) {
    if (overview.loadingStatus == LoadingStatus.LOADING) {
        // Show a small loading indicator
        LinearProgressIndicator(modifier = modifier.fillMaxWidth())
    } else if (overview.loadingStatus == LoadingStatus.ERROR) {
        // Show error state if overview failed
        Text("Error loading overview", color = MaterialTheme.colorScheme.error, modifier = modifier.padding(16.dp))
    } else {
        Column(modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Total Records: ${overview.totalCount}", style = MaterialTheme.typography.bodyMedium)

            // Optional: Display group counts if grouping is active
            if (overview.activeGrouping != UriRecordGroupField.NONE && overview.groupCounts.isNotEmpty()) {
                Text("Group Counts (${overview.activeGrouping.name}):", style = MaterialTheme.typography.bodyMedium)
                overview.groupCounts.take(5).forEach { group -> // Show top N groups
                    Text("${group.groupValue ?: "N/A"}: ${group.count}", style = MaterialTheme.typography.bodySmall)
                }
                if (overview.groupCounts.size > 5) {
                    Text("...", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}


// --- Filter Bottom Sheet ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFilterSheet(
    currentQuery: UriHistoryQuery,
    filterOptions: FilterOptions?,
    filterOptionsLoading: LoadingStatus,
    onApplyFilters: (
        sources: Set<UriSource>,
        actions: Set<InteractionAction>,
        browsers: Set<String?>,
        hosts: Set<String>,
        dateRange: Pair<Instant, Instant>?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    // Local state for selections
    var selectedSources by remember { mutableStateOf(currentQuery.filterByUriSource ?: emptySet()) }
    var selectedActions by remember { mutableStateOf(currentQuery.filterByInteractionAction ?: emptySet()) }
    var selectedBrowsers by remember { mutableStateOf(currentQuery.filterByChosenBrowser ?: emptySet()) }
    var selectedHosts by remember { mutableStateOf(currentQuery.filterByHost ?: emptySet()) }
    var selectedDateRange by remember { mutableStateOf(currentQuery.filterByDateRange) }

    // Remember options once loaded
    var distinctHosts by remember { mutableStateOf(filterOptions?.distinctHistoryHosts ?: emptyList()) }
    var distinctBrowsers by remember { mutableStateOf(filterOptions?.distinctChosenBrowsers ?: emptyList()) }

    // Update options when loaded from ViewModel
    LaunchedEffect(filterOptions) {
        filterOptions?.let {
            distinctHosts = it.distinctHistoryHosts
            distinctBrowsers = it.distinctChosenBrowsers
        }
    }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Filter History", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

            // --- Filter by Source ---
            Text("Source:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            FlowRow(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                UriSource.entries.forEach { source ->
                    FilterChip(
                        selected = selectedSources.contains(source),
                        onClick = {
                            selectedSources = if (selectedSources.contains(source)) {
                                selectedSources - source
                            } else {
                                selectedSources + source
                            }
                        },
                        label = { Text(source.name) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // --- Filter by Interaction Action ---
            Text("Action:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            FlowRow(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                // Filter out UNKNOWN and internal actions for user selection
                InteractionAction.entries
                    .filter { it != InteractionAction.UNKNOWN && it != InteractionAction.BLOCKED_URI_ENFORCED && it != InteractionAction.OPENED_BY_PREFERENCE }
                    .forEach { action ->
                        FilterChip(
                            selected = selectedActions.contains(action),
                            onClick = {
                                selectedActions = if (selectedActions.contains(action)) {
                                    selectedActions - action
                                } else {
                                    selectedActions + action
                                }
                            },
                            label = { Text(action.name) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
            }

            // --- Filter by Browser ---
            Text("Browser:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            // TODO: Implement a scrollable dropdown/dialog for potentially long lists
            if (filterOptionsLoading == LoadingStatus.LOADING) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
            } else if (filterOptionsLoading == LoadingStatus.ERROR) {
                Text("Error loading browsers.", color = MaterialTheme.colorScheme.error)
            } else if (distinctBrowsers.isEmpty()) {
                Text("No browser data available for filtering.")
            } else {
                // Simple example: list top N or use a picker dialog
                distinctBrowsers.take(10).forEach { browser -> // Show top few unique browsers
                    FilterChip(
                        selected = selectedBrowsers.contains(browser),
                        onClick = {
                            selectedBrowsers = if (selectedBrowsers.contains(browser)) {
                                selectedBrowsers - browser
                            } else {
                                selectedBrowsers + browser
                            }
                        },
                        label = { Text(browser ?: "Unknown/None") },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                if (distinctBrowsers.size > 10) {
                    Text("...", style = MaterialTheme.typography.bodySmall)
                }
            }


            // --- Filter by Host ---
            Text("Host:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            // TODO: Implement a scrollable dropdown/dialog for potentially long lists
            if (filterOptionsLoading == LoadingStatus.LOADING) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
            } else if (filterOptionsLoading == LoadingStatus.ERROR) {
                Text("Error loading hosts.", color = MaterialTheme.colorScheme.error)
            } else if (distinctHosts.isEmpty()) {
                Text("No host data available for filtering.")
            } else {
                // Simple example: list top N or use a picker dialog
                distinctHosts.take(10).forEach { host -> // Show top few unique hosts
                    FilterChip(
                        selected = selectedHosts.contains(host),
                        onClick = {
                            selectedHosts = if (selectedHosts.contains(host)) {
                                selectedHosts - host
                            } else {
                                selectedHosts + host
                            }
                        },
                        label = { Text(host) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                if (distinctHosts.size > 10) {
                    Text("...", style = MaterialTheme.typography.bodySmall)
                }
            }

            // --- Filter by Date Range ---
            Text("Date Range:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            // TODO: Implement a Date Range Picker UI
            Text("Date Range Picker Placeholder", modifier = Modifier.padding(bottom = 16.dp)) // Placeholder

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    // Clear all filters
                    selectedSources = emptySet()
                    selectedActions = emptySet()
                    selectedBrowsers = emptySet()
                    selectedHosts = emptySet()
                    selectedDateRange = null
                }) {
                    Text("Clear Filters")
                }
                Button(onClick = {
                    onApplyFilters(
                        selectedSources,
                        selectedActions,
                        selectedBrowsers,
                        selectedHosts,
                        selectedDateRange
                    )
                    // No need to hide sheet here, onApplyFilters callback should handle it
                }) {
                    Text("Apply Filters")
                }
            }

            Spacer(modifier = Modifier.navigationBarsPadding()) // Account for nav bars
        }
    }
}


// --- Sort Bottom Sheet ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySortSheet(
    currentSortField: UriRecordSortField,
    currentSortOrder: SortOrder,
    onApplySort: (field: UriRecordSortField, order: SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedField by remember { mutableStateOf(currentSortField) }
    var selectedOrder by remember { mutableStateOf(currentSortOrder) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sort History By", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

            // --- Sort Field Selection ---
            Text("Sort Field:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            FlowRow(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                UriRecordSortField.entries.forEach { field ->
                    FilterChip(
                        selected = selectedField == field,
                        onClick = { selectedField = field },
                        label = { Text(field.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }, // Format enum name
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // --- Sort Order Selection ---
            Text("Sort Order:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                SortOrder.entries.forEach { order ->
                    ChoiceChip( // Use ChoiceChip for mutually exclusive selection
                        selected = selectedOrder == order,
                        onClick = { selectedOrder = order },
                        label = { Text(order.name) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(onClick = { onApplySort(selectedField, selectedOrder) }) {
                    Text("Apply Sort")
                }
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

// Custom composable for a ChoiceChip (Material 3 doesn't have one directly, often built from SelectableChip)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoiceChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use FilterChip visually, but implies single selection in logic
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else {
            null
        }
    )
}

// --- Group Bottom Sheet ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryGroupSheet(
    currentGroupField: UriRecordGroupField,
    currentGroupSortOrder: SortOrder,
    groupCounts: List<GroupCount>, // Display counts here
    onApplyGroup: (field: UriRecordGroupField, order: SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedField by remember { mutableStateOf(currentGroupField) }
    var selectedOrder by remember { mutableStateOf(currentGroupSortOrder) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Group History By", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

            // --- Group Field Selection ---
            Text("Group Field:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
            FlowRow(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                UriRecordGroupField.entries
                    .filter { it != UriRecordGroupField.NONE } // NONE is default, not a grouping option here
                    .forEach { field ->
                        ChoiceChip(
                            selected = selectedField == field,
                            onClick = { selectedField = field },
                            label = { Text(field.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }, // Format enum name
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
            }

            // --- Group Sort Order Selection ---
            if (selectedField != UriRecordGroupField.NONE) {
                Text("Group Order:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    SortOrder.entries.forEach { order ->
                        ChoiceChip(
                            selected = selectedOrder == order,
                            onClick = { selectedOrder = order },
                            label = { Text(order.name) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                // Optional: Display Group Counts within the sheet
                if (groupCounts.isNotEmpty()) {
                    Text("Counts:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    Column(modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp).clickable { /* Optional: Click to select group */ }) {
                        // Display actual counts for the selected group field if available
                        LazyColumn { // Use LazyColumn for potentially many groups
                            items(groupCounts) { group ->
                                // TODO: Format date group value if needed
                                Text("${group.groupValue ?: "N/A"}: ${group.count}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                } else {
                    Text("Loading counts...", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    // Clear grouping
                    selectedField = UriRecordGroupField.NONE
                    selectedOrder = SortOrder.ASC // Default
                }) {
                    Text("Clear Grouping")
                }
                Button(onClick = { onApplyGroup(selectedField, selectedOrder) }) {
                    Text("Apply Group")
                }
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}
