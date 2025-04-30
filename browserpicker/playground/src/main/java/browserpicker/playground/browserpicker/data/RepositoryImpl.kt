package browserpicker.playground.browserpicker.data

import androidx.paging.*
import androidx.room.*
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import kotlinx.coroutines.flow.*
import timber.log.Timber
import browserpicker.data.local.datasource.*
import browserpicker.data.local.db.BrowserPickerDatabase
import browserpicker.data.local.query.model.*
import browserpicker.domain.model.*
import browserpicker.domain.model.query.*
import browserpicker.domain.repository.*
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

    override fun getTotalUriRecordCount(query: UriHistoryQuery): Flow<Int> {
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
            flowOf(0).flowOn(ioDispatcher)
        }
    }

    override fun getGroupCounts(query: UriHistoryQuery): Flow<List<DomainGroupCount>> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getGroupCounts(dataQueryConfig).map { dataGroupCounts ->
                dataGroupCounts.map { DomainGroupCount(it.groupValue, it.count) }
            }.catch { e ->
                Timber.e(e, "[Repository] Error fetching group counts for query: %s", query)
                emit(emptyList())
            }.flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create GroupCounts Flow for query: %s", query)
            flowOf(emptyList<DomainGroupCount>())
        }
    }

    override fun getDateCounts(query: UriHistoryQuery): Flow<List<DomainDateCount>> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getDateCounts(dataQueryConfig).map { dataDateCounts ->
                dataDateCounts.map { DomainDateCount(it.date, it.count) }
            }.catch { e ->
                Timber.e(e, "[Repository] Error fetching date counts for query: %s", query)
                emit(emptyList())
            }.flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create DateCounts Flow for query: %s", query)
            flowOf(emptyList<DomainDateCount>())
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
        uriParser.parseAndValidateWebUri(uriString).getOrThrow()
        require(host.isNotBlank()) { "Host cannot be blank." }
        if (chosenBrowser != null) {
            require(chosenBrowser.isNotBlank()) { "Chosen browser package name cannot be blank if provided." }
        }

        withContext(ioDispatcher) {
            val record = UriRecord(
                id = 0,
                uriString = uriString,
                host = host,
                timestamp = instantProvider.now(),
                uriSource = source,
                interactionAction = action,
                chosenBrowserPackage = chosenBrowser,
                associatedHostRuleId = associatedHostRuleId
            )
            dataSource.insertUriRecord(record)
        }
    }.onFailure { e ->
        Timber.e(e, "[Repository] Failed to add URI record: uriString='$uriString', host='$host', source='$source', action='$action'")
    }

    override suspend fun getUriRecord(id: Long): UriRecord? = runCatching {
        withContext(ioDispatcher) {
            dataSource.getUriRecord(id)
        }
    }.onFailure { e ->
        Timber.e(e, "[Repository] Failed to get URI record with id: %d", id)
    }.getOrNull()

    override suspend fun deleteUriRecord(id: Long): Boolean {
        return runCatching {
            withContext(ioDispatcher) {
                dataSource.deleteUriRecord(id)
            }
        }.onFailure { e ->
            Timber.e(e, "[Repository] Failed to delete URI record with id: $id")
        }.getOrDefault(false)
    }

    override suspend fun deleteAllUriRecords(): Result<Unit> = runCatching {
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
                val folder = folderDataSource.getFolder(effectiveFolderId).firstOrNull()
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

    private fun UriStatus.toFolderType(): FolderType? = when (this) {
        UriStatus.BOOKMARKED -> FolderType.BOOKMARK
        UriStatus.BLOCKED -> FolderType.BLOCK
        else -> null
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
                // Note: Consider adding suspend getFolderByIdSuspend(id) to DataSource
                val parentFolder = folderDataSource.getFolder(parentFolderId).firstOrNull()
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

        // Note: Consider adding suspend getFolderByIdSuspend(id) to DataSource
        val parentId = folderDataSource.getFolder(folderId).firstOrNull()?.parentFolderId
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
                // Note: Consider adding suspend getFolderByIdSuspend(id) to DataSource
                val currentFolder = folderDataSource.getFolder(folder.id).firstOrNull()
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
                        // Note: Consider adding suspend getFolderByIdSuspend(id) to DataSource
                        val newParent = folderDataSource.getFolder(newParentId).firstOrNull()
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
                throw IllegalStateException("Cannot delete folder ID $folderId because it contains child folders. Delete or move children first.")
            }
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
            dataSource.deleteAllStats()
        }
    }.onFailure { e -> Timber.e(e, "[Repository] Failed to delete all browser stats") }
}
