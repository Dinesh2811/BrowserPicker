package browserpicker.domain.repository

import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import kotlinx.coroutines.flow.Flow

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