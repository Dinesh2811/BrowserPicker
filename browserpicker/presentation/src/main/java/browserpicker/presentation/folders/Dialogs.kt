package browserpicker.presentation.folders

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import browserpicker.domain.model.FolderType

@Composable
fun AddFolderDialog(
    parentFolderId: Long?,
    type: FolderType,
    onConfirm: (name: String, parentFolderId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf(TextFieldValue("")) }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Create Folder (${type.name})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                TextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(folderName.text, parentFolderId) },
                        enabled = folderName.text.isNotBlank() // Enable button if name is not blank
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}


@Composable
fun EditFolderDialog(
    folder: browserpicker.domain.model.Folder, // Use domain model
    onConfirm: (folder: browserpicker.domain.model.Folder, newName: String, newParentId: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    // TODO: Reparenting UI would make this more complex (e.g., dropdown of parent folders)
    // For simplicity, only allow editing the name for now.
    var folderName by remember { mutableStateOf(TextFieldValue(folder.name)) }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Edit Folder",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                TextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder Name") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
                // TODO: Add Parent selection UI here if reparenting is enabled

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(folder, folderName.text, folder.parentFolderId) }, // Pass original parent ID for simplicity
                        enabled = folderName.text.isNotBlank() // Enable button if name is not blank
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// Optional: AlertDialog for delete confirmation
@Composable
fun DeleteConfirmationDialog(
    folderName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Deletion") },
        text = { Text("Are you sure you want to delete the folder \"$folderName\"? This cannot be undone. Any rules inside will be moved to the root.") },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
