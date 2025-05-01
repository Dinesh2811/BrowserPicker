package browserpicker.data.local.repository

import androidx.paging.*
import androidx.room.*
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.results.AppError
import browserpicker.core.results.MyResult
import browserpicker.core.results.UriValidationError
import browserpicker.data.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import browserpicker.data.local.datasource.*
import browserpicker.data.local.db.BrowserPickerDatabase
import browserpicker.data.local.query.model.*
import browserpicker.domain.model.*
import browserpicker.domain.model.query.*
import browserpicker.domain.repository.*
import browserpicker.domain.service.ParsedUri
import browserpicker.domain.service.UriParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UriHistoryRepositoryImpl @Inject constructor(
    private val dataSource: UriHistoryLocalDataSource,
    private val uriParser: UriParser,
    private val instantProvider: InstantProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
): UriHistoryRepository {
    private fun mapQueryToConfig(query: UriHistoryQuery): UriRecordQueryConfig {
        return UriRecordQueryConfig(
            searchQuery = query.searchQuery,
            filterByUriSource = query.filterByUriSource,
            filterByInteractionAction = query.filterByInteractionAction,
            filterByChosenBrowser = query.filterByChosenBrowser,
            filterByHost = query.filterByHost,
            filterByDateRange = query.filterByDateRange,
            sortBy = query.sortBy,
            sortOrder = query.sortOrder,
            groupBy = query.groupBy,
            groupSortOrder = query.groupSortOrder,
            advancedFilters = query.advancedFilters
        )
    }

    override fun getPagedUriRecords(query: UriHistoryQuery, pagingConfig: PagingConfig): Flow<PagingData<UriRecord>> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getPagedUriRecords(dataQueryConfig, pagingConfig).flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create PagedUriRecords Flow for query: %s", query)
            flowOf(PagingData.empty<UriRecord>()).flowOn(ioDispatcher)
        }
    }

    override fun getTotalUriRecordCount(query: UriHistoryQuery): Flow<Long> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getTotalUriRecordCount(dataQueryConfig)
                .catch { e ->
                    Timber.e(e, "[Repository] Error fetching total URI record count for query: %s", query)
                    emit(0)
                }
                .flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create TotalUriRecordCount Flow for query: %s", query)
            flowOf(0L).flowOn(ioDispatcher)
        }
    }

    override fun getGroupCounts(query: UriHistoryQuery): Flow<List<GroupCount>> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getGroupCounts(dataQueryConfig).map { dataGroupCounts ->
                dataGroupCounts.map { GroupCount(it.groupValue, it.count) }
            }.catch { e ->
                Timber.e(e, "[Repository] Error fetching group counts for query: %s", query)
                emit(emptyList())
            }.flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create GroupCounts Flow for query: %s", query)
            flowOf(emptyList<GroupCount>())
        }
    }

    override fun getDateCounts(query: UriHistoryQuery): Flow<List<DateCount>> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getDateCounts(dataQueryConfig).map { dataDateCounts ->
                dataDateCounts.map { DateCount(it.date, it.count) }
            }.catch { e ->
                Timber.e(e, "[Repository] Error fetching date counts for query: %s", query)
                emit(emptyList())
            }.flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create DateCounts Flow for query: %s", query)
            flowOf(emptyList<DateCount>())
        }
    }

    override suspend fun addUriRecord(
        uriString: String,
        host: String,
        source: UriSource,
        action: InteractionAction,
        chosenBrowser: String?,
        associatedHostRuleId: Long?,
    ): MyResult<Long, AppError> = withContext(ioDispatcher) {
        try {
            when {
                uriString.isBlank() -> throw IllegalArgumentException("URI string cannot be blank or empty")
                host.isBlank() -> throw IllegalArgumentException("Host cannot be blank or empty")
                source == UriSource.UNKNOWN -> throw IllegalArgumentException("URI Source cannot be UNKNOWN; use a valid source type")
                action == InteractionAction.UNKNOWN -> throw IllegalArgumentException("Interaction Action cannot be UNKNOWN; use a valid action type")
            }
            if (chosenBrowser != null) {
                if (chosenBrowser.isBlank()) throw IllegalArgumentException("Chosen browser package name cannot be blank if provided.")
            }
            val parsedUriResult = uriParser.parseAndValidateWebUri(uriString)
            if (parsedUriResult.isError()) {
                return@withContext MyResult.Error(parsedUriResult.errorOrNull()!!)
            }

            val record = UriRecord(
                uriString = uriString,
                host = host,
                uriSource = source,
                interactionAction = action,
                chosenBrowserPackage = chosenBrowser,
                timestamp = instantProvider.now(),
                associatedHostRuleId = associatedHostRuleId
            )

            val id = dataSource.insertUriRecord(record)
            if (id <= 0) {
                throw IllegalStateException("Failed to insert URI record: received invalid ID $id")
            }

            MyResult.Success(id)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to add URI record: $uriString, host=$host, source=$source, action=$action, browser=$chosenBrowser")
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message?: "Invalid input data", e)
                is IllegalStateException -> AppError.DataIntegrityError(e.message?: "Data integrity issue", e)
                is UriValidationError -> e
                else -> AppError.UnknownError("Failed to add URI record", e)
            }
            MyResult.Error(appError)
        }
    }

    override suspend fun getUriRecord(id: Long): MyResult<UriRecord?, AppError> = withContext(ioDispatcher) {
        try {
            val record = dataSource.getUriRecord(id)
            MyResult.Success(record)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to get URI record with id: %d", id)
            val appError = when(e) {
                is IllegalArgumentException -> AppError.DataIntegrityError("Data mapping error for record $id", e)
                else -> AppError.UnknownError("Failed to get URI record $id", e)
            }
            MyResult.Error(appError)
        }
    }

    override suspend fun deleteUriRecord(id: Long): MyResult<Unit, AppError> = withContext(ioDispatcher) {
        try {
            val deleted = dataSource.deleteUriRecord(id)
            if (deleted) {
                MyResult.Success(Unit)
            } else {
                Timber.w("[Repository] URI record with id: $id not found for deletion or delete failed in data source. Reporting as success (item not present).")
                MyResult.Success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to delete URI record with id: %d", id)
            MyResult.Error(AppError.UnknownError("Failed to delete URI record $id", e))
        }
    }

    override suspend fun deleteAllUriRecords(): MyResult<Int, AppError> = withContext(ioDispatcher) {
        try {
            val count = dataSource.deleteAllUriRecords()
            MyResult.Success(count)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to delete all URI records")
            MyResult.Error(AppError.UnknownError("Failed to delete all URI records", e))
        }
    }

    override fun getDistinctHosts(): Flow<List<String>> {
        return dataSource.getDistinctHosts()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching distinct hosts")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getDistinctChosenBrowsers(): Flow<List<String?>> {
        return dataSource.getDistinctChosenBrowsers()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching distinct chosen browsers")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }
}

@Singleton
class HostRuleRepositoryImpl @Inject constructor(
    private val hostRuleDataSource: HostRuleLocalDataSource,
    private val folderDataSource: FolderLocalDataSource,
    private val instantProvider: InstantProvider,
    private val browserPickerDatabase: BrowserPickerDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
): HostRuleRepository {

    override fun getHostRuleByHost(host: String): Flow<HostRule?> {
        return hostRuleDataSource.getHostRuleByHost(host)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching HostRule for host: %s", host)
                emit(null)
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getHostRuleById(id: Long): MyResult<HostRule?, AppError> = withContext(ioDispatcher) {
        try {
            val rule = hostRuleDataSource.getHostRuleById(id)
            MyResult.Success(rule)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to get HostRule by ID: %d", id)
            val appError = when(e) {
                is IllegalArgumentException -> AppError.DataIntegrityError("Data mapping error for host rule $id", e)
                else -> AppError.UnknownError("Failed to get host rule $id", e)
            }
            MyResult.Error(appError)
        }
    }

    override fun getAllHostRules(): Flow<List<HostRule>> {
        return hostRuleDataSource.getAllHostRules()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching all HostRules")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getHostRulesByStatus(status: UriStatus): Flow<List<HostRule>> {
        return hostRuleDataSource.getHostRulesByStatus(status)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching HostRules by status: %s", status)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getHostRulesByFolder(folderId: Long): Flow<List<HostRule>> {
        return hostRuleDataSource.getHostRulesByFolder(folderId)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching HostRules by folderId: %d", folderId)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getRootHostRulesByStatus(status: UriStatus): Flow<List<HostRule>> {
        return hostRuleDataSource.getRootHostRulesByStatus(status)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching root HostRules by status: %s", status)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getDistinctRuleHosts(): Flow<List<String>> {
        return hostRuleDataSource.getDistinctRuleHosts()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching distinct rule hosts")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveHostRule(
        host: String,
        status: UriStatus,
        folderId: Long?,
        preferredBrowser: String?,
        isPreferenceEnabled: Boolean,
    ): MyResult<Long, AppError> = browserPickerDatabase.withTransaction {
        try {
            val trimmedHost = host.trim()
            if (trimmedHost.isEmpty()) {
                throw IllegalArgumentException("Host cannot be blank.")
            }
            if (status == UriStatus.UNKNOWN) {
                throw IllegalArgumentException("Cannot save rule with UNKNOWN status.")
            }
            val trimmedPreferredBrowser = preferredBrowser?.trim()
            if (trimmedPreferredBrowser != null && trimmedPreferredBrowser.isEmpty()) {
                throw IllegalArgumentException("Preferred browser package cannot be blank if provided.")
            }

            val now = instantProvider.now()
            val currentRule = hostRuleDataSource.getHostRuleByHost(trimmedHost).firstOrNull()

            var effectiveFolderId = folderId
            var effectivePreferredBrowser = trimmedPreferredBrowser
            var effectiveIsPreferenceEnabled = isPreferenceEnabled

            if (status == UriStatus.BLOCKED) {
                effectivePreferredBrowser = null
                effectiveIsPreferenceEnabled = false
            }

            if (status == UriStatus.NONE) {
                effectiveFolderId = null
                effectivePreferredBrowser = null
                effectiveIsPreferenceEnabled = false
            }

            if (effectiveFolderId != null && (status == UriStatus.BOOKMARKED || status == UriStatus.BLOCKED)) {
                val folder = folderDataSource.getFolderByIdSuspend(effectiveFolderId)
                if (folder == null) {
                    throw IllegalStateException("Folder with ID $effectiveFolderId does not exist.")
                }
                val expectedFolderType = status.toFolderType()

                if (expectedFolderType == null) {
                    Timber.w("Rule status $status unexpectedly requires a folder check but has no matching FolderType. Clearing folder ID.")
                    effectiveFolderId = null
                } else if (folder.type != expectedFolderType) {
                    throw IllegalArgumentException("Folder type mismatch: Rule status ($status) requires folder type $expectedFolderType, but folder $effectiveFolderId has type ${folder.type}.")
                }
            } else if (status != UriStatus.NONE) {
                effectiveFolderId = null
            }

            val ruleToSave = HostRule(
                id = currentRule?.id ?: 0,
                host = trimmedHost,
                uriStatus = status,
                folderId = effectiveFolderId,
                preferredBrowserPackage = effectivePreferredBrowser,
                isPreferenceEnabled = effectiveIsPreferenceEnabled,
                createdAt = currentRule?.createdAt ?: now,
                updatedAt = now
            )

            val ruleId = hostRuleDataSource.upsertHostRule(ruleToSave)
            MyResult.Success(ruleId)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed transaction to save host rule: host=$host, status=$status, folderId=$folderId, preferredBrowser=$preferredBrowser \n ${e.message}")
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message?: "Invalid input data", e)
                is IllegalStateException -> AppError.DataIntegrityError(e.message?: "Data integrity or state issue", e)
                else -> AppError.UnknownError("Failed to save host rule", e)
            }
            MyResult.Error(appError)
        }
    }

    override suspend fun deleteHostRuleById(id: Long): MyResult<Unit, AppError> = withContext(ioDispatcher) {
        try {
            val deleted = hostRuleDataSource.deleteHostRuleById(id)
            if (deleted) {
                MyResult.Success(Unit)
            } else {
                Timber.w("[Repository] Host rule with ID $id not found for deletion or delete failed. Reporting as success.")
                MyResult.Success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to delete host rule by ID: $id")
            MyResult.Error(AppError.UnknownError("Failed to delete host rule $id", e))
        }
    }

    override suspend fun deleteHostRuleByHost(host: String): MyResult<Unit, AppError> = withContext(ioDispatcher) {
        try {
            val trimmedHost = host.trim()
            if (trimmedHost.isEmpty()) throw IllegalArgumentException("Host cannot be blank for deletion.")
            val deleted = hostRuleDataSource.deleteHostRuleByHost(trimmedHost)
            if (deleted) {
                MyResult.Success(Unit)
            } else {
                Timber.w("[Repository] Host rule for host '$trimmedHost' not found for deletion or delete failed. Reporting as success.")
                MyResult.Success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to delete host rule by host: $host")
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message?: "Invalid input data", e)
                else -> AppError.UnknownError("Failed to delete host rule by host", e)
            }
            MyResult.Error(appError)
        }
    }

    override suspend fun clearFolderAssociation(folderId: Long): MyResult<Unit, AppError> = withContext(ioDispatcher) {
        try {
            hostRuleDataSource.clearFolderIdForRules(folderId)
            MyResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to clear folder association for folderId: $folderId")
            MyResult.Error(AppError.UnknownError("Failed to clear folder association for folder $folderId", e))
        }
    }

}

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
                // This is a critical startup operation. Log error but don't return MyResult here
                // as this is likely called during app initialization and failure may be unrecoverable
                Timber.e(e, "[Repository] Failed to ensure default folders exist")
            }
        }
    }

    override fun getFolder(folderId: Long): Flow<Folder?> {
        return folderDataSource.getFolder(folderId)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching Folder by id: %d", folderId)
                emit(null)
            }
            .flowOn(ioDispatcher)
    }

    override fun getChildFolders(parentFolderId: Long): Flow<List<Folder>> {
        return folderDataSource.getChildFolders(parentFolderId)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching child folders for parentId: %d", parentFolderId)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getRootFoldersByType(type: FolderType): Flow<List<Folder>> {
        return folderDataSource.getRootFoldersByType(type)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching root folders for type: %s", type)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getAllFoldersByType(type: FolderType): Flow<List<Folder>> {
        return folderDataSource.getAllFoldersByType(type)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching all folders for type: %s", type)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun findFolderByNameAndParent(name: String, parentFolderId: Long?, type: FolderType): Folder? {
        return runCatching {
            withContext(ioDispatcher) {
                folderDataSource.findFolderByNameAndParent(name.trim(), parentFolderId, type)
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
            if (parentFolderId == null && isReservedRootName(trimmedName)) {
                val existingDefault = findFolderByNameAndParent(trimmedName, parentFolderId, type)
                if (existingDefault != null && (existingDefault.id == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID || existingDefault.id == Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID)) {
                    Timber.w("[Repository] Attempted to recreate default folder: Name='$trimmedName', Type='$type'. Returning existing ID ${existingDefault.id}.")
                    return@withTransaction MyResult.Success(existingDefault.id)
                } else {
                    throw IllegalArgumentException("Cannot create folder with reserved root name '$trimmedName'.")
                }
            }

            if (parentFolderId != null) {
                val parentFolder = folderDataSource.getFolderByIdSuspend(parentFolderId)
                    ?: throw IllegalStateException("Parent folder with ID $parentFolderId not found during creation.")
                if (parentFolder.type != type) {
                    throw IllegalArgumentException("Parent folder type (${parentFolder.type}) must match new folder type ($type).")
                }
            }

            val existing = folderDataSource.findFolderByNameAndParent(trimmedName, parentFolderId, type)
            if (existing != null) {
                throw IllegalStateException("A folder named '$trimmedName' already exists in this location with the same type (ID: ${existing.id}).")
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
            val folderId = folderDataSource.createFolder(newFolder)
            MyResult.Success(folderId)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create folder: Name='$name', Parent='$parentFolderId', Type='$type'")
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message?: "Invalid input data", e)
                is IllegalStateException -> AppError.DataIntegrityError(e.message?: "Data integrity or state issue", e)
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

            val currentFolder = folderDataSource.getFolderByIdSuspend(folder.id)
                ?: throw IllegalStateException("Folder with ID ${folder.id} not found for update during transaction.") // Should exist if starting update

            if (currentFolder.type != folder.type) {
                throw IllegalArgumentException("Cannot change the type of an existing folder (from ${currentFolder.type} to ${folder.type}).")
            }

            if (currentFolder.parentFolderId != folder.parentFolderId) {
                folder.parentFolderId?.let { newParentId ->
                    if (newParentId == folder.id) {
                        throw IllegalArgumentException("Cannot move a folder into itself.")
                    }
                    if (isDescendantRecursive(newParentId, folder.id)) {
                        throw IllegalStateException("Cannot move folder ID ${folder.id} into its own descendant (potential new parent ID $newParentId). Circular reference detected.")
                    }
                    val newParent = folderDataSource.getFolderByIdSuspend(newParentId)
                        ?: throw IllegalStateException("New parent folder with ID $newParentId not found during transaction.") // Should exist if parentId is set
                    if (newParent.type != folder.type) {
                        throw IllegalArgumentException("New parent folder type (${newParent.type}) must match folder type (${folder.type}).")
                    }
                }
            }

            if (currentFolder.name != trimmedName || currentFolder.parentFolderId != folder.parentFolderId) {
                val conflictingFolder = folderDataSource.findFolderByNameAndParent(trimmedName, folder.parentFolderId, folder.type)
                if (conflictingFolder != null && conflictingFolder.id != folder.id) {
                    throw IllegalStateException("A folder named '$trimmedName' already exists in the target location with the same type (ID: ${conflictingFolder.id}) during transaction.")
                }
            }

            val folderToUpdate = currentFolder.copy(
                name = trimmedName,
                parentFolderId = folder.parentFolderId,
                updatedAt = instantProvider.now()
            )

            val updated = folderDataSource.updateFolder(folderToUpdate)
            if (!updated) {
                // This case is unlikely if the folder existed at the start of the transaction,
                // but can indicate a serious data integrity issue or concurrent modification.
                throw IllegalStateException("Failed to update folder with ID ${folder.id} in data source during transaction (record might not exist anymore or update failed).")
            }
            MyResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed transaction to update folder: ID='${folder.id}', name='${folder.name}', parentFolderId='${folder.parentFolderId}', type='${folder.type}'")
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message?: "Invalid input data", e)
                is IllegalStateException -> AppError.DataIntegrityError(e.message?: "Data integrity or state issue", e)
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
                // This could happen if the folder was deleted concurrently outside the transaction,
                // or if the folderId was invalid to begin with. Report as DataNotFound if delete count is 0.
                throw IllegalStateException("Folder with ID $folderId not found for deletion or delete failed.")
            }
            MyResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed transaction to delete folder: ID='$folderId'")
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message?: "Invalid input data", e)
                is FolderNotEmptyException -> AppError.FolderNotEmptyError(folderId, e.message, e)
                is IllegalStateException -> AppError.DataNotFound(e.message?: "Folder not found or delete failed", e)
                else -> AppError.UnknownError("Failed to delete folder", e)
            }
            MyResult.Error(appError)
        }
    }
}

@Singleton
class BrowserStatsRepositoryImpl @Inject constructor(
    private val dataSource: BrowserStatsLocalDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
): BrowserStatsRepository {

    override suspend fun recordBrowserUsage(packageName: String): MyResult<Unit, AppError> = withContext(ioDispatcher) {
        try {
            if (packageName.isBlank()) throw IllegalArgumentException("Package name cannot be blank.")
            dataSource.recordBrowserUsage(packageName)
            MyResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to record browser usage for: $packageName")
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message?: "Invalid input data", e)
                else -> AppError.UnknownError("Failed to record browser usage", e)
            }
            MyResult.Error(appError)
        }
    }

    override fun getBrowserStat(packageName: String): Flow<BrowserUsageStat?> {
        return dataSource.getBrowserStat(packageName)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching browser stat for: %s", packageName)
                emit(null)
            }
            .flowOn(ioDispatcher)
    }

    override fun getAllBrowserStats(): Flow<List<BrowserUsageStat>> {
        return dataSource.getAllBrowserStats()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching all browser stats")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getAllBrowserStatsSortedByLastUsed(): Flow<List<BrowserUsageStat>> {
        return dataSource.getAllBrowserStatsSortedByLastUsed()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching all browser stats sorted by last used")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun deleteBrowserStat(packageName: String): MyResult<Unit, AppError> = withContext(ioDispatcher) {
        try {
            if (packageName.isBlank()) throw IllegalArgumentException("Package name cannot be blank.")
            val deleted = dataSource.deleteBrowserStat(packageName)
            if (deleted) {
                MyResult.Success(Unit)
            } else {
                Timber.w("[Repository] Browser stat for '$packageName' not found or delete failed. Reporting as success.")
                MyResult.Success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to delete browser stat for: $packageName")
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message?: "Invalid input data", e)
                else -> AppError.UnknownError("Failed to delete browser stat", e)
            }
            MyResult.Error(appError)
        }
    }

    override suspend fun deleteAllStats(): MyResult<Unit, AppError> = withContext(ioDispatcher) {
        try {
            val count = dataSource.deleteAllStats()
            Timber.d("[Repository] Deleted $count browser stats.")
            MyResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to delete all browser stats")
            MyResult.Error(AppError.UnknownError("Failed to delete all stats", e))
        }
    }
}


/*
package browserpicker.data.local.repository

import androidx.paging.*
import androidx.room.*
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.results.MyResult
import browserpicker.core.results.UriValidationError
import browserpicker.data.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import browserpicker.data.local.datasource.*
import browserpicker.data.local.db.BrowserPickerDatabase
import browserpicker.data.local.query.model.*
import browserpicker.domain.model.*
import browserpicker.domain.model.query.*
import browserpicker.domain.repository.*
import browserpicker.domain.service.ParsedUri
import browserpicker.domain.service.UriParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UriHistoryRepositoryImpl @Inject constructor(
    private val dataSource: UriHistoryLocalDataSource,
    private val uriParser: UriParser,
    private val instantProvider: InstantProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
): UriHistoryRepository {
    private fun mapQueryToConfig(query: UriHistoryQuery): UriRecordQueryConfig {
        return UriRecordQueryConfig(
            searchQuery = query.searchQuery,
            filterByUriSource = query.filterByUriSource,
            filterByInteractionAction = query.filterByInteractionAction,
            filterByChosenBrowser = query.filterByChosenBrowser,
            filterByHost = query.filterByHost,
            filterByDateRange = query.filterByDateRange,
            sortBy = query.sortBy,
            sortOrder = query.sortOrder,
            groupBy = query.groupBy,
            groupSortOrder = query.groupSortOrder,
            advancedFilters = query.advancedFilters
        )
    }

    override fun getPagedUriRecords(query: UriHistoryQuery, pagingConfig: PagingConfig): Flow<PagingData<UriRecord>> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getPagedUriRecords(dataQueryConfig, pagingConfig).flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create PagedUriRecords Flow for query: %s", query)
            flowOf(PagingData.empty<UriRecord>()).flowOn(ioDispatcher)
        }
    }

    override fun getTotalUriRecordCount(query: UriHistoryQuery): Flow<Long> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getTotalUriRecordCount(dataQueryConfig)
                .catch { e ->
                    Timber.e(e, "[Repository] Error fetching total URI record count for query: %s", query)
                    emit(0)
                }
                .flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create TotalUriRecordCount Flow for query: %s", query)
            flowOf(0L).flowOn(ioDispatcher)
        }
    }

    override fun getGroupCounts(query: UriHistoryQuery): Flow<List<GroupCount>> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getGroupCounts(dataQueryConfig).map { dataGroupCounts ->
                dataGroupCounts.map { GroupCount(it.groupValue, it.count) }
            }.catch { e ->
                Timber.e(e, "[Repository] Error fetching group counts for query: %s", query)
                emit(emptyList())
            }.flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create GroupCounts Flow for query: %s", query)
            flowOf(emptyList<GroupCount>())
        }
    }

    override fun getDateCounts(query: UriHistoryQuery): Flow<List<DateCount>> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getDateCounts(dataQueryConfig).map { dataDateCounts ->
                dataDateCounts.map { DateCount(it.date, it.count) }
            }.catch { e ->
                Timber.e(e, "[Repository] Error fetching date counts for query: %s", query)
                emit(emptyList())
            }.flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create DateCounts Flow for query: %s", query)
            flowOf(emptyList<DateCount>())
        }
    }

    override suspend fun addUriRecord(
        uriString: String,
        host: String,
        source: UriSource,
        action: InteractionAction,
        chosenBrowser: String?,
        associatedHostRuleId: Long?,
    ): Result<Long> = runCatching {
        withContext(ioDispatcher) {
            when {
                uriString.isBlank() -> throw InvalidInputDataException("URI string cannot be blank or empty")
                host.isBlank() -> throw InvalidInputDataException("Host cannot be blank or empty")
                source == UriSource.UNKNOWN -> throw InvalidInputDataException("URI Source cannot be UNKNOWN; use a valid source type")
                action == InteractionAction.UNKNOWN -> throw InvalidInputDataException("Interaction Action cannot be UNKNOWN; use a valid action type")
            }
            if (chosenBrowser != null) {
                if (chosenBrowser.isBlank()) throw InvalidInputDataException("Chosen browser package name cannot be blank if provided.")
            }
            uriParser.parseAndValidateWebUri(uriString).getOrThrow()

            val record = UriRecord(
                uriString = uriString,
                host = host,
                uriSource = source,
                interactionAction = action,
                chosenBrowserPackage = chosenBrowser,
                timestamp = instantProvider.now(),
                associatedHostRuleId = associatedHostRuleId
            )

            val id = dataSource.insertUriRecord(record)
            if (id <= 0) {
                throw DataIntegrityException("Failed to insert URI record: received invalid ID $id")
            }

            id
        }
    }.onFailure { e ->
        when (e) {
            is InvalidInputDataException -> Timber.e(e, "[Repository] Invalid input for adding URI record: %s", e.message)
            is DataIntegrityException -> Timber.e(e, "[Repository] Data integrity issue when adding URI record: %s", e.message)
            is UriValidationError -> Timber.e(e, "[Repository] URI validation failed when adding URI record: %s", e.message)
            else -> Timber.e(e, "[Repository] Failed to add URI record: $uriString, host=$host, source=$source, action=$action, browser=$chosenBrowser")
        }
    }

    override suspend fun getUriRecord(id: Long): UriRecord? = runCatching {
        withContext(ioDispatcher) {
            dataSource.getUriRecord(id)
        }
    }.onFailure { e ->
        Timber.e(e, "[Repository] Failed to get URI record with id: %d", id)
    }.getOrNull()

    override suspend fun deleteUriRecord(id: Long): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val deleted = dataSource.deleteUriRecord(id)
            if (!deleted) {
                Timber.w("[Repository] URI record with id: $id not found or delete failed in data source.")
            }
        }
    }.onFailure { e ->
        Timber.e(e, "[Repository] Failed to delete URI record with id: $id")
    }

    override suspend fun deleteAllUriRecords(): Result<Int> = runCatching {
        withContext(ioDispatcher) {
            dataSource.deleteAllUriRecords()
        }
    }.onFailure { e -> Timber.e(e, "[Repository] Failed to delete all URI records") }

    override fun getDistinctHosts(): Flow<List<String>> {
        return dataSource.getDistinctHosts()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching distinct hosts")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getDistinctChosenBrowsers(): Flow<List<String?>> {
        return dataSource.getDistinctChosenBrowsers()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching distinct chosen browsers")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }
}

@Singleton
class HostRuleRepositoryImpl @Inject constructor(
    private val hostRuleDataSource: HostRuleLocalDataSource,
    private val folderDataSource: FolderLocalDataSource,
    private val instantProvider: InstantProvider,
    private val browserPickerDatabase: BrowserPickerDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
): HostRuleRepository {

    override fun getHostRuleByHost(host: String): Flow<HostRule?> {
        return hostRuleDataSource.getHostRuleByHost(host)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching HostRule for host: %s", host)
                emit(null)
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getHostRuleById(id: Long): HostRule? {
        return runCatching {
            withContext(ioDispatcher) {
                hostRuleDataSource.getHostRuleById(id)
            }
        }.onFailure { e ->
            Timber.e(e, "[Repository] Failed to get HostRule by ID: %d", id)
        }.getOrNull()
    }

    override fun getAllHostRules(): Flow<List<HostRule>> {
        return hostRuleDataSource.getAllHostRules()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching all HostRules")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getHostRulesByStatus(status: UriStatus): Flow<List<HostRule>> {
        return hostRuleDataSource.getHostRulesByStatus(status)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching HostRules by status: %s", status)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getHostRulesByFolder(folderId: Long): Flow<List<HostRule>> {
        return hostRuleDataSource.getHostRulesByFolder(folderId)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching HostRules by folderId: %d", folderId)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getRootHostRulesByStatus(status: UriStatus): Flow<List<HostRule>> {
        return hostRuleDataSource.getRootHostRulesByStatus(status)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching root HostRules by status: %s", status)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getDistinctRuleHosts(): Flow<List<String>> {
        return hostRuleDataSource.getDistinctRuleHosts()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching distinct rule hosts")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveHostRule(
        host: String,
        status: UriStatus,
        folderId: Long?,
        preferredBrowser: String?,
        isPreferenceEnabled: Boolean,
    ): Result<Long> = browserPickerDatabase.withTransaction {
        runCatching {
            val trimmedHost = host.trim()
            if (trimmedHost.isEmpty()) {
                throw InvalidInputDataException("Host cannot be blank.")
            }
            if (status == UriStatus.UNKNOWN) {
                throw InvalidInputDataException("Cannot save rule with UNKNOWN status.")
            }
            val trimmedPreferredBrowser = preferredBrowser?.trim()
            if (trimmedPreferredBrowser != null && trimmedPreferredBrowser.isEmpty()) {
                throw InvalidInputDataException("Preferred browser package cannot be blank if provided.")
            }

            val now = instantProvider.now()
            val currentRule = hostRuleDataSource.getHostRuleByHost(trimmedHost).firstOrNull()

            var effectiveFolderId = folderId
            var effectivePreferredBrowser = trimmedPreferredBrowser
            var effectiveIsPreferenceEnabled = isPreferenceEnabled

            if (status == UriStatus.BLOCKED) {
                effectivePreferredBrowser = null
                effectiveIsPreferenceEnabled = false
            }

            if (status == UriStatus.NONE) {
                effectiveFolderId = null
                effectivePreferredBrowser = null
                effectiveIsPreferenceEnabled = false
            }

            if (effectiveFolderId != null && (status == UriStatus.BOOKMARKED || status == UriStatus.BLOCKED)) {
                val folder = folderDataSource.getFolderByIdSuspend(effectiveFolderId)
                if (folder == null) {
                    throw DataNotFoundException("Folder with ID $effectiveFolderId does not exist.")
                }
                val expectedFolderType = status.toFolderType()

                if (expectedFolderType == null) {
                    Timber.w("Rule status $status unexpectedly requires a folder check but has no matching FolderType. Clearing folder ID.")
                    effectiveFolderId = null
                } else if (folder.type != expectedFolderType) {
                    throw InvalidInputDataException("Folder type mismatch: Rule status ($status) requires folder type $expectedFolderType, but folder $effectiveFolderId has type ${folder.type}.")
                }
            } else if (status != UriStatus.NONE) {
                effectiveFolderId = null
            }

            val ruleToSave = HostRule(
                id = currentRule?.id ?: 0,
                host = trimmedHost,
                uriStatus = status,
                folderId = effectiveFolderId,
                preferredBrowserPackage = effectivePreferredBrowser,
                isPreferenceEnabled = effectiveIsPreferenceEnabled,
                createdAt = currentRule?.createdAt ?: now,
                updatedAt = now
            )

            hostRuleDataSource.upsertHostRule(ruleToSave)
        }.onFailure { e ->
            when (e) {
                is InvalidInputDataException -> Timber.e(e, "[Repository] Invalid input data when saving host rule: %s", e.message)
                is DataNotFoundException -> Timber.e(e, "[Repository] Referenced data not found when saving host rule: %s", e.message)
                else -> {
                    Timber.e(e, "[Repository] Failed transaction to save host rule: host=$host, status=$status, folderId=$folderId, preferredBrowser=$preferredBrowser \n ${e.message}")
                }
            }
        }
    }

    override suspend fun deleteHostRuleById(id: Long): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val deleted = hostRuleDataSource.deleteHostRuleById(id)
            if (!deleted) {
                Timber.w("[Repository] Host rule with ID $id not found for deletion or delete failed.")
            }
        }
    }.onFailure { e -> Timber.e(e, "[Repository] Failed to delete host rule by ID: $id") }

    override suspend fun deleteHostRuleByHost(host: String): Result<Unit> = runCatching {
        val trimmedHost = host.trim()
        if (trimmedHost.isEmpty()) throw InvalidInputDataException("Host cannot be blank for deletion.")
        withContext(ioDispatcher) {
            val deleted = hostRuleDataSource.deleteHostRuleByHost(trimmedHost)
            if (!deleted) {
                Timber.w("[Repository] Host rule for host '$trimmedHost' not found for deletion or delete failed.")
            }
        }
    }.onFailure { e ->
        when (e) {
            is InvalidInputDataException -> Timber.e(e, "[Repository] Invalid input data when deleting host rule by host: %s", e.message)
            else -> Timber.e(e, "[Repository] Failed to delete host rule by host: $host")
        }
    }

    override suspend fun clearFolderAssociation(folderId: Long): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            hostRuleDataSource.clearFolderIdForRules(folderId)
        }
    }.onFailure { e -> Timber.e(e, "[Repository] Failed to clear folder association for folderId: $folderId") }

}

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
        runCatching {
            withContext(ioDispatcher) {
                folderDataSource.ensureDefaultFoldersExist()
            }
        }.onFailure { e ->
            Timber.e(e, "[Repository] Failed to ensure default folders exist")
        }
    }

    override fun getFolder(folderId: Long): Flow<Folder?> {
        return folderDataSource.getFolder(folderId)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching Folder by id: %d", folderId)
                emit(null)
            }
            .flowOn(ioDispatcher)
    }

    override fun getChildFolders(parentFolderId: Long): Flow<List<Folder>> {
        return folderDataSource.getChildFolders(parentFolderId)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching child folders for parentId: %d", parentFolderId)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getRootFoldersByType(type: FolderType): Flow<List<Folder>> {
        return folderDataSource.getRootFoldersByType(type)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching root folders for type: %s", type)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getAllFoldersByType(type: FolderType): Flow<List<Folder>> {
        return folderDataSource.getAllFoldersByType(type)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching all folders for type: %s", type)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun findFolderByNameAndParent(name: String, parentFolderId: Long?, type: FolderType): Folder? {
        return runCatching {
            withContext(ioDispatcher) {
                folderDataSource.findFolderByNameAndParent(name.trim(), parentFolderId, type)
            }
        }.onFailure { e ->
            Timber.e(e, "[Repository] Failed to find folder by name/parent: Name='%s', Parent='%s', Type='%s'", name, parentFolderId, type)
        }.getOrNull()
    }

    override suspend fun createFolder(
        name: String,
        parentFolderId: Long?,
        type: FolderType,
    ): Result<Long> = runCatching {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            throw InvalidInputDataException("Folder name cannot be empty.")
        }
        if (parentFolderId == null && isReservedRootName(trimmedName)) {
            val existingDefault = findFolderByNameAndParent(trimmedName, parentFolderId, type)
            if (existingDefault != null && (existingDefault.id == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID || existingDefault.id == Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID)) {
                Timber.w("[Repository] Attempted to recreate default folder: Name='$trimmedName', Type='$type'. Returning existing ID ${existingDefault.id}.")
                return@runCatching existingDefault.id
            } else {
                throw InvalidInputDataException("Cannot create folder with reserved root name '$trimmedName'.")
            }
        }

        withContext(ioDispatcher) {
            if (parentFolderId != null) {
                val parentFolder = folderDataSource.getFolderByIdSuspend(parentFolderId)
                    ?: throw DataNotFoundException("Parent folder with ID $parentFolderId not found.")
                if (parentFolder.type != type) {
                    throw InvalidInputDataException("Parent folder type (${parentFolder.type}) must match new folder type ($type).")
                }
            }

            val existing = folderDataSource.findFolderByNameAndParent(trimmedName, parentFolderId, type)
            if (existing != null) {
                throw DataIntegrityException("A folder named '$trimmedName' already exists in this location with the same type (ID: ${existing.id}).")
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
            folderDataSource.createFolder(newFolder)
        }
    }.onFailure { e ->
        when (e) {
            is InvalidInputDataException -> Timber.e(e, "[Repository] Invalid input data when creating folder: %s", e.message)
            is DataNotFoundException -> Timber.e(e, "[Repository] Referenced data not found when creating folder: %s", e.message)
            is DataIntegrityException -> Timber.e(e, "[Repository] Data integrity issue when creating folder: %s", e.message)
            else -> Timber.e(e, "[Repository] Failed to create folder: Name='$name', Parent='$parentFolderId', Type='$type'")
        }
    }

    private suspend fun isDescendantRecursive(folderId: Long?, targetAncestorId: Long): Boolean {
        if (folderId == null) return false
        if (folderId == targetAncestorId) return true

        val parentId = folderDataSource.getFolderByIdSuspend(folderId)?.parentFolderId
        return isDescendantRecursive(parentId, targetAncestorId)
    }

    override suspend fun updateFolder(folder: Folder): Result<Unit> = browserPickerDatabase.withTransaction {
        runCatching {
            Timber.tag("FolderRepo").d("Attempting to update folder: id='%d', name='%s', parentId='%s'", folder.id, folder.name, folder.parentFolderId)
            val trimmedName = folder.name.trim()
            if (trimmedName.isEmpty()) {
                throw InvalidInputDataException("Folder name cannot be empty.")
            }
            if (folder.id == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID || folder.id == Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID) {
                throw InvalidInputDataException("Cannot modify the default root folders.")
            }
            // Prevent renaming a folder to a reserved root name if it's at the root
            if (folder.parentFolderId == null && isReservedRootName(trimmedName)) {
                throw InvalidInputDataException("Cannot rename a root folder to the reserved name '$trimmedName'.")
            }

            withContext(ioDispatcher) {
                val currentFolder = folderDataSource.getFolderByIdSuspend(folder.id)
                    ?: throw DataNotFoundException("Folder with ID ${folder.id} not found for update.")

                // Type cannot be changed
                if (currentFolder.type != folder.type) {
                    throw InvalidInputDataException("Cannot change the type of an existing folder (from ${currentFolder.type} to ${folder.type}).")
                }

                // Validate potential parent change (circular reference check)
                if (currentFolder.parentFolderId != folder.parentFolderId) {
                    folder.parentFolderId?.let { newParentId ->
                        if (newParentId == folder.id) {
                            throw InvalidInputDataException("Cannot move a folder into itself.")
                        }
                        // This recursive check also needs to happen within the transaction context
                        if (isDescendantRecursive(newParentId, folder.id)) {
                            throw DataIntegrityException("Cannot move folder ID ${folder.id} into its own descendant (potential new parent ID $newParentId). Circular reference detected.")
                        }
                        // Check new parent existence and type
                        val newParent = folderDataSource.getFolderByIdSuspend(newParentId)
                            ?: throw DataNotFoundException("New parent folder with ID $newParentId not found.")
                        if (newParent.type != folder.type) {
                            throw InvalidInputDataException("New parent folder type (${newParent.type}) must match folder type (${folder.type}).")
                        }
                    }
                }

                // Check uniqueness in the *new* location if name or parent changed
                if (currentFolder.name != trimmedName || currentFolder.parentFolderId != folder.parentFolderId) {
                    val conflictingFolder = folderDataSource.findFolderByNameAndParent(trimmedName, folder.parentFolderId, folder.type)
                    if (conflictingFolder != null && conflictingFolder.id != folder.id) {
                        throw DataIntegrityException("A folder named '$trimmedName' already exists in the target location with the same type (ID: ${conflictingFolder.id}).")
                    }
                }

                // Prepare updated folder entity (only update allowed fields)
                val folderToUpdate = currentFolder.copy(
                    name = trimmedName,
                    parentFolderId = folder.parentFolderId,
                    updatedAt = instantProvider.now()
                )

                val updated = folderDataSource.updateFolder(folderToUpdate)
                if (!updated) {
                    throw DataIntegrityException("Failed to update folder with ID ${folder.id} in data source (record might not exist anymore or update failed).")
                }
            }
        }.onFailure { e ->
            when (e) {
                is InvalidInputDataException -> Timber.e(e, "[Repository] Invalid input data when updating folder: %s", e.message)
                is DataNotFoundException -> Timber.e(e, "[Repository] Referenced data not found when updating folder: %s", e.message)
                is DataIntegrityException -> Timber.e(e, "[Repository] Data integrity issue when updating folder: %s", e.message)
                else -> Timber.e(e, "[Repository] Failed to update folder: ID='${folder.id}', name='${folder.name}', parentFolderId='${folder.parentFolderId}', type='${folder.type}'")
            }
        }
    }

    override suspend fun deleteFolder(folderId: Long): Result<Unit> = browserPickerDatabase.withTransaction {
        runCatching {
            if (folderId == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID || folderId == Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID) {
                throw InvalidInputDataException("Cannot delete the default root folders.")
            }

            val hasChildren = folderDataSource.hasChildFolders(folderId)
            if (hasChildren) {
                // This repository method requires that child folders are handled (deleted or moved)
                // by the caller (e.g., Use Case) *before* this method is invoked.
                throw FolderNotEmptyException(folderId)
            }
            // Clear association for any HostRules linked to this folder BEFORE deleting the folder.
            hostRuleDataSource.clearFolderIdForRules(folderId)

            val deleted = folderDataSource.deleteFolder(folderId)
            if (!deleted) {
                Timber.w("[Repository] Folder with ID $folderId not found during deletion attempt inside transaction.")
            }
        }.onFailure { e ->
            when (e) {
                is InvalidInputDataException -> Timber.e(e, "[Repository] Invalid input data when deleting folder: %s", e.message)
                is FolderNotEmptyException -> Timber.e(e, "[Repository] Folder not empty when deleting folder: %s", e.message)
                // Note: DataNotFoundException from folderDataSource.deleteFolder (if it throws for not found) will also be caught here
                else -> Timber.e(e, "[Repository] Failed transaction to delete folder: ID='$folderId'")
            }
        }
    }
}

@Singleton
class BrowserStatsRepositoryImpl @Inject constructor(
    private val dataSource: BrowserStatsLocalDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
): BrowserStatsRepository {

    override suspend fun recordBrowserUsage(packageName: String): Result<Unit> = runCatching {
        if (packageName.isBlank()) throw InvalidInputDataException("Package name cannot be blank.")
        withContext(ioDispatcher) {
            dataSource.recordBrowserUsage(packageName)
        }
    }.onFailure { e ->
        when (e) {
            is InvalidInputDataException -> Timber.e(e, "[Repository] Invalid input data when recording browser usage: %s", e.message)
            else -> Timber.e(e, "[Repository] Failed to record browser usage for: $packageName")
        }
    }

    override fun getBrowserStat(packageName: String): Flow<BrowserUsageStat?> {
        return dataSource.getBrowserStat(packageName)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching browser stat for: %s", packageName)
                emit(null)
            }
            .flowOn(ioDispatcher)
    }

    override fun getAllBrowserStats(): Flow<List<BrowserUsageStat>> {
        return dataSource.getAllBrowserStats()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching all browser stats")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getAllBrowserStatsSortedByLastUsed(): Flow<List<BrowserUsageStat>> {
        return dataSource.getAllBrowserStatsSortedByLastUsed()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching all browser stats sorted by last used")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun deleteBrowserStat(packageName: String): Result<Unit> = runCatching {
        if (packageName.isBlank()) throw InvalidInputDataException("Package name cannot be blank.")
        withContext(ioDispatcher) {
            val deleted = dataSource.deleteBrowserStat(packageName)
            if (!deleted) Timber.w("[Repository] Browser stat for '$packageName' not found or delete failed.")
        }
    }.onFailure { e ->
        when (e) {
            is InvalidInputDataException -> Timber.e(e, "[Repository] Invalid input data when deleting browser stat: %s", e.message)
            else -> Timber.e(e, "[Repository] Failed to delete browser stat for: $packageName")
        }
    }

    override suspend fun deleteAllStats(): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val count = dataSource.deleteAllStats()
            Timber.d("[Repository] Deleted $count browser stats.")
        }
    }.onFailure { e -> Timber.e(e, "[Repository] Failed to delete all browser stats") }
}

 */

/*
package browserpicker.data.local.repository

import androidx.paging.*
import androidx.room.*
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.results.MyResult
import browserpicker.core.results.UriValidationError
import kotlinx.coroutines.flow.*
import timber.log.Timber
import browserpicker.data.local.datasource.*
import browserpicker.data.local.db.BrowserPickerDatabase
import browserpicker.data.local.query.model.*
import browserpicker.domain.model.*
import browserpicker.domain.model.query.*
import browserpicker.domain.repository.*
import browserpicker.domain.service.ParsedUri
import browserpicker.domain.service.UriParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UriHistoryRepositoryImpl @Inject constructor(
    private val dataSource: UriHistoryLocalDataSource,
    private val uriParser: UriParser,
    private val instantProvider: InstantProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
): UriHistoryRepository {
    private fun mapQueryToConfig(query: UriHistoryQuery): UriRecordQueryConfig {
        return UriRecordQueryConfig(
            searchQuery = query.searchQuery,
            filterByUriSource = query.filterByUriSource,
            filterByInteractionAction = query.filterByInteractionAction,
            filterByChosenBrowser = query.filterByChosenBrowser,
            filterByHost = query.filterByHost,
            filterByDateRange = query.filterByDateRange,
            sortBy = query.sortBy,
            sortOrder = query.sortOrder,
            groupBy = query.groupBy,
            groupSortOrder = query.groupSortOrder,
            advancedFilters = query.advancedFilters
        )
    }

    override fun getPagedUriRecords(query: UriHistoryQuery, pagingConfig: PagingConfig): Flow<PagingData<UriRecord>> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getPagedUriRecords(dataQueryConfig, pagingConfig).flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create PagedUriRecords Flow for query: %s", query)
            flowOf(PagingData.empty<UriRecord>()).flowOn(ioDispatcher)
        }
    }

    override fun getTotalUriRecordCount(query: UriHistoryQuery): Flow<Long> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getTotalUriRecordCount(dataQueryConfig)
                .catch { e ->
                    Timber.e(e, "[Repository] Error fetching total URI record count for query: %s", query)
                    emit(0)
                }
                .flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create TotalUriRecordCount Flow for query: %s", query)
            flowOf(0L).flowOn(ioDispatcher)
        }
    }

    override fun getGroupCounts(query: UriHistoryQuery): Flow<List<GroupCount>> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getGroupCounts(dataQueryConfig).map { dataGroupCounts ->
                dataGroupCounts.map { GroupCount(it.groupValue, it.count) }
            }.catch { e ->
                Timber.e(e, "[Repository] Error fetching group counts for query: %s", query)
                emit(emptyList())
            }.flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create GroupCounts Flow for query: %s", query)
            flowOf(emptyList<GroupCount>())
        }
    }

    override fun getDateCounts(query: UriHistoryQuery): Flow<List<DateCount>> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getDateCounts(dataQueryConfig).map { dataDateCounts ->
                dataDateCounts.map { DateCount(it.date, it.count) }
            }.catch { e ->
                Timber.e(e, "[Repository] Error fetching date counts for query: %s", query)
                emit(emptyList())
            }.flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create DateCounts Flow for query: %s", query)
            flowOf(emptyList<DateCount>())
        }
    }

    override suspend fun addUriRecord(
        uriString: String,
        host: String,
        source: UriSource,
        action: InteractionAction,
        chosenBrowser: String?,
        associatedHostRuleId: Long?,
    ): Result<Long> = runCatching {
        withContext(ioDispatcher) {
            when {
                uriString.isBlank() -> throw IllegalArgumentException("URI string cannot be blank or empty")
                host.isBlank() -> throw IllegalArgumentException("Host cannot be blank or empty")
                source == UriSource.UNKNOWN -> throw IllegalArgumentException("URI Source cannot be UNKNOWN; use a valid source type")
                action == InteractionAction.UNKNOWN -> throw IllegalArgumentException("Interaction Action cannot be UNKNOWN; use a valid action type")
            }
            if (chosenBrowser != null) {
                require(chosenBrowser.isNotBlank()) { "Chosen browser package name cannot be blank if provided." }
            }
            uriParser.parseAndValidateWebUri(uriString).getOrThrow()

            val record = UriRecord(
                uriString = uriString,
                host = host,
                uriSource = source,
                interactionAction = action,
                chosenBrowserPackage = chosenBrowser,
                timestamp = instantProvider.now(),
                associatedHostRuleId = associatedHostRuleId
            )

            val id = dataSource.insertUriRecord(record)
            if (id <= 0) {
                throw IllegalStateException("Failed to insert URI record: received invalid ID $id")
            }

            id
        }
    }.onFailure { e ->
        Timber.e(e, "[Repository] Failed to add URI record: $uriString, host=$host, source=$source, action=$action, browser=$chosenBrowser")
    }

    override suspend fun getUriRecord(id: Long): UriRecord? = runCatching {
        withContext(ioDispatcher) {
            dataSource.getUriRecord(id)
        }
    }.onFailure { e ->
        Timber.e(e, "[Repository] Failed to get URI record with id: %d", id)
    }.getOrNull()

    override suspend fun deleteUriRecord(id: Long): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val deleted = dataSource.deleteUriRecord(id)
            if (!deleted) {
                Timber.w("[Repository] URI record with id: $id not found or delete failed in data source.")
            }
        }
    }.onFailure { e ->
        Timber.e(e, "[Repository] Failed to delete URI record with id: $id")
    }

    override suspend fun deleteAllUriRecords(): Result<Int> = runCatching {
        withContext(ioDispatcher) {
            dataSource.deleteAllUriRecords()
        }
    }.onFailure { e -> Timber.e(e, "[Repository] Failed to delete all URI records") }

    override fun getDistinctHosts(): Flow<List<String>> {
        return dataSource.getDistinctHosts()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching distinct hosts")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getDistinctChosenBrowsers(): Flow<List<String?>> {
        return dataSource.getDistinctChosenBrowsers()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching distinct chosen browsers")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }
}

@Singleton
class HostRuleRepositoryImpl @Inject constructor(
    private val hostRuleDataSource: HostRuleLocalDataSource,
    private val folderDataSource: FolderLocalDataSource,
    private val instantProvider: InstantProvider,
    private val browserPickerDatabase: BrowserPickerDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
): HostRuleRepository {

    override fun getHostRuleByHost(host: String): Flow<HostRule?> {
        return hostRuleDataSource.getHostRuleByHost(host)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching HostRule for host: %s", host)
                emit(null)
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getHostRuleById(id: Long): HostRule? {
        return runCatching {
            withContext(ioDispatcher) {
                hostRuleDataSource.getHostRuleById(id)
            }
        }.onFailure { e ->
            Timber.e(e, "[Repository] Failed to get HostRule by ID: %d", id)
        }.getOrNull()
    }

    override fun getAllHostRules(): Flow<List<HostRule>> {
        return hostRuleDataSource.getAllHostRules()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching all HostRules")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getHostRulesByStatus(status: UriStatus): Flow<List<HostRule>> {
        return hostRuleDataSource.getHostRulesByStatus(status)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching HostRules by status: %s", status)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getHostRulesByFolder(folderId: Long): Flow<List<HostRule>> {
        return hostRuleDataSource.getHostRulesByFolder(folderId)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching HostRules by folderId: %d", folderId)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getRootHostRulesByStatus(status: UriStatus): Flow<List<HostRule>> {
        return hostRuleDataSource.getRootHostRulesByStatus(status)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching root HostRules by status: %s", status)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getDistinctRuleHosts(): Flow<List<String>> {
        return hostRuleDataSource.getDistinctRuleHosts()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching distinct rule hosts")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun saveHostRule(
        host: String,
        status: UriStatus,
        folderId: Long?,
        preferredBrowser: String?,
        isPreferenceEnabled: Boolean,
    ): Result<Long> = browserPickerDatabase.withTransaction {
        runCatching {
            val trimmedHost = host.trim()
            if (trimmedHost.isEmpty()) {
                throw IllegalArgumentException("Host cannot be blank.")
            }
            if (status == UriStatus.UNKNOWN) {
                throw IllegalArgumentException("Cannot save rule with UNKNOWN status.")
            }
            val trimmedPreferredBrowser = preferredBrowser?.trim()
            if (trimmedPreferredBrowser != null && trimmedPreferredBrowser.isEmpty()) {
                throw IllegalArgumentException("Preferred browser package cannot be blank if provided.")
            }

            val now = instantProvider.now()
            val currentRule = hostRuleDataSource.getHostRuleByHost(trimmedHost).firstOrNull()

            var effectiveFolderId = folderId
            var effectivePreferredBrowser = trimmedPreferredBrowser
            var effectiveIsPreferenceEnabled = isPreferenceEnabled

            if (status == UriStatus.BLOCKED) {
                effectivePreferredBrowser = null
                effectiveIsPreferenceEnabled = false
            }

            if (status == UriStatus.NONE) {
                effectiveFolderId = null
                effectivePreferredBrowser = null
                effectiveIsPreferenceEnabled = false
            }

            if (effectiveFolderId != null && (status == UriStatus.BOOKMARKED || status == UriStatus.BLOCKED)) {
                val folder = folderDataSource.getFolderByIdSuspend(effectiveFolderId)
                if (folder == null) {
                    throw IllegalArgumentException("Folder with ID $effectiveFolderId does not exist.")
                }
                val expectedFolderType = status.toFolderType()

                if (expectedFolderType == null) {
                    Timber.w("Rule status $status unexpectedly requires a folder check but has no matching FolderType. Clearing folder ID.")
                    effectiveFolderId = null
                } else if (folder.type != expectedFolderType) {
                    throw IllegalArgumentException("Folder type mismatch: Rule status ($status) requires folder type $expectedFolderType, but folder $effectiveFolderId has type ${folder.type}.")
                }
            } else if (status != UriStatus.NONE) {
                effectiveFolderId = null
            }

            val ruleToSave = HostRule(
                id = currentRule?.id ?: 0,
                host = trimmedHost,
                uriStatus = status,
                folderId = effectiveFolderId,
                preferredBrowserPackage = effectivePreferredBrowser,
                isPreferenceEnabled = effectiveIsPreferenceEnabled,
                createdAt = currentRule?.createdAt ?: now,
                updatedAt = now
            )

            hostRuleDataSource.upsertHostRule(ruleToSave)
        }.onFailure { e ->
            Timber.e(e, "[Repository] Failed transaction to save host rule: host='%s', status='%s', folderId='%s', preferredBrowser='%s'", host, status, folderId, preferredBrowser)
        }
    }

    override suspend fun deleteHostRuleById(id: Long): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val deleted = hostRuleDataSource.deleteHostRuleById(id)
            if (!deleted) {
                Timber.w("[Repository] Host rule with ID $id not found for deletion or delete failed.")
            }
        }
    }.onFailure { e -> Timber.e(e, "[Repository] Failed to delete host rule by ID: $id") }

    override suspend fun deleteHostRuleByHost(host: String): Result<Unit> = runCatching {
        val trimmedHost = host.trim()
        if (trimmedHost.isEmpty()) throw IllegalArgumentException("Host cannot be blank for deletion.")
        withContext(ioDispatcher) {
            val deleted = hostRuleDataSource.deleteHostRuleByHost(trimmedHost)
            if (!deleted) {
                Timber.w("[Repository] Host rule for host '$trimmedHost' not found for deletion or delete failed.")
            }
        }
    }.onFailure { e -> Timber.e(e, "[Repository] Failed to delete host rule by host: $host") }

    override suspend fun clearFolderAssociation(folderId: Long): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            hostRuleDataSource.clearFolderIdForRules(folderId)
        }
    }.onFailure { e -> Timber.e(e, "[Repository] Failed to clear folder association for folderId: $folderId") }

}

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
        runCatching {
            withContext(ioDispatcher) {
                folderDataSource.ensureDefaultFoldersExist()
            }
        }.onFailure { e ->
            Timber.e(e, "[Repository] Failed to ensure default folders exist")
        }
    }

    override fun getFolder(folderId: Long): Flow<Folder?> {
        return folderDataSource.getFolder(folderId)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching Folder by id: %d", folderId)
                emit(null)
            }
            .flowOn(ioDispatcher)
    }

    override fun getChildFolders(parentFolderId: Long): Flow<List<Folder>> {
        return folderDataSource.getChildFolders(parentFolderId)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching child folders for parentId: %d", parentFolderId)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getRootFoldersByType(type: FolderType): Flow<List<Folder>> {
        return folderDataSource.getRootFoldersByType(type)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching root folders for type: %s", type)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getAllFoldersByType(type: FolderType): Flow<List<Folder>> {
        return folderDataSource.getAllFoldersByType(type)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching all folders for type: %s", type)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun findFolderByNameAndParent(name: String, parentFolderId: Long?, type: FolderType): Folder? {
        return runCatching {
            withContext(ioDispatcher) {
                folderDataSource.findFolderByNameAndParent(name.trim(), parentFolderId, type)
            }
        }.onFailure { e ->
            Timber.e(e, "[Repository] Failed to find folder by name/parent: Name='%s', Parent='%s', Type='%s'", name, parentFolderId, type)
        }.getOrNull()
    }

    override suspend fun createFolder(
        name: String,
        parentFolderId: Long?,
        type: FolderType,
    ): Result<Long> = runCatching {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            throw IllegalArgumentException("Folder name cannot be empty.")
        }
        if (parentFolderId == null && isReservedRootName(trimmedName)) {
            val existingDefault = findFolderByNameAndParent(trimmedName, parentFolderId, type)
            if (existingDefault != null && (existingDefault.id == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID || existingDefault.id == Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID)) {
                Timber.w("[Repository] Attempted to recreate default folder: Name='$trimmedName', Type='$type'. Returning existing ID ${existingDefault.id}.")
                return@runCatching existingDefault.id
            } else {
                throw IllegalArgumentException("Cannot create folder with reserved root name '$trimmedName'.")
            }
        }

        withContext(ioDispatcher) {
            if (parentFolderId != null) {
                val parentFolder = folderDataSource.getFolderByIdSuspend(parentFolderId)
                    ?: throw IllegalArgumentException("Parent folder with ID $parentFolderId not found.")
                if (parentFolder.type != type) {
                    throw IllegalArgumentException("Parent folder type (${parentFolder.type}) must match new folder type ($type).")
                }
            }

            val existing = folderDataSource.findFolderByNameAndParent(trimmedName, parentFolderId, type)
            if (existing != null) {
                throw IllegalStateException("A folder named '$trimmedName' already exists in this location with the same type (ID: ${existing.id}).")
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
            folderDataSource.createFolder(newFolder)
        }
    }.onFailure { e ->
        Timber.e(e, "[Repository] Failed to create folder: Name='$name', Parent='$parentFolderId', Type='$type'")
    }

    private suspend fun isDescendantRecursive(folderId: Long?, targetAncestorId: Long): Boolean {
        if (folderId == null) return false
        if (folderId == targetAncestorId) return true

        val parentId = folderDataSource.getFolderByIdSuspend(folderId)?.parentFolderId
        return isDescendantRecursive(parentId, targetAncestorId)
    }

    override suspend fun updateFolder(folder: Folder): Result<Unit> = browserPickerDatabase.withTransaction {
        runCatching {
            Timber.tag("FolderRepo").d("Attempting to update folder: id='%d', name='%s', parentId='%s'", folder.id, folder.name, folder.parentFolderId)
            val trimmedName = folder.name.trim()
            if (trimmedName.isEmpty()) {
                throw IllegalArgumentException("Folder name cannot be empty.")
            }
            if (folder.id == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID || folder.id == Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID) {
                throw IllegalArgumentException("Cannot modify the default root folders.")
            }
            // Prevent renaming a folder to a reserved root name if it's at the root
            if (folder.parentFolderId == null && isReservedRootName(trimmedName)) {
                throw IllegalArgumentException("Cannot rename a root folder to the reserved name '$trimmedName'.")
            }

            withContext(ioDispatcher) {
                val currentFolder = folderDataSource.getFolderByIdSuspend(folder.id)
                    ?: throw IllegalArgumentException("Folder with ID ${folder.id} not found for update.")

                // Type cannot be changed
                if (currentFolder.type != folder.type) {
                    throw IllegalArgumentException("Cannot change the type of an existing folder (from ${currentFolder.type} to ${folder.type}).")
                }

                // Validate potential parent change (circular reference check)
                if (currentFolder.parentFolderId != folder.parentFolderId) {
                    folder.parentFolderId?.let { newParentId ->
                        if (newParentId == folder.id) {
                            throw IllegalArgumentException("Cannot move a folder into itself.")
                        }
                        // This recursive check also needs to happen within the transaction context
                        if (isDescendantRecursive(newParentId, folder.id)) {
                            throw IllegalArgumentException("Cannot move folder ID ${folder.id} into its own descendant (potential new parent ID $newParentId). Circular reference detected.")
                        }
                        // Check new parent existence and type
                        val newParent = folderDataSource.getFolderByIdSuspend(newParentId)
                            ?: throw IllegalArgumentException("New parent folder with ID $newParentId not found.")
                        if (newParent.type != folder.type) {
                            throw IllegalArgumentException("New parent folder type (${newParent.type}) must match folder type (${folder.type}).")
                        }
                    }
                }

                // Check uniqueness in the *new* location if name or parent changed
                if (currentFolder.name != trimmedName || currentFolder.parentFolderId != folder.parentFolderId) {
                    val conflictingFolder = folderDataSource.findFolderByNameAndParent(trimmedName, folder.parentFolderId, folder.type)
                    if (conflictingFolder != null && conflictingFolder.id != folder.id) {
                        throw IllegalStateException("A folder named '$trimmedName' already exists in the target location with the same type (ID: ${conflictingFolder.id}).")
                    }
                }

                // Prepare updated folder entity (only update allowed fields)
                val folderToUpdate = currentFolder.copy(
                    name = trimmedName,
                    parentFolderId = folder.parentFolderId,
                    updatedAt = instantProvider.now()
                )

                val updated = folderDataSource.updateFolder(folderToUpdate)
                if (!updated) {
                    throw IllegalStateException("Failed to update folder with ID ${folder.id} in data source (record might not exist anymore or update failed).")
                }
            }
        }.onFailure { e ->
            Timber.e(e, "[Repository] Failed to update folder: ID='${folder.id}', name='${folder.name}', parentFolderId='${folder.parentFolderId}', type='${folder.type}'")
        }
    }

    override suspend fun deleteFolder(folderId: Long): Result<Unit> = browserPickerDatabase.withTransaction {
        runCatching {
            if (folderId == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID || folderId == Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID) {
                throw IllegalArgumentException("Cannot delete the default root folders.")
            }

            val hasChildren = folderDataSource.hasChildFolders(folderId)
            if (hasChildren) {
                // This repository method requires that child folders are handled (deleted or moved)
                // by the caller (e.g., Use Case) *before* this method is invoked.
                throw IllegalStateException("Cannot delete folder ID $folderId because it contains child folders. Delete or move children first.")
            }
            // Clear association for any HostRules linked to this folder BEFORE deleting the folder.
            hostRuleDataSource.clearFolderIdForRules(folderId)

            val deleted = folderDataSource.deleteFolder(folderId)
            if (!deleted) {
                Timber.w("[Repository] Folder with ID $folderId not found during deletion attempt inside transaction.")
            }
        }.onFailure { e -> Timber.e(e, "[Repository] Failed transaction to delete folder: ID='$folderId'") }
    }
}

@Singleton
class BrowserStatsRepositoryImpl @Inject constructor(
    private val dataSource: BrowserStatsLocalDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
): BrowserStatsRepository {

    override suspend fun recordBrowserUsage(packageName: String): Result<Unit> = runCatching {
        if (packageName.isBlank()) throw IllegalArgumentException("Package name cannot be blank.")
        withContext(ioDispatcher) {
            dataSource.recordBrowserUsage(packageName)
        }
    }.onFailure { e -> Timber.e(e, "[Repository] Failed to record browser usage for: $packageName") }

    override fun getBrowserStat(packageName: String): Flow<BrowserUsageStat?> {
        return dataSource.getBrowserStat(packageName)
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching browser stat for: %s", packageName)
                emit(null)
            }
            .flowOn(ioDispatcher)
    }

    override fun getAllBrowserStats(): Flow<List<BrowserUsageStat>> {
        return dataSource.getAllBrowserStats()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching all browser stats")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getAllBrowserStatsSortedByLastUsed(): Flow<List<BrowserUsageStat>> {
        return dataSource.getAllBrowserStatsSortedByLastUsed()
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching all browser stats sorted by last used")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun deleteBrowserStat(packageName: String): Result<Unit> = runCatching {
        if (packageName.isBlank()) throw IllegalArgumentException("Package name cannot be blank.")
        withContext(ioDispatcher) {
            val deleted = dataSource.deleteBrowserStat(packageName)
            if (!deleted) Timber.w("[Repository] Browser stat for '$packageName' not found or delete failed.")
        }
    }.onFailure { e -> Timber.e(e, "[Repository] Failed to delete browser stat for: $packageName") }

    override suspend fun deleteAllStats(): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val count = dataSource.deleteAllStats()
            Timber.d("[Repository] Deleted $count browser stats.")
        }
    }.onFailure { e -> Timber.e(e, "[Repository] Failed to delete all browser stats") }
}

 */