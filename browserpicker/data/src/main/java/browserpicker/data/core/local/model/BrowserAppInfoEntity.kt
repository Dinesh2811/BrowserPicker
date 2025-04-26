package browserpicker.data.core.local.model

import androidx.annotation.Keep

@Keep
data class BrowserAppInfoEntity(
    val appName: String?,
    val packageName: String?,
)
