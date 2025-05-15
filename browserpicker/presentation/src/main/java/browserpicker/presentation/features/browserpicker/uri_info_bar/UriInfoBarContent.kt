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
//                    val uriToShare = when (label) {
//                        "URL" -> parsedUri?.originalUri?.toString()?: BrowserDefault.URL
//                        else -> (toValidWebUri((parsedUri?.host?: BrowserDefault.URL.toUri().host).toString()))?.toString()?: BrowserDefault.URL
//                    }

                    shareUri(
                        context = context,
                        uri = (parsedUri?.originalUri?.toString()?: BrowserDefault.URL).toUri()
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
