package browserpicker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import browserpicker.data.local.entity.HostRuleEntity
import browserpicker.domain.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HostRuleDao {
    @Upsert
    suspend fun upsertHostRule(hostRule: HostRuleEntity): Long

    @Query("SELECT * FROM host_rules WHERE host_rule_id = :id")
    suspend fun getHostRuleById(id: Long): HostRuleEntity?

    @Query("SELECT * FROM host_rules WHERE host = :host LIMIT 1")
    suspend fun getHostRuleByHost(host: String): HostRuleEntity?

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