package browserpicker.data.local.datasource

import browserpicker.data.local.dao.HostRuleDao
import browserpicker.data.local.mapper.HostRuleMapper
import browserpicker.domain.model.HostRule
import browserpicker.domain.model.UriStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface HostRuleLocalDataSource {
    fun getHostRuleByHost(host: String): Flow<HostRule?>
    suspend fun getHostRuleById(id: Long): HostRule?
    suspend fun upsertHostRule(rule: HostRule): Long
    fun getAllHostRules(): Flow<List<HostRule>>
    fun getHostRulesByStatus(status: UriStatus): Flow<List<HostRule>>
    fun getHostRulesByFolder(folderId: Long): Flow<List<HostRule>>
    fun getRootHostRulesByStatus(status: UriStatus): Flow<List<HostRule>>
    suspend fun deleteHostRuleById(id: Long): Boolean
    suspend fun deleteHostRuleByHost(host: String): Boolean
    suspend fun clearFolderIdForRules(folderId: Long)
    fun getDistinctRuleHosts(): Flow<List<String>>
}

@Singleton
class HostRuleLocalDataSourceImpl @Inject constructor(
    private val hostRuleDao: HostRuleDao,
): HostRuleLocalDataSource {

    override fun getHostRuleByHost(host: String): Flow<HostRule?> {
        return hostRuleDao.getHostRuleByHost(host).map { entity ->
            entity?.let { HostRuleMapper.toDomainModel(it) }
        }
    }

    override suspend fun getHostRuleById(id: Long): HostRule? {
        return hostRuleDao.getHostRuleById(id)?.let { HostRuleMapper.toDomainModel(it) }
    }

    override suspend fun upsertHostRule(rule: HostRule): Long {

        // --- Start ---
        var ruleToSave = rule

        // Constraint 2 & 6: If uriStatus is NONE, folderId MUST be null.
        // Constraint 5: If uriStatus is BLOCKED, preferredBrowserPackage MUST be null and isPreferenceEnabled MUST be false.
        when (ruleToSave.uriStatus) {
            UriStatus.NONE -> {
                if (ruleToSave.folderId != null || ruleToSave.preferredBrowserPackage != null || ruleToSave.isPreferenceEnabled) {
                    ruleToSave = ruleToSave.copy(folderId = null, preferredBrowserPackage = null, isPreferenceEnabled = false)
                    println("Warning: HostRule with NONE status had folderId, preference, or enabled status set. Clearing them.")
                }
            }
            UriStatus.BLOCKED -> {
                if (ruleToSave.preferredBrowserPackage != null || ruleToSave.isPreferenceEnabled) {
                    ruleToSave = ruleToSave.copy(preferredBrowserPackage = null, isPreferenceEnabled = false)
                    println("Warning: HostRule with BLOCKED status had preference fields set. Clearing them.")
                }
                // Complex check: Ensure folderId (if not null) points to a BLOCK folder.
                // This validation is primarily handled in the Repository layer.
                // This DataSource assumes the Repository provided a valid folderId if one is set.
            }
            UriStatus.BOOKMARKED -> {
                // Complex check: Ensure folderId (if not null) points to a BOOKMARK folder.
                // This validation is primarily handled in the Repository layer.
                // This DataSource assumes the Repository provided a valid folderId if one is set.
            }
            UriStatus.UNKNOWN -> throw IllegalArgumentException("Cannot upsert HostRule with UNKNOWN uriStatus.")   // Constraint 1
        }
        // --- End ---

        return hostRuleDao.upsertHostRule(HostRuleMapper.toEntity(ruleToSave))
    }

    override fun getAllHostRules(): Flow<List<HostRule>> {
        return hostRuleDao.getAllHostRules().map { HostRuleMapper.toDomainModels(it) }
    }

    override fun getHostRulesByStatus(status: UriStatus): Flow<List<HostRule>> {
        return hostRuleDao.getHostRulesByStatus(status).map { HostRuleMapper.toDomainModels(it) }
    }

    override fun getHostRulesByFolder(folderId: Long): Flow<List<HostRule>> {
        return hostRuleDao.getHostRulesByFolderId(folderId).map { HostRuleMapper.toDomainModels(it) }
    }

    override fun getRootHostRulesByStatus(status: UriStatus): Flow<List<HostRule>> {
        return hostRuleDao.getRootHostRulesByStatus(status).map { HostRuleMapper.toDomainModels(it) }
    }

    override suspend fun deleteHostRuleById(id: Long): Boolean {
        return hostRuleDao.deleteHostRuleById(id) > 0
    }

    override suspend fun deleteHostRuleByHost(host: String): Boolean {
        return hostRuleDao.deleteHostRuleByHost(host) > 0
    }

    override suspend fun clearFolderIdForRules(folderId: Long) {
        hostRuleDao.clearFolderIdForRules(folderId)
    }

    override fun getDistinctRuleHosts(): Flow<List<String>> {
        return hostRuleDao.getDistinctRuleHosts()
    }
}
