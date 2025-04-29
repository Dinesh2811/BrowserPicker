//package browserpicker.data.local.repository
//
//import androidx.room.Transaction
//import androidx.room.withTransaction
//import browserpicker.core.di.InstantProvider
//import browserpicker.core.di.IoDispatcher
//import browserpicker.data.local.datasource.FolderLocalDataSource
//import browserpicker.data.local.datasource.HostRuleLocalDataSource
//import browserpicker.data.local.db.BrowserPickerDatabase
//import browserpicker.domain.model.Folder
//import browserpicker.domain.model.FolderType
//import browserpicker.domain.repository.FolderRepository
//import kotlinx.coroutines.CoroutineDispatcher
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.firstOrNull
//import kotlinx.coroutines.flow.flowOn
//import kotlinx.coroutines.withContext
//import timber.log.Timber
//import javax.inject.Inject
//import javax.inject.Singleton
//
//@Singleton
//class FolderRepositoryImpl @Inject constructor(
//    private val folderDataSource: FolderLocalDataSource,
//    private val hostRuleDataSource: HostRuleLocalDataSource,
//    private val instantProvider: InstantProvider,
//    private val browserPickerDatabase: BrowserPickerDatabase,
//    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
//): FolderRepository {
//
//    override suspend fun ensureDefaultFoldersExist() {
//        withContext(ioDispatcher) {
//            folderDataSource.ensureDefaultFoldersExist()
//        }
//    }
//
//    override fun getFolder(folderId: Long): Flow<Folder?> {
//        return folderDataSource.getFolder(folderId).flowOn(ioDispatcher)
//    }
//
//    override fun getChildFolders(parentFolderId: Long): Flow<List<Folder>> {
//        return folderDataSource.getChildFolders(parentFolderId).flowOn(ioDispatcher)
//    }
//
//    override fun getRootFoldersByType(type: FolderType): Flow<List<Folder>> {
//        return folderDataSource.getRootFoldersByType(type).flowOn(ioDispatcher)
//    }
//
//    override fun getAllFoldersByType(type: FolderType): Flow<List<Folder>> {
//        return folderDataSource.getAllFoldersByType(type).flowOn(ioDispatcher)
//    }
//
//    override suspend fun findFolderByNameAndParent(name: String, parentFolderId: Long?, type: FolderType): Folder? {
//        return withContext(ioDispatcher) {
//            folderDataSource.findFolderByNameAndParent(name, parentFolderId, type)
//        }
//    }
//
//    override suspend fun createFolder(
//        name: String,
//        parentFolderId: Long?,
//        type: FolderType,
//    ): Result<Long> = runCatching {
//        val trimmedName = name.trim()
//        if (trimmedName.isEmpty()) {
//            throw IllegalArgumentException("Folder name cannot be empty.")
//        }
//        // Basic reserved name check (can be expanded)
//        if (parentFolderId == null && (trimmedName.equals(Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_NAME, ignoreCase = true) || trimmedName.equals(Folder.DEFAULT_BLOCKED_ROOT_FOLDER_NAME, ignoreCase = true))) {
//            if (type == FolderType.BOOKMARK && trimmedName.equals(Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_NAME, ignoreCase = true)) {
//                // Allow finding default
//            } else if (type == FolderType.BLOCK && trimmedName.equals(Folder.DEFAULT_BLOCKED_ROOT_FOLDER_NAME, ignoreCase = true)) {
//                // Allow finding default
//            } else {
//                throw IllegalArgumentException("Cannot manually create folder with reserved root name '$trimmedName'.")
//            }
//        }
//
//
//        withContext(ioDispatcher) {
//            // Validate parent existence and type
//            if (parentFolderId != null) {
//                val parentFolder = folderDataSource.getFolder(parentFolderId).firstOrNull()
//                    ?: throw IllegalArgumentException("Parent folder with ID $parentFolderId not found.")
//                if (parentFolder.type != type) {
//                    throw IllegalArgumentException("Parent folder type (${parentFolder.type}) must match new folder type ($type).")
//                }
//            }
//
//            // Check uniqueness (name + parent + type)
//            val existing = folderDataSource.findFolderByNameAndParent(trimmedName, parentFolderId, type)
//            if (existing != null) {
//                // Handle case where user tries to create default folder again
//                if (existing.id == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID || existing.id == Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID) {
//                    Timber.w("Attempted to recreate default folder: Name='$trimmedName', Parent='$parentFolderId', Type='$type'. Returning existing ID.")
//                    return@withContext existing.id // Return existing default folder ID
//                }
//                throw IllegalStateException("A folder named '$trimmedName' already exists in this location with the same type.")
//            }
//
//            val now = instantProvider.now()
//            val newFolder = Folder(
//                name = trimmedName,
//                parentFolderId = parentFolderId,
//                type = type,
//                createdAt = now,
//                updatedAt = now
//                // id = 0 for auto-generation
//            )
//            folderDataSource.createFolder(newFolder)
//        }
//    }.onFailure { Timber.e(it, "Failed to create folder: Name='$name', Parent='$parentFolderId', Type='$type'") }
//
//    private suspend fun isDescendant(folderId: Long, potentialAncestorId: Long): Boolean {
//        var currentParentId = folderDataSource.getFolder(folderId).firstOrNull()?.parentFolderId
//        while (currentParentId != null) {
//            if (currentParentId == potentialAncestorId) return true
//            currentParentId = folderDataSource.getFolder(currentParentId).firstOrNull()?.parentFolderId
//        }
//        return false
//    }
//
//    override suspend fun updateFolder(folder: Folder): Result<Unit> = runCatching {
//        val trimmedName = folder.name.trim()
//        if (trimmedName.isEmpty()) {
//            throw IllegalArgumentException("Folder name cannot be empty.")
//        }
//        if (folder.id == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID || folder.id == Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID) {
//            throw IllegalArgumentException("Cannot modify the default root folders.")
//        }
//
//        withContext(ioDispatcher) {
//            // Get current state
//            val currentFolder = folderDataSource.getFolder(folder.id).firstOrNull()
//                ?: throw IllegalArgumentException("Folder with ID ${folder.id} not found for update.")
//
//            // Type cannot be changed
//            if (currentFolder.type != folder.type) {
//                throw IllegalArgumentException("Cannot change the type of an existing folder.")
//            }
//
//            // Validate potential parent change
//            if (currentFolder.parentFolderId != folder.parentFolderId) {
//                folder.parentFolderId?.let { parentFolderId ->
//                    if (parentFolderId == folder.id) {
//                        throw IllegalArgumentException("Cannot move a folder into itself.")
//                    }
//
//                    if (isDescendant(parentFolderId, folder.id)) {
//                        throw IllegalArgumentException("Cannot move a folder into its own descendant.")
//                    }
//                    val newParent = folderDataSource.getFolder(parentFolderId).firstOrNull()
//                        ?: throw IllegalArgumentException("New parent folder with ID $parentFolderId not found.")
//                    if (newParent.type != folder.type) {
//                        throw IllegalArgumentException("New parent folder type (${newParent.type}) must match folder type (${folder.type}).")
//                    }
//                }
//            }
//
//            // Check uniqueness in the *new* location if name or parent changed
//            if (currentFolder.name != trimmedName || currentFolder.parentFolderId != folder.parentFolderId) {
//                val conflictingFolder = folderDataSource.findFolderByNameAndParent(trimmedName, folder.parentFolderId, folder.type)
//                if (conflictingFolder != null && conflictingFolder.id != folder.id) {
//                    throw IllegalStateException("A folder named '$trimmedName' already exists in the target location with the same type.")
//                }
//            }
//
//            // Prepare updated folder entity (only update allowed fields)
//            val folderToUpdate = currentFolder.copy(
//                name = trimmedName,
//                parentFolderId = folder.parentFolderId,
//                updatedAt = instantProvider.now() // Update timestamp
//            )
//
//            val updated = folderDataSource.updateFolder(folderToUpdate)
//            if (!updated) {
//                // Should generally not happen if initial fetch succeeded, but good practice
//                throw IllegalStateException("Failed to update folder with ID ${folder.id} in data source.")
//            }
//        }
//    }.onFailure { e ->
//        Timber.e(e, "Failed to update folder: ID='${folder.id}', name='${folder.name}', parentFolderId='${folder.parentFolderId}', type='${folder.type}'")
//    }
//
//    @Transaction
//    override suspend fun deleteFolder(folderId: Long): Result<Unit> = browserPickerDatabase.withTransaction {
//        runCatching {
//            if (folderId == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID || folderId == Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID) {
//                throw IllegalArgumentException("Cannot delete the default root folders.")
//            }
//
//            val hasChildren = folderDataSource.hasChildFolders(folderId)
//            if (hasChildren) {
//                throw IllegalStateException("Folder with ID $folderId has child folders and cannot be deleted directly.")
//            }
//            hostRuleDataSource.clearFolderIdForRules(folderId)
//
//            val deleted = folderDataSource.deleteFolder(folderId)
//            if (!deleted) {
//                Timber.w("Folder with ID $folderId not found during deletion attempt inside transaction.")
//            }
//        }.onFailure { Timber.e(it, "Failed to delete folder: ID='$folderId' inside transaction") }
//    }
//}