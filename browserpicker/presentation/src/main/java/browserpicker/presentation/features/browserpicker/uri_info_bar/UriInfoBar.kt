package browserpicker.presentation.features.browserpicker.uri_info_bar

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import browserpicker.domain.service.ParsedUri
import browserpicker.domain.service.ParsedUri.Companion.uriInfoBar
import browserpicker.presentation.features.browserpicker.UriProcessingResult
import browserpicker.presentation.features.common.components.MyIcon
import browserpicker.presentation.util.BrowserDefault
import browserpicker.presentation.util.helper.ClipboardHelper
import browserpicker.presentation.util.helper.ShareHelper

@Composable
fun UriInfoBar(
    parsedUri: ParsedUri? = null,
    uriProcessingResult: UriProcessingResult? = null,
    onUriEdited: (Uri) -> Unit = {},
    onBookmarkUri: () -> Unit = {},
    onBlockUri: () -> Unit = {},
    onSecurityIconClick: () -> Unit = {},
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    val handleEditClick = remember {
        {
            showEditDialog = true
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val handleMoreClick = remember {
        {
            showMoreMenu = true
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val handleDismissMenu = remember { { showMoreMenu = false } }

    val handleCopyClick = remember(parsedUri?.originalUri, context, hapticFeedback) {
        {
            ClipboardHelper.copyToClipboard(
                context = context,
                label = "URL",
                text = parsedUri?.originalUri?.toString()?: BrowserDefault.URL
            ) {
                if (it && parsedUri?.originalUri?.toString().isNullOrEmpty()) Toast.makeText(context, "Default URL copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            showMoreMenu = false
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val handleShareClick = remember(parsedUri?.originalUri, context, hapticFeedback) {
        {
            ShareHelper.shareUri(context, parsedUri?.originalUri)
            showMoreMenu = false
            // No haptic on share, system usually provides feedback
        }
    }

    val handleBookmarkClick = remember(hapticFeedback) {
        {
            onBookmarkUri()
            showMoreMenu = false
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val handleBlockClick = remember(hapticFeedback) {
        {
            onBlockUri()
            showMoreMenu = false
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val handleDialogConfirm = remember {
        { editedUri: Uri ->
            onUriEdited(editedUri)
            showEditDialog = false
        }
    }

    val handleDialogDismiss = remember { { showEditDialog = false } }


    // --- UI Composition ---

    UriInfoBarContent(
        parsedUri = parsedUri,
        isBookmarked = uriProcessingResult?.isBookmarked,
        isBlocked = uriProcessingResult?.isBlocked,
        showMoreMenu = showMoreMenu,
        onEditClick = handleEditClick,
        onMoreClick = handleMoreClick,
        onDismissMenu = handleDismissMenu,
        onCopyClick = handleCopyClick,
        onShareClick = handleShareClick,
        onBookmarkClick = handleBookmarkClick,
        onBlockClick = handleBlockClick,
        onSecurityIconClick = onSecurityIconClick,
    )

    // --- Dialog ---
    if (showEditDialog) {
        EditUriDialog(
            uri = parsedUri?.originalUri, // Pass the original URI to edit
            onConfirm = handleDialogConfirm,
            onDismiss = handleDialogDismiss
        )
    }
}

fun shareUri(
    context: Context,
    uri: Uri,
    isSuccessful: ((Boolean, Exception?) -> Unit)? = null
) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, uri.toString())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(shareIntent, "Share via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooser)

        isSuccessful?.invoke(true, null)
    } catch (e: ActivityNotFoundException) {
        isSuccessful?.invoke(false, e) ?: Toast.makeText(context, "No app available to share the content", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        isSuccessful?.invoke(false, e) ?: Toast.makeText(context, "Failed to share content", Toast.LENGTH_SHORT).show()
    }
}

// --- Previews ---

@Preview(showBackground = true, name = "Secure URL")
@Composable
private fun UriInfoBarPreviewSecure() {
    var uri by remember { mutableStateOf("https://developer.android.com/jetpack/compose".toUri()) }
    MaterialTheme {
        UriInfoBar(
            parsedUri = ParsedUri(originalString = uri.toString(), originalUri = uri, scheme = uri.scheme!!, host = uri.host!!),
            onUriEdited = {
                uri = it
            },
            onBlockUri = {

            }
        )
    }
}

@Preview(showBackground = true, name = "Insecure URL")
@Composable
private fun UriInfoBarPreviewInsecure() {
    var uri by remember { mutableStateOf("http://example.com/some/path".toUri()) }
    MaterialTheme {
        UriInfoBar(
            parsedUri = ParsedUri(originalString = uri.toString(), originalUri = uri, scheme = uri.scheme!!, host = uri.host!!),
            onUriEdited = {},
            onBlockUri = {}
        )
    }
}

@Preview(showBackground = true, name = "No URL")
@Composable
private fun UriInfoBarPreviewNoUrl() {
    MaterialTheme {
        UriInfoBar(
            parsedUri = null,
            onUriEdited = {},
            onBlockUri = {}
        )
    }
}

@Preview(showBackground = true, name = "File URL")
@Composable
private fun UriInfoBarPreviewFileUrl() {
    var uri by remember { mutableStateOf("file:///storage/emulated/0/Download/document.pdf".toUri()) }
    MaterialTheme {
        UriInfoBar(
            parsedUri = ParsedUri(originalString = uri.toString(), originalUri = uri, scheme = uri.scheme!!, host = uri.host!!),
            onUriEdited = {},
            onBlockUri = {}
        )
    }
}
