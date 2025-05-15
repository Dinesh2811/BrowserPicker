package browserpicker.presentation.features.browserpicker.uri_info_bar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import browserpicker.presentation.features.common.components.MyIcon

@Composable
internal fun ActionButtons(
    showMoreMenu: Boolean,
    isBookmarked: Boolean?,
    isBlocked: Boolean?,
    onEditClick: () -> Unit,
    onMoreClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onBlockClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Edit Icon
        MyIcon(
            imageVector = Icons.Filled.Edit,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
            onClick = onEditClick
        )

        // More Menu Anchor Box
        Box {
            IconButton(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(40.dp)
                    .semantics { contentDescription = "More options" },
                onClick = onMoreClick,
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Dropdown Menu
            DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = onDismissMenu
            ) {
                DropdownMenuItem(
                    text = { Text("Copy URL") },
                    onClick = onCopyClick,
                    leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Share URL") },
                    onClick = onShareClick,
                    leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text(if (isBookmarked == true) "Remove Bookmark" else "Bookmark URL") },
                    onClick = onBookmarkClick,
                    leadingIcon = {
                        Icon(
                            if (isBookmarked == true) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = null
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(
                    text = { Text(if (isBlocked == true) "Unblock URL" else "Block URL") },
                    onClick = onBlockClick,
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Block,
                            contentDescription = null,
                            tint = if (isBlocked == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    },
                    colors = if (isBlocked == true)
                        MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.primary)
                    else
                        MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.error)
                )
            }
        }
    }
}
