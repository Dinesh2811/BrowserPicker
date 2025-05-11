package browserpicker.presentation.browserpicker

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import browserpicker.domain.model.UriSource
import browserpicker.presentation.common.components.LoadingIndicator

/**
 * Browser Picker Screen - Core functionality of the app.
 * 
 * This screen is shown when a URI is intercepted and displays:
 * - The intercepted URI
 * - List of available browsers to open the URI
 * - Options to bookmark or block the URI
 * - Option to set a preferred browser for the host
 * 
 * It's designed to provide a clean, intuitive interface for users
 * to select which browser they want to use for a specific URI.
 * 
 * Uses: BrowserPickerViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserPickerScreen(
    navController: androidx.navigation.NavController,
    uriString: String? = null,
    viewModel: BrowserPickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Process the URI when the screen is first composed
    LaunchedEffect(uriString) {
        uriString?.let {
            viewModel.processUri(it, UriSource.INTENT)
        }
    }
    
    // Handle result state
    LaunchedEffect(uiState.resultState) {
        val resultState = uiState.resultState ?: return@LaunchedEffect
        
        when (resultState) {
            is PickerResultState.OpenInBrowser -> {
                // Handle opening URI in selected browser
                // This would typically finish the activity
            }
            PickerResultState.Blocked -> {
                // Handle blocked URI
                // This would typically finish the activity
            }
            PickerResultState.Dismissed -> {
                // Handle dismissal
                // This would typically finish the activity
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Browser") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.dismiss() }) {
                        // Close icon
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingIndicator(message = "Loading browsers...")
        } else if (uiState.error != null) {
            // Error view
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Error: ${uiState.error}")
                Button(onClick = { 
                    uriString?.let { viewModel.processUri(it, UriSource.INTENT) }
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
                // URI display
                Text(
                    text = uiState.uriString,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Browser list would go here
                // Implementation would iterate through uiState.availableBrowsers
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Bookmark button
                    Button(
                        onClick = { viewModel.bookmarkUri() },
                        enabled = !uiState.isBlocked
                    ) {
                        Text(if (uiState.isBookmarked) "Unbookmark" else "Bookmark")
                    }
                    
                    // Block button
                    Button(
                        onClick = { viewModel.blockUri() },
                        enabled = !uiState.isBookmarked
                    ) {
                        Text(if (uiState.isBlocked) "Unblock" else "Block")
                    }
                }
            }
        }
    }
} 