package browserpicker.presentation.features.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground = true, name = "MyIcon")
@Composable
fun MyIcon(
    imageVector: ImageVector = Icons.Default.Search,
    tint: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
    onClick: () -> Unit = {}
) {
    IconButton(
        modifier = Modifier
            .clip(CircleShape)
            .size(40.dp)
            .background(backgroundColor),
        onClick = onClick,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp).semantics { contentDescription = imageVector.name }
        )
    }
}
