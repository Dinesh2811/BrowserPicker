package browserpicker.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriRecord
import browserpicker.presentation.UiState
import browserpicker.presentation.navigation.BookmarksRoute
import browserpicker.presentation.navigation.SettingsRoute
import browserpicker.presentation.navigation.UriAnalyticsRoute
import browserpicker.presentation.navigation.UriHistoryRoute

/**
 * Home screen content.
 *
 * This screen shows:
 * - App status (default browser)
 * - Quick actions
 * - Recently handled URIs
 * - Browser stats summary
 */
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App status section
        item {
            DefaultBrowserStatusCard(
                isDefaultBrowser = uiState.isDefaultBrowser,
                onSetDefaultClick = { viewModel.setAsDefaultBrowser() },
                onOpenSettingsClick = { viewModel.openBrowserSettings() }
            )
        }

        // Quick Actions section
        item {
            QuickActionsSection(
                // Navigate using serializable route objects
                onNavigateToHistory = { navController.navigate(UriHistoryRoute) },
                onNavigateToBookmarks = { navController.navigate(BookmarksRoute) },
                onNavigateToBlocked = { /* TODO: Navigate to blocked URLs screen */ },
                onNavigateToSettings = { navController.navigate(SettingsRoute) }
            )
        }

        // Recent URIs section
        item {
            RecentUrisSection(
                recentUris = uiState.recentUris,
                mostFrequentBrowser = uiState.mostFrequentBrowser,
                availableBrowsers = uiState.availableBrowsers,
                onUriClick = { uri ->
                    // When a recent URI is clicked, optionally handle it again
                    viewModel.handleUri(uri.uriString)
                },
                onShareUri = { uri -> viewModel.shareUri(uri.uriString) },
                onRefreshClick = { viewModel.refreshData() }
            )
        }

        // Browser stats summary
        item {
            BrowserStatsSummary(
                mostFrequentBrowser = uiState.mostFrequentBrowser,
                onNavigateToAnalytics = { navController.navigate(UriAnalyticsRoute) }
            )
        }

        // Processing result handling (if any)
        uiState.processingResult?.let { result ->
            item {
                ProcessingResultCard(
                    result = result,
                    onDismiss = { viewModel.clearProcessingResult() },
                    onOpenInBrowser = { uriString, browserPackage ->
                        viewModel.openUriInBrowser(uriString, browserPackage)
                    }
                )
            }
        }
    }
}

@Composable
private fun DefaultBrowserStatusCard(
    isDefaultBrowser: UiState<Boolean>,
    onSetDefaultClick: () -> Unit,
    onOpenSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Browser Picker Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            when (isDefaultBrowser) {
                is UiState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checking default browser status...")
                    }
                }
                is UiState.Success -> {
                    if (isDefaultBrowser.data) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browser Picker is your default browser")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browser Picker is not your default browser")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onSetDefaultClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Set as Default")
                            }

                            FilledTonalButton(
                                onClick = onOpenSettingsClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Open Settings")
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    Text(
                        text = "Failed to check default browser status",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionsSection(
    onNavigateToHistory: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToBlocked: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Column {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionItem(
                icon = Icons.Default.History,
                title = "History",
                onClick = onNavigateToHistory,
                modifier = Modifier.weight(1f)
            )

            QuickActionItem(
                icon = Icons.Default.Star,
                title = "Bookmarks",
                onClick = onNavigateToBookmarks,
                modifier = Modifier.weight(1f)
            )

            QuickActionItem(
                icon = Icons.Outlined.Block,
                title = "Blocked",
                onClick = onNavigateToBlocked,
                modifier = Modifier.weight(1f)
            )

            QuickActionItem(
                icon = Icons.Default.Settings,
                title = "Settings",
                onClick = onNavigateToSettings,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RecentUrisSection(
    recentUris: UiState<List<UriRecord>>,
    mostFrequentBrowser: UiState<browserpicker.domain.model.BrowserAppInfo?>,
    availableBrowsers: UiState<List<browserpicker.domain.model.BrowserAppInfo>>,
    onUriClick: (UriRecord) -> Unit,
    onShareUri: (UriRecord) -> Unit,
    onRefreshClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent URIs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = onRefreshClick) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (recentUris) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Success -> {
                if (recentUris.data.isEmpty()) {
                    EmptyRecentUris()
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentUris.data) { uriRecord ->
                            UriCard(
                                uriRecord = uriRecord,
                                onClick = { onUriClick(uriRecord) },
                                onShare = { onShareUri(uriRecord) },
                                availableBrowsers = availableBrowsers.getOrNull() ?: emptyList()
                            )
                        }
                    }
                }
            }
            is UiState.Error -> {
                ErrorMessage(
                    message = "Failed to load recent URIs: ${recentUris.errorMessageOrNull}"
                )
            }
        }
    }
}

@Composable
private fun UriCard(
    uriRecord: UriRecord,
    onClick: () -> Unit,
    onShare: () -> Unit,
    availableBrowsers: List<browserpicker.domain.model.BrowserAppInfo>
) {
    OutlinedCard(
        modifier = Modifier
            .width(240.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = uriRecord.host,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = uriRecord.uriString,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Display browser used if available
            uriRecord.chosenBrowserPackage?.let { browserPackage ->
                val browserInfo = availableBrowsers.find { it.packageName == browserPackage }
                if (browserInfo != null) {
                    Text(
                        text = "Opened with: ${browserInfo.appName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action type and source
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val actionText = when (uriRecord.interactionAction) {
                    InteractionAction.OPENED_ONCE -> "Opened"
                    InteractionAction.OPENED_BY_PREFERENCE -> "Auto-opened"
                    InteractionAction.BLOCKED_URI_ENFORCED -> "Blocked"
                    InteractionAction.PREFERENCE_SET -> "Preferred"
                    InteractionAction.DISMISSED -> "Dismissed"
                    else -> "Unknown"
                }

                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelSmall
                )

                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share URI",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRecentUris() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "No recent URIs",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun BrowserStatsSummary(
    mostFrequentBrowser: UiState<browserpicker.domain.model.BrowserAppInfo?>,
    onNavigateToAnalytics: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Browser Stats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                FilledTonalButton(
                    onClick = onNavigateToAnalytics
                ) {
                    Text("View Analytics")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (mostFrequentBrowser) {
                is UiState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Loading browser stats...")
                    }
                }
                is UiState.Success -> {
                    val browser = mostFrequentBrowser.data
                    if (browser != null) {
                        Text(
                            text = "Your most used browser is:",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = browser.appName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "No browser usage statistics yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is UiState.Error -> {
                    Text(
                        text = "Failed to load browser statistics",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessingResultCard(
    result: HandleUriResultUi,
    onDismiss: () -> Unit,
    onOpenInBrowser: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (result) {
                is HandleUriResultUi.Blocked -> MaterialTheme.colorScheme.errorContainer
                is HandleUriResultUi.InvalidUri -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            when (result) {
                is HandleUriResultUi.Blocked -> {
                    Text(
                        text = "Blocked URI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "The URI was blocked according to your preferences",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                is HandleUriResultUi.OpenDirectly -> {
                    Text(
                        text = "URI Opened",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "The URI was opened with ${result.browserName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is HandleUriResultUi.ShowPicker -> {
                    Text(
                        text = "Select Browser",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Host: ${result.host}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = result.uriString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Browser picker would go here
                    // This is a placeholder - the actual browser picker UI would be implemented separately
                    Text(
                        text = "Browser picker UI would appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                is HandleUriResultUi.InvalidUri -> {
                    Text(
                        text = "Invalid URI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = result.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
    }
}
