package browserpicker.domain.repository

import browserpicker.domain.model.BrowserUsageStat
import kotlinx.coroutines.flow.Flow

interface BrowserStatsRepository {
    suspend fun recordBrowserUsage(packageName: String): Result<Unit>
    fun getBrowserStat(packageName: String): Flow<BrowserUsageStat?>
    fun getAllBrowserStats(): Flow<List<BrowserUsageStat>> // Default: sorted by count
    fun getAllBrowserStatsSortedByLastUsed(): Flow<List<BrowserUsageStat>>
    suspend fun deleteBrowserStat(packageName: String): Result<Unit>
    suspend fun deleteAllStats(): Result<Int>
}
