package browserpicker.domain.repository

import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    suspend fun ensureDefaultFoldersExist()
    fun getFolder(folderId: Long): Flow<DomainResult<Folder?, AppError>>
    fun getChildFolders(parentFolderId: Long): Flow<DomainResult<List<Folder>, AppError>>
    fun getRootFoldersByType(type: FolderType): Flow<DomainResult<List<Folder>, AppError>>
    fun getAllFoldersByType(type: FolderType): Flow<DomainResult<List<Folder>, AppError>>
//    suspend fun findFolderByNameAndParent(name: String, parentFolderId: Long?, type: FolderType): Folder?
    suspend fun findFolderByNameAndParent(name: String, parentFolderId: Long?, type: FolderType): DomainResult<Folder?, AppError>

    suspend fun createFolder(
        name: String,
        parentFolderId: Long?,
        type: FolderType
    ): DomainResult<Long, AppError>

    suspend fun updateFolder(folder: Folder): DomainResult<Unit, AppError>

    suspend fun deleteFolder(folderId: Long): DomainResult<Unit, AppError>
}
