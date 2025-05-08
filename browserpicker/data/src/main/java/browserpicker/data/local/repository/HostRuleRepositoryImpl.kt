package browserpicker.data.local.repository

import androidx.room.withTransaction
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.results.AppError
import browserpicker.core.results.MyResult
import browserpicker.data.local.datasource.FolderLocalDataSource
import browserpicker.data.local.datasource.HostRuleLocalDataSource
import browserpicker.data.local.db.BrowserPickerDatabase
import browserpicker.data.local.mapper.HostRuleMapper
import browserpicker.domain.model.FolderType
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriStatus
import browserpicker.domain.model.toFolderType
import browserpicker.domain.repository.HostRuleRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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

    override fun getHostRuleByHost(host: String): Flow<HostRule?> {
        return hostRuleDataSource.getHostRuleByHost(host)
            .map { entity ->
                entity?.let { ent ->
                    runCatching { HostRuleMapper.toDomainModel(ent) }
                        .onFailure { e -> Timber.e(e, "[Repository] Failed to map HostRuleEntity for host: %s", host) }
                        .getOrNull()
                }
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
            val rule = entity?.let {
                runCatching { HostRuleMapper.toDomainModel(it) }
                    .onFailure { e -> Timber.e(e, "[Repository] Failed to map HostRuleEntity for ID: %d", id) }
                    .getOrNull()
            }
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
                entities.mapNotNull { entity ->
                    runCatching {
                        HostRuleMapper.toDomainModel(entity)
                    }.onFailure { e ->
                        Timber.e(e, "[Repository] Failed to map HostRuleEntity ${entity.id} to domain model, skipping.")
                    }.getOrNull()
                }
            }
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching or processing all HostRules flow")
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
                entities.mapNotNull { entity ->
                    runCatching {
                        HostRuleMapper.toDomainModel(entity)
                    }.onFailure { e ->
                        Timber.e(e, "[Repository] Failed to map HostRuleEntity ${entity.id} to domain model for status $status, skipping.")
                    }.getOrNull()
                }
            }
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching or processing HostRules by status $status flow")
                emit(emptyList())
            }
            .flowOn(ioDispatcher)
    }

    override fun getHostRulesByFolder(folderId: Long): Flow<List<HostRule>> {
        return hostRuleDataSource.getHostRulesByFolder(folderId)
            .map { entities ->
                entities.mapNotNull { entity ->
                    runCatching {
                        HostRuleMapper.toDomainModel(entity)
                    }.onFailure { e ->
                        Timber.e(e, "[Repository] Failed to map HostRuleEntity ${entity.id} to domain model for folder $folderId, skipping.")
                    }.getOrNull()
                }
            }
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching or processing HostRules by folderId $folderId flow")
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
                entities.mapNotNull { entity ->
                    runCatching {
                        HostRuleMapper.toDomainModel(entity)
                    }.onFailure { e ->
                        Timber.e(e, "[Repository] Failed to map HostRuleEntity ${entity.id} to domain model for root status $status, skipping.")
                    }.getOrNull()
                }
            }
            .catch { e ->
                Timber.e(e, "[Repository] Error fetching or processing root HostRules by status $status flow")
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
