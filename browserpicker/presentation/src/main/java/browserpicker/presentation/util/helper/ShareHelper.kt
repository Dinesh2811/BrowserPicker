package browserpicker.presentation.util.helper

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object ShareHelper {
    fun shareUri(
        context: Context,
        uri: Uri?,
        subject: String = "Check this URL",
        isSuccessful: ((Boolean, Exception?) -> Unit)? = null,
    ) {
        if (uri == null) {
            isSuccessful?.invoke(false, null) ?: Toast.makeText(context, "Invalid URL", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, uri.toString())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share URL via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)

            isSuccessful?.invoke(true, null) ?: Toast.makeText(context, "URL shared successfully", Toast.LENGTH_SHORT).show()
        } catch (e: ActivityNotFoundException) {
            isSuccessful?.invoke(false, e) ?: Toast.makeText(context, "No app available to share the content", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            isSuccessful?.invoke(false, e) ?: Toast.makeText(context, "Failed to share content", Toast.LENGTH_SHORT).show()
        }
    }
}