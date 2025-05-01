package browserpicker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import browserpicker.data.local.entity.BrowserUsageStatEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface BrowserUsageStatDao {
    @Upsert
    suspend fun upsertBrowserUsageStat(stat: BrowserUsageStatEntity): Long

    @Query("SELECT * FROM browser_usage_stats WHERE browser_package_name = :packageName")
    fun getBrowserUsageStat(packageName: String): Flow<BrowserUsageStatEntity?>

    @Query("SELECT * FROM browser_usage_stats ORDER BY usage_count DESC, last_used_timestamp DESC")
    fun getAllBrowserUsageStats(): Flow<List<BrowserUsageStatEntity>>

    @Query("SELECT * FROM browser_usage_stats ORDER BY last_used_timestamp DESC")
    fun getAllBrowserUsageStatsSortedByLastUsed(): Flow<List<BrowserUsageStatEntity>>

    // Transaction to ensure atomic update when incrementing count
    @Transaction
    suspend fun incrementUsage(packageName: String, timestamp: Instant) {
        val currentStat = getBrowserUsageStatBlocking(packageName)
        val newStat = currentStat?.copy(
            usageCount = currentStat.usageCount + 1,
            lastUsedTimestamp = timestamp
        ) ?: BrowserUsageStatEntity(
            browserPackageName = packageName,
            usageCount = 1,
            lastUsedTimestamp = timestamp
        )
        upsertBrowserUsageStat(newStat)
    }

    // Non-Flow version for use within transactions
    @Query("SELECT * FROM browser_usage_stats WHERE browser_package_name = :packageName")
    suspend fun getBrowserUsageStatBlocking(packageName: String): BrowserUsageStatEntity?

    @Query("DELETE FROM browser_usage_stats WHERE browser_package_name = :packageName")
    suspend fun deleteBrowserUsageStat(packageName: String): Int

    @Query("DELETE FROM browser_usage_stats")
    suspend fun deleteAllStats(): Int
}