package browserpicker.playground.browserpicker.data

import androidx.paging.*
import androidx.room.*
import browserpicker.core.di.IoDispatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.*

@Singleton
class UriHistoryRepositoryImpl @Inject constructor(
    private val dataSource: UriHistoryLocalDataSource,
    private val instantProvider: InstantProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
): UriHistoryRepository {
    private fun mapQueryToConfig(query: UriHistoryQuery): UriRecordQueryConfig {
        // NOTE: Advanced filters from the domain layer are not currently supported.
        // If needed, map them here from the domain query to the data config.
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
            advancedFilters = emptyList()
        )
    }

    override fun getPagedUriRecords(
        query: UriHistoryQuery,
        pagingConfig: PagingConfig,
    ): Flow<PagingData<UriRecord>> {
        val dataQueryConfig = mapQueryToConfig(query)
        return dataSource.getPagedUriRecords(dataQueryConfig, pagingConfig)
            .catch { Timber.e(it, "Error in getPagedUriRecords") }
            .flowOn(ioDispatcher)
    }

    override fun getTotalUriRecordCount(query: UriHistoryQuery): Flow<Int> {
        val dataQueryConfig = mapQueryToConfig(query)
        return dataSource.getTotalUriRecordCount(dataQueryConfig)
            .catch { Timber.e(it, "Error in getTotalUriRecordCount") }
            .flowOn(ioDispatcher)
    }

    override fun getGroupCounts(query: UriHistoryQuery): Flow<List<DomainGroupCount>> {
        val dataQueryConfig = mapQueryToConfig(query)
        return dataSource.getGroupCounts(dataQueryConfig).map { list ->
            list.map { DomainGroupCount(it.groupValue, it.count) }
        }.catch { Timber.e(it, "Error in getGroupCounts") }
            .flowOn(ioDispatcher)
    }

    override fun getDateCounts(query: UriHistoryQuery): Flow<List<DomainDateCount>> {
        val dataQueryConfig = mapQueryToConfig(query)
        return dataSource.getDateCounts(dataQueryConfig).map { list ->
            list.map { DomainDateCount(it.date, it.count) }
        }.catch { Timber.e(it, "Error in getDateCounts") }
            .flowOn(ioDispatcher)
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
            val record = UriRecord(
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
        Timber.e(e, "Failed to add URI record: uriString='$uriString', host='$host', source='$source', action='$action'")
    }

    override suspend fun getUriRecord(id: Long): UriRecord? = runCatching {
        withContext(ioDispatcher) {
            dataSource.getUriRecord(id)
        }
    }.getOrElse {
        Timber.e(it, "Failed to get URI record with id: %d", id)
        null
    }

    override suspend fun deleteUriRecord(id: Long): Boolean {
        return runCatching {
            withContext(ioDispatcher) {
                dataSource.deleteUriRecord(id)
            }
        }.getOrElse {
            Timber.e(it, "Failed to delete URI record with id: $id")
            false
        }
    }

    override suspend fun deleteAllUriRecords(): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            dataSource.deleteAllUriRecords()
        }
    }.onFailure { Timber.e(it, "Failed to delete all URI records") }

    override fun getDistinctHosts(): Flow<List<String>> {
        return dataSource.getDistinctHosts()
            .catch { Timber.e(it, "Error in getDistinctHosts") }
            .flowOn(ioDispatcher)
    }

    override fun getDistinctChosenBrowsers(): Flow<List<String?>> {
        return dataSource.getDistinctChosenBrowsers()
            .catch { Timber.e(it, "Error in getDistinctChosenBrowsers") }
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
        return hostRuleDataSource.getHostRuleByHost(host).flowOn(ioDispatcher)
    }

    override suspend fun getHostRuleById(id: Long): HostRule? {
        return withContext(ioDispatcher) {
            hostRuleDataSource.getHostRuleById(id)
        }
    }

    override fun getAllHostRules(): Flow<List<HostRule>> {
        return hostRuleDataSource.getAllHostRules().flowOn(ioDispatcher)
    }

    override fun getHostRulesByStatus(status: UriStatus): Flow<List<HostRule>> {
        return hostRuleDataSource.getHostRulesByStatus(status).flowOn(ioDispatcher)
    }

    override fun getHostRulesByFolder(folderId: Long): Flow<List<HostRule>> {
        return hostRuleDataSource.getHostRulesByFolder(folderId).flowOn(ioDispatcher)
    }

    override fun getRootHostRulesByStatus(status: UriStatus): Flow<List<HostRule>> {
        return hostRuleDataSource.getRootHostRulesByStatus(status).flowOn(ioDispatcher)
    }

    override fun getDistinctRuleHosts(): Flow<List<String>> {
        return hostRuleDataSource.getDistinctRuleHosts().flowOn(ioDispatcher)
    }

    @Transaction
    override suspend fun saveHostRule(
        host: String,
        status: UriStatus,
        folderId: Long?,
        preferredBrowser: String?,
        isPreferenceEnabled: Boolean,
    ): Result<Long> = browserPickerDatabase.withTransaction {
        runCatching {
            if (host.isBlank()) {
                throw IllegalArgumentException("Host cannot be blank.")
            }
            if (status == UriStatus.UNKNOWN) {
                throw IllegalArgumentException("Cannot save rule with UNKNOWN status.")
            }
            if (preferredBrowser != null && preferredBrowser.isBlank()) {
                throw IllegalArgumentException("Preferred browser package cannot be blank.")
            }

            val now = instantProvider.now()
            val currentRule = hostRuleDataSource.getHostRuleByHost(host).firstOrNull()

            var effectiveFolderId = folderId
            var effectivePreferredBrowser = preferredBrowser
            var effectiveIsPreferenceEnabled = isPreferenceEnabled

            if (status == UriStatus.BLOCKED) {
                effectivePreferredBrowser = null
                effectiveIsPreferenceEnabled = false
            }

            if (status == UriStatus.NONE) {
                effectiveFolderId = null
            }

            if (effectiveFolderId != null && status != UriStatus.NONE) {
                val folder = folderDataSource.getFolder(effectiveFolderId).firstOrNull()
                if (folder == null) {
                    throw IllegalArgumentException("Folder with ID $effectiveFolderId does not exist.")
                }
                val expectedFolderType = when (status) {
                    UriStatus.BOOKMARKED -> FolderType.BOOKMARK
                    UriStatus.BLOCKED -> FolderType.BLOCK
                    else -> null
                }
                if (expectedFolderType != null && folder.type != expectedFolderType) {
                    throw IllegalArgumentException("Folder type mismatch: Rule status ($status) requires folder type $expectedFolderType, but folder $effectiveFolderId has type ${folder.type}.")
                }
            } else {
                effectiveFolderId = null
            }

            val ruleToSave = HostRule(
                id = currentRule?.id ?: 0,
                host = host,
                uriStatus = status,
                folderId = effectiveFolderId,
                preferredBrowserPackage = effectivePreferredBrowser,
                isPreferenceEnabled = effectiveIsPreferenceEnabled,
                createdAt = currentRule?.createdAt ?: now,
                updatedAt = now
            )

            hostRuleDataSource.upsertHostRule(ruleToSave)
        }.onFailure { e ->
            Timber.e(e, "Failed to save host rule: host='%s', status='%s', folderId='%s', preferredBrowser='%s'", host, status, folderId, preferredBrowser)
        }
    }

    override suspend fun deleteHostRuleById(id: Long): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val deleted = hostRuleDataSource.deleteHostRuleById(id)
            if (!deleted) {
                Timber.w("Host rule with ID $id not found or delete failed.")
            }
        }
    }.onFailure { Timber.e(it, "Failed to delete host rule by ID: $id") }

    override suspend fun deleteHostRuleByHost(host: String): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            val deleted = hostRuleDataSource.deleteHostRuleByHost(host)
            if (!deleted) {
                Timber.w("Host rule for host '$host' not found or delete failed.")
            }
        }
    }.onFailure { Timber.e(it, "Failed to delete host rule by host: $host") }

    override suspend fun clearFolderAssociation(folderId: Long): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            hostRuleDataSource.clearFolderIdForRules(folderId)
        }
    }.onFailure { Timber.e(it, "Failed to clear folder association for folderId: $folderId") }

}

@Singleton
class FolderRepositoryImpl @Inject constructor(
    private val folderDataSource: FolderLocalDataSource,
    private val hostRuleDataSource: HostRuleLocalDataSource,
    private val instantProvider: InstantProvider,
    private val browserPickerDatabase: BrowserPickerDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
): FolderRepository {

    override suspend fun ensureDefaultFoldersExist() {
        withContext(ioDispatcher) {
            folderDataSource.ensureDefaultFoldersExist()
        }
    }

    override fun getFolder(folderId: Long): Flow<Folder?> {
        return folderDataSource.getFolder(folderId).flowOn(ioDispatcher)
    }

    override fun getChildFolders(parentFolderId: Long): Flow<List<Folder>> {
        return folderDataSource.getChildFolders(parentFolderId).flowOn(ioDispatcher)
    }

    override fun getRootFoldersByType(type: FolderType): Flow<List<Folder>> {
        return folderDataSource.getRootFoldersByType(type).flowOn(ioDispatcher)
    }

    override fun getAllFoldersByType(type: FolderType): Flow<List<Folder>> {
        return folderDataSource.getAllFoldersByType(type).flowOn(ioDispatcher)
    }

    override suspend fun findFolderByNameAndParent(name: String, parentFolderId: Long?, type: FolderType): Folder? {
        return withContext(ioDispatcher) {
            folderDataSource.findFolderByNameAndParent(name, parentFolderId, type)
        }
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
        // Basic reserved name check (can be expanded)
        if (parentFolderId == null && (trimmedName.equals(Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_NAME, ignoreCase = true) || trimmedName.equals(Folder.DEFAULT_BLOCKED_ROOT_FOLDER_NAME, ignoreCase = true))) {
            if (type == FolderType.BOOKMARK && trimmedName.equals(Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_NAME, ignoreCase = true)) {
                // Allow finding default
            } else if (type == FolderType.BLOCK && trimmedName.equals(Folder.DEFAULT_BLOCKED_ROOT_FOLDER_NAME, ignoreCase = true)) {
                // Allow finding default
            } else {
                throw IllegalArgumentException("Cannot manually create folder with reserved root name '$trimmedName'.")
            }
        }


        withContext(ioDispatcher) {
            // Validate parent existence and type
            if (parentFolderId != null) {
                val parentFolder = folderDataSource.getFolder(parentFolderId).firstOrNull()
                    ?: throw IllegalArgumentException("Parent folder with ID $parentFolderId not found.")
                if (parentFolder.type != type) {
                    throw IllegalArgumentException("Parent folder type (${parentFolder.type}) must match new folder type ($type).")
                }
            }

            // Check uniqueness (name + parent + type)
            val existing = folderDataSource.findFolderByNameAndParent(trimmedName, parentFolderId, type)
            if (existing != null) {
                // Handle case where user tries to create default folder again
                if (existing.id == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID || existing.id == Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID) {
                    Timber.w("Attempted to recreate default folder: Name='$trimmedName', Parent='$parentFolderId', Type='$type'. Returning existing ID.")
                    return@withContext existing.id // Return existing default folder ID
                }
                throw IllegalStateException("A folder named '$trimmedName' already exists in this location with the same type.")
            }

            val now = instantProvider.now()
            val newFolder = Folder(
                name = trimmedName,
                parentFolderId = parentFolderId,
                type = type,
                createdAt = now,
                updatedAt = now
                // id = 0 for auto-generation
            )
            folderDataSource.createFolder(newFolder)
        }
    }.onFailure { Timber.e(it, "Failed to create folder: Name='$name', Parent='$parentFolderId', Type='$type'") }

    private suspend fun isDescendant(folderId: Long, potentialAncestorId: Long): Boolean {
        var currentParentId = folderDataSource.getFolder(folderId).firstOrNull()?.parentFolderId
        while (currentParentId != null) {
            if (currentParentId == potentialAncestorId) return true
            currentParentId = folderDataSource.getFolder(currentParentId).firstOrNull()?.parentFolderId
        }
        return false
    }

    override suspend fun updateFolder(folder: Folder): Result<Unit> = runCatching {
        val trimmedName = folder.name.trim()
        if (trimmedName.isEmpty()) {
            throw IllegalArgumentException("Folder name cannot be empty.")
        }
        if (folder.id == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID || folder.id == Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID) {
            throw IllegalArgumentException("Cannot modify the default root folders.")
        }

        withContext(ioDispatcher) {
            // Get current state
            val currentFolder = folderDataSource.getFolder(folder.id).firstOrNull()
                ?: throw IllegalArgumentException("Folder with ID ${folder.id} not found for update.")

            // Type cannot be changed
            if (currentFolder.type != folder.type) {
                throw IllegalArgumentException("Cannot change the type of an existing folder.")
            }

            // Validate potential parent change
            if (currentFolder.parentFolderId != folder.parentFolderId) {
                folder.parentFolderId?.let { parentFolderId ->
                    if (parentFolderId == folder.id) {
                        throw IllegalArgumentException("Cannot move a folder into itself.")
                    }

                    if (isDescendant(parentFolderId, folder.id)) {
                        throw IllegalArgumentException("Cannot move a folder into its own descendant.")
                    }
                    val newParent = folderDataSource.getFolder(parentFolderId).firstOrNull()
                        ?: throw IllegalArgumentException("New parent folder with ID $parentFolderId not found.")
                    if (newParent.type != folder.type) {
                        throw IllegalArgumentException("New parent folder type (${newParent.type}) must match folder type (${folder.type}).")
                    }
                }
            }

            // Check uniqueness in the *new* location if name or parent changed
            if (currentFolder.name != trimmedName || currentFolder.parentFolderId != folder.parentFolderId) {
                val conflictingFolder = folderDataSource.findFolderByNameAndParent(trimmedName, folder.parentFolderId, folder.type)
                if (conflictingFolder != null && conflictingFolder.id != folder.id) {
                    throw IllegalStateException("A folder named '$trimmedName' already exists in the target location with the same type.")
                }
            }

            // Prepare updated folder entity (only update allowed fields)
            val folderToUpdate = currentFolder.copy(
                name = trimmedName,
                parentFolderId = folder.parentFolderId,
                updatedAt = instantProvider.now() // Update timestamp
            )

            val updated = folderDataSource.updateFolder(folderToUpdate)
            if (!updated) {
                // Should generally not happen if initial fetch succeeded, but good practice
                throw IllegalStateException("Failed to update folder with ID ${folder.id} in data source.")
            }
        }
    }.onFailure { e ->
        Timber.e(e, "Failed to update folder: ID='${folder.id}', name='${folder.name}', parentFolderId='${folder.parentFolderId}', type='${folder.type}'")
    }

    @Transaction
    override suspend fun deleteFolder(folderId: Long): Result<Unit> = browserPickerDatabase.withTransaction {
        runCatching {
            if (folderId == Folder.DEFAULT_BOOKMARK_ROOT_FOLDER_ID || folderId == Folder.DEFAULT_BLOCKED_ROOT_FOLDER_ID) {
                throw IllegalArgumentException("Cannot delete the default root folders.")
            }

            val hasChildren = folderDataSource.hasChildFolders(folderId)
            if (hasChildren) {
                throw IllegalStateException("Folder with ID $folderId has child folders and cannot be deleted directly.")
            }
            hostRuleDataSource.clearFolderIdForRules(folderId)

            val deleted = folderDataSource.deleteFolder(folderId)
            if (!deleted) {
                Timber.w("Folder with ID $folderId not found during deletion attempt inside transaction.")
            }
        }.onFailure { Timber.e(it, "Failed to delete folder: ID='$folderId' inside transaction") }
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
    }.onFailure { Timber.e(it, "Failed to record browser usage for: $packageName") }


    override fun getBrowserStat(packageName: String): Flow<BrowserUsageStat?> {
        return dataSource.getBrowserStat(packageName).flowOn(ioDispatcher)
    }

    override fun getAllBrowserStats(): Flow<List<BrowserUsageStat>> {
        return dataSource.getAllBrowserStats().flowOn(ioDispatcher)
    }

    override fun getAllBrowserStatsSortedByLastUsed(): Flow<List<BrowserUsageStat>> {
        return dataSource.getAllBrowserStatsSortedByLastUsed().flowOn(ioDispatcher)
    }

    override suspend fun deleteBrowserStat(packageName: String): Result<Unit> = runCatching {
        if (packageName.isBlank()) throw IllegalArgumentException("Package name cannot be blank.")
        withContext(ioDispatcher) {
            val deleted = dataSource.deleteBrowserStat(packageName)
            if (!deleted) Timber.w("Browser stat for '$packageName' not found or delete failed.")
        }
    }.onFailure { Timber.e(it, "Failed to delete browser stat for: $packageName") }


    override suspend fun deleteAllStats(): Result<Unit> = runCatching {
        withContext(ioDispatcher) {
            dataSource.deleteAllStats()
        }
    }.onFailure { Timber.e(it, "Failed to delete all browser stats") }
}
