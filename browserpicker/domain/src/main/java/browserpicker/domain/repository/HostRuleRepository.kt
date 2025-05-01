package browserpicker.domain.repository

import browserpicker.core.results.AppError
import browserpicker.core.results.MyResult
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriStatus
import kotlinx.coroutines.flow.Flow

interface HostRuleRepository {
    fun getHostRuleByHost(host: String): Flow<HostRule?>
    suspend fun getHostRuleById(id: Long): MyResult<HostRule?, AppError>
    fun getAllHostRules(): Flow<List<HostRule>>
    fun getHostRulesByStatus(status: UriStatus): Flow<List<HostRule>>
    fun getHostRulesByFolder(folderId: Long): Flow<List<HostRule>>
    fun getRootHostRulesByStatus(status: UriStatus): Flow<List<HostRule>>
    fun getDistinctRuleHosts(): Flow<List<String>>

    /**
     * Creates or updates a HostRule, enforcing business logic.
     * - If status is BLOCKED, preferredBrowser is cleared, preference is disabled.
     * - If status is NONE, folderId is cleared.
     * - If folderId is provided, validates folder existence and type match with status.
     * - Updates timestamps automatically.
     * @return Result containing the ID of the upserted rule or an error.
     */
    suspend fun saveHostRule(
        host: String, // Use host as the primary business key for upsert logic
        status: UriStatus,
        folderId: Long?,
        preferredBrowser: String?,
        isPreferenceEnabled: Boolean
    ): MyResult<Long, AppError>

    suspend fun deleteHostRuleById(id: Long): MyResult<Unit, AppError>
    suspend fun deleteHostRuleByHost(host: String): MyResult<Unit, AppError>

    /**
     * Sets the folderId to null for all rules currently associated with the given folderId.
     * Typically used before deleting a folder.
     */
    suspend fun clearFolderAssociation(folderId: Long): MyResult<Unit, AppError>
}

/*

interface HostRuleRepository {
    fun getHostRuleByHost(host: String): Flow<HostRule?>
    suspend fun getHostRuleById(id: Long): HostRule?
    fun getAllHostRules(): Flow<List<HostRule>>
    fun getHostRulesByStatus(status: UriStatus): Flow<List<HostRule>>
    fun getHostRulesByFolder(folderId: Long): Flow<List<HostRule>>
    fun getRootHostRulesByStatus(status: UriStatus): Flow<List<HostRule>>
    fun getDistinctRuleHosts(): Flow<List<String>>

    /**
     * Creates or updates a HostRule, enforcing business logic.
     * - If status is BLOCKED, preferredBrowser is cleared, preference is disabled.
     * - If status is NONE, folderId is cleared.
     * - If folderId is provided, validates folder existence and type match with status.
     * - Updates timestamps automatically.
     * @return Result containing the ID of the upserted rule or an error.
     */
    suspend fun saveHostRule(
        host: String, // Use host as the primary business key for upsert logic
        status: UriStatus,
        folderId: Long?,
        preferredBrowser: String?,
        isPreferenceEnabled: Boolean
    ): Result<Long>

    suspend fun deleteHostRuleById(id: Long): Result<Unit>
    suspend fun deleteHostRuleByHost(host: String): Result<Unit>

    /**
     * Sets the folderId to null for all rules currently associated with the given folderId.
     * Typically used before deleting a folder.
     */
    suspend fun clearFolderAssociation(folderId: Long): Result<Unit>
}
 */