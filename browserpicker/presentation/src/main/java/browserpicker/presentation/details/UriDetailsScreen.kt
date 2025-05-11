package com.example.browserpicker.presentation.details

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * Composable function for the URI Details screen.
 *
 * This screen will display detailed information about a specific URI.
 */
@Composable
fun UriDetailsScreen(
    viewModel: UriDetailsViewModel
) {
    // TODO: Implement UI for displaying URI details, history, bookmarks/blocked status, etc.
    Text("URI Details Screen", modifier = Modifier.fillMaxSize())
}

@Preview(showBackground = true)
@Composable
fun PreviewUriDetailsScreen() {
    // TODO: Provide a mock ViewModel for previewing
    // UriDetailsScreen(viewModel = MockUriDetailsViewModel())
    Text("URI Details Screen Preview")
}
