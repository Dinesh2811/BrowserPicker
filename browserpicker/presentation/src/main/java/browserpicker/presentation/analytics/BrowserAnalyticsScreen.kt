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
import browserpicker.domain.model.BrowserUsageStat
import browserpicker.presentation.common.components.LoadingIndicator

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
    navController: androidx.navigation.NavController,
    viewModel: BrowserAnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var showDateRangePicker by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browser Analytics") },
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
            LoadingIndicator(message = "Loading browser stats...")
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
                // Summary cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Most used browser card
                    Card(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Most Used", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(uiState.mostUsedBrowser ?: "None", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    
                    // Most recent browser card
                    Card(
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Most Recent", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(uiState.mostRecentBrowser ?: "None", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Usage trends chart would go here
                // This would be implemented with a charting library
                Text("Browser Usage Trends", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Chart placeholder - This would show browser usage over time")
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Browser statistics
                Text("Browser Statistics", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Browser stats list
                Column {
                    uiState.browserStats.forEach { stat ->
                        BrowserStatItem(stat = stat)
                    }
                }
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
            text = { Text("Do you want to export the browser usage report?") },
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
 * Display a browser usage stat item
 */
@Composable
private fun BrowserStatItem(
    stat: BrowserUsageStat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Browser icon and name
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Browser icon would go here
            Spacer(modifier = Modifier.width(8.dp))
            Text(stat.browserPackageName)
        }
        
        // Usage count
        Text("Used ${stat.usageCount} times")
    }
} 