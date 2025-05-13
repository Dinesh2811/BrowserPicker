package browserpicker.presentation.features.uri_history

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun UriHistoryScreen(
    modifier: Modifier = Modifier,
    onNavigateToNested1: () -> Unit,
    onNavigateToNested2: () -> Unit,
    onNavigateToNested3: () -> Unit
) {}

@Composable
fun NestedUriHistoryScreen1(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {}
@Composable
fun NestedUriHistoryScreen2(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {}
@Composable
fun NestedUriHistoryScreen3(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {}
