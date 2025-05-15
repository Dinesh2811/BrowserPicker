package browserpicker.presentation.features.browserpicker.uri_info_bar.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import browserpicker.domain.service.ParsedUri
import browserpicker.domain.service.ParsedUri.Companion.uriInfoBar
import browserpicker.presentation.features.common.components.MyIcon

@Composable
internal fun SecurityIcon(
    parsedUri: ParsedUri?,
    onSecurityIconClick: () -> Unit,
) {
    val tint = when (parsedUri.uriInfoBar.first) {
        Icons.Filled.Lock -> MaterialTheme.colorScheme.primary
        Icons.Filled.Warning -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    MyIcon(
        imageVector = parsedUri.uriInfoBar.first,
        tint = tint,
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
        onClick = onSecurityIconClick
    )
}
