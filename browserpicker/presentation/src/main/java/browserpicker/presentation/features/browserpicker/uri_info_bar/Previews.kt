package browserpicker.presentation.features.browserpicker.uri_info_bar

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import browserpicker.domain.service.ParsedUri

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
