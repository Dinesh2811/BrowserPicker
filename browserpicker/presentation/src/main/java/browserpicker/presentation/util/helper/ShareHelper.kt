package browserpicker.presentation.util.helper

import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareHelper {
    fun shareUri(context: Context, uri: Uri?, subject: String = "Check this URL") {
        if (uri == null) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, uri.toString())
        }
        val chooser = Intent.createChooser(intent, "Share URL via")
        context.startActivity(chooser)
    }
}