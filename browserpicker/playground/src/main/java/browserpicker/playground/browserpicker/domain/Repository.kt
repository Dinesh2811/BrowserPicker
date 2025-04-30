package browserpicker.playground.browserpicker.domain

import androidx.paging.*
import kotlinx.coroutines.flow.Flow

interface UriHistoryRepository {
    fun getPagedUriRecords(query: UriHistoryQuery, pagingConfig: PagingConfig): Flow<PagingData<UriRecord>>
    fun getTotalUriRecordCount(query: UriHistoryQuery): Flow<Int>
    fun getGroupCounts(query: UriHistoryQuery): Flow<List<DomainGroupCount>>
    fun getDateCounts(query: UriHistoryQuery): Flow<List<DomainDateCount>>
    suspend fun addUriRecord(uriString: String, host: String, source: UriSource, action: InteractionAction, chosenBrowser: String?, associatedHostRuleId: Long? = null): Result<Long>
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
    suspend fun saveHostRule(host: String, status: UriStatus, folderId: Long?, preferredBrowser: String?, isPreferenceEnabled: Boolean): Result<Long>
    suspend fun deleteHostRuleById(id: Long): Result<Unit>
    suspend fun deleteHostRuleByHost(host: String): Result<Unit>
    suspend fun clearFolderAssociation(folderId: Long): Result<Unit>
}

interface FolderRepository {
    suspend fun ensureDefaultFoldersExist()
    fun getFolder(folderId: Long): Flow<Folder?>
    fun getChildFolders(parentFolderId: Long): Flow<List<Folder>>
    fun getRootFoldersByType(type: FolderType): Flow<List<Folder>>
    fun getAllFoldersByType(type: FolderType): Flow<List<Folder>>
    suspend fun findFolderByNameAndParent(name: String, parentFolderId: Long?, type: FolderType): Folder?
    suspend fun createFolder(name: String, parentFolderId: Long?, type: FolderType): Result<Long>
    suspend fun updateFolder(folder: Folder): Result<Unit>
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
