package browserpicker.domain.repository

import browserpicker.core.results.AppError
import browserpicker.core.results.MyResult
import browserpicker.domain.model.BrowserUsageStat
import kotlinx.coroutines.flow.Flow

interface BrowserStatsRepository {
    suspend fun recordBrowserUsage(packageName: String): MyResult<Unit, AppError>
    fun getBrowserStat(packageName: String): Flow<BrowserUsageStat?>
    fun getAllBrowserStats(): Flow<List<BrowserUsageStat>> // Default: sorted by count
    fun getAllBrowserStatsSortedByLastUsed(): Flow<List<BrowserUsageStat>>
    suspend fun deleteBrowserStat(packageName: String): MyResult<Unit, AppError>
    suspend fun deleteAllStats(): MyResult<Unit, AppError>
}

/*

interface BrowserStatsRepository {
    suspend fun recordBrowserUsage(packageName: String): Result<Unit>
    fun getBrowserStat(packageName: String): Flow<BrowserUsageStat?>
    fun getAllBrowserStats(): Flow<List<BrowserUsageStat>> // Default: sorted by count
    fun getAllBrowserStatsSortedByLastUsed(): Flow<List<BrowserUsageStat>>
    suspend fun deleteBrowserStat(packageName: String): Result<Unit>
    suspend fun deleteAllStats(): Result<Unit>
}

 */