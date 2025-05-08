package browserpicker.data.local.repository

import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.filter
import androidx.paging.flatMap
import androidx.paging.map
import androidx.room.withTransaction
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.core.results.MyResult
import browserpicker.core.utils.logDebug
import browserpicker.core.utils.logError
import browserpicker.data.DataNotFoundException
import browserpicker.data.FolderNotEmptyException
import browserpicker.data.local.datasource.BrowserStatsLocalDataSource
import browserpicker.data.local.datasource.FolderLocalDataSource
import browserpicker.data.local.datasource.HostRuleLocalDataSource
import browserpicker.data.local.datasource.UriHistoryLocalDataSource
import browserpicker.data.local.db.BrowserPickerDatabase
import browserpicker.data.local.mapper.BrowserUsageStatMapper
import browserpicker.data.local.mapper.FolderMapper
import browserpicker.data.local.mapper.HostRuleMapper
import browserpicker.data.local.mapper.MappingException
import browserpicker.data.local.mapper.UriRecordMapper
import browserpicker.data.local.query.model.UriRecordQueryConfig
import browserpicker.domain.model.BrowserUsageStat
import browserpicker.domain.model.DateCount
import browserpicker.domain.model.Folder
import browserpicker.domain.model.FolderType
import browserpicker.domain.model.GroupCount
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.InteractionAction
import browserpicker.domain.model.UriRecord
import browserpicker.domain.model.UriSource
import browserpicker.domain.model.UriStatus
import browserpicker.domain.model.query.UriHistoryQuery
import browserpicker.domain.model.toFolderType
import browserpicker.domain.repository.BrowserStatsRepository
import browserpicker.domain.repository.FolderRepository
import browserpicker.domain.repository.HostRuleRepository
import browserpicker.domain.repository.UriHistoryRepository
import browserpicker.domain.service.UriParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
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
            dataSource.getPagedUriRecords(dataQueryConfig, pagingConfig)
                .map { pagingDataEntity ->
                    pagingDataEntity
//                        .filter { entity -> entity.uriSource != UriSource.UNKNOWN.value }
                        .flatMap { entity ->
                            runCatching {
                                listOf(UriRecordMapper.toDomainModel(entity))
                            }.onFailure {
                                Timber.e(it, "[Repository] Failed to map UriRecordEntity ${entity.id} to domain model, skipping.")
                            }.getOrElse { emptyList() }
                        }
                }
                .catch {
                    Timber.e(it, "[Repository] Error fetching paged URI records for query: %s", query)
                    emit(PagingData.empty())
                }
                .filterNotNull()
                .flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create PagedUriRecords Flow for query: %s", query)
            flowOf(PagingData.empty<UriRecord>()).flowOn(ioDispatcher)
        }
    }

    override fun getTotalUriRecordCount(query: UriHistoryQuery): Flow<DomainResult<Long, AppError>> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getTotalUriRecordCount(dataQueryConfig)
                .map { count ->
                    DomainResult.Success(count) as DomainResult<Long, AppError>
                }
                .catch { e ->
                    Timber.e(e, "[Repository] Error fetching total URI record count for query: %s", query)
                    emit(DomainResult.Failure(AppError.DatabaseError(e.message ?: "Database error fetching total count", e)))
                }
                .flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create TotalUriRecordCount Flow for query: %s", query)
            flowOf(DomainResult.Failure(AppError.UnknownError("Failed to create TotalUriRecordCount Flow", e))).flowOn(ioDispatcher)
        }
    }

    override fun getGroupCounts(query: UriHistoryQuery): Flow<DomainResult<List<GroupCount>, AppError>> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getGroupCounts(dataQueryConfig)
                .map { counts ->
                    DomainResult.Success(counts) as DomainResult<List<GroupCount>, AppError>
                }
                .catch { e ->
                    Timber.e(e, "[Repository] Error fetching group counts for query: %s", query)
                    emit(DomainResult.Failure(AppError.DatabaseError(e.message ?: "Database error fetching group counts", e)))
                }
                .flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create GroupCounts Flow for query: %s", query)
            flowOf(DomainResult.Failure(AppError.UnknownError("Failed to create GroupCounts Flow", e))).flowOn(ioDispatcher)
        }
    }

    override fun getDateCounts(query: UriHistoryQuery): Flow<DomainResult<List<DateCount>, AppError>> {
        return try {
            val dataQueryConfig = mapQueryToConfig(query)
            dataSource.getDateCounts(dataQueryConfig)
                .map { counts ->
                    DomainResult.Success(counts) as DomainResult<List<DateCount>, AppError>
                }
                .catch { e ->
                    Timber.e(e, "[Repository] Error fetching date counts for query: %s", query)
                    emit(DomainResult.Failure(AppError.DatabaseError(e.message ?: "Database error fetching date counts", e)))
                }
                .flowOn(ioDispatcher)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to create DateCounts Flow for query: %s", query)
            flowOf(DomainResult.Failure(AppError.UnknownError("Failed to create DateCounts Flow", e))).flowOn(ioDispatcher)
        }
    }
    private fun validateAddUriRecordInput(
        uriString: String,
        host: String,
        source: UriSource,
        action: InteractionAction,
        chosenBrowser: String?
    ): AppError.ValidationError? {
        return when {
            uriString.isBlank() -> AppError.ValidationError("URI string cannot be blank or empty")
            host.isBlank() -> AppError.ValidationError("Host cannot be blank or empty")
            source == UriSource.UNKNOWN -> AppError.ValidationError("URI Source cannot be UNKNOWN; use a valid source type")
            action == InteractionAction.UNKNOWN -> AppError.ValidationError("Interaction Action cannot be UNKNOWN; use a valid action type")
            chosenBrowser != null && chosenBrowser.isBlank() -> AppError.ValidationError("Chosen browser package name cannot be blank if provided.")
            else -> null
        }
    }

    override suspend fun addUriRecord(
        uriString: String,
        host: String,
        source: UriSource,
        action: InteractionAction,
        chosenBrowser: String?,
        associatedHostRuleId: Long?,
    ): DomainResult<Long, AppError> = withContext(ioDispatcher) {
        try {
            validateAddUriRecordInput(uriString, host, source, action, chosenBrowser)?.let { validationError ->
                Timber.e("[Repository] Failed to add URI record due to validation error: ${validationError.message}")
                return@withContext DomainResult.Failure(validationError)
            }

            val parsedUriResult = uriParser.parseAndValidateWebUri(uriString)
            if (parsedUriResult is DomainResult.Failure) {
                Timber.e(parsedUriResult.error.cause, "[Repository] URI parsing and validation failed for $uriString: ${parsedUriResult.error.message}")
                return@withContext DomainResult.Failure(parsedUriResult.error)
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

            val entity = UriRecordMapper.toEntity(record)
            val id = dataSource.insertUriRecord(entity)
            if (id <= 0) {
                Timber.e("[Repository] Failed to insert URI record: received invalid ID $id for $uriString")
                return@withContext DomainResult.Failure(AppError.DataIntegrityError("Failed to insert URI record: received invalid ID $id"))
            }

            DomainResult.Success(id)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] An unexpected error occurred during URI record addition process for $uriString")
            DomainResult.Failure(AppError.UnknownError("An unexpected error occurred while adding URI record.", e))
        }
    }

    override suspend fun getUriRecord(id: Long): DomainResult<UriRecord?, AppError> = withContext(ioDispatcher) {
        try {
            val entity = dataSource.getUriRecord(id)
//            val record = entity?.let { UriRecordMapper.toDomainModel(it) }
            val record = entity?.let { recordEntity ->
                runCatching {
                    UriRecordMapper.toDomainModel(recordEntity)
                }.onFailure {
                    Timber.e(it, "[Repository] Failed to map UriRecordEntity ${recordEntity.id} to domain model during getUriRecord.")
                }.getOrNull()
            }
            DomainResult.Success(record)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to get URI record with id: %d", id)
            val appError = when (e) {
                is IllegalArgumentException -> AppError.DataIntegrityError("Data mapping error for record $id", e)
                is MappingException -> AppError.DataIntegrityError("Data mapping error for record $id", e)
                is DataNotFoundException -> AppError.DataNotFound("URI record with id $id not found", e)
                else -> AppError.UnknownError("An unexpected error occurred while getting URI record $id.", e)
            }
            DomainResult.Failure(appError)
        }
    }

    override suspend fun deleteUriRecord(id: Long): DomainResult<Unit, AppError> = withContext(ioDispatcher) {
        try {
            val deleted = dataSource.deleteUriRecord(id)
            if (deleted > 0) {
                DomainResult.Success(Unit)
            } else {
                Timber.w("[Repository] URI record with id: $id not found for deletion or delete failed in data source. Reporting as success (item not present).")
                DomainResult.Success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to delete URI record with id: %d", id)
            val appError = AppError.DatabaseError(e.message ?: "Database error deleting URI record $id", e)
            DomainResult.Failure(appError)
        }
    }

    override suspend fun deleteAllUriRecords(): DomainResult<Int, AppError> = withContext(ioDispatcher) {
        try {
            val count = dataSource.deleteAllUriRecords()
            DomainResult.Success(count)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to delete all URI records")
            val appError = AppError.DatabaseError(e.message ?: "Database error deleting all URI records", e)
            DomainResult.Failure(appError)
        }
    }

    override fun getDistinctHosts(): Flow<DomainResult<List<String>, AppError>> {
        return dataSource.getDistinctHosts()
            .map { hosts ->
                DomainResult.Success(hosts) as DomainResult<List<String>, AppError>
            }
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching distinct hosts")
                emit(DomainResult.Failure(AppError.DatabaseError(e.message ?: "Database error fetching distinct hosts", e)))
            }
            .flowOn(ioDispatcher)
    }

    override fun getDistinctChosenBrowsers(): Flow<DomainResult<List<String?>, AppError>> {
        return dataSource.getDistinctChosenBrowsers()
            .map { browsers ->
                DomainResult.Success(browsers) as DomainResult<List<String?>, AppError>
            }
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching distinct chosen browsers")
                emit(DomainResult.Failure(AppError.DatabaseError(e.message ?: "Database error fetching distinct chosen browsers", e)))
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
            .map { entity ->
                entity?.let { HostRuleMapper.toDomainModel(it) }
            }
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching HostRule for host: %s", host)
                emit(null)
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getHostRuleById(id: Long): MyResult<HostRule?, AppError> = withContext(ioDispatcher) {
        try {
            val entity = hostRuleDataSource.getHostRuleById(id)
            val rule = entity?.let { HostRuleMapper.toDomainModel(it) }
            MyResult.Success(rule)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to get HostRule by ID: %d", id)
            val appError = when (e) {
                is IllegalArgumentException -> AppError.DataIntegrityError("Data mapping error for host rule $id", e)
                else -> AppError.UnknownError("Failed to get host rule $id", e)
            }
            MyResult.Error(appError)
        }
    }

    override fun getAllHostRules(): Flow<List<HostRule>> {
        return hostRuleDataSource.getAllHostRules()
            .map { entities ->
                HostRuleMapper.toDomainModels(entities)
            }
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching all HostRules")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getHostRulesByStatus(status: UriStatus): Flow<List<HostRule>> {
        if (status == UriStatus.UNKNOWN) {
            Timber.w("[Repository] Requesting HostRules with UNKNOWN status, returning empty list.")
            return flowOf(emptyList())
        }
        return hostRuleDataSource.getHostRulesByStatus(status)
            .map { entities ->
                HostRuleMapper.toDomainModels(entities)
            }
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching HostRules by status: %s", status)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getHostRulesByFolder(folderId: Long): Flow<List<HostRule>> {
        return hostRuleDataSource.getHostRulesByFolder(folderId)
            .map { entities ->
                HostRuleMapper.toDomainModels(entities)
            }
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching HostRules by folderId: %d", folderId)
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getRootHostRulesByStatus(status: UriStatus): Flow<List<HostRule>> {
        if (status == UriStatus.UNKNOWN) {
            Timber.w("[Repository] Requesting Root HostRules with UNKNOWN status, returning empty list.")
            return flowOf(emptyList())
        }
        return hostRuleDataSource.getRootHostRulesByStatus(status)
            .map { entities ->
                HostRuleMapper.toDomainModels(entities)
            }
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
            val currentRuleEntity = hostRuleDataSource.getHostRuleByHost(trimmedHost).firstOrNull()
            val currentRuleDomain = currentRuleEntity?.let { HostRuleMapper.toDomainModel(it) }

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
                val folderEntity = folderDataSource.getFolderByIdSuspend(effectiveFolderId)
                if (folderEntity == null) {
                    throw IllegalStateException("Folder with ID $effectiveFolderId does not exist.")
                }
                val expectedFolderType = status.toFolderType()
                val actualFolderType = FolderType.fromValue(folderEntity.folderType)

                if (expectedFolderType == null) {
                    Timber.w("Rule status $status unexpectedly requires a folder check but has no matching FolderType. Clearing folder ID.")
                    effectiveFolderId = null
                } else if (actualFolderType != expectedFolderType) {
                    throw IllegalArgumentException("Folder type mismatch: Rule status ($status) requires folder type $expectedFolderType, but folder $effectiveFolderId has type $actualFolderType.")
                }
            } else if (status != UriStatus.NONE) {
                effectiveFolderId = null
            }

            val ruleToSave = HostRule(
                id = currentRuleDomain?.id ?: 0,
                host = trimmedHost,
                uriStatus = status,
                folderId = effectiveFolderId,
                preferredBrowserPackage = effectivePreferredBrowser,
                isPreferenceEnabled = effectiveIsPreferenceEnabled,
                createdAt = currentRuleDomain?.createdAt ?: now,
                updatedAt = now
            )

            val entityToSave = HostRuleMapper.toEntity(ruleToSave)
            val ruleId = hostRuleDataSource.upsertHostRule(entityToSave)
            MyResult.Success(ruleId)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed transaction to save host rule: host=$host, status=$status, folderId=$folderId, preferredBrowser=$preferredBrowser \n ${e.message}")
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message ?: "Invalid input data")
                is IllegalStateException -> AppError.DataIntegrityError(e.message ?: "Data integrity or state issue", e)
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
                is IllegalArgumentException -> AppError.ValidationError(e.message ?: "Invalid input data")
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
                is IllegalArgumentException -> AppError.ValidationError(e.message ?: "Invalid input data")
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
                is IllegalArgumentException -> AppError.ValidationError(e.message ?: "Invalid input data")
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
                throw DataNotFoundException("Folder with ID $folderId not found for deletion or delete failed.")
            }
            MyResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed transaction to delete folder: ID='$folderId'")
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message ?: "Invalid input data")
                is FolderNotEmptyException -> AppError.FolderNotEmptyError(folderId, e.message, e)
                is DataNotFoundException -> AppError.DataNotFound(e.message, e)
                is IllegalStateException -> AppError.DataIntegrityError(e.message ?: "Unexpected state issue during deletion", e)
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
                is IllegalArgumentException -> AppError.ValidationError(e.message ?: "Invalid input data")
                else -> AppError.UnknownError("Failed to record browser usage", e)
            }
            MyResult.Error(appError)
        }
    }

    override fun getBrowserStat(packageName: String): Flow<BrowserUsageStat?> {
        return dataSource.getBrowserStat(packageName)
            .map { entity ->
                entity?.let { BrowserUsageStatMapper.toDomainModel(it) }
            }
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching browser stat for: %s", packageName)
                emit(null)
            }
            .flowOn(ioDispatcher)
    }

    override fun getAllBrowserStats(): Flow<List<BrowserUsageStat>> {
        return dataSource.getAllBrowserStats()
            .map { entities ->
                BrowserUsageStatMapper.toDomainModels(entities)
            }
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching all browser stats")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getAllBrowserStatsSortedByLastUsed(): Flow<List<BrowserUsageStat>> {
        return dataSource.getAllBrowserStatsSortedByLastUsed()
            .map { entities ->
                BrowserUsageStatMapper.toDomainModels(entities)
            }
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
                is IllegalArgumentException -> AppError.ValidationError(e.message ?: "Invalid input data")
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
