package browserpicker.presentation.features.folder

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun FolderDetailsScreen(
    modifier: Modifier = Modifier,
    onNavigateToNested1: () -> Unit,
    onNavigateToNested2: () -> Unit,
    onNavigateToNested3: () -> Unit
) {}

@Composable
fun NestedFolderDetailsScreen1(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {}
@Composable
fun NestedFolderDetailsScreen2(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {}
@Composable
fun NestedFolderDetailsScreen3(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {}
