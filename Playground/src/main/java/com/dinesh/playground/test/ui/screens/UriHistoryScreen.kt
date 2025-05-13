package com.dinesh.playground.test.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun UriHistoryScreen(
    modifier: Modifier = Modifier,
    onNavigateToNested1: () -> Unit,
    onNavigateToNested2: () -> Unit,
    onNavigateToNested3: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("URI History Screen")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNavigateToNested1) {
            Text("Go to Nested URI History 1")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onNavigateToNested2) {
            Text("Go to Nested URI History 2")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onNavigateToNested3) {
            Text("Go to Nested URI History 3")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UriHistoryScreenPreview() {
    UriHistoryScreen(onNavigateToNested1 = {}, onNavigateToNested2 = {}, onNavigateToNested3 = {})
} 