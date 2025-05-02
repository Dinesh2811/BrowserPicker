package browserpicker.data.local.repository

import androidx.room.withTransaction
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.results.AppError
import browserpicker.core.results.MyResult
import browserpicker.data.FolderNotEmptyException
import browserpicker.data.local.datasource.FolderLocalDataSource
import browserpicker.data.local.datasource.HostRuleLocalDataSource
import browserpicker.data.local.db.BrowserPickerDatabase
import browserpicker.data.local.mapper.FolderMapper
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import browserpicker.domain.repository.FolderRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepositoryImpl @Inject constructor(
    private val folderDataSource: FolderLocalDataSource,
    private val hostRuleDataSource: HostRuleLocalDataSource,
    private val instantProvider: InstantProvider,
    private val browserPickerDatabase: BrowserPickerDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
): FolderRepository {

    private fun isReservedRootName(name: String): Boolean {
        return name.equals(Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_NAME, ignoreCase = true) ||
                name.equals(Folder.DEFAULT_BLOCKED_ROOT_FOLDER_NAME, ignoreCase = true)
    }

    override suspend fun ensureDefaultFoldersExist() {
        withContext(ioDispatcher) {
            try {
                folderDataSource.ensureDefaultFoldersExist()
            } catch (e: Exception) {
                Timber.e(e, "[Repository] Failed to ensure default folders exist")
            }
        }
    }

    override fun getFolder(folderId: Long): Flow<Folder?> {
        return folderDataSource.getFolder(folderId)
            .map { entity ->
                entity?.let { FolderMapper.toDomainModel(it) }
            }
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching Folder by id: %d", folderId)
                emit(null)
            }
            .flowOn(ioDispatcher)
    }

    override fun getChildFolders(parentFolderId: Long): Flow<List<Folder>> {
        return folderDataSource.getChildFolders(parentFolderId)
            .map { entities ->
                FolderMapper.toDomainModels(entities)
            }
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching child folders for parentId: %d", parentFolderId)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getRootFoldersByType(type: FolderType): Flow<List<Folder>> {
        if (type == FolderType.UNKNOWN) {
            Timber.w("[Repository] Requesting root folders with UNKNOWN type, returning empty list.")
            return flowOf(emptyList())
        }
        return folderDataSource.getRootFoldersByType(type)
            .map { entities ->
                FolderMapper.toDomainModels(entities)
            }
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching root folders for type: %s", type)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getAllFoldersByType(type: FolderType): Flow<List<Folder>> {
        if (type == FolderType.UNKNOWN) {
            Timber.w("[Repository] Requesting all folders with UNKNOWN type, returning empty list.")
            return flowOf(emptyList())
        }
        return folderDataSource.getAllFoldersByType(type)
            .map { entities ->
                FolderMapper.toDomainModels(entities)
            }
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching all folders for type: %s", type)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun findFolderByNameAndParent(name: String, parentFolderId: Long?, type: FolderType): Folder? {
        if (type == FolderType.UNKNOWN) {
            Timber.w("[Repository] Finding folder with UNKNOWN type, returning null.")
            return null
        }
        return runCatching {
            withContext(ioDispatcher) {
                val entity = folderDataSource.findFolderByNameAndParent(name.trim(), parentFolderId, type)
                entity?.let { FolderMapper.toDomainModel(it) }
            }
        }.onFailure { e ->
            Timber.e(e, "[Repository] Failed to find folder by name/parent: Name='%s', Parent='%s', Type='%s'", name, parentFolderId, type)
        }.getOrNull()
    }

    override suspend fun createFolder(
        name: String,
        parentFolderId: Long?,
        type: FolderType,
    ): MyResult<Long, AppError> = browserPickerDatabase.withTransaction {
        try {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) {
                throw IllegalArgumentException("Folder name cannot be empty.")
            }
            if (type == FolderType.UNKNOWN) {
                throw IllegalArgumentException("Cannot create folder with UNKNOWN type.")
            }

            if (parentFolderId == null && isReservedRootName(trimmedName)) {
                val existingDefaultEntity = folderDataSource.findFolderByNameAndParent(trimmedName, parentFolderId, type)
                if (existingDefaultEntity != null && (existingDefaultEntity.id == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID || existingDefaultEntity.id == Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID)) {
                    Timber.w("[Repository] Attempted to recreate default folder: Name='$trimmedName', Type='$type'. Returning existing ID ${existingDefaultEntity.id}.")
                    return@withTransaction MyResult.Success(existingDefaultEntity.id)
                } else {
                    throw IllegalArgumentException("Cannot create folder with reserved root name '$trimmedName'.")
                }
            }

            if (parentFolderId != null) {
                val parentFolderEntity = folderDataSource.getFolderByIdSuspend(parentFolderId)
                    ?: throw IllegalStateException("Parent folder with ID $parentFolderId not found during creation.")
                val parentFolderType = FolderType.fromValue(parentFolderEntity.folderType)
                if (parentFolderType != type) {
                    throw IllegalArgumentException("Parent folder type ($parentFolderType) must match new folder type ($type).")
                }
            }

            val existingEntity = folderDataSource.findFolderByNameAndParent(trimmedName, parentFolderId, type)
            if (existingEntity != null) {
                throw IllegalStateException("A folder named '$trimmedName' already exists in this location with the same type (ID: ${existingEntity.id}).")
            }

            val now = instantProvider.now()
            val newFolder = Folder(
                id = 0,
                name = trimmedName,
                parentFolderId = parentFolderId,
                type = type,
                createdAt = now,
                updatedAt = now
            )
            val newFolderEntity = FolderMapper.toEntity(newFolder)
            val folderId = folderDataSource.createFolder(newFolderEntity)
            MyResult.Success(folderId)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create folder: Name='$name', Parent='$parentFolderId', Type='$type'")
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message ?: "Invalid input data", e)
                is IllegalStateException -> AppError.DataIntegrityError(e.message ?: "Data integrity or state issue", e)
                else -> AppError.UnknownError("Failed to create folder", e)
            }
            MyResult.Error(appError)
        }
    }

    private suspend fun isDescendantRecursive(folderId: Long?, targetAncestorId: Long): Boolean {
        if (folderId == null) return false
        if (folderId == targetAncestorId) return true

        val parentId = folderDataSource.getFolderByIdSuspend(folderId)?.parentFolderId
        return isDescendantRecursive(parentId, targetAncestorId)
    }

    override suspend fun updateFolder(folder: Folder): MyResult<Unit, AppError> = browserPickerDatabase.withTransaction {
        try {
            Timber.tag("FolderRepo").d("Attempting to update folder: id='%d', name='%s', parentId='%s'", folder.id, folder.name, folder.parentFolderId)
            val trimmedName = folder.name.trim()
            if (trimmedName.isEmpty()) {
                throw IllegalArgumentException("Folder name cannot be empty.")
            }
            if (folder.id == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID || folder.id == Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID) {
                throw IllegalArgumentException("Cannot modify the default root folders.")
            }
            if (folder.parentFolderId == null && isReservedRootName(trimmedName)) {
                throw IllegalArgumentException("Cannot rename a root folder to the reserved name '$trimmedName'.")
            }

            val currentFolderEntity = folderDataSource.getFolderByIdSuspend(folder.id)
                ?: throw IllegalStateException("Folder with ID ${folder.id} not found for update during transaction.")
            val currentFolderDomain = FolderMapper.toDomainModel(currentFolderEntity)

            if (currentFolderDomain.type != folder.type) {
                throw IllegalArgumentException("Cannot change the type of an existing folder (from ${currentFolderDomain.type} to ${folder.type}).")
            }

            if (currentFolderDomain.parentFolderId != folder.parentFolderId) {
                folder.parentFolderId?.let { newParentId ->
                    if (newParentId == folder.id) {
                        throw IllegalArgumentException("Cannot move a folder into itself.")
                    }
                    if (isDescendantRecursive(newParentId, folder.id)) {
                        throw IllegalStateException("Cannot move folder ID ${folder.id} into its own descendant (potential new parent ID $newParentId). Circular reference detected.")
                    }
                    val newParentEntity = folderDataSource.getFolderByIdSuspend(newParentId)
                        ?: throw IllegalStateException("New parent folder with ID $newParentId not found during transaction.")
                    val newParentType = FolderType.fromValue(newParentEntity.folderType)
                    if (newParentType != folder.type) {
                        throw IllegalArgumentException("New parent folder type ($newParentType) must match folder type (${folder.type}).")
                    }
                }
            }

            if (currentFolderDomain.name != trimmedName || currentFolderDomain.parentFolderId != folder.parentFolderId) {
                val conflictingFolderEntity = folderDataSource.findFolderByNameAndParent(trimmedName, folder.parentFolderId, folder.type)
                if (conflictingFolderEntity != null && conflictingFolderEntity.id != folder.id) {
                    throw IllegalStateException("A folder named '$trimmedName' already exists in the target location with the same type (ID: ${conflictingFolderEntity.id}) during transaction.")
                }
            }

            val updatedFolder = currentFolderDomain.copy(
                name = trimmedName,
                parentFolderId = folder.parentFolderId,
                updatedAt = instantProvider.now()
            )

            val folderEntityToUpdate = FolderMapper.toEntity(updatedFolder)
            val updated = folderDataSource.updateFolder(folderEntityToUpdate)
            if (!updated) {
                throw IllegalStateException("Failed to update folder with ID ${folder.id} in data source during transaction (record might not exist anymore or update failed).")
            }
            MyResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed transaction to update folder: ID='${folder.id}', name='${folder.name}', parentFolderId='${folder.parentFolderId}', type='${folder.type}'")
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message ?: "Invalid input data", e)
                is IllegalStateException -> AppError.DataIntegrityError(e.message ?: "Data integrity or state issue", e)
                is FolderNotEmptyException -> AppError.FolderNotEmptyError(folder.id, e.message, e)
                else -> AppError.UnknownError("Failed to update folder", e)
            }
            MyResult.Error(appError)
        }
    }

    override suspend fun deleteFolder(folderId: Long): MyResult<Unit, AppError> = browserPickerDatabase.withTransaction {
        try {
            if (folderId == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID || folderId == Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID) {
                throw IllegalArgumentException("Cannot delete the default root folders.")
            }

            val hasChildren = folderDataSource.hasChildFolders(folderId)
            if (hasChildren) {
                throw FolderNotEmptyException(folderId)
            }
            hostRuleDataSource.clearFolderIdForRules(folderId)

            val deleted = folderDataSource.deleteFolder(folderId)
            if (!deleted) {
                throw IllegalStateException("Folder with ID $folderId not found for deletion or delete failed.")
            }
            MyResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed transaction to delete folder: ID='$folderId'")
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message ?: "Invalid input data", e)
                is FolderNotEmptyException -> AppError.FolderNotEmptyError(folderId, e.message, e)
                is IllegalStateException -> AppError.DataNotFound(e.message ?: "Folder not found or delete failed", e)
                else -> AppError.UnknownError("Failed to delete folder", e)
            }
            MyResult.Error(appError)
        }
    }
}