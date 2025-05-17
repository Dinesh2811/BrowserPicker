package browserpicker.domain.repository

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriStatus
import kotlinx.coroutines.flow.Flow

interface HostRuleRepository {
    suspend fun getHostRuleByHost(host: String): DomainResult<HostRule?, AppError>
    suspend fun getHostRuleById(id: Long): DomainResult<HostRule?, AppError>
    fun getAllHostRules(): Flow<DomainResult<List<HostRule>, AppError>>
    fun getHostRulesByStatus(status: UriStatus): Flow<DomainResult<List<HostRule>, AppError>>
    fun getHostRulesByFolder(folderId: Long): Flow<DomainResult<List<HostRule>, AppError>>
    fun getRootHostRulesByStatus(status: UriStatus): Flow<DomainResult<List<HostRule>, AppError>>
    fun getDistinctRuleHosts(): Flow<DomainResult<List<String>, AppError>>

    suspend fun saveHostRule(
        host: String,
        status: UriStatus,
        folderId: Long?,
        preferredBrowser: String?,
        isPreferenceEnabled: Boolean
    ): DomainResult<Long, AppError>

    suspend fun deleteHostRuleById(id: Long): DomainResult<Unit, AppError>
    suspend fun deleteHostRuleByHost(host: String): DomainResult<Unit, AppError>
    suspend fun clearFolderAssociation(folderId: Long): DomainResult<Unit, AppError>
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