package browserpicker.data.core.local.datasource

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import browserpicker.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri
import browserpicker.core.utils.logError

interface PackageManagerDataSource {
    suspend fun getInstalledBrowserApps(): List<ResolveInfo>
    suspend fun launchBrowserActivity(packageName: String, uri: Uri)
}

@Singleton
class PackageManagerDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PackageManagerDataSource {

    companion object {
        private const val TAG = "log_BrowserDataSource"
    }

    private val packageManager: PackageManager by lazy { context.packageManager }
    private fun createBrowserIntent(): Intent = Intent(Intent.ACTION_VIEW, "https://".toUri()).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }

    override suspend fun getInstalledBrowserApps(): List<ResolveInfo> = withContext(ioDispatcher) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(createBrowserIntent(), PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
            } else {
                packageManager.queryIntentActivities(createBrowserIntent(), PackageManager.MATCH_ALL)
            }
        } catch (e: Exception) {
            logError("Error querying browser intents: ${e.message}", e)
            throw BrowserDataException("Failed to query installed browsers", e)
        }
    }

    override suspend fun launchBrowserActivity(packageName: String, uri: Uri) = withContext(ioDispatcher) {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(packageName) // Target the specific package
            // FLAG_ACTIVITY_NEW_TASK is crucial when starting an Activity from a non-Activity context (like a service or the application context)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            // Add other flags as needed, e.g., FLAG_ACTIVITY_CLEAR_TOP
        }

        try {
            // Optional but good practice: Check if the intent can be resolved *within the target package*
            // before attempting to start. This catches cases where setPackage() somehow fails resolution.
            if (intent.resolveActivity(packageManager) == null) {
                throw ActivityNotFoundException("No activity found for package $packageName to handle URI $uri")
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Map platform exceptions to data layer exceptions
            throw BrowserDataException("Activity not found for package $packageName", e)
        } catch (e: SecurityException) {
            // Handle potential security issues
            throw BrowserDataException("Security error launching package $packageName", e)
        } catch (e: Exception) {
            // Catch any other unexpected exceptions during launch
            throw BrowserDataException("Failed to launch package $packageName", e)
        }
    }
}

class BrowserDataException(message: String, cause: Throwable? = null): RuntimeException(message, cause)
