package browserpicker.presentation.folders

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import browserpicker.domain.model.Folder

@Composable
fun FolderTreeItem(
    node: FolderTreeNode,
    // onFolderClick: (Folder) -> Unit, // Optional: Navigate into folder?
    onAddChildClick: (parentFolderId: Long) -> Unit,
    onEditClick: (Folder) -> Unit,
    onDeleteClick: (Folder) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // .clickable { onFolderClick(node.folder) } // Enable if clicking navigates
            .padding(
                start = (node.level * 16 + 16).dp, // Indentation based on level
                top = 8.dp,
                bottom = 8.dp,
                end = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Folder Name and Type
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = node.folder.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Optional: Show folder type or other info
            // Text(node.folder.type.name, style = MaterialTheme.typography.labelSmall)
        }

        // Actions
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Only allow adding children if it's a valid parent type (Bookmarks/Blocked)
            // Note: Default root folders might be handled differently (e.g. cannot add children to the "Blocked" folder if viewing Bookmark rules)
            // Ensure this logic matches the ViewModel/Domain layer's expectations.
            // For now, assume any folder can have children of the same type.
            IconButton(onClick = { onAddChildClick(node.folder.id) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "Add Child Folder")
            }
            IconButton(onClick = { onEditClick(node.folder) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Folder")
            }
            IconButton(onClick = { onDeleteClick(node.folder) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Folder")
            }
        }
    }
}
