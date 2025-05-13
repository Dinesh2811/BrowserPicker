package browserpicker.presentation.test.analytics

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
import browserpicker.domain.model.DateCount
import browserpicker.domain.model.GroupCount
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriSource
import browserpicker.presentation.UiState
import browserpicker.presentation.test.common.components.EmptyStateView
import browserpicker.presentation.test.common.components.ErrorView
import browserpicker.presentation.test.common.components.LoadingIndicator
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * URI Analytics Screen - Shows statistics and trends related to URI usage.
 *
 * This screen displays:
 * - Most visited hosts
 * - URI usage trends over time (with charts)
 * - Breakdown of user actions (opened, blocked, bookmarked)
 * - Analysis of URI status changes
 * - Export options for reports
 *
 * It provides insights into the user's browsing habits and how the app is handling URIs.
 *
 * Uses: UriAnalyticsViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UriAnalyticsScreen(
    navController: androidx.navigation.NavController,
    viewModel: UriAnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedHost by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("URI Analytics") },
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
            // Most Visited Hosts
            item {
                MostVisitedHostsSection(state, viewModel) { host ->
                    selectedHost = host
                    viewModel.getHostDetails(host)
                }
            }

            // URI Trends
            item {
                UriTrendsSection(state)
            }

            // Status Changes
            item {
                StatusChangesSection(state)
            }

            // Report Generation
            item {
                ReportGenerationSection(state, viewModel)
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        UriAnalyticsFilterDialog(
            currentFilter = state.filterOptions,
            hostList = state.hostList.getOrNull() ?: emptyList(),
            onDismiss = { showFilterDialog = false },
            onApplyFilter = { filter ->
                if (filter.timeRange != state.filterOptions.timeRange) {
                    viewModel.updateTimeRange(
                        from = filter.timeRange?.first,
                        to = filter.timeRange?.second
                    )
                }
                if (filter.selectedHosts != state.filterOptions.selectedHosts) {
                    viewModel.updateSelectedHosts(filter.selectedHosts)
                }
                if (filter.selectedActions != state.filterOptions.selectedActions) {
                    viewModel.updateSelectedActions(filter.selectedActions)
                }
                if (filter.selectedSources != state.filterOptions.selectedSources) {
                    viewModel.updateSelectedSources(filter.selectedSources)
                }
                showFilterDialog = false
            },
            onResetFilter = {
                viewModel.resetFilters()
                showFilterDialog = false
            }
        )
    }

    // Host Details Dialog
    selectedHost?.let { host ->
        val hostActions = when (val topActions = state.topActionsByHost) {
            is UiState.Success -> topActions.data[host]
            else -> null
        }

        HostDetailsDialog(
            host = host,
            actions = hostActions,
            isLoading = state.topActionsByHost is UiState.Loading,
            onDismiss = { selectedHost = null }
        )
    }
}

@Composable
private fun MostVisitedHostsSection(
    state: UriAnalyticsUiState,
    viewModel: UriAnalyticsViewModel,
    onHostSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Most Visited Hosts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            when (val mostVisited = state.mostVisitedHosts) {
                is UiState.Loading -> LoadingIndicator()

                is UiState.Error -> ErrorView(
                    message = mostVisited.message,
                    onRetry = { viewModel.refreshData() }
                )

                is UiState.Success -> {
                    val hosts = mostVisited.data
                    if (hosts.isEmpty()) {
                        EmptyStateView("No host visit data available yet.")
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            hosts.forEach { hostCount ->
                                hostCount.groupValue?.let { host ->
                                    HostCountItem(
                                        host = host,
                                        count = hostCount.count,
                                        onClick = { onHostSelected(host) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HostCountItem(
    host: String,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = host,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun UriTrendsSection(state: UriAnalyticsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "URI Usage Trends",
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
                        // For simplicity, we'll just show the host names and counts
                        Column {
                            trends.forEach { (host, counts) ->
                                Text(
                                    text = host,
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
private fun StatusChangesSection(state: UriAnalyticsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "URI Status Changes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            when (val statusData = state.statusChangeData) {
                is UiState.Loading -> LoadingIndicator()

                is UiState.Error -> Text(
                    text = "Error loading status data: ${statusData.message}",
                    color = MaterialTheme.colorScheme.error
                )

                is UiState.Success -> {
                    val statuses = statusData.data
                    if (statuses.isEmpty()) {
                        EmptyStateView("No status change data available yet.")
                    } else {
                        // In a real app, you would render a chart here
                        // For simplicity, we'll just show the host names and counts
                        Column {
                            statuses.forEach { (host, counts) ->
                                Text(
                                    text = host,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Status changes: ${counts.sumOf { it.count }}",
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
    state: UriAnalyticsUiState,
    viewModel: UriAnalyticsViewModel
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.generateReport() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Report")
                    }

                    Button(
                        onClick = { viewModel.generateReport(exportToFile = true) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export")
                    }
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
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Report Summary",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Total Records: ${data.totalRecords}")
                                Text("Date Range: ${formatInstant(data.timeRange.first)} to ${formatInstant(data.timeRange.second)}")

                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                Text("Top Hosts:", fontWeight = FontWeight.Medium)
                                data.topHosts.take(3).forEach { host ->
                                    host.groupValue?.let {
                                        Text("• $it (${host.count} visits)")
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                Text("Action Breakdown:", fontWeight = FontWeight.Medium)
                                data.actionBreakdown.forEach { (action, count) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("• ${action.name}")
                                        Text("$count", fontWeight = FontWeight.Bold)
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 8.dp))

                                Text("Source Breakdown:", fontWeight = FontWeight.Medium)
                                data.sourceBreakdown.forEach { (source, count) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("• ${source?.name ?: "Unknown"}")
                                        Text("$count", fontWeight = FontWeight.Bold)
                                    }
                                }
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
private fun UriAnalyticsFilterDialog(
    currentFilter: UriAnalyticsFilterOptions,
    hostList: List<String>,
    onDismiss: () -> Unit,
    onApplyFilter: (UriAnalyticsFilterOptions) -> Unit,
    onResetFilter: () -> Unit
) {
    var timeRangeFrom by remember { mutableStateOf<Instant?>(currentFilter.timeRange?.first) }
    var timeRangeTo by remember { mutableStateOf<Instant?>(currentFilter.timeRange?.second) }
    var selectedHosts by remember { mutableStateOf(currentFilter.selectedHosts) }
    var selectedActions by remember { mutableStateOf(currentFilter.selectedActions) }
    var selectedSources by remember { mutableStateOf(currentFilter.selectedSources) }

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

                // Host Selection
                Text("Filter by Host", fontWeight = FontWeight.Bold)
                if (hostList.isEmpty()) {
                    Text("No hosts available", color = Color.Gray)
                } else {
                    Column(
                        modifier = Modifier.height(150.dp)
                    ) {
                        hostList.forEach { host ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = selectedHosts.contains(host),
                                    onCheckedChange = { checked ->
                                        selectedHosts = if (checked) {
                                            selectedHosts + host
                                        } else {
                                            selectedHosts - host
                                        }
                                    }
                                )
                                Text(
                                    text = host,
                                    modifier = Modifier
                                        .clickable {
                                            selectedHosts = if (selectedHosts.contains(host)) {
                                                selectedHosts - host
                                            } else {
                                                selectedHosts + host
                                            }
                                        }
                                        .padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                Divider()

                // Action Selection
                Text("Filter by Action", fontWeight = FontWeight.Bold)
                Column {
                    InteractionAction.entries.filter { it != InteractionAction.UNKNOWN }.forEach { action ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = selectedActions.contains(action),
                                onCheckedChange = { checked ->
                                    selectedActions = if (checked) {
                                        selectedActions + action
                                    } else {
                                        selectedActions - action
                                    }
                                }
                            )
                            Text(
                                text = action.name,
                                modifier = Modifier
                                    .clickable {
                                        selectedActions = if (selectedActions.contains(action)) {
                                            selectedActions - action
                                        } else {
                                            selectedActions + action
                                        }
                                    }
                                    .padding(start = 8.dp)
                            )
                        }
                    }
                }

                Divider()

                // Source Selection
                Text("Filter by Source", fontWeight = FontWeight.Bold)
                Column {
                    UriSource.entries.filter { it != UriSource.UNKNOWN }.forEach { source ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = selectedSources.contains(source),
                                onCheckedChange = { checked ->
                                    selectedSources = if (checked) {
                                        selectedSources + source
                                    } else {
                                        selectedSources - source
                                    }
                                }
                            )
                            Text(
                                text = source.name,
                                modifier = Modifier
                                    .clickable {
                                        selectedSources = if (selectedSources.contains(source)) {
                                            selectedSources - source
                                        } else {
                                            selectedSources + source
                                        }
                                    }
                                    .padding(start = 8.dp)
                            )
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
                        UriAnalyticsFilterOptions(
                            timeRange = timeRange,
                            selectedHosts = selectedHosts,
                            selectedActions = selectedActions,
                            selectedSources = selectedSources
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
private fun HostDetailsDialog(
    host: String,
    actions: List<GroupCount>?,
    isLoading: Boolean,
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
                    text = host,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Actions Breakdown",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    LoadingIndicator()
                } else if (actions == null || actions.isEmpty()) {
                    EmptyStateView("No action data available for this host.")
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        actions.forEach { actionCount ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = actionCount.groupValue ?: "Unknown",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "${actionCount.count}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Divider()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun formatInstant(instant: Instant): String {
    val formatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())

    return formatter.format(instant.toJavaInstant())
}
