package browserpicker.playground.browserpicker.data

import androidx.paging.*
import androidx.room.*
import androidx.sqlite.db.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*

@Dao
interface UriRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUriRecord(uriRecord: UriRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUriRecords(uriRecords: List<UriRecordEntity>)

    @Upsert
    suspend fun upsertUriRecord(uriRecord: UriRecordEntity)

    @RawQuery(observedEntities = [UriRecordEntity::class])
    fun getPagedUriRecords(query: SupportSQLiteQuery): PagingSource<Int, UriRecordEntity>

    // For Total Count
    @RawQuery(observedEntities = [UriRecordEntity::class])
    fun getTotalUriRecordCount(query: SupportSQLiteQuery): Flow<Int>

    // For Grouping Counts (Generic)
    @RawQuery(observedEntities = [UriRecordEntity::class])
    fun getGroupCounts(query: SupportSQLiteQuery): Flow<List<GroupCount>>

    // For Grouping Counts by Date specifically
    @RawQuery(observedEntities = [UriRecordEntity::class])
    fun getDateCounts(query: SupportSQLiteQuery): Flow<List<DateCount>>

    @Query("SELECT * FROM uri_records WHERE uri_record_id = :id")
    suspend fun getUriRecordById(id: Long): UriRecordEntity?

    @Query("DELETE FROM uri_records WHERE uri_record_id = :id")
    suspend fun deleteUriRecordById(id: Long): Int

    // Potentially useful for filters/suggestions
    @Query("SELECT DISTINCT host FROM uri_records ORDER BY host ASC")
    fun getDistinctHosts(): Flow<List<String>>

    // Potentially useful for filters/suggestions (including null)
    @Query("SELECT DISTINCT chosen_browser_package FROM uri_records ORDER BY chosen_browser_package ASC")
    fun getDistinctChosenBrowsers(): Flow<List<String?>>

    @Query("DELETE FROM uri_records")
    suspend fun deleteAllUriRecords()

    @Query("SELECT COUNT(*) FROM uri_records")
    suspend fun count(): Int
}

@Dao
interface HostRuleDao {
    @Upsert
    suspend fun upsertHostRule(hostRule: HostRuleEntity): Long

    @Query("SELECT * FROM host_rules WHERE host_rule_id = :id")
    suspend fun getHostRuleById(id: Long): HostRuleEntity?

    @Query("SELECT * FROM host_rules WHERE host = :host LIMIT 1")
    fun getHostRuleByHost(host: String): Flow<HostRuleEntity?>

    @Query("SELECT * FROM host_rules ORDER BY updated_at DESC")
    fun getAllHostRules(): Flow<List<HostRuleEntity>>

    @Query("SELECT * FROM host_rules WHERE uri_status = :status ORDER BY host ASC")
    fun getHostRulesByStatus(status: UriStatus): Flow<List<HostRuleEntity>>

    @Query("SELECT * FROM host_rules WHERE folder_id = :folderId ORDER BY host ASC")
    fun getHostRulesByFolderId(folderId: Long): Flow<List<HostRuleEntity>>

    // For root bookmarked/blocked rules (folderId is null)
    @Query("SELECT * FROM host_rules WHERE folder_id IS NULL AND uri_status = :status ORDER BY host ASC")
    fun getRootHostRulesByStatus(status: UriStatus): Flow<List<HostRuleEntity>>

    // Note: More complex updates (like ensuring folder type matches status) should happen
    // in the Repository/Use Case layer before calling upsert.
    // Direct updates here bypass that logic. Use upsert for most cases.

    @Query("UPDATE host_rules SET folder_id = NULL WHERE folder_id = :folderId")
    suspend fun clearFolderIdForRules(folderId: Long)

    @Query("DELETE FROM host_rules WHERE host_rule_id = :id")
    suspend fun deleteHostRuleById(id: Long): Int

    @Query("DELETE FROM host_rules WHERE host = :host")
    suspend fun deleteHostRuleByHost(host: String): Int

    // Potentially useful
    @Query("SELECT DISTINCT host FROM host_rules ORDER BY host ASC")
    fun getDistinctRuleHosts(): Flow<List<String>>
}

@Dao
interface FolderDao {
    @Upsert
    suspend fun upsertFolder(folder: FolderEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFoldersIgnoreConflict(folders: List<FolderEntity>): List<Long>

    @Query("SELECT * FROM folders WHERE folder_id = :id")
    fun getFolderById(id: Long): Flow<FolderEntity?>

    // Get direct children of a folder (parentFolderId matches)
    @Query("SELECT * FROM folders WHERE parent_folder_id = :parentFolderId ORDER BY name ASC")
    fun getChildFolders(parentFolderId: Long): Flow<List<FolderEntity>>

    // Get root folders (parentFolderId is NULL) of a specific type
    @Query("SELECT * FROM folders WHERE parent_folder_id IS NULL AND folder_type = :folderType ORDER BY name ASC")
    fun getRootFoldersByType(folderType: FolderType): Flow<List<FolderEntity>>

    // Get all folders of a specific type
    @Query("SELECT * FROM folders WHERE folder_type = :folderType ORDER BY name ASC")
    fun getAllFoldersByType(folderType: FolderType): Flow<List<FolderEntity>>

    // Check for uniqueness constraint: name + parent + type
    @Query("SELECT * FROM folders WHERE name = :name AND parent_folder_id = :parentFolderId AND folder_type = :folderType LIMIT 1")
    suspend fun findFolderByNameAndParent(name: String, parentFolderId: Long?, folderType: FolderType): FolderEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM folders WHERE parent_folder_id = :folderId LIMIT 1)")
    suspend fun hasChildFolders(folderId: Long): Boolean

    @Update
    suspend fun updateFolder(folder: FolderEntity): Int

    @Query("DELETE FROM folders WHERE folder_id = :id")
    suspend fun deleteFolderById(id: Long): Int

    // Get all folder IDs (potentially for cleanup or validation)
    @Query("SELECT folder_id FROM folders")
    suspend fun getAllFolderIds(): List<Long>
}

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
    suspend fun deleteAllStats()
}