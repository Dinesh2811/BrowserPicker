package browserpicker.data.core.local.mapper

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import kotlin.random.Random

data class BrowserAppInfoEntity(
    val appName: String?,
    val packageName: String?,
    val appIcon: Drawable?,
    val isDefaultBrowser: Boolean = false,
//    val versionName: String?,
//    val versionCode: Long?,
)
