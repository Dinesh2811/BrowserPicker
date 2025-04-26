package browserpicker.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import browserpicker.data.local.entity.HostRuleEntity
import browserpicker.domain.model.RuleType
import kotlinx.coroutines.flow.Flow

@Dao
interface HostRuleDao {

    @Insert(onConflict = OnConflictStrategy.ABORT) // Use ABORT to prevent replacing on unique constraint violation (host)
    suspend fun insert(rule: HostRuleEntity): Long

    @Update
    suspend fun update(rule: HostRuleEntity): Int // Returns number of rows affected

    // Note: An insertOrUpdate requires checking existence first or custom upsert logic if ABORT is used.
    // Often handled in the Repository.

    @Query("DELETE FROM host_rules WHERE host_rule_id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM host_rules WHERE host = :host")
    suspend fun deleteByHost(host: String): Int

    // --- Rule Retrieval ---
    @Query("SELECT * FROM host_rules WHERE host_rule_id = :id")
    suspend fun getRuleById(id: Long): HostRuleEntity?

    @Query("SELECT * FROM host_rules WHERE host = :host LIMIT 1")
    suspend fun getRuleByHost(host: String): HostRuleEntity?

    @Query("SELECT * FROM host_rules WHERE host = :host LIMIT 1")
    fun observeRuleByHost(host: String): Flow<HostRuleEntity?> // For observing changes to a specific host rule

    @Query("SELECT host_rule_id FROM host_rules WHERE host = :host LIMIT 1")
    suspend fun findHostRuleId(host: String): Long? // Efficiently get just the ID

    // --- Rule Lists / Pagination ---
    // Simple pagination example for bookmarks/blocks
    @Query("""
        SELECT * FROM host_rules
        WHERE rule_type = :ruleTypeValue AND (:searchTerm IS NULL OR host LIKE '%' || :searchTerm || '%')
        ORDER BY
            CASE WHEN :isSortAsc = 1 THEN host END ASC,
            CASE WHEN :isSortAsc = 0 THEN host END DESC
    """)
    fun getRulesPagingSource(
        ruleTypeValue: Int, // Pass Int value of RuleType
        searchTerm: String?,
        isSortAsc: Boolean
    ): PagingSource<Int, HostRuleEntity>

    // RawQuery example for more complex filtering/sorting
    @RawQuery(observedEntities = [HostRuleEntity::class])
    fun getRulesPagingSourceRaw(query: SupportSQLiteQuery): PagingSource<Int, HostRuleEntity>

    // Get all rules of a certain type (non-paginated, use with caution if list can be large)
    @Query("SELECT * FROM host_rules WHERE rule_type = :ruleTypeValue ORDER BY host ASC")
    fun observeRulesByType(ruleTypeValue: Int): Flow<List<HostRuleEntity>>

    // --- Folder Association Queries ---
    @Query("SELECT * FROM host_rules WHERE bookmark_folder_id = :folderId ORDER BY host ASC")
    suspend fun getRulesByBookmarkFolder(folderId: Long): List<HostRuleEntity>

    @Query("SELECT * FROM host_rules WHERE block_folder_id = :folderId ORDER BY host ASC")
    suspend fun getRulesByBlockFolder(folderId: Long): List<HostRuleEntity>

    // --- Update Folder References ---
    // Needed when deleting a folder to set associated rule folder IDs to null
    @Query("UPDATE host_rules SET bookmark_folder_id = NULL, updated_at = :timestamp WHERE bookmark_folder_id = :folderId")
    suspend fun clearBookmarkFolderId(folderId: Long, timestamp: kotlinx.datetime.Instant)

    @Query("UPDATE host_rules SET block_folder_id = NULL, updated_at = :timestamp WHERE block_folder_id = :folderId")
    suspend fun clearBlockFolderId(folderId: Long, timestamp: kotlinx.datetime.Instant)
}
