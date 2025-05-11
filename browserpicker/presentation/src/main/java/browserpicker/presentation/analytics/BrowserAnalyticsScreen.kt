package browserpicker.presentation.analytics

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import browserpicker.domain.model.BrowserAppInfo
import browserpicker.domain.model.BrowserUsageStat
import browserpicker.domain.model.DateCount
import browserpicker.domain.model.query.BrowserStatSortField
import browserpicker.domain.model.query.SortOrder
import browserpicker.presentation.UiState
import browserpicker.presentation.common.components.EmptyStateView
import browserpicker.presentation.common.components.ErrorView
import browserpicker.presentation.common.components.LoadingIndicator
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Browser Analytics Screen - Shows browser usage statistics and trends.
 *
 * This screen displays:
 * - Overall browser usage statistics
 * - Usage trends over time (with charts)
 * - Most frequently used browsers
 * - Most recently used browsers
 * - Export options for reports
 *
 * It provides insights into how the user interacts with different browsers,
 * helping them understand their browsing patterns.
 *
 * Uses: BrowserAnalyticsViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserAnalyticsScreen(
    navController: NavController,
    viewModel: BrowserAnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browser Analytics") },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Cards
            item {
                BrowserSummaryCards(state)
            }

            // Browser Usage Stats
            item {
                BrowserUsageStatsSection(state, viewModel)
            }

            // Trend Analysis
            item {
                BrowserTrendsSection(state)
            }

            // Report Generation
            item {
                ReportGenerationSection(state, viewModel)
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        BrowserAnalyticsFilterDialog(
            currentFilter = state.filterOptions,
            availableBrowsers = state.availableBrowsers.getOrNull() ?: emptyList(),
            onDismiss = { showFilterDialog = false },
            onApplyFilter = { filter ->
                if (filter.timeRange != state.filterOptions.timeRange) {
                    viewModel.updateTimeRange(
                        from = filter.timeRange?.first,
                        to = filter.timeRange?.second
                    )
                }
                if (filter.sortField != state.filterOptions.sortField ||
                    filter.sortOrder != state.filterOptions.sortOrder) {
                    viewModel.setSortingOptions(filter.sortField, filter.sortOrder)
                }
                if (filter.selectedBrowsers != state.filterOptions.selectedBrowsers) {
                    viewModel.updateSelectedBrowsers(filter.selectedBrowsers)
                }
                showFilterDialog = false
            },
            onResetFilter = {
                viewModel.resetFilters()
                showFilterDialog = false
            }
        )
    }
}

@Composable
private fun BrowserSummaryCards(state: BrowserAnalyticsUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Browser Usage Summary",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Most Frequent Browser Card
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "Most Used",
                content = {
                    when (val mostFrequent = state.mostFrequentBrowser) {
                        is UiState.Loading -> LoadingIndicator(modifier = Modifier.size(40.dp))
                        is UiState.Error -> Text(
                            text = "Error loading data",
                            color = MaterialTheme.colorScheme.error
                        )
                        is UiState.Success -> {
                            val browser = mostFrequent.data
                            if (browser != null) {
                                Text(
                                    text = browser.appName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "No data available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            )

            // Most Recent Browser Card
            SummaryCard(
                modifier = Modifier.weight(1f),
                title = "Recently Used",
                content = {
                    when (val mostRecent = state.mostRecentBrowser) {
                        is UiState.Loading -> LoadingIndicator(modifier = Modifier.size(40.dp))
                        is UiState.Error -> Text(
                            text = "Error loading data",
                            color = MaterialTheme.colorScheme.error
                        )
                        is UiState.Success -> {
                            val browser = mostRecent.data
                            if (browser != null) {
                                Text(
                                    text = browser.appName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "No data available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun BrowserStatItem(
    stat: BrowserUsageStat,
    onClick: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = { onClick(stat.browserPackageName) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stat.browserPackageName.substringAfterLast('.'),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Last used: ${formatInstant(stat.lastUsedTimestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stat.usageCount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "View Details",
                    modifier = Modifier.padding(start = 8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BrowserDetailsDialog(
    browserStat: BrowserUsageStat,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = browserStat.browserPackageName.substringAfterLast('.'),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Usage Count:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${browserStat.usageCount}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Last Used:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = formatInstant(browserStat.lastUsedTimestamp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Package Name:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Text(
                    text = browserStat.browserPackageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun BrowserUsageStatsSection(
    state: BrowserAnalyticsUiState,
    viewModel: BrowserAnalyticsViewModel
) {
    var selectedBrowser by remember { mutableStateOf<BrowserUsageStat?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Browser Usage Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            when (val usageStats = state.usageStats) {
                is UiState.Loading -> LoadingIndicator()

                is UiState.Error -> ErrorView(
                    message = usageStats.message,
                    onRetry = { viewModel.refreshData() }
                )

                is UiState.Success -> {
                    val stats = usageStats.data
                    if (stats.isEmpty()) {
                        EmptyStateView("No browser usage data available yet.")
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            stats.forEach { stat ->
                                BrowserStatItem(
                                    stat = stat,
                                    onClick = { packageName ->
                                        selectedBrowser = stat
                                        viewModel.getBrowserDetails(packageName)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Show browser details dialog when a browser is selected
    selectedBrowser?.let { browser ->
        BrowserDetailsDialog(
            browserStat = browser,
            onDismiss = { selectedBrowser = null }
        )
    }
}

@Composable
private fun BrowserTrendsSection(state: BrowserAnalyticsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Usage Trends",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            when (val trendData = state.trendData) {
                is UiState.Loading -> LoadingIndicator()

                is UiState.Error -> Text(
                    text = "Error loading trend data: ${trendData.message}",
                    color = MaterialTheme.colorScheme.error
                )

                is UiState.Success -> {
                    val trends = trendData.data
                    if (trends.isEmpty()) {
                        EmptyStateView("No trend data available yet.")
                    } else {
                        // In a real app, you would render a chart here
                        // For simplicity, we'll just show the browser names and counts
                        Column {
                            trends.forEach { (browser, counts) ->
                                Text(
                                    text = browser.substringAfterLast('.'),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Total entries: ${counts.sumOf { it.count }}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportGenerationSection(
    state: BrowserAnalyticsUiState,
    viewModel: BrowserAnalyticsViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Report Generation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (state.isGeneratingReport) {
                LoadingIndicator(message = "Generating report...")
            } else {
                Button(
                    onClick = { viewModel.generateReport() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Assessment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Report")
                }

                when (val report = state.fullReport) {
                    is UiState.Loading -> {
                        // Already handled with isGeneratingReport
                    }

                    is UiState.Error -> Text(
                        text = "Error generating report: ${report.message}",
                        color = MaterialTheme.colorScheme.error
                    )

                    is UiState.Success -> {
                        val data = report.data
                        Column {
                            Text(
                                text = "Report Summary",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Total Browsers: ${data.totalUsage.size}")
                            Text("Date Range: ${formatInstant(data.timeRange.first)} to ${formatInstant(data.timeRange.second)}")

                            if (data.mostUsed != null) {
                                Text("Most Used: ${data.mostUsed!!.browserPackageName.substringAfterLast('.')} (${data.mostUsed!!.usageCount} times)")
                            }

                            if (data.mostRecent != null) {
                                Text("Most Recent: ${data.mostRecent!!.browserPackageName.substringAfterLast('.')} (${formatInstant(data.mostRecent!!.lastUsedTimestamp)})")
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
private fun BrowserAnalyticsFilterDialog(
    currentFilter: BrowserAnalyticsFilterOptions,
    availableBrowsers: List<BrowserAppInfo>,
    onDismiss: () -> Unit,
    onApplyFilter: (BrowserAnalyticsFilterOptions) -> Unit,
    onResetFilter: () -> Unit
) {
    var timeRangeFrom by remember { mutableStateOf<Instant?>(currentFilter.timeRange?.first) }
    var timeRangeTo by remember { mutableStateOf<Instant?>(currentFilter.timeRange?.second) }
    var sortField by remember { mutableStateOf(currentFilter.sortField) }
    var sortOrder by remember { mutableStateOf(currentFilter.sortOrder) }
    var selectedBrowsers by remember { mutableStateOf(currentFilter.selectedBrowsers) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Options") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Time Range Filter UI
                Text("Time Range", fontWeight = FontWeight.Bold)
                // Simplified - in a real app you would use DatePickers
                Row {
                    Button(onClick = {
                        // Set to 30 days ago in a real implementation
                        // Simplified for this example
                    }) {
                        Text("Last 30 Days")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        timeRangeFrom = null
                        timeRangeTo = null
                    }) {
                        Text("All Time")
                    }
                }

                Divider()

                // Sort Options
                Text("Sort By", fontWeight = FontWeight.Bold)
                Row {
                    RadioButton(
                        selected = sortField == BrowserStatSortField.USAGE_COUNT,
                        onClick = { sortField = BrowserStatSortField.USAGE_COUNT }
                    )
                    Text(
                        text = "Usage Count",
                        modifier = Modifier
                            .clickable { sortField = BrowserStatSortField.USAGE_COUNT }
                            .padding(start = 8.dp)
                            .align(Alignment.CenterVertically)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    RadioButton(
                        selected = sortField == BrowserStatSortField.LAST_USED_TIMESTAMP,
                        onClick = { sortField = BrowserStatSortField.LAST_USED_TIMESTAMP }
                    )
                    Text(
                        text = "Last Used",
                        modifier = Modifier
                            .clickable { sortField = BrowserStatSortField.LAST_USED_TIMESTAMP }
                            .padding(start = 8.dp)
                            .align(Alignment.CenterVertically)
                    )
                }

                Row {
                    RadioButton(
                        selected = sortOrder == SortOrder.DESC,
                        onClick = { sortOrder = SortOrder.DESC }
                    )
                    Text(
                        text = "Descending",
                        modifier = Modifier
                            .clickable { sortOrder = SortOrder.DESC }
                            .padding(start = 8.dp)
                            .align(Alignment.CenterVertically)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    RadioButton(
                        selected = sortOrder == SortOrder.ASC,
                        onClick = { sortOrder = SortOrder.ASC }
                    )
                    Text(
                        text = "Ascending",
                        modifier = Modifier
                            .clickable { sortOrder = SortOrder.ASC }
                            .padding(start = 8.dp)
                            .align(Alignment.CenterVertically)
                    )
                }

                Divider()

                // Browser Selection
                Text("Filter by Browser", fontWeight = FontWeight.Bold)
                if (availableBrowsers.isEmpty()) {
                    Text("No browsers available", color = Color.Gray)
                } else {
                    Column {
                        availableBrowsers.forEach { browser ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = selectedBrowsers.contains(browser.packageName),
                                    onCheckedChange = { checked ->
                                        selectedBrowsers = if (checked) {
                                            selectedBrowsers + browser.packageName
                                        } else {
                                            selectedBrowsers - browser.packageName
                                        }
                                    }
                                )
                                Text(
                                    text = browser.appName,
                                    modifier = Modifier
                                        .clickable {
                                            selectedBrowsers = if (selectedBrowsers.contains(browser.packageName)) {
                                                selectedBrowsers - browser.packageName
                                            } else {
                                                selectedBrowsers + browser.packageName
                                            }
                                        }
                                        .padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val timeRange = if (timeRangeFrom != null && timeRangeTo != null) {
                        Pair(timeRangeFrom!!, timeRangeTo!!)
                    } else {
                        null
                    }

                    onApplyFilter(
                        BrowserAnalyticsFilterOptions(
                            timeRange = timeRange,
                            sortField = sortField,
                            sortOrder = sortOrder,
                            selectedBrowsers = selectedBrowsers
                        )
                    )
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onResetFilter) {
                    Text("Reset")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun formatInstant(instant: Instant): String {
    val formatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())

    return formatter.format(instant.toJavaInstant())
}
