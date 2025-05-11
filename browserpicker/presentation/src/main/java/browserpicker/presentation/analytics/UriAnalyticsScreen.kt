package browserpicker.presentation.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import browserpicker.domain.model.GroupCount
import browserpicker.presentation.common.components.LoadingIndicator

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
    val uiState by viewModel.uiState.collectAsState()

    var showDateRangePicker by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("URI Analytics") },
                actions = {
                    // Date range filter
                    IconButton(onClick = { showDateRangePicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Date Range")
                    }

                    // Export report
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Download, contentDescription = "Export Report")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingIndicator(message = "Loading URI analytics...")
        } else if (uiState.error != null) {
            // Error view
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Error: ${uiState.error}")
                Button(onClick = {
                    // Retry loading
                }) {
                    Text("Retry")
                }
            }
        } else {
            // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // Top Hosts section
                Text("Most Visited Hosts", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))

                // List of top hosts
                Column {
                    uiState.topHosts.forEach { hostCount ->
                        HostCountItem(hostCount = hostCount)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // URI Trends chart would go here
                // This would be implemented with a charting library showing uriTrends
                Text("URI Trends", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Chart placeholder - This would show URI usage over time by source/action")

                Spacer(modifier = Modifier.height(24.dp))

                // Status Changes chart would go here
                // This would be implemented with a charting library showing statusChanges
                Text("Status Changes", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Chart placeholder - This would show URI status changes over time")
            }
        }
    }

    // Date range picker dialog
    if (showDateRangePicker) {
        // Date range picker implementation would go here
        // After selection:
        // viewModel.setDateRange(startDate, endDate)
    }

    // Export dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Report") },
            text = { Text("Do you want to export the URI history report?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.generateReport(exportToFile = true)
                        showExportDialog = false
                    }
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Display a host count item
 */
@Composable
private fun HostCountItem(
    hostCount: GroupCount
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(hostCount.groupValue ?: "Unknown")
        Text("${hostCount.count} visits")
    }
}
