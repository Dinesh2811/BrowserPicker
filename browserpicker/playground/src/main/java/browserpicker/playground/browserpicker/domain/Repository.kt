package browserpicker.playground.browserpicker.domain

import androidx.paging.*
import kotlinx.coroutines.flow.Flow

interface UriHistoryRepository {
    fun getPagedUriRecords(query: UriHistoryQuery, pagingConfig: PagingConfig): Flow<PagingData<UriRecord>>
    fun getTotalUriRecordCount(query: UriHistoryQuery): Flow<Int>
    fun getGroupCounts(query: UriHistoryQuery): Flow<List<DomainGroupCount>>
    fun getDateCounts(query: UriHistoryQuery): Flow<List<DomainDateCount>>

    suspend fun addUriRecord(
        uriString: String,
        host: String,
        source: UriSource,
        action: InteractionAction,
        chosenBrowser: String?,
        associatedHostRuleId: Long? = null
    ): Result<Long>

    suspend fun getUriRecord(id: Long): UriRecord?
    suspend fun deleteUriRecord(id: Long): Boolean
    suspend fun deleteAllUriRecords(): Result<Unit>

    fun getDistinctHosts(): Flow<List<String>>
    fun getDistinctChosenBrowsers(): Flow<List<String?>>
}

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

interface FolderRepository {
    suspend fun ensureDefaultFoldersExist()
    fun getFolder(folderId: Long): Flow<Folder?>
    fun getChildFolders(parentFolderId: Long): Flow<List<Folder>>
    fun getRootFoldersByType(type: FolderType): Flow<List<Folder>>
    fun getAllFoldersByType(type: FolderType): Flow<List<Folder>>
    suspend fun findFolderByNameAndParent(name: String, parentFolderId: Long?, type: FolderType): Folder?

    /**
     * Creates a new folder, validating name, parent existence, type consistency, and uniqueness.
     * @return Result containing the ID of the created folder or an error.
     */
    suspend fun createFolder(
        name: String,
        parentFolderId: Long?,
        type: FolderType
    ): Result<Long>

    /**
     * Updates an existing folder, validating changes (e.g., parent move, name uniqueness).
     * Cannot change the folder type.
     * @param folder The folder object with updated details (ID must match existing).
     * @return Result indicating success or error.
     */
    suspend fun updateFolder(folder: Folder): Result<Unit>

    /**
     * Deletes a folder.
     * Handles unlinking associated HostRules.
     * Does NOT delete child folders unless `deleteChildren` is true (potential future enhancement).
     * @param folderId ID of the folder to delete.
     * @return Result indicating success or error.
     */
    suspend fun deleteFolder(folderId: Long): Result<Unit>
}

interface BrowserStatsRepository {
    suspend fun recordBrowserUsage(packageName: String): Result<Unit>
    fun getBrowserStat(packageName: String): Flow<BrowserUsageStat?>
    fun getAllBrowserStats(): Flow<List<BrowserUsageStat>> // Default: sorted by count
    fun getAllBrowserStatsSortedByLastUsed(): Flow<List<BrowserUsageStat>>
    suspend fun deleteBrowserStat(packageName: String): Result<Unit>
    suspend fun deleteAllStats(): Result<Unit>
}
