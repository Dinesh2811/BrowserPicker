package browserpicker.data.local.datasource

import browserpicker.core.di.InstantProvider
import browserpicker.data.local.dao.BrowserUsageStatDao
import browserpicker.data.local.mapper.BrowserUsageStatMapper
import browserpicker.domain.model.BrowserUsageStat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface BrowserStatsLocalDataSource {
    suspend fun recordBrowserUsage(packageName: String)
    fun getBrowserStat(packageName: String): Flow<BrowserUsageStat?>
    fun getAllBrowserStats(): Flow<List<BrowserUsageStat>> // Default: sorted by count
    fun getAllBrowserStatsSortedByLastUsed(): Flow<List<BrowserUsageStat>>
    suspend fun deleteBrowserStat(packageName: String): Boolean
    suspend fun deleteAllStats()
}

@Singleton
class BrowserStatsLocalDataSourceImpl @Inject constructor(
    private val browserUsageStatDao: BrowserUsageStatDao,
    private val instantProvider: InstantProvider, // Inject InstantProvider
): BrowserStatsLocalDataSource {

    override suspend fun recordBrowserUsage(packageName: String) {
        // Use the DAO's transaction method which handles incrementing/inserting
        browserUsageStatDao.incrementUsage(packageName, instantProvider.now())
    }

    override fun getBrowserStat(packageName: String): Flow<BrowserUsageStat?> {
        return browserUsageStatDao.getBrowserUsageStat(packageName).map { entity ->
            entity?.let { BrowserUsageStatMapper.toDomainModel(it) }
        }
    }

    override fun getAllBrowserStats(): Flow<List<BrowserUsageStat>> {
        return browserUsageStatDao.getAllBrowserUsageStats().map { BrowserUsageStatMapper.toDomainModels(it) }
    }

    override fun getAllBrowserStatsSortedByLastUsed(): Flow<List<BrowserUsageStat>> {
        return browserUsageStatDao.getAllBrowserUsageStatsSortedByLastUsed().map { BrowserUsageStatMapper.toDomainModels(it) }
    }

    override suspend fun deleteBrowserStat(packageName: String): Boolean {
        return browserUsageStatDao.deleteBrowserUsageStat(packageName) > 0
    }

    override suspend fun deleteAllStats() {
        browserUsageStatDao.deleteAllStats()
    }
}
