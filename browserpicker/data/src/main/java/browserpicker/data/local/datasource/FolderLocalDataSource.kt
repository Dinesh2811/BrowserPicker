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
    suspend fun createFolder(folder: FolderEntity): Long
    suspend fun updateFolder(folder: FolderEntity): Boolean
    suspend fun deleteFolder(folderId: Long): Boolean
    suspend fun getFolderByIdSuspend(folderId: Long): FolderEntity?
    fun getFolder(folderId: Long): Flow<FolderEntity?>
    fun getChildFolders(parentFolderId: Long): Flow<List<FolderEntity>>
    fun getRootFoldersByType(type: FolderType): Flow<List<FolderEntity>>
    fun getAllFoldersByType(type: FolderType): Flow<List<FolderEntity>>
    suspend fun findFolderByNameAndParent(name: String, parentFolderId: Long?, type: FolderType): FolderEntity?
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

    override suspend fun createFolder(folder: FolderEntity): Long {
        return folderDao.upsertFolder(folder)
    }

    override suspend fun updateFolder(folder: FolderEntity): Boolean {
        return folderDao.updateFolder(folder) > 0
    }

    override suspend fun deleteFolder(folderId: Long): Boolean {
        return folderDao.deleteFolderById(folderId) > 0
    }

    override suspend fun getFolderByIdSuspend(folderId: Long): FolderEntity? =
        folderDao.getFolderById(folderId).firstOrNull()

    override fun getFolder(folderId: Long): Flow<FolderEntity?> {
        return folderDao.getFolderById(folderId)
    }

    override fun getChildFolders(parentFolderId: Long): Flow<List<FolderEntity>> {
        return folderDao.getChildFolders(parentFolderId)
    }

    override fun getRootFoldersByType(type: FolderType): Flow<List<FolderEntity>> {
        return folderDao.getRootFoldersByType(type)
    }

    override fun getAllFoldersByType(type: FolderType): Flow<List<FolderEntity>> {
        return folderDao.getAllFoldersByType(type)
    }

    override suspend fun findFolderByNameAndParent(name: String, parentFolderId: Long?, type: FolderType): FolderEntity? {
        return folderDao.findFolderByNameAndParent(name, parentFolderId, type)
    }

    override suspend fun hasChildFolders(folderId: Long): Boolean {
        return folderDao.hasChildFolders(folderId)
    }

    override suspend fun getAllFolderIds(): List<Long> {
        return folderDao.getAllFolderIds()
    }
}
