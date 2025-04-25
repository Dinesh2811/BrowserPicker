package browserpicker.data.core.local.datasource

import android.content.ComponentName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import kotlinx.coroutines.Dispatchers
import androidx.core.net.toUri
import browserpicker.core.utils.LogLevel
import browserpicker.core.utils.log
import kotlin.apply
import kotlin.text.isEmpty
import kotlin.text.isNullOrEmpty

class BrowserDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "log_BrowserDataSource"
    }

    val packageManager: PackageManager by lazy { context.packageManager }
    val browserResolveInfoList: List<ResolveInfo> by lazy {
//        browserResolveInfoList.forEach {
//            val componentName = ComponentName("packageName", it.activityInfo.name)
//            val filter = IntentFilter(Intent.ACTION_VIEW).apply {
//                addCategory(Intent.CATEGORY_BROWSABLE)
//                addDataScheme("http")
//                addDataScheme("https")
//            }
//            context.packageManager.addPreferredActivity(filter, IntentFilter.MATCH_CATEGORY_SCHEME, arrayOf(componentName), componentName)
////            Toast.makeText(context, "Default browser set to ${appName}", Toast.LENGTH_SHORT).show()
//        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(createBrowserIntent(), PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
            } else {
                packageManager.queryIntentActivities(createBrowserIntent(), PackageManager.MATCH_ALL)
            }
        } catch (e: Exception) {
            LogLevel.Error.log("Error querying browser intents: ${e.message}", TAG)
            emptyList()
        }
    }

    fun createBrowserIntent(): Intent {
        return Intent(Intent.ACTION_VIEW, "https://".toUri()).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
    }

    fun getAppName(resolveInfo: ResolveInfo): String? {
        return try {
            resolveInfo.loadLabel(packageManager).toString()
        } catch (e: Exception) {
            LogLevel.Error.log("Error getting app name: ${e.message}", TAG)
            null
        }
    }

    fun getPackageName(activityInfo: ActivityInfo): String? {
        return try {
            activityInfo.packageName
        } catch (e: Exception) {
            LogLevel.Error.log("Error getting app name: ${e.message}", TAG)
            null
        }
    }

    fun getAppIcon(resolveInfo: ResolveInfo): Drawable? {
        return try {
            resolveInfo.loadIcon(packageManager)
        } catch (e: Exception) {
            LogLevel.Error.log("Unexpected exception: ${e.message}", TAG)
            null
        }
    }

    fun isDefaultBrowser(packageName: String?, flag: Int): Boolean {
        if (packageName.isNullOrEmpty()) return false
//        val resolveInfo = packageManager.resolveActivity(createBrowserIntent(), PackageManager.MATCH_DEFAULT_ONLY)
        val resolveInfo = packageManager.resolveActivity(createBrowserIntent(), flag)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    fun isBrowserApp(activityInfo: ActivityInfo): Boolean {
        return try {
            createBrowserIntent().apply { `package` = activityInfo.packageName }.resolveActivity(packageManager) != null &&
                    activityInfo.exported == true &&
                    activityInfo.permission == null || activityInfo.permission == "android.permission.INTERNET"
        } catch (e: Exception) {
            LogLevel.Error.log("Failed to check browser app: ${e.message}", TAG)
            false
        }
    }

    fun getPackageInfo(packageName: String, flags: Int = 0): PackageInfo? = try {
        when {
            packageName.isEmpty() -> null
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            else -> {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, flags)
            }
        }
    } catch (e: PackageManager.NameNotFoundException) {
        LogLevel.Error.log("Name not found: ${e.message}", TAG)
        null
    } catch (e: SecurityException) {
        LogLevel.Error.log("Security exception: ${e.message}", TAG)
        null
    } catch (e: Exception) {
        LogLevel.Error.log("Unexpected exception: ${e.message}", TAG)
        null
    }

    suspend fun setDefaultBrowser(packageName: String) = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val componentName = ComponentName(packageName, "$packageName.MainActivity")
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        // Additional logic to set the default browser (e.g., using RoleManager on Android 10+)
    }

//    fun setDefaultBrowser(context: Context, url: String, selectedApp: BrowserAppInfoEntity?) {
//        selectedApp ?: return
//        // Clear the previous default (if any)
//        val clearIntent = Intent(Intent.ACTION_VIEW, url.toUri())
//        val clearResolveList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            context.packageManager.queryIntentActivities(clearIntent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
//        } else {
//            context.packageManager.queryIntentActivities(clearIntent, PackageManager.MATCH_DEFAULT_ONLY)
//        }
//        for (resolveInfo in clearResolveList) {
//            val packageName = resolveInfo.activityInfo.packageName
//            if (packageName != selectedApp.packageName) {
//                val componentName = ComponentName(packageName, resolveInfo.activityInfo.name)
//                context.packageManager.clearPackagePreferredActivities(componentName.packageName)
//            }
//        }
//    }
}

/*

@Singleton
class BrowserDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "log_BrowserDataSource"
    }

    val packageManager: PackageManager by lazy { context.packageManager }
    val browserResolveInfoList: List<ResolveInfo> by lazy { packageManager.queryIntentActivities(createBrowserIntent(), PackageManager.MATCH_ALL) }

    fun createBrowserIntent(): Intent {
        browserResolveInfoList.forEach {
            createBrowserAppInfo(it)
        }
        return Intent(Intent.ACTION_VIEW, "https://".toUri()).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
    }

//    suspend fun getBrowserApps(): List<BrowserAppInfo> = withContext(Dispatchers.IO) {
//        packageManager.queryIntentActivities(createBrowserIntent(), PackageManager.MATCH_ALL)
//            .mapNotNull { resolveInfo -> createBrowserAppInfo(resolveInfo) }
//            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
//    }

    suspend fun createBrowserAppInfo(resolveInfo: ResolveInfo): BrowserAppInfo? = withContext(Dispatchers.IO) {
        val activityInfo = resolveInfo.activityInfo
        val packageName = activityInfo.packageName
        val packageInfo = getPackageInfo(packageName, flags = 0) // flags = PackageManager.GET_ACTIVITIES
        val appIcon = getAppIcon(packageName)
        val bitmap = appIcon?.let { convertDrawableToBitmap(it) }

        if (isBrowserApp(packageName, activityInfo)) {
            BrowserAppInfo(
                appName = resolveInfo.loadLabel(packageManager).toString(),
                packageName = packageName,
                versionName = packageInfo?.versionName,
                versionCode = packageInfo?.longVersionCode,
                isDefaultBrowser = isDefaultBrowser(packageName),
                appIcon = appIcon,
                bitmap = bitmap
            )
        } else {
            null
        }
    }

    fun getPackageInfo(packageName: String, flags: Int = 0): PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, flags)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Name not found: ${e.message}", e)
            null
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception: ${e.message}", e)
            null
        }
    }

    fun isBrowserApp(packageName: String, activityInfo: ActivityInfo? = null): Boolean {
        return try {
            createBrowserIntent().apply { `package` = packageName }.resolveActivity(packageManager) != null &&
            activityInfo?.exported == true &&
            (activityInfo.permission == null || activityInfo.permission == "android.permission.INTERNET")
        } catch (e: Exception) {
            Log.e(TAG, "Error checking browser app: ${e.message}", e)
            false
        }
    }

    private fun isDefaultBrowser(packageName: String, flag: Int = PackageManager.MATCH_DEFAULT_ONLY): Boolean {
        val resolveInfo = packageManager.resolveActivity(createBrowserIntent(), flag)
        return resolveInfo?.activityInfo?.packageName == packageName
    }

    fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Name not found: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception: ${e.message}", e)
            null
        }
    }

    private suspend fun convertDrawableToBitmap(drawable: Drawable): Bitmap? {
        return withContext(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
                bitmap
            }

            bitmap = try {
                createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight).apply {
                    val canvas = Canvas(this)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                }
            } catch (e: Exception) {
                Log.e("log_convertDrawableToBitmap", "Failed to create bitmap: $e")
                null
            }

            bitmap
        }
    }
}


 */