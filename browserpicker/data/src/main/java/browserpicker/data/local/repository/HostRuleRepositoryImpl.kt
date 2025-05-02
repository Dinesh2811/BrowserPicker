package browserpicker.data.local.repository

import androidx.room.withTransaction
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.core.results.AppError
import browserpicker.core.results.MyResult
import browserpicker.data.local.datasource.FolderLocalDataSource
import browserpicker.data.local.datasource.HostRuleLocalDataSource
import browserpicker.data.local.db.BrowserPickerDatabase
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriStatus
import browserpicker.domain.model.toFolderType
import browserpicker.domain.repository.HostRuleRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
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
            val appError = when (e) {
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
                is IllegalArgumentException -> AppError.ValidationError(e.message ?: "Invalid input data", e)
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
                is IllegalArgumentException -> AppError.ValidationError(e.message ?: "Invalid input data", e)
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
