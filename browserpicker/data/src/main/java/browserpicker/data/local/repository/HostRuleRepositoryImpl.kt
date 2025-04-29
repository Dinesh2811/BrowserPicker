package browserpicker.data.local.repository

import androidx.room.Transaction
import androidx.room.withTransaction
import browserpicker.core.di.InstantProvider
import browserpicker.core.di.IoDispatcher
import browserpicker.data.local.datasource.FolderLocalDataSource
import browserpicker.data.local.datasource.HostRuleLocalDataSource
import browserpicker.data.local.db.BrowserPickerDatabase
import browserpicker.domain.model.FolderType
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriStatus
import browserpicker.domain.repository.HostRuleRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
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