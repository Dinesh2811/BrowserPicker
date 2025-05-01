package browserpicker.data.local.datasource

import browserpicker.core.di.InstantProvider
import browserpicker.data.local.dao.FolderDao
import browserpicker.data.local.entity.FolderEntity
import browserpicker.data.local.mapper.FolderMapper
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface FolderLocalDataSource {
    suspend fun ensureDefaultFoldersExist()
    suspend fun createFolder(folder: Folder): Long
    suspend fun updateFolder(folder: Folder): Boolean
    suspend fun deleteFolder(folderId: Long): Boolean
    suspend fun getFolderByIdSuspend(folderId: Long): Folder?
    fun getFolder(folderId: Long): Flow<Folder?>
    fun getChildFolders(parentFolderId: Long): Flow<List<Folder>>
    fun getRootFoldersByType(type: FolderType): Flow<List<Folder>>
    fun getAllFoldersByType(type: FolderType): Flow<List<Folder>>
    suspend fun findFolderByNameAndParent(name: String, parentFolderId: Long?, type: FolderType): Folder?
    suspend fun hasChildFolders(folderId: Long): Boolean
    suspend fun getAllFolderIds(): List<Long>
}

@Singleton
class FolderLocalDataSourceImpl @Inject constructor(
    private val folderDao: FolderDao,
    private val instantProvider: InstantProvider,
): FolderLocalDataSource {

    override suspend fun ensureDefaultFoldersExist() {
        val now = instantProvider.now()
        val defaultBookmarkFolder = FolderEntity(
            id = Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID,
            parentFolderId = null,
            name = Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_NAME,
            folderType = FolderType.BOOKMARK.value,
            createdAt = now,
            updatedAt = now
        )
        val defaultBlockedFolder = FolderEntity(
            id = Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID,
            parentFolderId = null,
            name = Folder.DEFAULT_BLOCKED_ROOT_FOLDER_NAME,
            folderType = FolderType.BLOCK.value,
            createdAt = now,
            updatedAt = now
        )

        folderDao.insertFoldersIgnoreConflict(listOf(defaultBookmarkFolder, defaultBlockedFolder))
    }

    override suspend fun createFolder(folder: Folder): Long {
        val now = instantProvider.now()
        val entity = FolderMapper.toEntity(folder).let {
            it.copy(
                createdAt = if (it.createdAt.epochSeconds == 0L) now else it.createdAt,
                updatedAt = now
            )
        }

        val existing = findFolderByNameAndParent(entity.name, entity.parentFolderId, FolderType.fromValue(entity.folderType))
        if (existing != null) {
            throw IllegalArgumentException("Folder with name '${entity.name}' already exists in this parent folder and type.")
        }

        entity.parentFolderId?.let {
            val parentExists = folderDao.getFolderById(it).firstOrNull() != null
            if (!parentExists) {
                throw IllegalArgumentException("Parent folder with ID $it does not exist.")
            }
        }

        return folderDao.upsertFolder(entity)
    }

    override suspend fun updateFolder(folder: Folder): Boolean {
        val entity = FolderMapper.toEntity(folder).copy(updatedAt = instantProvider.now())
        if (!FolderType.isValidValue(entity.folderType)) {
            throw IllegalArgumentException("Invalid folder type value: ${entity.folderType}")
        }

        entity.parentFolderId?.let {
            val parentExists = folderDao.getFolderById(it).firstOrNull() != null
            if (!parentExists) {
                throw IllegalArgumentException("Parent folder with ID $it does not exist for update.")
            }
        }
        return folderDao.updateFolder(entity) > 0
    }

    override suspend fun deleteFolder(folderId: Long): Boolean {
        val folder = getFolderByIdSuspend(folderId)
        if (folder == null) {
            throw IllegalArgumentException("Folder with ID $folderId does not exist.")
        }

        if (hasChildFolders(folderId)) {
            throw IllegalStateException("Cannot delete folder with ID $folderId as it contains child folders. Handle cleanup in the Repository layer first.")
        }

        return folderDao.deleteFolderById(folderId) > 0
    }

    override suspend fun getFolderByIdSuspend(folderId: Long): Folder? = folderDao.getFolderById(folderId).firstOrNull()?.let { FolderMapper.toDomainModel(it) }

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