package browserpicker.domain.model

import androidx.compose.runtime.Immutable


@Immutable
data class BrowserAppInfo(
    val appName: String,
    val packageName: String,
)