package com.dinesh.browserpicker.mock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.animation.*

@Composable
fun DebugScreen(
    debugViewModel: DebugViewModel = hiltViewModel(),
    onGenerateMockData: () -> Unit = { debugViewModel.generateMockData() },
    onClearMockData: () -> Unit = { debugViewModel.clearMockData() },
    onClearMessage: () -> Unit = { debugViewModel.clearMessage() },
) {
    val isDatabaseEmpty by debugViewModel.isDatabaseEmpty.collectAsState()
    AnimatedVisibility(
        visible = isDatabaseEmpty,
        enter = fadeIn() + slideInVertically(initialOffsetY = { fullHeight -> fullHeight / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { fullHeight -> fullHeight / 2 })
    ) {
        val isLoading by debugViewModel.isLoading.collectAsState()
        val message by debugViewModel.message.collectAsState()

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(message ?: "No messages", style = MaterialTheme.typography.bodyLarge)

            if (!isLoading) {
                Button(onClick = onGenerateMockData) {
                    Text("Generate Mock Data")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onClearMockData) {
                    Text("Clear Mock Data")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onClearMessage) {
                    Text("Clear Message")
                }
            } else {
                CircularProgressIndicator()
            }
        }
    }
}
