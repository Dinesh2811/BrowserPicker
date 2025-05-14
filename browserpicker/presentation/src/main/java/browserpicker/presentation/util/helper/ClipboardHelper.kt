package browserpicker.presentation.util.helper

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object ClipboardHelper {
    fun getText(context: Context): String? {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
        } catch (e : Exception) {
            e.printStackTrace()
            null
        }
    }

    fun hasText(context: Context): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.isNotEmpty() == true
        } catch (e : Exception) {
            e.printStackTrace()
            false
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String?, onCompletion: (Boolean) -> Unit = {}) {
        text?.toString()?.let { uriString ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, uriString)
            clipboard.setPrimaryClip(clip)
            onCompletion(true)
        }?: { onCompletion(false) }
    }
}