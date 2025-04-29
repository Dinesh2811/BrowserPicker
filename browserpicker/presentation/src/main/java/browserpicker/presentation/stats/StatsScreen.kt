package browserpicker.presentation.stats


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import browserpicker.domain.model.BrowserUsageStat // Import Domain model
import browserpicker.domain.model.query.BrowserStatSortField
import browserpicker.domain.service.DomainError
import browserpicker.domain.usecase.stats.ClearBrowserStatsUseCase
import browserpicker.domain.usecase.stats.GetBrowserStatsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant // Import kotlinx.datetime
import java.util.Locale // For formatting numbers/dates

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    // Collect the UI state from the ViewModel
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Snackbar Host State for local screen messages
    val snackbarHostState = remember { SnackbarHostState() }

    // Effect to show Snackbar messages from the ViewModel
    LaunchedEffect(state.userMessages) {
        state.userMessages.firstOrNull()?.let { userMessage ->
            snackbarHostState.showSnackbar(
                message = userMessage.message,
                duration = SnackbarDuration.Short
            )
            // Consume the message after showing
            viewModel.clearMessage(userMessage.id)
        }
    }

    Scaffold(
        topBar = {
            StatsTopAppBar(
                currentSortField = state.sortField,
                onSortFieldSelected = viewModel::setSortField,
                onClearStatsClick = viewModel::clearStats,
                isLoading = state.isLoading // Indicate loading in the app bar?
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) } // Attach SnackbarHost
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (state.isLoading && state.stats.isEmpty()) {
                // Show initial loading indicator if no data is loaded yet
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.stats.isEmpty() && state.isLoading == false) {
                // Show empty state message
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "No browser usage statistics recorded yet.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                // Display the list of stats
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp) // Add some vertical padding
                ) {
                    items(
                        items = state.stats,
                        key = { it.browserPackageName } // Use package name as a stable key
                    ) { stat ->
                        BrowserStatItem(stat = stat)
                        Divider() // Add a divider between items
                    }
                    // Optionally show a loading indicator at the bottom if appending data (though not using Paging here)
                    if (state.isLoading && state.stats.isNotEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsTopAppBar(
    currentSortField: BrowserStatSortField,
    onSortFieldSelected: (BrowserStatSortField) -> Unit,
    onClearStatsClick: () -> Unit,
    isLoading: Boolean
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showClearConfirmationDialog by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("Browser Stats") },
        actions = {
            // Clear Stats Action
            IconButton(onClick = { showClearConfirmationDialog = true }, enabled = !isLoading) {
                Icon(Icons.Default.Delete, contentDescription = "Clear Stats")
            }

            // Sort Action
            IconButton(onClick = { showSortMenu = true }, enabled = !isLoading) {
                Icon(Icons.Default.Sort, contentDescription = "Sort Stats")
            }
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Sort by Usage Count") },
                    onClick = {
                        onSortFieldSelected(BrowserStatSortField.USAGE_COUNT)
                        showSortMenu = false
                    },
                    leadingIcon = {
                        if (currentSortField == BrowserStatSortField.USAGE_COUNT) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sort by Last Used") },
                    onClick = {
                        onSortFieldSelected(BrowserStatSortField.LAST_USED_TIMESTAMP)
                        showSortMenu = false
                    },
                    leadingIcon = {
                        if (currentSortField == BrowserStatSortField.LAST_USED_TIMESTAMP) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                )
            }
        }
    )

    // Clear Stats Confirmation Dialog
    if (showClearConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmationDialog = false },
            title = { Text("Confirm Clear Stats") },
            text = { Text("Are you sure you want to clear all browser usage statistics? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearStatsClick()
                        showClearConfirmationDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearConfirmationDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BrowserStatItem(
    stat: BrowserUsageStat
    // TODO: Add onClick for potentially showing more details or linking to browser info
) {
    val context = LocalContext.current
    val packageManager = context.packageManager

    // Get App Name (requires a suspending function/lookup, or pass in stat model)
    // For simplicity, using package name directly or dummy name for now.
    // val appName = remember {
    //     runCatching {
    //         packageManager.getApplicationInfo(stat.browserPackageName, 0).loadLabel(packageManager).toString()
    //     }.getOrDefault(stat.browserPackageName)
    // }
    // To make this reactive and efficient, the ViewModel/Domain layer should ideally
    // fetch/provide app names combined with stats, or use async lookup here.
    // For this example, let's use the package name directly.

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // .clickable { /* onClick() */ } // Enable if item is clickable
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // TODO: Add App Icon here (requires PackageManager lookup or passing bitmap/painter)
        // Icon(painter = ..., contentDescription = stat.browserPackageName)
        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(stat.browserPackageName, style = MaterialTheme.typography.titleMedium) // Display package name for now
            // If you want to display app name, you'd need an async lookup or pre-fetched data
            // Text(appName, style = MaterialTheme.typography.titleMedium)
            Text("Used ${stat.usageCount} times", style = MaterialTheme.typography.bodyMedium)
            Text("Last used: ${formatInstant(stat.lastUsedTimestamp)}", style = MaterialTheme.typography.bodySmall)
        }

        // Optionally display the usage count prominently
        // Text("${stat.usageCount}", style = MaterialTheme.typography.headlineSmall)
    }
}

// Helper function to format Instant (consider moving to a utility module)
@Composable
fun formatInstant(instant: Instant): String {
    // Use platform-specific date formatting for user locale
    return remember(instant) {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) // Or use a more flexible formatter
        formatter.format(java.util.Date(instant.toEpochMilliseconds()))
    }
}


// --- Previews ---

@Preview(showBackground = true)
@Composable
fun PreviewStatsScreenEmpty() {
    MaterialTheme {
        // Use a dummy ViewModel with empty state for preview
        StatsScreen(viewModel = object : StatsViewModel(
            getBrowserStatsUseCase = object : GetBrowserStatsUseCase {
                override fun invoke(sortBy: BrowserStatSortField): Flow<List<BrowserUsageStat>> = flowOf(emptyList())
            },
            clearBrowserStatsUseCase = object : ClearBrowserStatsUseCase {
                override suspend fun invoke(onSuccess: () -> Unit, onError: (DomainError) -> Unit) {
                    // TODO("Not yet implemented")
                }
            }
        ){
            override val uiState = MutableStateFlow(StatsScreenState(isLoading = false, stats = emptyList())).asStateFlow()
        })
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewStatsScreenWithData() {
    MaterialTheme {
        val dummyStats = listOf(
            BrowserUsageStat(
                browserPackageName = "com.android.chrome",
                usageCount = 150,
                lastUsedTimestamp = Instant.parse("2023-10-27T10:00:00Z")
            ),
            BrowserUsageStat(
                browserPackageName = "org.mozilla.firefox",
                usageCount = 80,
                lastUsedTimestamp = Instant.parse("2023-10-27T11:30:00Z")
            ),
            BrowserUsageStat(
                browserPackageName = "com.sec.android.app.sbrowser",
                usageCount = 30,
                lastUsedTimestamp = Instant.parse("2023-10-26T15:45:00Z")
            )
        )
        StatsScreen(viewModel = object : StatsViewModel(
            getBrowserStatsUseCase = object : GetBrowserStatsUseCase {
                override fun invoke(sortBy: BrowserStatSortField): Flow<List<BrowserUsageStat>> = flowOf(dummyStats)
            },
            clearBrowserStatsUseCase = object : ClearBrowserStatsUseCase {
                override suspend fun invoke(onSuccess: () -> Unit, onError: (DomainError) -> Unit) {
                    // TODO("Not yet implemented")
                }
            }
        ){
            override val uiState = MutableStateFlow(StatsScreenState(isLoading = false, stats = dummyStats)).asStateFlow()
        })
    }
}
