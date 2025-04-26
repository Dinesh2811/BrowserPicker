package browserpicker.data.core.local.repository

import android.content.pm.ResolveInfo
import browserpicker.core.utils.LogLevel
import browserpicker.core.utils.log
import browserpicker.data.core.local.datasource.BrowserDataSource
import browserpicker.data.core.local.mapper.BrowserAppMapper
import browserpicker.data.core.local.model.BrowserAppInfoEntity
import browserpicker.domain.BrowserAppInfo
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlin.collections.filterNotNull
import kotlin.collections.sortedWith

interface BrowserRepository {
    fun getBrowserApps(): Flow<List<BrowserAppInfo>>
}

class BrowserRepositoryImpl @Inject constructor(
    private val browserDataSource: BrowserDataSource,
    private val browserAppMapper: BrowserAppMapper,
): BrowserRepository {
    private val TAG = "log_BrowserRepositoryImpl"

    override fun getBrowserApps(): Flow<List<BrowserAppInfo>> = flow {
        try {
            val browserApps: List<BrowserAppInfo> = coroutineScope {
                browserDataSource.getResolveInfos()
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
            val packageName = browserDataSource.getPackageName(activityInfo)?: return null
            val appName = browserDataSource.getAppName(resolveInfo)?: return null
            browserAppMapper.mapToDomain(
                BrowserAppInfoEntity(
                    appName = appName,
                    packageName = packageName,
                )
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun isValidBrowserApp(resolveInfo: ResolveInfo): Boolean {
        return try {
            browserDataSource.isBrowserApp(resolveInfo.activityInfo) &&
                    resolveInfo.activityInfo.packageName != "com.dinesh.mybrowser"
        } catch (e: Exception) {
            false
        }
    }

    private fun createBrowserAppInfoEntity(resolveInfo: ResolveInfo): BrowserAppInfoEntity? {
        return try {
            val activityInfo = resolveInfo.activityInfo?: return null
            val packageName = browserDataSource.getPackageName(activityInfo)?: return null
            val appName = browserDataSource.getAppName(resolveInfo)?: return null
//            val appIcon = browserDataSource.getAppIcon(resolveInfo)
//            val isDefaultBrowser = browserDataSource.isDefaultBrowser(packageName, 0)

            BrowserAppInfoEntity(
                appName = appName,
                packageName = packageName,
//                appIcon = appIcon,
//                isDefaultBrowser = isDefaultBrowser
            )
        } catch (e: Exception) {
            null
        }
    }
}
