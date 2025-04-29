package browserpicker.playground.browserpicker.data

import androidx.paging.*
import kotlinx.coroutines.flow.*

interface UriHistoryLocalDataSource {
    fun getPagedUriRecords(config: UriRecordQueryConfig, pagingConfig: PagingConfig): Flow<PagingData<UriRecord>>
    fun getTotalUriRecordCount(config: UriRecordQueryConfig): Flow<Int>
    fun getGroupCounts(config: UriRecordQueryConfig): Flow<List<GroupCount>>
    fun getDateCounts(config: UriRecordQueryConfig): Flow<List<DateCount>>
    suspend fun insertUriRecord(record: UriRecord): Long
    suspend fun insertUriRecords(records: List<UriRecord>)
    suspend fun getUriRecord(id: Long): UriRecord?
    suspend fun deleteUriRecord(id: Long): Boolean
    suspend fun deleteAllUriRecords()
    fun getDistinctHosts(): Flow<List<String>>
    fun getDistinctChosenBrowsers(): Flow<List<String?>>
}

interface HostRuleLocalDataSource {
    fun getHostRuleByHost(host: String): Flow<HostRule?>
    suspend fun getHostRuleById(id: Long): HostRule?
    suspend fun upsertHostRule(rule: HostRule): Long
    fun getAllHostRules(): Flow<List<HostRule>>
    fun getHostRulesByStatus(status: UriStatus): Flow<List<HostRule>>
    fun getHostRulesByFolder(folderId: Long): Flow<List<HostRule>>
    fun getRootHostRulesByStatus(status: UriStatus): Flow<List<HostRule>>
    suspend fun deleteHostRuleById(id: Long): Boolean
    suspend fun deleteHostRuleByHost(host: String): Boolean
    suspend fun clearFolderIdForRules(folderId: Long)
    fun getDistinctRuleHosts(): Flow<List<String>>
}

interface FolderLocalDataSource {
    suspend fun ensureDefaultFoldersExist()
    suspend fun createFolder(folder: Folder): Long
    suspend fun updateFolder(folder: Folder): Boolean
    suspend fun deleteFolder(folderId: Long): Boolean
    fun getFolder(folderId: Long): Flow<Folder?>
    fun getChildFolders(parentFolderId: Long): Flow<List<Folder>>
    fun getRootFoldersByType(type: FolderType): Flow<List<Folder>>
    fun getAllFoldersByType(type: FolderType): Flow<List<Folder>>
    suspend fun findFolderByNameAndParent(name: String, parentFolderId: Long?, type: FolderType): Folder?
    suspend fun hasChildFolders(folderId: Long): Boolean
    suspend fun getAllFolderIds(): List<Long>
}

interface BrowserStatsLocalDataSource {
    suspend fun recordBrowserUsage(packageName: String)
    fun getBrowserStat(packageName: String): Flow<BrowserUsageStat?>
    fun getAllBrowserStats(): Flow<List<BrowserUsageStat>> // Default: sorted by count
    fun getAllBrowserStatsSortedByLastUsed(): Flow<List<BrowserUsageStat>>
    suspend fun deleteBrowserStat(packageName: String): Boolean
    suspend fun deleteAllStats()
}
