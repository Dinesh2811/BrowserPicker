package browserpicker.presentation.features.browserpicker.uri_info_bar.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import browserpicker.presentation.features.browserpicker.uri_info_bar.shareUri
import browserpicker.presentation.util.BrowserDefault
import browserpicker.presentation.util.helper.ClipboardHelper

@Composable
internal fun RowScope.UriText(parsedUri: ParsedUri?) {
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
