package browserpicker.presentation.features.browserpicker.uri_info_bar

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import browserpicker.domain.service.ParsedUri
import browserpicker.presentation.features.browserpicker.UriProcessingResult
import browserpicker.presentation.features.browserpicker.uri_info_bar.components.ActionButtons
import browserpicker.presentation.features.browserpicker.uri_info_bar.components.EditUriDialog
import browserpicker.presentation.features.browserpicker.uri_info_bar.components.SecurityIcon
import browserpicker.presentation.features.browserpicker.uri_info_bar.components.UriText
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
                    .padding(end = 8.dp)
            ) {
                SecurityIcon(parsedUri = parsedUri, onSecurityIconClick = onSecurityIconClick)
                // Show Bookmark icon if bookmarked
                if (uriProcessingResult?.isBookmarked == true) {
                    Spacer(modifier = Modifier.width(8.dp))
                    MyIcon(
                        imageVector = Icons.Filled.Bookmark,
                        tint = MaterialTheme.colorScheme.primary,
                        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                        onClick = handleBookmarkClick,
                    )
                }
                // Show Unblock icon if blocked
                if (uriProcessingResult?.isBlocked == true) {
                    Spacer(modifier = Modifier.width(8.dp))
                    MyIcon(
                        imageVector = Icons.Filled.Block,
                        tint = MaterialTheme.colorScheme.error,
                        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                        onClick = handleBlockClick,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                UriText(parsedUri = parsedUri)
            }

            // Action Buttons (fixed size)
            ActionButtons(
                showMoreMenu = showMoreMenu,
                isBookmarked = uriProcessingResult?.isBookmarked,
                isBlocked = uriProcessingResult?.isBlocked,
                onEditClick = handleEditClick,
                onMoreClick = handleMoreClick,
                onDismissMenu = handleDismissMenu,
                onCopyClick = handleCopyClick,
                onShareClick = handleShareClick,
                onBookmarkClick = handleBookmarkClick,
                onBlockClick = handleBlockClick,
            )
        }
    }

    // --- Dialog ---
    if (showEditDialog) {
        EditUriDialog(
            uri = parsedUri?.originalUri,
            onConfirm = handleDialogConfirm,
            onDismiss = handleDialogDismiss
        )
    }
}
