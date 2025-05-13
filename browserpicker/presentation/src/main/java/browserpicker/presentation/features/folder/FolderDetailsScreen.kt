package browserpicker.presentation.features.folder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun FolderDetailsScreen(
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
        Text("Folder Details Screen")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNavigateToNested1) {
            Text("Go to Nested Folder 1")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onNavigateToNested2) {
            Text("Go to Nested Folder 2")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onNavigateToNested3) {
            Text("Go to Nested Folder 3")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FolderDetailsScreenPreview() {
    FolderDetailsScreen(onNavigateToNested1 = {}, onNavigateToNested2 = {}, onNavigateToNested3 = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NestedFolderDetailsScreen1(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nested Folder Details 1") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier.fillMaxSize().padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Nested Folder Details Screen 1 Content")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NestedFolderDetailsScreen2(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nested Folder Details 2") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier.fillMaxSize().padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Nested Folder Details Screen 2 Content")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NestedFolderDetailsScreen3(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nested Folder Details 3") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier.fillMaxSize().padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Nested Folder Details Screen 3 Content")
        }
    }
}
