package browserpicker.presentation.util

import android.util.Patterns

object BrowserDefault {
    const val URL = "https://www.google.com/"

    fun isValidUrl(url: String): Boolean {
        return Patterns.WEB_URL.matcher(url.trim().toString()).matches()
    }
}
