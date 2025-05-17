package browserpicker.data.local.repository

import androidx.room.withTransaction
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.results.AppError
import browserpicker.core.results.DomainResult
import browserpicker.core.results.catchUnexpected
import browserpicker.data.local.datasource.FolderLocalDataSource
import browserpicker.data.local.datasource.HostRuleLocalDataSource
import browserpicker.data.local.db.BrowserPickerDatabase
import browserpicker.data.local.entity.HostRuleEntity
import browserpicker.data.local.mapper.HostRuleMapper
import browserpicker.domain.model.FolderType
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriStatus
import browserpicker.domain.model.toFolderType
import browserpicker.domain.repository.HostRuleRepository
import browserpicker.domain.repository.SystemRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HostRuleRepositoryImpl @Inject constructor(
    private val hostRuleDataSource: HostRuleLocalDataSource,
    private val folderDataSource: FolderLocalDataSource,
    private val instantProvider: InstantProvider,
    private val browserPickerDatabase: BrowserPickerDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
): HostRuleRepository {
    override suspend fun getHostRuleByHost(host: String): DomainResult<HostRule?, AppError> = withContext(ioDispatcher) {
        try {
            val entity = hostRuleDataSource.getHostRuleByHost(host)
            val domainModel = entity?.let {
                runCatching { HostRuleMapper.toDomainModel(it) }
                    .onFailure { e -> Timber.e(e, "[Repository] Failed to map HostRuleEntity for host: %s", host) }
                    .getOrNull()
            }
            DomainResult.Success(domainModel)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Error fetching HostRule for host: %s", host)
            DomainResult.Failure(AppError.UnknownError("Failed to fetch host rule $host", e))
        }
    }

    override suspend fun getHostRuleById(id: Long): DomainResult<HostRule?, AppError> = withContext(ioDispatcher) {
        try {
            val entity = hostRuleDataSource.getHostRuleById(id)
            val rule = entity?.let {
                runCatching { HostRuleMapper.toDomainModel(it) }
                    .onFailure { e -> Timber.e(e, "[Repository] Failed to map HostRuleEntity for ID: %d", id) }
                    .getOrNull()
            }
            DomainResult.Success(rule)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to get HostRule by ID: %d", id)
            val appError = when (e) {
                is IllegalArgumentException -> AppError.DataIntegrityError("Data mapping error for host rule $id", e)
                else -> AppError.UnknownError("Failed to get host rule $id", e)
            }
            DomainResult.Failure(appError)
        }
    }

    override fun getAllHostRules(): Flow<DomainResult<List<HostRule>, AppError>> {
        return hostRuleDataSource.getAllHostRules()
            .map<List<HostRuleEntity>, DomainResult<List<HostRule>, AppError>> { entities ->
                val domainModels = entities.mapNotNull { entity ->
                    runCatching {
                        HostRuleMapper.toDomainModel(entity)
                    }.onFailure { e ->
                        Timber.e(e, "[Repository] Failed to map HostRuleEntity ${entity.id} to domain model, skipping.")
                    }.getOrNull()
                }
                DomainResult.Success(domainModels)
            }
            .catchUnexpected()
            .flowOn(ioDispatcher)
    }

    override fun getHostRulesByStatus(status: UriStatus): Flow<DomainResult<List<HostRule>, AppError>> {
        if (status == UriStatus.UNKNOWN) {
            Timber.w("[Repository] Requesting HostRules with UNKNOWN status, returning empty list.")
            return flowOf(DomainResult.Success(emptyList()))
        }
        return hostRuleDataSource.getHostRulesByStatus(status)
            .map<List<HostRuleEntity>, DomainResult<List<HostRule>, AppError>> { entities ->
                val domainModels = entities.mapNotNull { entity ->
                    runCatching {
                        HostRuleMapper.toDomainModel(entity)
                    }.onFailure { e ->
                        Timber.e(e, "[Repository] Failed to map HostRuleEntity ${entity.id} to domain model for status $status, skipping.")
                    }.getOrNull()
                }
                DomainResult.Success(domainModels)
            }
            .catchUnexpected()
            .flowOn(ioDispatcher)
    }

    override fun getHostRulesByFolder(folderId: Long): Flow<DomainResult<List<HostRule>, AppError>> {
        return hostRuleDataSource.getHostRulesByFolder(folderId)
            .map<List<HostRuleEntity>, DomainResult<List<HostRule>, AppError>> { entities ->
                val domainModels = entities.mapNotNull { entity ->
                    runCatching {
                        HostRuleMapper.toDomainModel(entity)
                    }.onFailure { e ->
                        Timber.e(e, "[Repository] Failed to map HostRuleEntity ${entity.id} to domain model for folder $folderId, skipping.")
                    }.getOrNull()
                }
                DomainResult.Success(domainModels)
            }
            .catchUnexpected()
            .flowOn(ioDispatcher)
    }

    override fun getRootHostRulesByStatus(status: UriStatus): Flow<DomainResult<List<HostRule>, AppError>> {
        if (status == UriStatus.UNKNOWN) {
            Timber.w("[Repository] Requesting Root HostRules with UNKNOWN status, returning empty list.")
            return flowOf(DomainResult.Success(emptyList()))
        }
        return hostRuleDataSource.getRootHostRulesByStatus(status)
            .map<List<HostRuleEntity>, DomainResult<List<HostRule>, AppError>> { entities ->
                val domainModels = entities.mapNotNull { entity ->
                    runCatching {
                        HostRuleMapper.toDomainModel(entity)
                    }.onFailure { e ->
                        Timber.e(e, "[Repository] Failed to map HostRuleEntity ${entity.id} to domain model for root status $status, skipping.")
                    }.getOrNull()
                }
                DomainResult.Success(domainModels)
            }
            .catchUnexpected()
            .flowOn(ioDispatcher)
    }

    override fun getDistinctRuleHosts(): Flow<DomainResult<List<String>, AppError>> {
        return hostRuleDataSource.getDistinctRuleHosts()
            .map<List<String>, DomainResult<List<String>, AppError>> { hosts -> DomainResult.Success(hosts) }
            .catchUnexpected()
            .flowOn(ioDispatcher)
    }

    override suspend fun saveHostRule(
        host: String,
        status: UriStatus,
        folderId: Long?,
        preferredBrowser: String?,
        isPreferenceEnabled: Boolean,
    ): DomainResult<Long, AppError> = browserPickerDatabase.withTransaction {
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
            val currentRuleEntity = hostRuleDataSource.getHostRuleByHost(trimmedHost)
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
            DomainResult.Success(ruleId)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed transaction to save host rule: host=$host, status=$status, folderId=$folderId, preferredBrowser=$preferredBrowser \n ${e.message}")
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message ?: "Invalid input data")
                is IllegalStateException -> AppError.DataIntegrityError(e.message ?: "Data integrity or state issue", e)
                else -> AppError.UnknownError("Failed to save host rule", e)
            }
            DomainResult.Failure(appError)
        }
    }

    override suspend fun deleteHostRuleById(id: Long): DomainResult<Unit, AppError> = withContext(ioDispatcher) {
        try {
            val deleted = hostRuleDataSource.deleteHostRuleById(id)
            if (deleted) {
                DomainResult.Success(Unit)
            } else {
                Timber.w("[Repository] Host rule with ID $id not found for deletion or delete failed. Reporting as success.")
                DomainResult.Success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to delete host rule by ID: $id")
            DomainResult.Failure(AppError.UnknownError("Failed to delete host rule $id", e))
        }
    }

    override suspend fun deleteHostRuleByHost(host: String): DomainResult<Unit, AppError> = withContext(ioDispatcher) {
        try {
            val trimmedHost = host.trim()
            if (trimmedHost.isEmpty()) throw IllegalArgumentException("Host cannot be blank for deletion.")
            val deleted = hostRuleDataSource.deleteHostRuleByHost(trimmedHost)
            if (deleted) {
                DomainResult.Success(Unit)
            } else {
                Timber.w("[Repository] Host rule for host '$trimmedHost' not found for deletion or delete failed. Reporting as success.")
                DomainResult.Success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to delete host rule by host: $host")
            val appError = when (e) {
                is IllegalArgumentException -> AppError.ValidationError(e.message ?: "Invalid input data")
                else -> AppError.UnknownError("Failed to delete host rule by host", e)
            }
            DomainResult.Failure(appError)
        }
    }

    override suspend fun clearFolderAssociation(folderId: Long): DomainResult<Unit, AppError> = withContext(ioDispatcher) {
        try {
            hostRuleDataSource.clearFolderIdForRules(folderId)
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "[Repository] Failed to clear folder association for folderId: $folderId")
            DomainResult.Failure(AppError.UnknownError("Failed to clear folder association for folder $folderId", e))
        }
    }

}
