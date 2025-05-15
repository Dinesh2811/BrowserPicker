package browserpicker.presentation.features.browserpicker.uri_info_bar.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun EditUriDialog(
    modifier: Modifier = Modifier,
    uri: Uri? = null, // Default removed, handle initial text in remember
    onConfirm: (Uri) -> Unit, // Changed to non-nullable Uri, ensure validity before calling
    onDismiss: () -> Unit,
) {
    // Implementation here
}