package browserpicker.presentation.features.browserpicker.uri_info_bar

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import browserpicker.domain.service.ParsedUri
import browserpicker.domain.service.ParsedUri.Companion.uriInfoBar
import browserpicker.presentation.features.browserpicker.uri_info_bar.components.ActionButtons
import browserpicker.presentation.features.browserpicker.uri_info_bar.components.SecurityIcon
import browserpicker.presentation.features.browserpicker.uri_info_bar.components.UriText
import browserpicker.presentation.features.common.components.MyIcon
import browserpicker.presentation.util.BrowserDefault
import browserpicker.presentation.util.helper.ClipboardHelper

@Composable
internal fun UriInfoBarContent(
    parsedUri: ParsedUri?,
    isBookmarked: Boolean?,
    isBlocked: Boolean?,
    showMoreMenu: Boolean,
    onEditClick: () -> Unit,
    onMoreClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onBlockClick: () -> Unit,
    onSecurityIconClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
//        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp)
                .height(IntrinsicSize.Min)
        ) {

            // Security Indicator and URI Text (takes available space)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp) // Weight and padding to avoid overlap
            ) {
                SecurityIcon(parsedUri = parsedUri, onSecurityIconClick = onSecurityIconClick)
                // Show Bookmark icon if bookmarked
                if (isBookmarked == true) {
                    Spacer(modifier = Modifier.width(8.dp))
                    MyIcon(
                        imageVector = Icons.Filled.Bookmark,
                        tint = MaterialTheme.colorScheme.primary,
                        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                        onClick = onBookmarkClick,
                    )
                }
                // Show Unblock icon if blocked
                if (isBlocked == true) {
                    Spacer(modifier = Modifier.width(8.dp))
                    MyIcon(
                        imageVector = Icons.Filled.Block,
                        tint = MaterialTheme.colorScheme.error,
                        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                        onClick = onBlockClick,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                UriText(parsedUri = parsedUri)
            }

            // Action Buttons (fixed size)
            ActionButtons(
                showMoreMenu = showMoreMenu,
                isBookmarked = isBookmarked,
                isBlocked = isBlocked,
                onEditClick = onEditClick,
                onMoreClick = onMoreClick,
                onDismissMenu = onDismissMenu,
                onCopyClick = onCopyClick,
                onShareClick = onShareClick,
                onBookmarkClick = onBookmarkClick,
                onBlockClick = onBlockClick,
            )

        }
    }
}
