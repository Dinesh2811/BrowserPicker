package browserpicker.presentation.features.browserpicker

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
import androidx.lifecycle.ViewModel
import browserpicker.core.di.IoDispatcher
import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.core.utils.LogLevel
import browserpicker.core.utils.log
import browserpicker.domain.model.BrowserAppInfo
import browserpicker.presentation.util.BrowserDefault
import dagger.Binds
import dagger.Module
import dagger.hilt.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import javax.inject.*
import kotlin.collections.map

@OptIn(FlowPreview::class)
@HiltViewModel
class BrowserPickerViewModel @Inject constructor(
    private val getInstalledBrowserAppsUseCase: GetInstalledBrowserAppsUseCase,
//    private val setDefaultBrowserUseCase: SetDefaultBrowserUseCase,
//    private val launchBrowserUseCase: LaunchBrowserUseCase,
//    private val getUriSecurityInfoUseCase: GetUriSecurityInfoUseCase,
//    private val appRepository: AppRepository,
    private val clock: Clock,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
): ViewModel() {

}

class GetInstalledBrowserAppsUseCase @Inject constructor(
    private val browserPickerRepository: BrowserPickerRepository,
) {
    operator fun invoke(): Flow<List<BrowserAppInfo>> {
        return browserPickerRepository.getBrowserApps()
            .map { browserList ->
                browserList.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
            }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BrowserPickerModule {
    @Binds
    @Singleton
    abstract fun bindBrowserPickerRepository(browserRepositoryImpl: BrowserPickerRepositoryImpl): BrowserPickerRepository
}

interface BrowserPickerRepository {
    fun getBrowserApps(): Flow<List<BrowserAppInfo>>
}

class BrowserPickerRepositoryImpl @Inject constructor(
    private val browserPickerDataSource: BrowserPickerDataSource,
    private val browserPickerAppMapper: BrowserPickerAppMapper,
): BrowserPickerRepository {
    private val TAG = "log_BrowserRepositoryImpl"

    override fun getBrowserApps(): Flow<List<BrowserAppInfo>> = flow {
        try {
            val browserApps: List<BrowserAppInfo> = coroutineScope {
                browserPickerDataSource.browserResolveInfoList
                    .asSequence()
                    .filter { isValidBrowserApp(it) }
                    .map { resolveInfo ->
                        async(Dispatchers.Default) { createBrowserAppInfo(resolveInfo) }
                    }
                    .toList()
                    .awaitAll()
                    .filterNotNull()
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
            }

            emit(browserApps)
        } catch (e: Exception) {
            LogLevel.Error.log("Error fetching browser apps: ${e.message}", TAG)
            emit(emptyList<BrowserAppInfo>())
        }
    }.flowOn(Dispatchers.Default)

    private suspend fun createBrowserAppInfo(resolveInfo: ResolveInfo): BrowserAppInfo? {
        return try {
            val activityInfo = resolveInfo.activityInfo?: return null
            val packageName = browserPickerDataSource.getPackageName(activityInfo)?: return null
            val appName = browserPickerDataSource.getAppName(resolveInfo)?: return null
            val appIcon = browserPickerDataSource.getAppIcon(resolveInfo)
            val isDefaultBrowser = browserPickerDataSource.isDefaultBrowser(packageName, 0)

            browserPickerAppMapper.mapToDomain(
                BrowserAppInfoEntity(
                    appName = appName,
                    packageName = packageName,
                    isDefaultBrowser = isDefaultBrowser
                )
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun isValidBrowserApp(resolveInfo: ResolveInfo): Boolean {
        return try {
            browserPickerDataSource.isBrowserApp(resolveInfo.activityInfo) &&
                    resolveInfo.activityInfo.packageName != BrowserDefault.CURRENT_PACKAGE
        } catch (e: Exception) {
            false
        }
    }

    private fun createBrowserAppInfoEntity(resolveInfo: ResolveInfo): BrowserAppInfoEntity? {
        return try {
            val activityInfo = resolveInfo.activityInfo?: return null
            val packageName = browserPickerDataSource.getPackageName(activityInfo)?: return null
            val appName = browserPickerDataSource.getAppName(resolveInfo)?: return null
            val appIcon = browserPickerDataSource.getAppIcon(resolveInfo)
            val isDefaultBrowser = browserPickerDataSource.isDefaultBrowser(packageName, 0)

            BrowserAppInfoEntity(
                appName = appName,
                packageName = packageName,
                isDefaultBrowser = isDefaultBrowser
            )
        } catch (e: Exception) {
            null
        }
    }
}

class BrowserPickerDataSource @Inject constructor(
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

data class BrowserAppInfoEntity(
    val appName: String?,
    val packageName: String?,
    val isDefaultBrowser: Boolean = false,
)

class BrowserPickerAppMapper @Inject constructor() {
    private val TAG = "log_BrowserPickerAppMapper"

    suspend fun mapToDomain(entity: BrowserAppInfoEntity): BrowserAppInfo? = coroutineScope {
        try {
            entity.run {
                BrowserAppInfo(
                    packageName = packageName?: return@coroutineScope null,
                    appName = appName?: return@coroutineScope null,
                )
            }
        } catch (e: Exception) {
            LogLevel.Error.log("Error mapping entity: ${e.message}", TAG)
            null
        }
    }

    suspend fun mapToDomain(entities: List<BrowserAppInfoEntity>): List<BrowserAppInfo> = coroutineScope {
        if (entities.isEmpty()) {
            return@coroutineScope emptyList()
        }

        try {
            val mappedList = try {
                if (entities.size > 10) {
                    // Parallel processing for large lists
                    entities.map { entity ->
                        async(Dispatchers.Default) { mapToDomain(entity) }
                    }.awaitAll().filterNotNull()
                } else {
                    // Sequential processing for small lists
                    entities.mapNotNull { entity -> mapToDomain(entity) }
                }
            } catch (e: Exception) {
                LogLevel.Error.log("Error during parallel mapping: ${e.message}", TAG)
                emptyList<BrowserAppInfo>()
            }

            mappedList.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })
        } catch (e: Exception) {
            LogLevel.Error.log("Error mapping entities: ${e.message}", TAG)
            emptyList()
        }
    }

    private fun mapToEntity(domain: BrowserAppInfo): BrowserAppInfoEntity {
        return BrowserAppInfoEntity(
            appName = domain.appName,
            packageName = domain.packageName,
        )
    }

    private fun mapToEntity(domains: List<BrowserAppInfo>): List<BrowserAppInfoEntity> {
        return domains.asSequence()
            .map { mapToEntity(it) }
            .toList()
    }
}
