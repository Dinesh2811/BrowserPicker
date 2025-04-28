package browserpicker.presentation.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import browserpicker.domain.model.Folder
import browserpicker.domain.model.HostRule

// Data needed specifically when the sheet is shown
// Managed by MainViewModel
data class PickerSheetContext(
    val uriString: String,
    val host: String,
    val sourceValue: Int, // Serialized UriSource value
    val associatedHostRuleId: Long?,
    val currentRule: HostRule?, // Optional existing rule
    val availableBookmarkFolders: List<Folder> = emptyList()
)

@Composable
fun BrowserPickerSheetContent(
    browsers: List<BrowserAppInfo>, // Provided by MainViewModel/Platform
    context: PickerSheetContext?, // Null if sheet shouldn't show picker content
    onBrowserSelected: (browser: BrowserAppInfo, context: PickerSheetContext) -> Unit,
    onSetPreferenceSelected: (browser: BrowserAppInfo, context: PickerSheetContext) -> Unit,
    onBookmarkSelected: (browser: BrowserAppInfo, folderId: Long?, context: PickerSheetContext) -> Unit,
    onBlockSelected: (context: PickerSheetContext) -> Unit,
    // Add other actions like 'Clear Preference' if needed
    modifier: Modifier = Modifier
) {
    if (context == null) {
        // Show empty or placeholder if context isn't set (sheet might be visible for other reasons)
        Box(modifier = modifier.fillMaxSize().padding(16.dp)) {
            Text("Select an action or browser", style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "Open \"${context.host}\"", // Show host for clarity
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = context.uriString,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // TODO: Add 'Block Host' Button
        Button(
            onClick = { onBlockSelected(context) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Block Host (${context.host})")
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Choose a browser:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (browsers.isEmpty()) {
            Text("No browsers found.") // Placeholder
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false) // Allow column to shrink if content is short
            ) {
                items(browsers, key = { it.packageName }) { browser ->
                    BrowserItem(
                        browser = browser,
                        onClick = { onBrowserSelected(browser, context) },
                        onSetPreference = { onSetPreferenceSelected(browser, context) }
                        // Add bookmark callback later if needed per-browser
                    )
                    Divider()
                }
            }
        }

        // TODO: Add Bookmark section (e.g., a button that opens a folder selection if needed)
        // Or add a "Bookmark & Open with..." option to each browser item? Simpler: global Bookmark button.
        Spacer(Modifier.height(16.dp))
        Button(
            // This needs refinement - how does user pick browser AND folder?
            // Maybe bookmark just bookmarks, doesn't open? Or opens with default/last used?
            // Simplification: Add a separate "Bookmark" button, maybe alongside "Block".
            onClick = { /* Decide how bookmark action works - maybe just saveHostRuleUseCase directly? */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Bookmark Host (Requires UI Refinement)") // Placeholder
        }
    }
}

@Composable
private fun BrowserItem(
    browser: BrowserAppInfo,
    onClick: () -> Unit,
    onSetPreference: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // Click row to open once
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // TODO: Add App Icon (using Accompanist or Coil/Glide)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = browser.appName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onSetPreference) {
            Text("Always") // Or "Set Default" / "Prefer"
        }
    }
}
