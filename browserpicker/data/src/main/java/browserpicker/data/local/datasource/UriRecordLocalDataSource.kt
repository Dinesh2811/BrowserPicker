package browserpicker.data.local.datasource

import androidx.paging.PagingSource
import androidx.sqlite.db.SupportSQLiteQuery
import browserpicker.data.local.dao.*
import browserpicker.data.local.entity.*
import browserpicker.data.local.mapper.UriRecordMapper.toEntity
import browserpicker.data.local.mapper.HostRuleMapper.toEntity
import browserpicker.data.local.mapper.FolderMapper.toEntity
import browserpicker.data.local.mapper.BrowserUsageStatMapper.toEntity
import browserpicker.data.local.mapper.UriRecordMapper.toDomainModel
import browserpicker.data.local.mapper.HostRuleMapper.toDomainModel
import browserpicker.data.local.mapper.FolderMapper.toDomainModel
import browserpicker.data.local.mapper.BrowserUsageStatMapper.toDomainModel
import browserpicker.data.local.mapper.UriRecordMapper.toDomainModels
import browserpicker.data.local.mapper.HostRuleMapper.toDomainModels
import browserpicker.data.local.mapper.FolderMapper.toDomainModels
import browserpicker.data.local.mapper.BrowserUsageStatMapper.toDomainModels
import browserpicker.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Interface for accessing UriRecord data
interface UriRecordLocalDataSource {
    suspend fun insert(uriRecord: UriRecord): Long
    suspend fun insertAll(uriRecords: List<UriRecord>)
    suspend fun getById(id: Long): UriRecord?
    fun getAllStream(): Flow<List<UriRecord>>
    fun getPagingSource(query: SupportSQLiteQuery): PagingSource<Int, UriRecordEntity> // Returns Entity for Paging library
    fun getRawStream(query: SupportSQLiteQuery): Flow<List<UriRecord>>
    suspend fun clearAll()
    suspend fun count(): Long
}

// Interface for accessing HostRule data
interface HostRuleLocalDataSource {
    suspend fun insert(hostRule: HostRule): Long
    suspend fun update(hostRule: HostRule)
    suspend fun delete(hostRule: HostRule)
    suspend fun deleteById(id: Long)
    suspend fun getById(id: Long): HostRule?
    suspend fun getByHost(host: String): HostRule?
    fun getByHostStream(host: String): Flow<HostRule?>
    fun getAllStream(): Flow<List<HostRule>>
    fun getPagingSource(query: SupportSQLiteQuery): PagingSource<Int, HostRuleEntity> // Returns Entity
    fun getRawStream(query: SupportSQLiteQuery): Flow<List<HostRule>>
    suspend fun removeRulesFromFolder(folderId: Long, timestamp: Instant)
    suspend fun clearFolderIdForRuleIds(ruleIds: List<Long>, timestamp: Instant)
    fun getRulesByFolderIdStream(folderId: Long): Flow<List<HostRule>>
    suspend fun getRulesByFolderId(folderId: Long): List<HostRule>
    suspend fun clearAll()
    suspend fun count(): Long
}

// Interface for accessing Folder data
interface FolderLocalDataSource {
    suspend fun insert(folder: Folder): Long
    suspend fun update(folder: Folder)
    suspend fun delete(folder: Folder) // Simple delete marker
    suspend fun deleteFolderRecursively(folderId: Long, timestamp: Instant) // Handles cascade logic
    suspend fun getById(id: Long): Folder?
    fun getFoldersByTypeStream(type: FolderType): Flow<List<Folder>>
    fun getAllFoldersStream(): Flow<List<Folder>>
    fun getChildFoldersStream(parentId: Long): Flow<List<Folder>>
    fun getRootFoldersByTypeStream(type: FolderType): Flow<List<Folder>>
    suspend fun findByNameAndParent(name: String, type: FolderType, parentId: Long?): Folder?
    suspend fun countByType(type: FolderType): Long
}

// Interface for accessing BrowserUsageStat data
interface BrowserUsageStatLocalDataSource {
    suspend fun upsert(stat: BrowserUsageStat)
    suspend fun upsertAll(stats: List<BrowserUsageStat>)
    suspend fun getByPackageName(packageName: String): BrowserUsageStat?
    fun getAllStream(): Flow<List<BrowserUsageStat>>
    fun getAllSortedByUsageCountStream(): Flow<List<BrowserUsageStat>>
    suspend fun deleteByPackageName(packageName: String)
    suspend fun clearAll()
    suspend fun incrementOrInsert(packageName: String, timestamp: Instant)
}


// --- UriRecord ---
@Singleton // Scope based on your DI setup
class UriRecordLocalDataSourceImpl @Inject constructor(
    private val uriRecordDao: UriRecordDao
) : UriRecordLocalDataSource {

    override suspend fun insert(uriRecord: UriRecord): Long {
        // Ensure host is present if mapping logic requires it elsewhere before insertion
        return uriRecordDao.insert(uriRecord.toEntity())
    }

    override suspend fun insertAll(uriRecords: List<UriRecord>) {
        uriRecordDao.insertAll(uriRecords.map { it.toEntity() })
    }

    override suspend fun getById(id: Long): UriRecord? {
        return uriRecordDao.getById(id)?.toDomainModel()
    }

    override fun getAllStream(): Flow<List<UriRecord>> {
        return uriRecordDao.getAllStream().map { it.toDomainModels() }
    }

    override fun getPagingSource(query: SupportSQLiteQuery): PagingSource<Int, UriRecordEntity> {
        // Pass the raw query to the DAO's paging source method
        // The Repository layer will handle mapping PagingData<Entity> to PagingData<Model>
        return uriRecordDao.getPagingSource(query)
    }

    override fun getRawStream(query: SupportSQLiteQuery): Flow<List<UriRecord>> {
        return uriRecordDao.getRawStream(query).map { it.toDomainModels() }
    }

    override suspend fun clearAll() {
        uriRecordDao.clearAll()
    }

    override suspend fun count(): Long {
        return uriRecordDao.count()
    }
}

// --- HostRule ---
@Singleton
class HostRuleLocalDataSourceImpl @Inject constructor(
    private val hostRuleDao: HostRuleDao
) : HostRuleLocalDataSource {

    override suspend fun insert(hostRule: HostRule): Long {
        return hostRuleDao.insert(hostRule.toEntity())
    }

    override suspend fun update(hostRule: HostRule) {
        hostRuleDao.update(hostRule.toEntity())
    }

    override suspend fun delete(hostRule: HostRule) {
        hostRuleDao.delete(hostRule.toEntity())
    }

    override suspend fun deleteById(id: Long) {
        hostRuleDao.deleteById(id)
    }


    override suspend fun getById(id: Long): HostRule? {
        return hostRuleDao.getById(id)?.toDomainModel()
    }

    override suspend fun getByHost(host: String): HostRule? {
        return hostRuleDao.getByHost(host)?.toDomainModel()
    }

    override fun getByHostStream(host: String): Flow<HostRule?> {
        return hostRuleDao.getByHostStream(host).map { it?.toDomainModel() }
    }

    override fun getAllStream(): Flow<List<HostRule>> {
        return hostRuleDao.getAllStream().map { it.toDomainModels() }
    }

    override fun getPagingSource(query: SupportSQLiteQuery): PagingSource<Int, HostRuleEntity> {
        // Repository layer maps PagingData<Entity> to PagingData<Model>
        return hostRuleDao.getPagingSource(query)
    }

    override fun getRawStream(query: SupportSQLiteQuery): Flow<List<HostRule>> {
        return hostRuleDao.getRawStream(query).map { it.toDomainModels() }
    }

    override suspend fun removeRulesFromFolder(folderId: Long, timestamp: Instant) {
        hostRuleDao.removeRulesFromFolder(folderId, timestamp)
    }

    override suspend fun clearFolderIdForRuleIds(ruleIds: List<Long>, timestamp: Instant) {
        hostRuleDao.clearFolderIdForRuleIds(ruleIds, timestamp)
    }

    override fun getRulesByFolderIdStream(folderId: Long): Flow<List<HostRule>> {
        return hostRuleDao.getRulesByFolderIdStream(folderId).map { it.toDomainModels() }
    }

    override suspend fun getRulesByFolderId(folderId: Long): List<HostRule> {
        return hostRuleDao.getRulesByFolderId(folderId).toDomainModels()
    }

    override suspend fun clearAll() {
        hostRuleDao.clearAll()
    }

    override suspend fun count(): Long {
        return hostRuleDao.count()
    }
}

// --- Folder ---
@Singleton
class FolderLocalDataSourceImpl @Inject constructor(
    private val folderDao: FolderDao,
    private val hostRuleDao: HostRuleDao // Needed for transactional delete logic
) : FolderLocalDataSource {

    override suspend fun insert(folder: Folder): Long {
        return folderDao.insert(folder.toEntity())
    }

    override suspend fun update(folder: Folder) {
        folderDao.update(folder.toEntity())
    }

    override suspend fun delete(folder: Folder) {
        // This is just a marker. Use deleteFolderRecursively for actual deletion.
        folderDao.delete(folder.toEntity())
    }

    // This implementation holds the transaction logic involving multiple DAOs for folder deletion.
    // It belongs here (or potentially a dedicated Repository method if more complex)
    // rather than the DAO itself.
    @androidx.room.Transaction // Ensure atomicity if called from a suspend fun in repository
    override suspend fun deleteFolderRecursively(folderId: Long, timestamp: Instant) {
        // 1. Unlink host rules associated with this folder
        hostRuleDao.removeRulesFromFolder(folderId, timestamp)
        // 2. Make immediate children top-level folders
        folderDao.makeChildrenRoot(folderId, timestamp)
        // 3. Delete the folder itself
        folderDao.deleteById(folderId)
        // Note: This doesn't handle deleting nested children recursively.
        // A full recursive delete would require querying children and calling this method for each.
        // This complexity is often handled in the Use Case/Repository layer.
    }

    override suspend fun getById(id: Long): Folder? {
        return folderDao.getById(id)?.toDomainModel()
    }

    override fun getFoldersByTypeStream(type: FolderType): Flow<List<Folder>> {
        return folderDao.getFoldersByTypeStream(type).map { it.toDomainModels() }
    }

    override fun getAllFoldersStream(): Flow<List<Folder>> {
        return folderDao.getAllFoldersStream().map { it.toDomainModels() }
    }

    override fun getChildFoldersStream(parentId: Long): Flow<List<Folder>> {
        return folderDao.getChildFoldersStream(parentId).map { it.toDomainModels() }
    }

    override fun getRootFoldersByTypeStream(type: FolderType): Flow<List<Folder>> {
        return folderDao.getRootFoldersByTypeStream(type).map { it.toDomainModels() }
    }

    override suspend fun findByNameAndParent(name: String, type: FolderType, parentId: Long?): Folder? {
        return folderDao.findByNameAndParent(name, type, parentId)?.toDomainModel()
    }

    override suspend fun countByType(type: FolderType): Long {
        return folderDao.countByType(type)
    }
}

// --- BrowserUsageStat ---
@Singleton
class BrowserUsageStatLocalDataSourceImpl @Inject constructor(
    private val browserUsageStatDao: BrowserUsageStatDao
) : BrowserUsageStatLocalDataSource {

    override suspend fun upsert(stat: BrowserUsageStat) {
        browserUsageStatDao.upsert(stat.toEntity())
    }

    override suspend fun upsertAll(stats: List<BrowserUsageStat>) {
        browserUsageStatDao.upsertAll(stats.map { it.toEntity() })
    }

    override suspend fun getByPackageName(packageName: String): BrowserUsageStat? {
        return browserUsageStatDao.getByPackageName(packageName)?.toDomainModel()
    }

    override fun getAllStream(): Flow<List<BrowserUsageStat>> {
        return browserUsageStatDao.getAllStream().map { it.toDomainModels() }
    }

    override fun getAllSortedByUsageCountStream(): Flow<List<BrowserUsageStat>> {
        return browserUsageStatDao.getAllSortedByUsageCountStream().map { it.toDomainModels() }
    }

    override suspend fun deleteByPackageName(packageName: String) {
        browserUsageStatDao.deleteByPackageName(packageName)
    }

    override suspend fun clearAll() {
        browserUsageStatDao.clearAll()
    }

    override suspend fun incrementOrInsert(packageName: String, timestamp: Instant) {
        browserUsageStatDao.incrementOrInsert(packageName, timestamp)
    }
}
