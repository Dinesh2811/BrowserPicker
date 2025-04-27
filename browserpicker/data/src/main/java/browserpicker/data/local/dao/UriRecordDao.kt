package browserpicker.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import browserpicker.data.local.entity.UriRecordEntity
import browserpicker.domain.model.InteractionAction
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import androidx.room.Delete
import androidx.room.Transaction
import androidx.room.Upsert
import browserpicker.data.local.entity.BrowserUsageStatEntity
import browserpicker.data.local.entity.FolderEntity
import browserpicker.data.local.entity.HostRuleEntity
import browserpicker.domain.model.FolderType
import browserpicker.domain.model.UriStatus

@Dao
interface UriRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(uriRecord: UriRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(uriRecords: List<UriRecordEntity>)

    @Update
    suspend fun update(uriRecord: UriRecordEntity)

    @Delete
    suspend fun delete(uriRecord: UriRecordEntity)

    @Query("SELECT * FROM uri_records WHERE uri_record_id = :id")
    suspend fun getById(id: Long): UriRecordEntity?

    @Query("SELECT * FROM uri_records ORDER BY timestamp DESC")
    fun getAllStream(): Flow<List<UriRecordEntity>>

    // --- Paging ---
    // Base query for PagingSource. Filtering/Sorting applied via RawQuery.
    @Query("SELECT * FROM uri_records")
    fun getPagingSource(): PagingSource<Int, UriRecordEntity>

    // RawQuery for dynamic filtering/sorting with Paging
    // Note: Room requires the observed table name for RawQuery PagingSource
    @RawQuery(observedEntities = [UriRecordEntity::class])
    fun getPagingSource(query: SupportSQLiteQuery): PagingSource<Int, UriRecordEntity>

    // --- Dynamic Queries (Non-Paging) ---
    @RawQuery
    fun getRaw(query: SupportSQLiteQuery): List<UriRecordEntity> // Use for specific, smaller queries

    @RawQuery
    fun getRawStream(query: SupportSQLiteQuery): Flow<List<UriRecordEntity>> // For observing dynamic queries

    @Query("DELETE FROM uri_records")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM uri_records")
    suspend fun count(): Long
}


@Dao
interface HostRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hostRule: HostRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(hostRules: List<HostRuleEntity>)

    @Update
    suspend fun update(hostRule: HostRuleEntity)

    @Delete
    suspend fun delete(hostRule: HostRuleEntity)

    @Query("DELETE FROM host_rules WHERE host_rule_id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM host_rules WHERE host_rule_id = :id")
    suspend fun getById(id: Long): HostRuleEntity?

    @Query("SELECT * FROM host_rules WHERE host = :host LIMIT 1")
    suspend fun getByHost(host: String): HostRuleEntity?

    @Query("SELECT * FROM host_rules WHERE host = :host LIMIT 1")
    fun getByHostStream(host: String): Flow<HostRuleEntity?>

    @Query("SELECT * FROM host_rules")
    fun getAllStream(): Flow<List<HostRuleEntity>>

    @Query("SELECT * FROM host_rules WHERE uri_status = :status")
    fun getByStatusStream(status: UriStatus): Flow<List<HostRuleEntity>>

    // --- Paging ---
    @Query("SELECT * FROM host_rules")
    fun getPagingSource(): PagingSource<Int, HostRuleEntity>

    // Dynamic Paging
    @RawQuery(observedEntities = [HostRuleEntity::class])
    fun getPagingSource(query: SupportSQLiteQuery): PagingSource<Int, HostRuleEntity>

    // --- Folder Operations ---
    @Query("UPDATE host_rules SET folder_id = NULL, updated_at = :timestamp WHERE folder_id = :folderId")
    suspend fun removeRulesFromFolder(folderId: Long, timestamp: Instant)

    // Set folder ID to null for specific rules (e.g., when changing status from BOOKMARK/BLOCK to NONE)
    @Query("UPDATE host_rules SET folder_id = NULL, updated_at = :timestamp WHERE host_rule_id IN (:ruleIds)")
    suspend fun clearFolderIdForRuleIds(ruleIds: List<Long>, timestamp: Instant)

    // Get rules belonging to a specific folder
    @Query("SELECT * FROM host_rules WHERE folder_id = :folderId")
    fun getRulesByFolderIdStream(folderId: Long): Flow<List<HostRuleEntity>>

    @Query("SELECT * FROM host_rules WHERE folder_id = :folderId")
    suspend fun getRulesByFolderId(folderId: Long): List<HostRuleEntity>


    // --- Dynamic Queries (Non-Paging) ---
    @RawQuery
    fun getRaw(query: SupportSQLiteQuery): List<HostRuleEntity>

    @RawQuery
    fun getRawStream(query: SupportSQLiteQuery): Flow<List<HostRuleEntity>>

    @Query("DELETE FROM host_rules")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM host_rules")
    suspend fun count(): Long

    // --- Transaction Example (Conceptual - Actual logic might be in Repository/Use Case) ---
    // This shows how you might combine operations.
    @Transaction
    suspend fun updateRuleAndTimestamp(hostRule: HostRuleEntity) {
        // Example: Ensure updated_at is always set during an update
        // You might inject a Clock/TimeProvider here if doing this in the DAO layer,
        // but usually, setting timestamps happens before calling the DAO.
        update(hostRule)
    }
}

@Dao
interface FolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: FolderEntity): Long

    @Update
    suspend fun update(folder: FolderEntity)

    @Delete
    suspend fun delete(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE folder_id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM folders WHERE folder_id = :id")
    suspend fun getById(id: Long): FolderEntity?

    @Query("SELECT * FROM folders WHERE type = :type ORDER BY name ASC")
    fun getFoldersByTypeStream(type: FolderType): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY type, name ASC")
    fun getAllFoldersStream(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parent_folder_id = :parentId ORDER BY name ASC")
    fun getChildFoldersStream(parentId: Long): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parent_folder_id IS NULL AND type = :type ORDER BY name ASC")
    fun getRootFoldersByTypeStream(type: FolderType): Flow<List<FolderEntity>>

    // Update children's parent ID when a parent is deleted (making them root folders)
    @Query("UPDATE folders SET parent_folder_id = NULL, updated_at = :timestamp WHERE parent_folder_id = :parentId")
    suspend fun makeChildrenRoot(parentId: Long, timestamp: Instant)


    // --- Transaction for Deletion (Handles unlinking rules and child folders) ---
    // Note: This requires access to HostRuleDao, which isn't directly possible here.
    // This kind of cross-DAO transaction logic BELONGS in the REPOSITORY layer.
    // The DAO methods here provide the building blocks.
    // Example of what the DAO needs to support the transaction:
    @Transaction
    suspend fun deleteFolderAndUpdateRelations(folderId: Long, timestamp: Instant) {
        // 1. Make child folders root folders
        makeChildrenRoot(folderId, timestamp)
        // 2. Delete the actual folder (Repository will call HostRuleDao.removeRulesFromFolder first)
        deleteById(folderId)
    }

    @Query("SELECT COUNT(*) FROM folders WHERE type = :type")
    suspend fun countByType(type: FolderType): Long

    @Query("SELECT * FROM folders WHERE name = :name AND type = :type AND (parent_folder_id = :parentId OR (:parentId IS NULL AND parent_folder_id IS NULL)) LIMIT 1")
    suspend fun findByNameAndParent(name: String, type: FolderType, parentId: Long?): FolderEntity?

}


@Dao
interface BrowserUsageStatDao {

    // Upsert is convenient for inserting or updating based on Primary Key
    @Upsert
    suspend fun upsert(stat: BrowserUsageStatEntity)

    @Upsert
    suspend fun upsertAll(stats: List<BrowserUsageStatEntity>)

    @Query("SELECT * FROM browser_usage_stats WHERE browser_package_name = :packageName")
    suspend fun getByPackageName(packageName: String): BrowserUsageStatEntity?

    @Query("SELECT * FROM browser_usage_stats ORDER BY last_used_timestamp DESC")
    fun getAllStream(): Flow<List<BrowserUsageStatEntity>>

    @Query("SELECT * FROM browser_usage_stats ORDER BY usage_count DESC")
    fun getAllSortedByUsageCountStream(): Flow<List<BrowserUsageStatEntity>>

    @Query("DELETE FROM browser_usage_stats WHERE browser_package_name = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("DELETE FROM browser_usage_stats")
    suspend fun clearAll()

    // Efficiently increment usage count and update timestamp
    @Query("""
        UPDATE browser_usage_stats
        SET usage_count = usage_count + 1, last_used_timestamp = :timestamp
        WHERE browser_package_name = :packageName
    """)
    suspend fun incrementUsageCount(packageName: String, timestamp: Instant)

    // Transaction to handle increment or insert if not exists
    @Query("SELECT COUNT(*) FROM browser_usage_stats WHERE browser_package_name = :packageName")
    suspend fun exists(packageName: String): Int

    @Transaction
    suspend fun incrementOrInsert(packageName: String, timestamp: Instant) {
        if (exists(packageName) > 0) {
            incrementUsageCount(packageName, timestamp)
        } else {
            upsert(BrowserUsageStatEntity(
                browserPackageName = packageName,
                usageCount = 1,
                lastUsedTimestamp = timestamp
            ))
        }
    }
}