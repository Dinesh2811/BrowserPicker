package com.dinesh.browserpicker.mock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DebugActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val debugViewModel: DebugViewModel = hiltViewModel()

            DebugScreen(
                debugViewModel = debugViewModel,
                onGenerateMockData = { debugViewModel.generateMockData() },
                onClearMockData = { debugViewModel.clearMockData() },
                onClearMessage = { debugViewModel.clearMessage() }
            )
        }
    }
}

@Composable
fun DebugScreen(
    debugViewModel: DebugViewModel = hiltViewModel(),
    onGenerateMockData: () -> Unit = { debugViewModel.generateMockData() },
    onClearMockData: () -> Unit = { debugViewModel.clearMockData() },
    onClearMessage: () -> Unit = { debugViewModel.clearMessage() },
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
