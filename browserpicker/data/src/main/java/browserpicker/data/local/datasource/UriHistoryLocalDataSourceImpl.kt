package browserpicker.data.local.datasource

import browserpicker.data.local.dao.BrowserUsageStatDao
import browserpicker.data.local.dao.FolderDao
import browserpicker.data.local.dao.HostRuleDao
import browserpicker.data.local.dao.UriRecordDao
import javax.inject.Inject
import javax.inject.Singleton
import androidx.paging.*
import browserpicker.core.di.InstantProvider
import browserpicker.data.local.entity.*
import browserpicker.data.local.mapper.*
import browserpicker.data.local.query.UriRecordQueryBuilder
import browserpicker.data.local.query.model.DateCount
import browserpicker.data.local.query.model.GroupCount
import browserpicker.data.local.query.model.UriRecordQueryConfig
import browserpicker.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

// --- UriHistoryLocalDataSourceImpl ---

@Singleton
class UriHistoryLocalDataSourceImpl @Inject constructor(
    private val uriRecordDao: UriRecordDao,
    private val queryBuilder: UriRecordQueryBuilder
) : UriHistoryLocalDataSource {

    override fun getPagedUriRecords(config: UriRecordQueryConfig, pagingConfig: PagingConfig): Flow<PagingData<UriRecord>> {
        return Pager(
            config = pagingConfig,
            pagingSourceFactory = {
                val query = queryBuilder.buildPagedQuery(config)
                uriRecordDao.getPagedUriRecords(query)
            }
        ).flow
            .map { pagingDataEntity: PagingData<UriRecordEntity> ->
                pagingDataEntity.map { entity ->
                    UriRecordMapper.toDomainModel(entity)
                }
            }
    }

    override fun getTotalUriRecordCount(config: UriRecordQueryConfig): Flow<Int> {
        val query = queryBuilder.buildTotalCountQuery(config)
        return uriRecordDao.getTotalUriRecordCount(query)
    }

    override fun getGroupCounts(config: UriRecordQueryConfig): Flow<List<GroupCount>> {
        val query = queryBuilder.buildGroupCountQuery(config)
        return uriRecordDao.getGroupCounts(query)
    }

    override fun getDateCounts(config: UriRecordQueryConfig): Flow<List<DateCount>> {
        val query = queryBuilder.buildDateCountQuery(config)
        return uriRecordDao.getDateCounts(query)
    }

    override suspend fun insertUriRecord(record: UriRecord): Long {
        return uriRecordDao.insertUriRecord(UriRecordMapper.toEntity(record))
    }

    override suspend fun insertUriRecords(records: List<UriRecord>) {
        uriRecordDao.insertUriRecords(records.map { UriRecordMapper.toEntity(it) })
    }

    override suspend fun getUriRecord(id: Long): UriRecord? {
        return uriRecordDao.getUriRecordById(id)?.let { UriRecordMapper.toDomainModel(it) }
    }

    override suspend fun deleteUriRecord(id: Long): Boolean {
        return uriRecordDao.deleteUriRecordById(id) > 0
    }

    override suspend fun deleteAllUriRecords() {
        uriRecordDao.deleteAllUriRecords()
    }

    override fun getDistinctHosts(): Flow<List<String>> {
        return uriRecordDao.getDistinctHosts()
    }

    override fun getDistinctChosenBrowsers(): Flow<List<String?>> {
        return uriRecordDao.getDistinctChosenBrowsers()
    }
}

// --- HostRuleLocalDataSourceImpl ---

@Singleton
class HostRuleLocalDataSourceImpl @Inject constructor(
    private val hostRuleDao: HostRuleDao
) : HostRuleLocalDataSource {

    override fun getHostRuleByHost(host: String): Flow<HostRule?> {
        return hostRuleDao.getHostRuleByHost(host).map { entity ->
            entity?.let { HostRuleMapper.toDomainModel(it) }
        }
    }

    override suspend fun getHostRuleById(id: Long): HostRule? {
        return hostRuleDao.getHostRuleById(id)?.let { HostRuleMapper.toDomainModel(it) }
    }

    override suspend fun upsertHostRule(rule: HostRule): Long {
        return hostRuleDao.upsertHostRule(HostRuleMapper.toEntity(rule))
    }

    override fun getAllHostRules(): Flow<List<HostRule>> {
        return hostRuleDao.getAllHostRules().map { HostRuleMapper.toDomainModels(it) }
    }

    override fun getHostRulesByStatus(status: UriStatus): Flow<List<HostRule>> {
        return hostRuleDao.getHostRulesByStatus(status).map { HostRuleMapper.toDomainModels(it) }
    }

    override fun getHostRulesByFolder(folderId: Long): Flow<List<HostRule>> {
        return hostRuleDao.getHostRulesByFolderId(folderId).map { HostRuleMapper.toDomainModels(it) }
    }

    override fun getRootHostRulesByStatus(status: UriStatus): Flow<List<HostRule>> {
        return hostRuleDao.getRootHostRulesByStatus(status).map { HostRuleMapper.toDomainModels(it) }
    }

    override suspend fun deleteHostRuleById(id: Long): Boolean {
        return hostRuleDao.deleteHostRuleById(id) > 0
    }

    override suspend fun deleteHostRuleByHost(host: String): Boolean {
        return hostRuleDao.deleteHostRuleByHost(host) > 0
    }

    override suspend fun clearFolderIdForRules(folderId: Long) {
        hostRuleDao.clearFolderIdForRules(folderId)
    }

    override fun getDistinctRuleHosts(): Flow<List<String>> {
        return hostRuleDao.getDistinctRuleHosts()
    }
}

// --- FolderLocalDataSourceImpl ---

@Singleton
class FolderLocalDataSourceImpl @Inject constructor(
    private val folderDao: FolderDao,
    private val instantProvider: InstantProvider // Inject InstantProvider
) : FolderLocalDataSource {

    override suspend fun ensureDefaultFoldersExist() {
        val now = instantProvider.now()
        val defaultBookmarkFolder = FolderEntity(
            id = Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID,
            parentFolderId = null,
            name = Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_NAME,
            folderType = FolderType.BOOKMARK,
            createdAt = now,
            updatedAt = now
        )
        val defaultBlockedFolder = FolderEntity(
            id = Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID,
            parentFolderId = null,
            name = Folder.DEFAULT_BLOCKED_ROOT_FOLDER_NAME,
            folderType = FolderType.BLOCK,
            createdAt = now,
            updatedAt = now
        )
        folderDao.insertFoldersIgnoreConflict(listOf(defaultBookmarkFolder, defaultBlockedFolder))
    }

    override suspend fun createFolder(folder: Folder): Long {
        // Ensure createdAt/updatedAt are set if not already
        val entity = FolderMapper.toEntity(folder).let {
            val now = instantProvider.now()
            it.copy(
                createdAt = if(it.createdAt.epochSeconds == 0L) now else it.createdAt, // Avoid overwriting if pre-set
                updatedAt = now // Always update 'updatedAt' on creation/modification
            )
        }
        // Consider adding uniqueness check here before inserting if strict guarantees needed beyond DB
        // val existing = findFolderByNameAndParent(entity.name, entity.parentFolderId, entity.folderType)
        // if (existing != null) throw SomeSpecificException("Folder already exists")
        return folderDao.upsertFolder(entity) // Using upsert handles potential ID conflicts if user provides one
    }

    override suspend fun updateFolder(folder: Folder): Boolean {
        // Ensure updatedAt is set
        val entity = FolderMapper.toEntity(folder).copy(updatedAt = instantProvider.now())
        // Optionally add checks here: e.g., ensure parent folder exists and is of the correct type if moving.
        return folderDao.updateFolder(entity) > 0
    }

    override suspend fun deleteFolder(folderId: Long): Boolean {
        // Business logic note: Repository layer should handle recursive deletion or
        // moving children/rules before calling this if necessary.
        // The FK constraint `onDelete = SET_NULL` handles DB integrity.
        return folderDao.deleteFolderById(folderId) > 0
    }

    override fun getFolder(folderId: Long): Flow<Folder?> {
        return folderDao.getFolderById(folderId).map { entity ->
            entity?.let { FolderMapper.toDomainModel(it) }
        }
    }

    override fun getChildFolders(parentFolderId: Long): Flow<List<Folder>> {
        return folderDao.getChildFolders(parentFolderId).map { FolderMapper.toDomainModels(it) }
    }

    override fun getRootFoldersByType(type: FolderType): Flow<List<Folder>> {
        return folderDao.getRootFoldersByType(type).map { FolderMapper.toDomainModels(it) }
    }

    override fun getAllFoldersByType(type: FolderType): Flow<List<Folder>> {
        return folderDao.getAllFoldersByType(type).map { FolderMapper.toDomainModels(it) }
    }

    override suspend fun findFolderByNameAndParent(name: String, parentFolderId: Long?, type: FolderType): Folder? {
        return folderDao.findFolderByNameAndParent(name, parentFolderId, type)?.let {
            FolderMapper.toDomainModel(it)
        }
    }

    override suspend fun hasChildFolders(folderId: Long): Boolean {
        return folderDao.hasChildFolders(folderId)
    }

    override suspend fun getAllFolderIds(): List<Long> {
        return folderDao.getAllFolderIds()
    }
}

// --- BrowserStatsLocalDataSourceImpl ---

@Singleton
class BrowserStatsLocalDataSourceImpl @Inject constructor(
    private val browserUsageStatDao: BrowserUsageStatDao,
    private val instantProvider: InstantProvider // Inject InstantProvider
) : BrowserStatsLocalDataSource {

    override suspend fun recordBrowserUsage(packageName: String) {
        // Use the DAO's transaction method which handles incrementing/inserting
        browserUsageStatDao.incrementUsage(packageName, instantProvider.now())
    }

    override fun getBrowserStat(packageName: String): Flow<BrowserUsageStat?> {
        return browserUsageStatDao.getBrowserUsageStat(packageName).map { entity ->
            entity?.let { BrowserUsageStatMapper.toDomainModel(it) }
        }
    }

    override fun getAllBrowserStats(): Flow<List<BrowserUsageStat>> {
        return browserUsageStatDao.getAllBrowserUsageStats().map { BrowserUsageStatMapper.toDomainModels(it) }
    }

    override fun getAllBrowserStatsSortedByLastUsed(): Flow<List<BrowserUsageStat>> {
        return browserUsageStatDao.getAllBrowserUsageStatsSortedByLastUsed().map { BrowserUsageStatMapper.toDomainModels(it) }
    }

    override suspend fun deleteBrowserStat(packageName: String): Boolean {
        return browserUsageStatDao.deleteBrowserUsageStat(packageName) > 0
    }

    override suspend fun deleteAllStats() {
        browserUsageStatDao.deleteAllStats()
    }
}
