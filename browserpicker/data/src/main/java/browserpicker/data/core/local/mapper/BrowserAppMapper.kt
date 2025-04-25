package browserpicker.data.core.local.mapper

import browserpicker.core.LogLevel
import browserpicker.core.log
import browserpicker.domain.BrowserAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import kotlin.collections.asSequence
import kotlin.collections.filterNotNull
import kotlin.collections.map
import kotlin.collections.mapNotNull
import kotlin.collections.sortedWith
import kotlin.let
import kotlin.run
import kotlin.sequences.map
import kotlin.sequences.toList
import kotlin.text.CASE_INSENSITIVE_ORDER

class BrowserAppMapper @Inject constructor(
    private val iconConverter: IconConverter,
) {
    private val TAG = "log_BrowserAppMapper"

    suspend fun mapToDomain(entity: BrowserAppInfoEntity): BrowserAppInfo? = coroutineScope {
        try {
            entity.run {
                val appIconBitmap = appIcon?.let { iconConverter.convertDrawableToBitmap(it, packageName) }
                BrowserAppInfo(
                    packageName = packageName ?: return@coroutineScope null,
                    appName = appName ?: return@coroutineScope null,
                    appIcon = appIconBitmap,
                    isDefaultBrowser = isDefaultBrowser
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
            appIcon = null,
            isDefaultBrowser = domain.isDefaultBrowser
        )
    }

    private fun mapToEntity(domains: List<BrowserAppInfo>): List<BrowserAppInfoEntity> {
        return domains.asSequence()
            .map { mapToEntity(it) }
            .toList()
    }
}
