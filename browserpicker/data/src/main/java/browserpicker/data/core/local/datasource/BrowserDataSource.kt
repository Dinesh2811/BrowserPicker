package browserpicker.data.core.local.datasource

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.net.toUri
import browserpicker.core.utils.LogLevel
import browserpicker.core.utils.log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BrowserDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "log_BrowserDataSource"
    }

    val packageManager: PackageManager by lazy { context.packageManager }

    fun getResolveInfos(): List<ResolveInfo> {
        return try {
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

    fun isBrowserApp(activityInfo: ActivityInfo): Boolean {
//        val intentFilters = activityInfo.metaData
//        return intentFilters != null && intentFilters.containsKey("android.intent.category.BROWSABLE")

//        activityInfo.packageName?.isNotEmpty() == true &&
//                getPackageInfo(activityInfo.packageName, PackageManager.GET_PERMISSIONS)
//                    ?.requestedPermissions
//                    ?.contains(android.Manifest.permission.INTERNET) == true
        return try {
            createBrowserIntent().apply { `package` = activityInfo.packageName }.resolveActivity(packageManager) != null &&
                    activityInfo.exported == true &&
                    activityInfo.permission == null || activityInfo.permission == "android.permission.INTERNET"
        } catch (e: Exception) {
            LogLevel.Error.log("Failed to check browser app: ${e.message}", TAG)
            false
        }
    }
}
