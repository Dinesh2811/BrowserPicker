package browserpicker.presentation.features.browserpicker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import android.content.ActivityNotFoundException
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.isPopupLayout
import androidx.core.content.ContextCompat
import browserpicker.domain.service.ParsedUri
import browserpicker.domain.service.ParsedUri.Companion.isSecure
import browserpicker.domain.service.ParsedUri.Companion.uriInfoBar
import browserpicker.presentation.features.common.components.MyIcon
import browserpicker.presentation.util.BrowserDefault
import browserpicker.presentation.util.helper.ClipboardHelper
import browserpicker.presentation.util.helper.ShareHelper

@Immutable
data class UriDisplayInfo(
    val uri: Uri? = null,
    val host: String? = null,
    val displayText: String? = "No URL",
    val isSecure: Boolean = false,
    val securityIcon: ImageVector = Icons.AutoMirrored.Filled.HelpOutline,
    val securityDescription: String = "URL status unknown",
//    val securityTint: Color
)

// --- Composable Functions ---

@Composable
fun UriInfoBar(
    parsedUri: ParsedUri? = null,
    uriProcessingResult: UriProcessingResult? = null,
    onUriEdited: (Uri) -> Unit = {},
    onBookmarkUri: () -> Unit = {},
    onBlockUri: () -> Unit = {},
    onSecurityIconClick: () -> Unit = {},
) {
    // State for controlling UI elements visibility
    var showEditDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Local dependencies
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
//    val securityTint = MaterialTheme.colorScheme.onSurface

    // Calculate display info only when URI changes
    val displayInfo = remember(parsedUri?.originalUri) {
        val host = parsedUri?.originalUri?.host?.takeIf { it.isNotEmpty() }
        val path = parsedUri?.originalUri?.path?.takeIf { it.isNotEmpty() && it != "/" }
        val scheme = parsedUri?.originalUri?.scheme?.lowercase()

        val text = host?.let { h -> path?.let { p -> "$h$p" } ?: h } // Combine host and path if available
            ?: parsedUri?.originalUri?.toString()

        val isSecure = scheme == "https"
        val (icon, desc) = when {
            parsedUri?.originalUri == null -> Icons.AutoMirrored.Filled.HelpOutline to "No URL provided"
            isSecure -> Icons.Filled.Lock to "Secure connection (HTTPS)"
            scheme == "http" -> Icons.Filled.Warning to "Insecure connection (HTTP)"
            else -> Icons.Filled.Link to "Connection type unknown" // For other schemes like ftp, file etc.
        }
        UriDisplayInfo(
            uri = parsedUri?.originalUri,
            host = parsedUri?.host,
            displayText = text,
            isSecure = parsedUri.isSecure,
            securityIcon = icon,
            securityDescription = desc,
//            securityTint = securityTint
        )
    }

    // --- Stable Lambdas for Actions ---

    val handleEditClick = remember {
        {
            showEditDialog = true
            hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        }
    }

    val handleMoreClick = remember {
        {
            showMoreMenu = true
            hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        }
    }

    val handleDismissMenu = remember { { showMoreMenu = false } }

    val handleCopyClick = remember(parsedUri?.originalUri, context, hapticFeedback) {
        {
            ClipboardHelper.copyToClipboard(
                context = context,
                label = "URL",
                text = displayInfo.uri?.toString()?: BrowserDefault.URL
            ) {
                if (it && displayInfo.uri?.toString().isNullOrEmpty()) Toast.makeText(context, "Default URL copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            showMoreMenu = false
            hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
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
            hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        }
    }

    val handleBlockClick = remember(hapticFeedback) {
        {
            onBlockUri()
            showMoreMenu = false
            hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
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

@Composable
private fun UriInfoBarContent(
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

@Composable
private fun SecurityIcon(
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

@Composable
private fun RowScope.UriText(parsedUri: ParsedUri?) {
    val context = LocalContext.current
    val displayText = parsedUri?.host?.let { h -> parsedUri.originalUri.path?.let { p -> "$h$p" }?: h }
    Column(modifier = Modifier.weight(1f)) {
//        SelectionContainer {}

        if(parsedUri?.host == displayText) {
            UrlText(
                modifier = Modifier
//                    .pointerInput(Unit) { // Use Unit as key since the gesture logic doesn't depend on external state
//                        detectTapGestures(
//                            onLongPress = {
//                                shareUri(
//                                    context = context,
//                                    uri = (displayInfo.displayText?: BrowserDefault.URL).toUri(),
//                                    isSuccessful = null
//                                )
//                            }
//                            // You can also define onTap, onDoubleTap, onPress here if needed
//                        )
//                    }
                ,
                parsedUri = parsedUri,
                text = displayText?: "No URL",
                label = "URL",
                context = context,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = if (displayText.isNullOrEmpty()) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge
            )
        } else {
            UrlText(
                parsedUri = parsedUri,
                text = parsedUri?.host?: "Unknown Host",
                label = "Host URL",
                context = context,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,   //  bodyMedium
            )

            UrlText(
                parsedUri = parsedUri,
                text = displayText?: "No URL",
                label = "URL",
                context = context,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall,
            )
        }

    }
}

@Composable
private fun ColumnScope.UrlText(
    modifier: Modifier = Modifier,
    parsedUri: ParsedUri?,
    text: String,
    label: String,
    context: Context,
    color: Color,
    style: TextStyle,
) {
    val scrollState = rememberScrollState()
//    val density = LocalDensity.current
//    val textWidthPx = with(density) { style.fontSize.toPx() * text.length * 0.6f } // Approximate text width
//    val containerWidthPx = with(density) { 200.dp.toPx() } // Example container width; adjust as needed
//
//    // Only enable scrolling if the text is wider than the container
//    val shouldScroll = textWidthPx > containerWidthPx
//
//    // Infinite transition for smooth scrolling animation
//    val infiniteTransition = rememberInfiniteTransition(label = "AutoScrollTransition")
//    val scrollOffset by infiniteTransition.animateFloat(
//        initialValue = 0f,
//        targetValue = if (shouldScroll) abs(textWidthPx - containerWidthPx) else 0f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(durationMillis = 5000, delayMillis = 0, easing = LinearEasing),
//            repeatMode = RepeatMode.Restart // Scroll back and forth
//        ),
//        label = "AutoScrollAnimation"
//    )
//
//    // Apply the scroll offset when the animation is running
//    LaunchedEffect(scrollOffset) {
//        if (shouldScroll) {
//            scrollState.scrollTo(scrollOffset.toInt())
//        }
//    }
    Text(
        text = text,
        style = style,
        color = color,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        softWrap = false,
        modifier = modifier
            .weight(1f, fill = false)
            .padding(end = 4.dp)
            .horizontalScroll(scrollState, enabled = label == "URL")
            .semantics { contentDescription = "Displayed URL: $text" }
            .combinedClickable(
                onClick = {
                    val textToCopy = when (label) {
                        "URL" -> parsedUri?.originalUri?.toString()?: BrowserDefault.URL
                        else -> parsedUri?.host?: BrowserDefault.URL.toUri().host?: "www.google.com"
                    }

                    ClipboardHelper.copyToClipboard(
                        context = context,
                        label = label,
                        text = textToCopy
                    ) {
                        if (it && textToCopy.isEmpty()) {
                            Toast.makeText(context, "Default URL copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onLongClick = {
                    val uriToShare = when (label) {
                        "URL" -> parsedUri?.originalUri?.toString()?: BrowserDefault.URL
                        else -> (toValidWebUri((parsedUri?.host?: BrowserDefault.URL.toUri().host).toString()))?.toString()?: BrowserDefault.URL
                    }

                    shareUri(
                        context = context,
                        uri = uriToShare.toUri()
                    )
                }
            ),
    )

    /*

        Text(
            text = displayInfo.host?: "Unknown Host",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(end = 4.dp)
                .semantics { contentDescription = "Displayed URL: ${displayInfo.host}" }
                .clickable(onClick = {
                    ClipboardHelper.copyToClipboard(
                        context = context,
                        label = "Host URL",
                        text = displayInfo.host ?: BrowserDefault.URL.toUri().host?.takeIf { it.isNotEmpty() }?: "www.google.com"
                    ) {
                        if (it && displayInfo.host.isNullOrEmpty()) Toast.makeText(context, "Default URL copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                }),
        )

        Text(
            text = displayInfo.displayText?.toString()?: "No URL",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(end = 4.dp)
                .semantics { contentDescription = "Displayed URL: ${displayInfo.uri?.toString()}" }
                .clickable(onClick = {
                    ClipboardHelper.copyToClipboard(
                        context = context,
                        label = "URL",
                        text = displayInfo.uri?.toString()?: BrowserDefault.URL
                    ) {
                        if (it && displayInfo.uri?.toString().isNullOrEmpty()) Toast.makeText(context, "Default URL copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                }),
        )

     */
}

@Composable
private fun ActionButtons(
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

fun toValidWebUri(uriString: String?): Uri? {
    if (uriString.isNullOrEmpty()) return null

    val trimmed = uriString.trim()
    var normalized = trimmed

    // If the input string doesn't contain "://", assume it's missing the scheme.
    if (!trimmed.contains("://")) {
        normalized = "https://$trimmed"
        // Ensure there's a "/" after the authority so that the parser correctly extracts the host.
        val schemeDelimiter = "://"
        val index = normalized.indexOf(schemeDelimiter)
        if (index != -1) {
            val afterScheme = normalized.substring(index + schemeDelimiter.length)
            if (!afterScheme.contains("/")) {
                normalized += "/"
            }
        }
    }

    // Parse the normalized string.
    val uri = normalized.toUri()
    val scheme = uri.scheme?.lowercase() ?: return null

    // Only allow http or https schemes.
    return if ((scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()) {
        uri
    } else {
        null
    }
}
