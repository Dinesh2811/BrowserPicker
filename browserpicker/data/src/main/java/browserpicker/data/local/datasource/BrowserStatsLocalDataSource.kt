package browserpicker.data.local.datasource

import browserpicker.core.di.InstantProvider
import browserpicker.data.local.dao.BrowserUsageStatDao
import browserpicker.data.local.entity.BrowserUsageStatEntity
import browserpicker.data.local.mapper.BrowserUsageStatMapper
import browserpicker.domain.model.BrowserUsageStat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface BrowserStatsLocalDataSource {
    suspend fun recordBrowserUsage(packageName: String)
    fun getBrowserStat(packageName: String): Flow<BrowserUsageStatEntity?>
    fun getAllBrowserStats(): Flow<List<BrowserUsageStatEntity>>
    fun getAllBrowserStatsSortedByLastUsed(): Flow<List<BrowserUsageStatEntity>>
    suspend fun deleteBrowserStat(packageName: String): Boolean
    suspend fun deleteAllStats(): Int
}

@Singleton
class BrowserStatsLocalDataSourceImpl @Inject constructor(
    private val browserUsageStatDao: BrowserUsageStatDao,
    private val instantProvider: InstantProvider,
): BrowserStatsLocalDataSource {

    override suspend fun recordBrowserUsage(packageName: String) {
        browserUsageStatDao.incrementUsage(packageName, instantProvider.now())
    }

    override fun getBrowserStat(packageName: String): Flow<BrowserUsageStatEntity?> {
        return browserUsageStatDao.getBrowserUsageStat(packageName)
    }

    override fun getAllBrowserStats(): Flow<List<BrowserUsageStatEntity>> {
        return browserUsageStatDao.getAllBrowserUsageStats()
    }

    override fun getAllBrowserStatsSortedByLastUsed(): Flow<List<BrowserUsageStatEntity>> {
        return browserUsageStatDao.getAllBrowserUsageStatsSortedByLastUsed()
    }

    override suspend fun deleteBrowserStat(packageName: String): Boolean {
        return browserUsageStatDao.deleteBrowserUsageStat(packageName) > 0
    }

    override suspend fun deleteAllStats(): Int {
        return browserUsageStatDao.deleteAllStats()
    }
}
