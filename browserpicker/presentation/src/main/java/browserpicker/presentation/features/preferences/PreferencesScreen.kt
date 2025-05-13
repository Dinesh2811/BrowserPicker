package browserpicker.presentation.features.preferences

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PreferencesScreen(
    modifier: Modifier = Modifier,
    onNavigateToNested1: () -> Unit,
    onNavigateToNested2: () -> Unit,
    onNavigateToNested3: () -> Unit
) {}

@Composable
fun NestedPreferencesScreen1(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {}
@Composable
fun NestedPreferencesScreen2(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {}
@Composable
fun NestedPreferencesScreen3(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {}
