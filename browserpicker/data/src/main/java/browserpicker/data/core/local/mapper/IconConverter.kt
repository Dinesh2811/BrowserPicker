package browserpicker.data.core.local.mapper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.graphics.createBitmap
import browserpicker.core.utils.LogLevel
import browserpicker.core.utils.log
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import kotlin.apply
import kotlin.let


class IconConverter @Inject constructor() {
    companion object {
        private const val TAG = "log_IconConverter"
        private const val CACHE_SIZE = 4 * 1024 * 1024 // 4MB cache
    }

    //    private val bitmapCache = LruCache<String, Bitmap>(CACHE_SIZE)
    private val bitmapCache = object : LruCache<String, Bitmap>(15) { // Limit to 50 entries
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024 // Approximate size in KB
        }
    }

    suspend fun convertDrawableToBitmap(drawable: Drawable?, packageName: String?): Bitmap? {
        when {
            drawable is BitmapDrawable -> return drawable.bitmap
            drawable == null || packageName == null -> return null
            drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0 -> return null
            else -> {
                // Check cache first
                val cacheKey = "$packageName-${drawable.hashCode()}"
                bitmapCache.get(cacheKey)?.let { return it }

                return coroutineScope {
                    try {
                        val bitmap = createBitmap(
                            width = drawable.intrinsicWidth,
                            height = drawable.intrinsicHeight
                        ).apply {
                            val canvas = Canvas(this)
                            drawable.setBounds(0, 0, canvas.width, canvas.height)
                            drawable.draw(canvas)
                        }
                        bitmapCache.put(cacheKey, bitmap)
                        bitmap
                    } catch (e: Exception) {
                        LogLevel.Error.log("Failed to create bitmap: $e", TAG)
                        null
                    }
                }
            }
        }
    }

    // Clear cache if needed (e.g., on configuration change or memory pressure)
    fun clearCache() {
        bitmapCache.evictAll()
    }
}